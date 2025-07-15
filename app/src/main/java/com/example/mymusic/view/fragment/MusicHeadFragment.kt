package com.example.mymusic.view.fragment

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.mymusic.R
import com.example.mymusic.viewmodel.MusicHeadViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import androidx.lifecycle.ViewModelProvider
import androidx.core.view.get
import androidx.core.view.size
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Observer
import com.example.mymusic.adapter.MusicPagerAdapter

class MusicHeadFragment : BaseMusicFragment() {
    private lateinit var fragmentList: List<BaseMusicFragment>
    private lateinit var mMusicHeadViewModel: MusicHeadViewModel
    private lateinit var viewPager2: ViewPager2
    private lateinit var mBottNavigationView: BottomNavigationView
    private var isProcessingClick = false  // 添加标志位 避免重复触发 底部导航栏
    // 保存对 MainMusicFragment 的引用
    private var mainMusicFragment: MainMusicFragment? = null

    override fun getLayoutResId(): Int = R.layout.fragment_music_head

    override fun initView() {
        mMusicHeadViewModel = ViewModelProvider(this).get(MusicHeadViewModel::class.java)
        mBottNavigationView = rootView.findViewById(R.id.bottomNavigationView)
        viewPager2 = rootView.findViewById(R.id.viewPager2_fragment)


        // 创建Fragment列表
        fragmentList = listOf(
            MainMusicFragment().also { mainMusicFragment = it },
            FindMusicFragment(),
            MyMusicFragment()
        )
        // 设置ViewPager2适配器
        val pagerAdapter = MusicPagerAdapter(requireActivity(),fragmentList)
        viewPager2.adapter = pagerAdapter

        // 禁止滑动，只能通过底部导航栏切换
        viewPager2.isUserInputEnabled = false
        viewPager2.currentItem = 0
        mBottNavigationView.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
        //初始时候设置图标是播放
        mBottNavigationView.menu.findItem(R.id.main).icon = resources.getDrawable(R.mipmap.runing, null)
        mBottNavigationView.menu.findItem(R.id.main).title = ""
        // 正确设置底部导航栏的选中项
        mBottNavigationView.selectedItemId = R.id.main // 使用资源ID而不是位置索引
    }

    override fun setListener() {
        // 1. 保存原始状态（提升为类变量，确保全局可访问）
        val mainMenuItem = mBottNavigationView.menu.findItem(R.id.main)
        val originalMainTitle = ""

        mBottNavigationView.setOnItemSelectedListener { item ->
            // 如果正在处理点击事件，直接返回，避免重复触发
            if (isProcessingClick) return@setOnItemSelectedListener true
            isProcessingClick = true  // 开始处理点击事件
            try {
            when (item.itemId) {
                R.id.main -> {
                    if (viewPager2.currentItem == 0) {
                        // 切换播放状态（需要在 MainMusicViewModel 中处理播放/暂停逻辑）
                        Log.d("MusicHeadFragment", "已经在播放页")
                        mainMusicFragment?.mMainMusicViewModel?.togglePlaying()
                        val isPlaying = mainMusicFragment?.mMainMusicViewModel?.isPlaying?.value == true
                        mMusicHeadViewModel.updatePlayIcon(mainMenuItem, isPlaying,resources)

                    } else {
                        Log.d("MusicHeadFragment", "切换到播放页 ")
                        viewPager2.currentItem = 0
                        val isPlaying = mainMusicFragment?.mMainMusicViewModel?.isPlaying?.value == true
                        mMusicHeadViewModel.updatePlayIcon(mainMenuItem, isPlaying,resources)
                        mainMenuItem.title=""
                    }
                }
                R.id.find -> {
                    viewPager2.currentItem = 1
                    // 恢复原始状态
                    mainMenuItem.title = originalMainTitle
                    mainMenuItem.icon= resources.getDrawable(R.mipmap.musicbottom, null)
                }
                R.id.mine -> {
                    viewPager2.currentItem = 2
                    // 恢复原始状态
                    mainMenuItem.title = originalMainTitle
                    mainMenuItem.icon= resources.getDrawable(R.mipmap.musicbottom, null)
                }
            }} finally {
                // 无论如何都在处理完成后重置标志位
                isProcessingClick = false
            }
            true
        }

        // ViewPager 切换时也需要恢复原始状态
        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // 关键修改：根据ViewPager页面索引，映射到对应的底部导航菜单项ID
                val targetItemId = when (position) {
                    0 -> R.id.main    // ViewPager第0页（Main）→ 对应菜单R.id.main
                    1 -> R.id.find    // ViewPager第1页（Find）→ 对应菜单R.id.find
                    2 -> R.id.mine    // ViewPager第2页（My）→ 对应菜单R.id.mine
                    else -> R.id.main
                }
                // 同步底部导航栏（BottomNavigationView）的选中状态
                mBottNavigationView.selectedItemId = targetItemId

                // 恢复"音乐"标签的状态（保持不变）
                if (position != 0) {
                    mainMenuItem.title = originalMainTitle
                    mainMenuItem.icon = resources.getDrawable(R.mipmap.musicbottom, null)
                } else {
                    val isPlaying = mainMusicFragment?.mMainMusicViewModel?.isPlaying?.value ?: false
                    mMusicHeadViewModel.updatePlayIcon(mainMenuItem, isPlaying, resources)
                    mainMenuItem.title = ""
                }
            }
        })

    }



    override fun initData() {
    }


    override fun onResume() {
        //当mainMusicFragment处于 detached 状态时，访问其 ViewModel 就会触发异常。
        //在访问 Fragment 的 ViewModel 之前，先检查它是否处于活动状态：
        super.onResume()
        // 更新音乐状态initView
        val isPlaying = if (mainMusicFragment?.isAdded == true) {
            mainMusicFragment?.mMainMusicViewModel?.isPlaying?.value ?: false
        } else {
            false
        }
        mMusicHeadViewModel.updatePlayIcon(mBottNavigationView.menu.findItem(R.id.main), isPlaying, resources)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 延迟到View创建后，检查MainMusicFragment是否已附加
        checkAndObserveMainMusicViewModel()
    }


    // 检查MainMusicFragment是否已附加，再观察ViewModel
    private fun checkAndObserveMainMusicViewModel() {
        // 监听mainMusicFragment的添加状态
        viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // 确保MainMusicFragment已被添加到Activity
                if (mainMusicFragment?.isAdded == true) {
                    // 此时可以安全访问ViewModel
                    mainMusicFragment?.mMainMusicViewModel?.isPlaying?.observe(viewLifecycleOwner) { isPlaying ->
                        mMusicHeadViewModel.updatePlayIcon(
                            mBottNavigationView.menu.findItem(R.id.main),
                            isPlaying,
                            resources
                        )
                    }
                } else {
                    // 未添加则延迟重试（最多重试3次，避免无限循环）
                    viewPager2.postDelayed({ checkAndObserveMainMusicViewModel() }, 300)
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
//        mainMusicFragment?.mMainMusicViewModel?.isPlaying?.removeObservers(viewLifecycleOwner)
    }
}

