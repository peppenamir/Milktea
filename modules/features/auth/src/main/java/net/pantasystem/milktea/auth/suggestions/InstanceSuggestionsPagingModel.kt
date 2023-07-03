package net.pantasystem.milktea.auth.suggestions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.pantasystem.milktea.api.misskey.InstanceInfoAPIBuilder
import net.pantasystem.milktea.api.misskey.infos.InstanceInfosResponse
import net.pantasystem.milktea.common.PageableState
import net.pantasystem.milktea.common.paginator.EntityConverter
import net.pantasystem.milktea.common.paginator.PaginationState
import net.pantasystem.milktea.common.paginator.PreviousLoader
import net.pantasystem.milktea.common.paginator.PreviousPagingController
import net.pantasystem.milktea.common.paginator.StateLocker
import net.pantasystem.milktea.common.runCancellableCatching
import net.pantasystem.milktea.common.throwIfHasError
import javax.inject.Inject

class InstanceSuggestionsPagingModel @Inject constructor(
    private val instancesInfoAPIBuilder: InstanceInfoAPIBuilder,
) : StateLocker,
    PaginationState<InstanceInfosResponse.InstanceInfo>,
    PreviousLoader<InstanceInfosResponse.InstanceInfo>,
    EntityConverter<InstanceInfosResponse.InstanceInfo, InstanceInfosResponse.InstanceInfo> {

    private var _offset = 0
    private var _name: String = ""
    private val _state =
        MutableStateFlow<PageableState<List<InstanceInfosResponse.InstanceInfo>>>(PageableState.Loading.Init())

    private var _job: Job? = null

    override suspend fun convertAll(list: List<InstanceInfosResponse.InstanceInfo>): List<InstanceInfosResponse.InstanceInfo> {
        return list
    }

    override val state: Flow<PageableState<List<InstanceInfosResponse.InstanceInfo>>>
        get() = _state

    override fun getState(): PageableState<List<InstanceInfosResponse.InstanceInfo>> {
        return _state.value
    }

    override fun setState(state: PageableState<List<InstanceInfosResponse.InstanceInfo>>) {
        _state.value = state
    }

    override suspend fun loadPrevious(): Result<List<InstanceInfosResponse.InstanceInfo>> =
        runCancellableCatching {
            instancesInfoAPIBuilder.build().getInstances(
                offset = _offset,
                name = _name,
            ).throwIfHasError().body()!!.also {
                _offset += it.size
            }
        }

    suspend fun setQueryName(name: String) {
        _job?.cancel()
        mutex.withLock {
            _name = name

        }
        setState(PageableState.Loading.Init())
    }

    override val mutex: Mutex = Mutex()

    private val previousPagingController = PreviousPagingController(
        this,
        this,
        this,
        this
    )

    fun onLoadNext(scope: CoroutineScope) {
        _job?.cancel()
        _job = scope.launch {
            previousPagingController.loadPrevious()
        }

    }
}