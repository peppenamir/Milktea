package jp.panta.misskeyandroidclient.viewmodel.notes

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import jp.panta.misskeyandroidclient.model.notes.LiveNotePagingStore
import jp.panta.misskeyandroidclient.model.notes.TimelineRequest
import java.lang.IllegalArgumentException

class TimelineViewModelFactory(private val type: TimelineViewModel.Type, private val baseTimelineRequest: TimelineRequest) : ViewModelProvider.Factory{

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass == TimelineViewModel::class.java)
            return TimelineViewModel(type, baseTimelineRequest) as T

        throw IllegalArgumentException("error")
    }
}