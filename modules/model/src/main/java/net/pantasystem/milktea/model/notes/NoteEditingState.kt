package net.pantasystem.milktea.model.notes


import kotlinx.datetime.Instant
import net.pantasystem.milktea.model.account.Account
import net.pantasystem.milktea.model.channel.Channel
import net.pantasystem.milktea.model.file.AppFile
import net.pantasystem.milktea.model.notes.poll.CreatePoll
import java.util.*

data class AddMentionResult(
    val cursorPos: Int,
    val state: NoteEditingState
)

/**
 * @param textCursorPos 次のカーソルの位置を明示的に示したい時に使用します。平常時はNullを指定します。
 */
data class NoteEditingState(
    val author: Account? = null,
    val visibility: Visibility = Visibility.Public(false),
    val text: String? = null,
    val textCursorPos: Int? = null,
    val cw: String? = null,
    val replyId: Note.Id? = null,
    val renoteId: Note.Id? = null,
    val files: List<AppFile> = emptyList(),
    val poll: PollEditingState? = null,
    val viaMobile: Boolean = true,
    val draftNoteId: Long? = null,
    val reservationPostingAt: Instant? = null,
    val channelId: Channel.Id? = null,
) {

    private val hasCw: Boolean
        get() = cw != null



    fun changeRenoteId(renoteId: Note.Id?): NoteEditingState {
        return copy(
            renoteId = renoteId
        )
    }



    fun checkValidate(textMaxLength: Int = 3000, maxFileCount: Int = 4): Boolean {
        if (this.files.size > maxFileCount) {
            return false
        }

        if ((this.text?.codePointCount(0, this.text.length) ?: 0) > textMaxLength) {
            return false
        }

        if (channelId != null && visibility != Visibility.Public(true)) {
            return false
        }

        if (this.renoteId != null) {
            return true
        }

        if (this.poll != null && this.poll.checkValidate()) {
            return true
        }

        return !(
                this.text.isNullOrBlank()
                        && this.files.isEmpty()
                )
    }

    fun changeText(text: String): NoteEditingState {
        return this.copy(
            text = text,
            textCursorPos = null,
        )
    }

    fun addMentionUserNames(userNames: List<String>, pos: Int): AddMentionResult {
        val mentionBuilder = StringBuilder()
        userNames.forEachIndexed { index, userName ->
            if (index < userNames.size - 1) {
                // NOTE: 次の文字がつながらないようにする
                mentionBuilder.appendLine("$userName ")
            } else {
                // NOTE: 次の文字がつながらないようにする
                mentionBuilder.append("$userName ")
            }
        }
        val builder = StringBuilder(text ?: "")
        builder.insert(pos, mentionBuilder.toString())
        val nextPos = pos + mentionBuilder.length
        return AddMentionResult(nextPos, copy(text = builder.toString(), textCursorPos = nextPos))
    }

    fun changeCw(text: String?): NoteEditingState {
        return this.copy(
            cw = text
        )
    }

    fun addFile(file: AppFile): NoteEditingState {
        return this.copy(
            files = this.files.toMutableList().apply {
                add(file)
            }
        )
    }

    fun removeFile(file: AppFile): NoteEditingState {
        return this.copy(
            files = this.files.removeFile(file)
        )
    }



    fun setAccount(account: Account?): NoteEditingState {
        if (author == null) {
            return this.copy(
                author = account
            )
        }
        if (account == null) {
            throw IllegalArgumentException("現在の状態に未指定のAccountを指定することはできません")
        }

        if (replyId != null) {
            if (replyId.accountId != account.accountId && author.instanceDomain != account.instanceDomain) {
                throw IllegalArgumentException("異なるインスタンスドメインのアカウントを切り替えることはできません(replyId)。")
            }
        }

        if (renoteId != null) {
            if (renoteId.accountId != account.accountId && author.instanceDomain != account.instanceDomain) {
                throw IllegalArgumentException("異なるインスタンスドメインのアカウントを切り替えることはできません(renoteId)。")
            }
        }


        if (visibility is Visibility.Specified
            && (visibility.visibleUserIds.isNotEmpty()
                    || author.instanceDomain == account.instanceDomain)
        ) {
            if (!visibility.visibleUserIds.all { it.accountId == account.accountId }) {
                throw IllegalArgumentException("異なるインスタンスドメインのアカウントを切り替えることはできません(visibility)。")
            }
        }

        return this.copy(
            author = account,
            files = files,
            replyId = replyId?.copy(accountId = account.accountId),
            renoteId = renoteId?.copy(accountId = account.accountId),
            visibility = if (visibility is Visibility.Specified) {
                visibility.copy(visibleUserIds = visibility.visibleUserIds.map {
                    it.copy(accountId = account.accountId)
                })
            } else {
                visibility
            }
        )

    }

    fun removePollChoice(id: UUID): NoteEditingState {
        return this.copy(
            poll = this.poll?.let {
                it.copy(
                    choices = it.choices.filterNot { choice ->
                        choice.id == id
                    }
                )
            }
        )
    }

    fun addPollChoice(): NoteEditingState {
        return this.copy(
            poll = this.poll?.let {
                it.copy(
                    choices = it.choices.toMutableList().also { list ->
                        list.add(
                            PollChoiceState("")
                        )
                    }
                )
            }
        )
    }

    fun updatePollChoice(id: UUID, text: String): NoteEditingState {
        return this.copy(
            poll = this.poll?.let {
                it.copy(
                    choices = it.choices.map { choice ->
                        if (choice.id == id) {
                            choice.copy(
                                text = text
                            )
                        } else {
                            choice
                        }
                    }
                )
            }
        )
    }

    fun toggleCw(): NoteEditingState {
        return this.copy(
            cw = if (this.hasCw) null else ""
        )
    }

    fun togglePoll(): NoteEditingState {
        return this.copy(
            poll = if (poll == null) PollEditingState(emptyList(), false) else null
        )
    }

    fun clear(): NoteEditingState {
        return NoteEditingState(author = this.author)
    }

    fun toggleFileSensitiveStatus(appFile: AppFile.Local): NoteEditingState {
        return copy(
            files = files.toggleFileSensitiveStatus(appFile)
        )
    }

    fun setChannelId(channelId: Channel.Id?): NoteEditingState {
        return copy(
            visibility = if (channelId == null) visibility else Visibility.Public(true),
            channelId = channelId
        )
    }

    fun setVisibility(visibility: Visibility): NoteEditingState {
        if (channelId == null) {
            return copy(
                visibility = visibility
            )
        }

        if (visibility != Visibility.Public(true)) {
            return copy(
                visibility = Visibility.Public(true)
            )
        }

        return this
    }

}

