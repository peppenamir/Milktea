package jp.panta.misskeyandroidclient.view.notes

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import jp.panta.misskeyandroidclient.MiApplication
import jp.panta.misskeyandroidclient.R
import jp.panta.misskeyandroidclient.model.account.page.Pageable
import jp.panta.misskeyandroidclient.viewmodel.MiCore
import jp.panta.misskeyandroidclient.viewmodel.notes.NotesViewModel
import jp.panta.misskeyandroidclient.viewmodel.notes.NotesViewModelFactory

class FavoriteNotesScreenFragment : Fragment(R.layout.fragment_favorite_notes_screen) {

    lateinit var notesViewModel: NotesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val miCore = requireContext().applicationContext as MiCore
        notesViewModel = ViewModelProvider(this, NotesViewModelFactory(miCore as MiApplication))[NotesViewModel::class.java]


    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragment = TimelineFragment.newInstance(
            Pageable.Favorite
        )
        childFragmentManager.beginTransaction().also {
            it.replace(R.id.favoriteNotesFragmentBase, fragment)
        }.commit()
    }
}