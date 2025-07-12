package com.example.mymusic.view.fragment

import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.mymusic.R
import com.example.mymusic.adapter.MusicRecycleViewAdapter
import com.example.mymusic.repo.entity.MusicInfo
import com.example.mymusic.view.SmartSeekBar
import com.example.mymusic.viewmodel.fragment.MainMusicViewModel

class MainMusicFragment : BaseMusicFragment() {
    private lateinit var mMainMusicViewModel: MainMusicViewModel
    private lateinit var mMusicRecycleViewAdapter: MusicRecycleViewAdapter
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var progressBar: SmartSeekBar
    // 修正列表声明语法（初始为空列表）
    private var musicList: List<MusicInfo> = emptyList()

    override fun getLayoutResId(): Int {
        return R.layout.fragment_main_music
    }

    override fun initView() {
        mMainMusicViewModel = ViewModelProvider(this)[MainMusicViewModel::class.java]
        mRecyclerView = rootView.findViewById(R.id.recycleViewMainMusic)
        setupRecyclerView() // 先初始化RecyclerView和适配器
        setupMusicList()    // 再监听数据变化
        progressBar = rootView.findViewById(R.id.SSB_my)
        setupProgressBar()
    }

    // 进度条双向绑定逻辑
    private fun setupProgressBar() {
        // 1. 监听播放总时长变化，设置进度条最大值
        mMainMusicViewModel.totalDuration.observe(viewLifecycleOwner) { total ->
            progressBar.max = total
        }

        // 2. 监听当前进度变化，更新进度条位置
        mMainMusicViewModel.currentPosition.observe(viewLifecycleOwner) { position ->
            // 避免拖动时进度条闪烁（仅在非拖动状态下更新）
            if (!progressBar.isDragging) {
                progressBar.progress = position
            }
        }

        // 3. 监听进度条拖动事件，更新播放位置
        progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // 仅处理用户拖动的情况
                if (fromUser) {
                    mMainMusicViewModel.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        // 4. 监听播放状态，控制进度条交互
        mMainMusicViewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            if (isPlaying) {
                mMainMusicViewModel.resumeMusic() // 继续播放
            } else {
                mMainMusicViewModel.pauseMusic() // 暂停播放
            }
        }
    }

    private fun setupMusicList() {
        // 监听歌单数据变化（首次加载/更新时触发）
        mMainMusicViewModel.playlist.observe(viewLifecycleOwner) { list ->
            if (list.isNotEmpty()) {
                musicList = list
                // 通过适配器的infoList更新数据（触发DiffUtil高效刷新）
                mMusicRecycleViewAdapter.infoList = list
                // 滚动到首个item
                mRecyclerView.scrollToPosition(0)
                // 初始选中第一首歌
                mMainMusicViewModel.setCurrentMusicId(list.first().songId.toString())
            }
        }

        // 监听播放状态变化（使用viewLifecycleOwner避免内存泄漏）
        mMainMusicViewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            Log.d("MainMusicFragment", "播放状态变化 → $isPlaying")
            // 更新UI（例如播放/暂停按钮状态）
            // 示例：updatePlayButtonState(isPlaying)
        }

        // 监听当前播放歌曲ID变化
        mMainMusicViewModel.currentMusicId.observe(viewLifecycleOwner) { currentId ->
            Log.d("MainMusicFragment", "当前播放歌曲ID → $currentId")
            // 可在此处更新当前选中项的UI（如高亮显示）
        }

        // 初始加载歌单数据
        mMainMusicViewModel.setPlayListDefault()
    }

    private fun setupRecyclerView() {
        // 使用已初始化的mRecyclerView（避免重复findViewById）
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        mRecyclerView.layoutManager = layoutManager

        // 初始化适配器（初始数据为空列表）
        mMusicRecycleViewAdapter = MusicRecycleViewAdapter(musicList)
        mRecyclerView.adapter = mMusicRecycleViewAdapter

        // 设置PagerSnapHelper实现整页滚动效果
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(mRecyclerView)

        // 设置列表项点击事件
//        mMusicRecycleViewAdapter.setOnItemClickListener { musicInfo ->
//            Log.d("MainMusicFragment", "点击了歌曲 → ${musicInfo.title}")
//            // 点击时更新当前播放歌曲ID
//            mMainMusicViewModel.setCurrentMusicId(musicInfo.songId.toString())
//            // 此处可添加播放逻辑（如启动播放、更新播放状态等）
//            mMainMusicViewModel.setPlayingState(true)
//        }

        // 监听滚动事件，配合ViewModel处理页面切换  获取当前显示的item并播放对应歌曲
        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                // 滚动停止时处理页面切换逻辑
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
//                    mMainMusicViewModel.handlePageSwitch(recyclerView, layoutManager)
                    // 通过snapHelper获取当前居中的item视图
                    val snapView = snapHelper.findSnapView(layoutManager) ?: return
                    //  获取该视图对应的位置
                    val currentPosition = layoutManager.getPosition(snapView)
                    // 校验位置合法性
                    if (currentPosition in musicList.indices) {
                        val currentMusic = musicList[currentPosition]
                        val currentId = currentMusic.songId.toString()
                        // 更新ViewModel的当前播放ID并触发播放
                        mMainMusicViewModel.setCurrentMusicId(currentId)
                        mMainMusicViewModel.playMusic(currentMusic) // 新增：播放当前歌曲
                    }
                }
            }
        })

        // 设置点赞/关注状态变化监听（如果需要处理交互）
        mMusicRecycleViewAdapter.setOnItemActionListener(object : MusicRecycleViewAdapter.OnItemActionListener {
            override fun onFollowStatusChanged(position: Int, isFollowed: Boolean) {
                // 处理关注状态变化（如调用接口更新服务器）
                musicList[position].let {
                    Log.d("MainMusicFragment", "关注状态变化 → ${it.singer}: $isFollowed")
                }
            }

            override fun onLikeStatusChanged(position: Int, isLiked: Boolean) {
                // 处理点赞状态变化
                musicList[position].let {
                    Log.d("MainMusicFragment", "点赞状态变化 → ${it.title}: $isLiked")
                    // 可在此处更新歌曲的点赞数
                }
            }
        })
    }

    override fun setListener() {
        // 可添加其他全局监听（如顶部标题栏点击等）
    }

    override fun initData() {
        // 额外初始化数据（如本地缓存读取等）
    }

    // 补充ViewModel中缺失的方法（如果编译报错，需在MainMusicViewModel中添加）
    // 注意：以下方法应在MainMusicViewModel中实现
    /*
    fun setCurrentMusicId(id: String) {
        _currentMusicId.value = id
    }

    fun setPlayingState(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }
    */
}
