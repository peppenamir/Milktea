package jp.panta.misskeyandroidclient.view.notes

import android.content.Context
import android.os.Bundle
import android.util.Log

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.wada811.databinding.dataBinding
import jp.panta.misskeyandroidclient.KeyStore
import jp.panta.misskeyandroidclient.MiApplication
import jp.panta.misskeyandroidclient.R
import jp.panta.misskeyandroidclient.databinding.FragmentTabBinding
import jp.panta.misskeyandroidclient.model.account.page.Page
import jp.panta.misskeyandroidclient.model.account.Account
import jp.panta.misskeyandroidclient.util.getPreferenceName
import jp.panta.misskeyandroidclient.view.PageableFragmentFactory
import jp.panta.misskeyandroidclient.view.ScrollableTop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

class TabFragment : Fragment(R.layout.fragment_tab), ScrollableTop{



    private lateinit var mPagerAdapter: TimelinePagerAdapter

    private val binding: FragmentTabBinding by dataBinding()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val miApp = context?.applicationContext as MiApplication

        val sharedPreferences = requireContext().getSharedPreferences(requireContext().getPreferenceName(), Context.MODE_PRIVATE)
        val includeMyRenotes = sharedPreferences.getBoolean(KeyStore.BooleanKey.INCLUDE_MY_RENOTES.name, true)
        val includeRenotedMyNotes = sharedPreferences.getBoolean(KeyStore.BooleanKey.INCLUDE_RENOTED_MY_NOTES.name, true)
        val includeLocalRenotes = sharedPreferences.getBoolean(KeyStore.BooleanKey.INCLUDE_LOCAL_RENOTES.name, true)

        val pager = binding.viewPager.adapter as? TimelinePagerAdapter
        if(pager == null){
            mPagerAdapter = TimelinePagerAdapter(this, emptyList())
            binding.viewPager.adapter = mPagerAdapter
        }


        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = mPagerAdapter.pages[position].title
        }.attach()
        Log.d("TabFragment", "設定:$includeLocalRenotes, $includeRenotedMyNotes, $includeMyRenotes")
        miApp.getCurrentAccount().filterNotNull().flowOn(Dispatchers.IO).onEach { account ->
            val pages = account.pages


            Log.d("TabFragment", "pages:$pages")

            mPagerAdapter.setPages(pages.sortedBy { it.weight })




            if(pages.size <= 1){
                binding.tabLayout.visibility = View.GONE
                binding.elevationView.visibility = View.VISIBLE
            }else{
                binding.tabLayout.visibility = View.VISIBLE
                binding.elevationView.visibility = View.GONE
                binding.tabLayout.elevation
                if(pages.size > 5) {
                    binding.tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
                }else{
                    binding.tabLayout.tabMode = TabLayout.MODE_FIXED
                }
            }
        }.launchIn(lifecycleScope)

    }


    class TimelinePagerAdapter( fragment: Fragment, list: List<Page>) : FragmentStateAdapter(fragment){


        var pages: List<Page> = list
            private set

        private var oldPages: List<Page> = emptyList()

        private val diffUtilCallback = object : DiffUtil.Callback() {

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return pages[newItemPosition] == oldPages[oldItemPosition]
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return pages[newItemPosition].pageId == oldPages[oldItemPosition].pageId
            }

            override fun getNewListSize(): Int {
                return pages.size
            }

            override fun getOldListSize(): Int {
                return oldPages.size
            }

        }

        var account: Account? = null

        val scrollableTopFragments = HashMap<Page, ScrollableTop>()

        override fun createFragment(position: Int): Fragment {
            val item = pages[position]
            return PageableFragmentFactory.create(item).also {
                if(it is ScrollableTop) {
                    scrollableTopFragments[item] = it
                }
            }
        }

        override fun getItemCount(): Int {
            return pages.size
        }

        override fun containsItem(itemId: Long): Boolean {
            return pages.any { it.pageId == itemId }
        }

        override fun getItemId(position: Int): Long {
            return pages[position].pageId
        }

        fun setPages(pages: List<Page>) {
            this.oldPages = this.pages
            this.pages = pages
            val result = DiffUtil.calculateDiff(diffUtilCallback)
            result.dispatchUpdatesTo(this)
        }






    }

    override fun showTop() {
        showTopCurrentFragment()
    }

    private fun showTopCurrentFragment(){
        try{
            mPagerAdapter.scrollableTopFragments.values.forEach{
                it.showTop()
            }
        }catch(e: UninitializedPropertyAccessException){

        }

    }


}