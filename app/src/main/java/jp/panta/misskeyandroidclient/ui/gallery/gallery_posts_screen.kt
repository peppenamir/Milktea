package jp.panta.misskeyandroidclient.ui.gallery

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.asLiveData
import jp.panta.misskeyandroidclient.util.PageableState
import jp.panta.misskeyandroidclient.viewmodel.gallery.GalleryPostsViewModel
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import jp.panta.misskeyandroidclient.util.StateContent
import jp.panta.misskeyandroidclient.viewmodel.gallery.GalleryPostState

@Composable
fun GalleryPostsScreen(
    galleryPostsViewModel: GalleryPostsViewModel
) {
    val state by galleryPostsViewModel.galleryPosts.asLiveData().observeAsState(PageableState.Fixed(
        StateContent.NotExist()
    ))

    val isRefreshing = state is PageableState.Loading.Init
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)

    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = {
            galleryPostsViewModel.loadInit()
        },
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        if(state.content is StateContent.Exist) {
            LazyColumn {
                /*this.items(state.content?.rawContent ?: emptyList(),
                    key = { item: GalleryPostState ->
                        item.gal
                    }
                ) {

                }*/
            }
        }else if(state is PageableState.Loading && state.content is StateContent.NotExist) {
            // ローディングアニメーションを表示する
            CircularProgressIndicator()
        }else if(state is PageableState.Error && state.content is StateContent.NotExist){
            Text("loading error")
        }else {
            Text("感情がない")
        }
    }

}