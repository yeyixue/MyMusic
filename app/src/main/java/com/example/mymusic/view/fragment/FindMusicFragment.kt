    package com.example.mymusic.view.fragment

    import android.os.Bundle
    import android.util.Log
    import androidx.fragment.app.Fragment
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import androidx.lifecycle.ViewModelProvider
    import androidx.recyclerview.widget.GridLayoutManager
    import androidx.recyclerview.widget.LinearLayoutManager
    import androidx.recyclerview.widget.PagerSnapHelper
    import androidx.recyclerview.widget.RecyclerView
    import com.example.mymusic.R
    import com.example.mymusic.adapter.FindRecycleViewAdapter
    import com.example.mymusic.adapter.MusicFindStyleInfo
    import com.example.mymusic.repo.entity.MusicInfo
    import com.example.mymusic.repo.playlist.PlaylistRepository
    import com.example.mymusic.viewmodel.fragment.FindMusicViewModel
    import com.example.mymusic.viewmodel.fragment.MainMusicViewModel
    import kotlin.collections.mutableListOf


    class FindMusicFragment : BaseMusicFragment() {
        private lateinit var mFindMusicViewModel: FindMusicViewModel
        private lateinit var mFindRecycleViewAdapter: FindRecycleViewAdapter
        // 不同 Fragment 会获取到不同的 ViewModel 实例。
        private val musicList = mutableListOf<MusicFindStyleInfo>()


        override fun getLayoutResId(): Int {
            return R.layout.fragment_find_music
        }

        override fun initView() {
            mFindMusicViewModel= ViewModelProvider(this).get(FindMusicViewModel::class.java)
            setupRecyclerView()
            mFindMusicViewModel.setSomeData(musicList) // 在 initView 后调用
            // 通知适配器数据已更新（建议调用 notifyDataSetChanged）
            mFindRecycleViewAdapter.notifyDataSetChanged()

            // 关键修改：在initView中只注册一次观察者
            observeSongList()
        }

        override fun setListener() {
        }

        private fun observeSongList() {
            //不能写在点击事件里面，不然在 每次点击事件 中被调用，导致生成多个观察者。
            mFindMusicViewModel.songListLiveData.observe(viewLifecycleOwner) { songs ->
                if (songs.isNotEmpty()) {
                    Log.d("FindMusicFragment", "收到歌曲列表: ${songs.size}首")
                    PlaylistRepository.updatePlaylist(songs)
                }
            }
        }
        private fun setupRecyclerView() {
            val recyclerView = rootView.findViewById<RecyclerView>(R.id.recycleViewFindMusic)

            // 设置垂直布局管理器，并启用页面切换模式
    //        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            val layoutManager =GridLayoutManager(requireContext(),2)
            recyclerView.layoutManager = layoutManager

            // 创建适配器并设置数据
            mFindRecycleViewAdapter = FindRecycleViewAdapter(musicList)
            recyclerView.adapter = mFindRecycleViewAdapter


            // 设置点击事件监听
            mFindRecycleViewAdapter.setOnItemClickListener(object : FindRecycleViewAdapter.OnItemClickListener{
                override fun onItemClick(noteInfo: MusicFindStyleInfo) {
                    Log.d("FindMusicFragment", "点击了: ${noteInfo.title}")

                    // 1. 触发加载歌曲
                    mFindMusicViewModel.loadSongsByStyle(noteInfo.title)

                }
            })

            // 监听滚动事件，实现超过中间位置自动切换
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                }
            })
        }

        override fun initData() {

        }


        override fun onDestroy() {
            super.onDestroy()
        }

    }