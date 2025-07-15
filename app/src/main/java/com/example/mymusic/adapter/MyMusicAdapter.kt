package com.example.mymusic.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.mymusic.view.fragment.MyMusicFragment

class MyMusicAdapter(
    fragmentActivity: MyMusicFragment,
    private val fragmentList: List<Fragment>
) : FragmentStateAdapter(fragmentActivity){
    override fun createFragment(position: Int): Fragment {
        return fragmentList[position]
    }

    override fun getItemCount(): Int {
        return fragmentList.size
    }

}