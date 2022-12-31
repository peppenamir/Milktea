package net.pantasystem.milktea.data.infrastructure.notes

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import net.pantasystem.milktea.api_streaming.NoteUpdated
import net.pantasystem.milktea.common.Logger
import net.pantasystem.milktea.model.account.Account
import net.pantasystem.milktea.model.account.AccountRepository
import net.pantasystem.milktea.model.notes.Note
import net.pantasystem.milktea.model.notes.NoteCaptureAPIAdapter
import net.pantasystem.milktea.model.notes.NoteDataSource

/**
 * Noteの更新イベントをWebSocket経由でキャプチャーして
 * その更新イベントをキャッシュに反映するための実装
 */
class NoteCaptureAPIAdapterImpl(
    private val accountRepository: AccountRepository,
    private val noteDataSource: NoteDataSource,
    private val noteCaptureAPIWithAccountProvider: NoteCaptureAPIWithAccountProvider,
    loggerFactory: Logger.Factory,
    cs: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : NoteDataSource.Listener, NoteCaptureAPIAdapter {

    private val logger = loggerFactory.create("NoteCaptureAPIAdapter")

    private val coroutineScope = CoroutineScope(cs.coroutineContext + dispatcher)


    init {
        noteDataSource.addEventListener(this)
    }

    private val noteIdWithListeners =
        mutableMapOf<Note.Id, MutableSet<(NoteDataSource.Event) -> Unit>>()

    private val noteIdWithJob = mutableMapOf<Note.Id, Job>()

    /**
     * Noteのキャプチャーによって発生したイベントのQueue。
     * ここから順番にイベントを取り出し、キャッシュに反映させるなどをしている。
     */
    private val noteUpdatedDispatcher = MutableSharedFlow<Pair<Account, NoteUpdated.Body>>()

//    /**
//     * 使用されなくなったNoteのリソースが順番に入れられるQueue。
//     */
//    private val noteResourceReleaseEvent = MutableSharedFlow<Note.Id>(extraBufferCapacity = 1000)

    init {
        // NOTE: Noteのキャプチャーによって発生したイベントのQueueであるnoteUpdatedDispatcherのイベントを順番にキャッシュに反映させている。
        coroutineScope.launch(dispatcher) {
            noteUpdatedDispatcher.collect {
                handleRemoteEvent(it.first, it.second)
            }
        }

//        // NOTE: Noteのキャプチャーが一切行われなくなった場合キャッシュ上からも削除している。
//        coroutineScope.launch(dispatcher) {
//            noteResourceReleaseEvent.filterNot {
//                isCaptured(it)
//            }.collect {
//                noteDataSource.remove(it)
//            }
//        }
    }

    override fun on(e: NoteDataSource.Event) {

        synchronized(noteIdWithListeners) {
            noteIdWithListeners[e.noteId]?.forEach { callback ->
                coroutineScope.launch {
                    callback.invoke(e)
                }
            }

        }

    }

    /**
     * ノートをキャプチャーするための実装。
     * Flowでイベントを返却し、Flowが使用されなくなると、自動的にListenerが解除される仕組みになっている。
     * また対象のNoteが他のどこからも購読されなくなくなれば、自動的にサーバからの購読を解除する仕組みになっている。
     * また自動的に購読を解除する際はキャッシュ上からもリソースを削除するようにしている。
     * @param id キャプチャーするNoteのId
     * @return channelFlowが返却される。Flowが使用されなくなりある一定の条件が満たされれば購読が解除される。
     */
    override fun capture(id: Note.Id): Flow<NoteDataSource.Event> = channelFlow {
        val account = accountRepository.get(id.accountId).getOrThrow()

        val repositoryEventListener: (NoteDataSource.Event) -> Unit = { ev ->
            trySend(ev)
        }

        synchronized(noteIdWithJob) {
            if (addRepositoryEventListener(id, repositoryEventListener)) {
                logger.debug("未登録だったのでRemoteに対して購読を開始する")
                val job = noteCaptureAPIWithAccountProvider.get(account)
                    .capture(id.noteId)
                    .catch { e ->
                        logger.error("ノート更新イベント受信中にエラー発生", e = e)
                    }
                    .onEach {
                        noteUpdatedDispatcher.emit(account to it)
                    }.launchIn(coroutineScope)
                noteIdWithJob[id] = job
            }
        }

        awaitClose {
            // NoteCaptureの購読を解除する
            synchronized(noteIdWithJob) {
                // リスナーを解除する
                removeRepositoryEventListener(id, repositoryEventListener).also { result ->
                    if (result) {

                        // すべてのリスナーが解除されていればRemoteへの購読も解除する
                        noteIdWithJob.remove(id)?.cancel() ?: run {
                            logger.warning("購読解除しようとしたところすでに解除されていた")
                        }
                    }
                }

            }

//            // NOTE: Noteのキャプチャーが一切行われなくなった場合キャッシュ上からも削除している。
//            if (result) {
//                noteResourceReleaseEvent.tryEmit(id)
//            }
        }
    }.shareIn(coroutineScope, replay = 1, started = SharingStarted.WhileSubscribed())


    /**
     * @return Note.Idが初めてListenerに登録されるとtrueが返されます。
     */
    private fun addRepositoryEventListener(
        noteId: Note.Id,
        listener: (NoteDataSource.Event) -> Unit
    ): Boolean {
        synchronized(noteIdWithListeners) {
            val listeners = noteIdWithListeners[noteId]
            return if (listeners.isNullOrEmpty()) {
                noteIdWithListeners[noteId] = mutableSetOf(listener)
                true
            } else {
                listeners.add(listener)
                noteIdWithListeners[noteId] = listeners
                false
            }
        }

    }

    /**
     * @return Note.Idに関連するListenerすべてが解除されるとfalseが返されます。
     */
    private fun removeRepositoryEventListener(
        noteId: Note.Id,
        listener: (NoteDataSource.Event) -> Unit
    ): Boolean {

        synchronized(noteIdWithListeners) {
            val listeners: MutableSet<(NoteDataSource.Event) -> Unit> =
                noteIdWithListeners[noteId] ?: return false

            if (!listeners.remove(listener)) {
                logger.warning("リスナーの削除に失敗しました。")
                return false
            }

            if (listeners.isEmpty()) {
                return true
            }
            return false
        }

    }

    /**
     * イベントに応じてキャッシュを更新している。
     */
    private suspend fun handleRemoteEvent(account: Account, e: NoteUpdated.Body) {
        val noteId = Note.Id(account.accountId, e.id)
        try {
            val note = noteDataSource.get(noteId).getOrThrow()
            when (e) {
                is NoteUpdated.Body.Deleted -> {
                    noteDataSource.delete(noteId)
                }
                is NoteUpdated.Body.Reacted -> {
                    noteDataSource.add(note.onReacted(account, e))
                }
                is NoteUpdated.Body.Unreacted -> {
                    noteDataSource.add(note.onUnReacted(account, e))
                }
                is NoteUpdated.Body.PollVoted -> {
                    noteDataSource.add(note.onPollVoted(account, e))
                }

            }
        } catch (e: Exception) {
            logger.warning("更新対称のノートが存在しませんでした:$noteId", e = e)
        }


    }



}