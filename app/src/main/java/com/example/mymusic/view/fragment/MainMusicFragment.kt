package com.example.mymusic.view.fragment

import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
//    internal lateinit var mMainMusicViewModel: MainMusicViewModel

    // 使用 by lazy 实现延迟初始化
    val mMainMusicViewModel: MainMusicViewModel by lazy {
        ViewModelProvider(this).get(MainMusicViewModel::class.java)
    }
    // 添加静态访问方法
    companion object {
        fun getInstance(): MainMusicFragment {
            return MainMusicFragment()
        }
    }
    private lateinit var mMusicRecycleViewAdapter: MusicRecycleViewAdapter
    private lateinit var mRecyclerView: RecyclerView

    // 记录当前播放的歌曲ID和中心页面位置
    private val _currentPlayingSongId = MutableLiveData<String?>()
    var currentPlayingSongId: LiveData<String?> = _currentPlayingSongId
    private var currentCenterPosition: Int = -1

    // 修正列表声明语法（初始为空列表）
    private var musicList: List<MusicInfo> = emptyList()

    override fun getLayoutResId(): Int {
        return R.layout.fragment_main_music
    }

    override fun initView() {
//        mMainMusicViewModel = ViewModelProvider(this)[MainMusicViewModel::class.java]
        mRecyclerView = rootView.findViewById(R.id.recycleViewMainMusic)
        setupRecyclerView() // 先初始化RecyclerView和适配器
        // 监听ViewModel的滚动指令，自动滚动到下一页
        mMainMusicViewModel.scrollToPosition.observe(viewLifecycleOwner) { position ->
            position?.let {
                // 1. 滚动到下一首的位置（带动画效果）
                mRecyclerView.smoothScrollToPosition(it)
                // 2. 清除指令，避免重复滚动
                mMainMusicViewModel.clearScrollCommand()
            }
        }

        setupMusicList()    // 再监听数据变化
    }


    private fun setupMusicList() {
        // 监听歌单数据变化（首次加载/更新时触发）
        mMainMusicViewModel.playlist.observe(viewLifecycleOwner) { list ->
            if (list.isNotEmpty()) {
                musicList = list
                mMusicRecycleViewAdapter.infoList = list
                mRecyclerView.scrollToPosition(0)

                // 设置当前播放ID
                val firstMusic = list.first()
                mMainMusicViewModel.setCurrentMusicId(firstMusic.songId.toString())
                // 主动播放第一首歌（解决启动无音乐问题）
                if(firstMusic.isVideo==false){
                    mMainMusicViewModel.playMusic(firstMusic)
                    _currentPlayingSongId.value = firstMusic.songId.toString() // 更新ID
                }else{
                    //处理播放视频逻辑
                    mMainMusicViewModel.setCurrentMusicId(firstMusic.songId.toString())
                    currentCenterPosition = 0
                    _currentPlayingSongId.value = firstMusic.songId.toString()
                    playVideo(0)
                }
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
            _currentPlayingSongId.value = currentId
        }
        // 初始加载歌单数据
        mMainMusicViewModel.setPlayListDefault()


        // 监听播放进度变化，更新当前显示项的进度条
        mMainMusicViewModel.currentProgressPercent.observe(viewLifecycleOwner) { percent ->
            val currentPosition = getCurrentPlayingPosition()
            if (currentPosition != -1) {
                mMusicRecycleViewAdapter.updateItemProgress(
                    currentPosition,
                    percent,
                    mMainMusicViewModel.formattedCurrentTime.value ?: "00:00"
                )
            }
        }
    }

    // 获取当前播放歌曲的位置
    private fun getCurrentPlayingPosition(): Int {
        val currentId = mMainMusicViewModel.currentMusicId.value ?: return -1
        return musicList.indexOfFirst { it.songId.toString() == currentId }
    }

    private fun setupRecyclerView() {
        // 使用已初始化的mRecyclerView（避免重复findViewById）
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        mRecyclerView.layoutManager = layoutManager

        // 初始化适配器（初始数据为空列表）
//        mMusicRecycleViewAdapter = MusicRecycleViewAdapter(musicList)
        mMusicRecycleViewAdapter = MusicRecycleViewAdapter(mMainMusicViewModel, musicList)

        mRecyclerView.adapter = mMusicRecycleViewAdapter
        // 将RecyclerView实例传递给适配器
        // 观察缓冲进度并传递给 Adapter
        mMainMusicViewModel.bufferProgress.observe(viewLifecycleOwner) { percent ->
            Log.d("BufferProgress", "Fragment接收到缓冲进度: $percent")
            mMusicRecycleViewAdapter.updateBufferProgress(percent)
        }
        mMusicRecycleViewAdapter.setRecyclerView(mRecyclerView)


        // 设置进度更新监听
        mMusicRecycleViewAdapter.setOnPlayProgressListener(object : MusicRecycleViewAdapter.OnPlayProgressListener {
            override fun onProgressUpdate(position: Int, progress: Int, currentTime: String, totalTime: String) {
                // 用户拖动进度条时，更新ViewModel
                val musicInfo = musicList[position]
                mMainMusicViewModel.setCurrentMusicId(musicInfo.songId.toString())
                mMainMusicViewModel.seekToPercent(progress)
            }
        })

        // 设置PagerSnapHelper实现整页滚动效果
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(mRecyclerView)


        // 监听滚动事件，确保切换到新页面时播放对应歌曲
        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // 获取当前居中的item位置
                    val snapView = snapHelper.findSnapView(layoutManager) ?: return
                    val newCenterPosition = layoutManager.getPosition(snapView)

                    // 检查是否真的切换到了新页面
                    if (newCenterPosition != currentCenterPosition && newCenterPosition in musicList.indices) {
                        val newMusic = musicList[newCenterPosition]
                        val oldMusic = musicList.getOrNull(currentCenterPosition)
                        if (oldMusic?.isVideo == true) {
                            pauseVideo(currentCenterPosition)
                        }
                        // 更新当前中心位置
                        currentCenterPosition = newCenterPosition
                        if(newMusic.isVideo==false){
                            //播放音乐
                            // 只有当新页面的歌曲ID与当前播放的不同时才切换播放
                            if (newMusic.songId.toString() != _currentPlayingSongId.value) {
                                // 暂停当前播放
                                mMainMusicViewModel.pauseMusic()
                                // 播放新歌曲
                                mMainMusicViewModel.setCurrentMusicId(newMusic.songId.toString())
                                mMainMusicViewModel.playMusic(newMusic)

                            }
                        }else{
                            //处理播放视频
                            //这里只是更新 isPlaying 状态
                            mMainMusicViewModel.pauseVideo()
                            playVideo(newCenterPosition)
                        }
                        // 更新当前播放ID
                        _currentPlayingSongId.value = newMusic.songId.toString()
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
                    // 可在此处更新歌曲的点赞数  !@#
                }
            }
        })
    }
    // 播放音乐时更新状态
    private fun playMusic() {
        mMainMusicViewModel.setPlaying(true) // 调用 ViewModel 方法更新状态
        // 其他播放逻辑...
    }

    // 暂停音乐时更新状态
    private fun pauseMusic() {
        mMainMusicViewModel.setPlaying(false)
        // 其他暂停逻辑...
    }

    // 播放视频时更新状态
    private fun playVideo(position: Int) {
        val viewHolder = mRecyclerView.findViewHolderForAdapterPosition(position) as? MusicRecycleViewAdapter.VideoViewHolder
        viewHolder?.playVideo()
    }

    // 暂停视频时更新状态
    private fun pauseVideo(position: Int) {
        val viewHolder = mRecyclerView.findViewHolderForAdapterPosition(position) as? MusicRecycleViewAdapter.VideoViewHolder
        viewHolder?.pauseVideo()
        // 可以在这里更新ViewModel中的播放状态
        mMainMusicViewModel.setPlaying(false)
    }

    override fun setListener() {
        // 可添加其他全局监听（如顶部标题栏点击等）
    }

    override fun initData() {
        // 额外初始化数据（如本地缓存读取等）
    }

}