sealed interface PollExpiresAt : java.io.Serializable {
    object Infinity : PollExpiresAt
    data class DateAndTime(val expiresAt: Date) : PollExpiresAt {
        constructor(expiresAt: Instant) : this(Date(expiresAt.toEpochMilliseconds()))
    }

    fun asDate(): Date? {
        return this.expiresAt()?.toEpochMilliseconds()?.let {
            Date(it)
        }
    }
}

fun PollExpiresAt.expiresAt(): Instant? {
    return when (this) {
        is PollExpiresAt.Infinity -> null
        is PollExpiresAt.DateAndTime -> Instant.fromEpochMilliseconds(this.expiresAt.time)
    }
}

data class PollEditingState(
    val choices: List<PollChoiceState>,
    val multiple: Boolean,
    val expiresAt: PollExpiresAt = PollExpiresAt.Infinity
) : java.io.Serializable {

    val isExpiresAtDateTime: Boolean
        get() = expiresAt is PollExpiresAt.DateAndTime

    fun checkValidate(): Boolean {
        return choices.all {
            it.text.isNotBlank()
        } && this.choices.size >= 2
    }

    fun toggleMultiple(): PollEditingState {
        return this.copy(
            multiple = !this.multiple
        )
    }
}

data class PollChoiceState(
    val text: String,
    val id: UUID = UUID.randomUUID()
) : java.io.Serializable

fun PollEditingState.toCreatePoll(): CreatePoll {
    return CreatePoll(
        choices = this.choices.map {
            it.text
        },
        multiple = multiple,
        expiresAt = expiresAt.expiresAt()?.toEpochMilliseconds()
    )
}




fun List<AppFile>.toggleFileSensitiveStatus(appFile: AppFile.Local): List<AppFile> {
    return this.map {
        if (it === appFile || it is AppFile.Local && it.isAttributeSame(appFile)) {
            appFile.copy(isSensitive = !appFile.isSensitive)
        } else {
            it
        }
    }
}

fun String?.addMentionUserNames(userNames: List<String>, pos: Int): Pair<String?, Int> {
    val mentionBuilder = StringBuilder()
    userNames.forEachIndexed { index, userName ->
        if (index < userNames.size - 1) {
            // NOTE: 次の文字がつながらないようにする
            mentionBuilder.appendLine("$userName ")
        } else {
            // NOTE: 次の文字がつながらないようにする
            mentionBuilder.append("$userName ")
        }
    }
    val builder = StringBuilder(this ?: "")
    builder.insert(pos, mentionBuilder.toString())
    val nextPos = pos + mentionBuilder.length
    return builder.toString() to nextPos
}

fun List<AppFile>.removeFile(appFile: AppFile): List<AppFile> {
    return toMutableList().apply {
        remove(appFile)
    }
}


fun PollEditingState?.removePollChoice(id: UUID): PollEditingState? {
    return this?.copy(
        choices = this.choices.filterNot { choice ->
            choice.id == id
        }
    )
}

fun PollEditingState?.updatePollChoice(id: UUID, text: String): PollEditingState? {
    return this?.copy(
        choices = choices.map { choice ->
            if (choice.id == id) {
                choice.copy(
                    text = text
                )
            } else {
                choice
            }
        }
    )
}

fun PollEditingState?.addPollChoice(): PollEditingState? {
    return this?.copy(
        choices = choices.toMutableList().also { list ->
            list.add(
                PollChoiceState("")
            )
        }
    )
}