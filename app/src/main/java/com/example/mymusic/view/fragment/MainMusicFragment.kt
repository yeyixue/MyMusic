package com.example.mymusic.view.fragment

import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
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
    // 在 setupRecyclerView() 的滚动监听中添加
    private var isInitialScroll = true // 新增标记
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

        mMainMusicViewModel.initPlayer(requireContext())
        // 启动进度监听（关键：在此处调用，确保视图和数据已准备好）
        observeProgressUpdates()
    }


    private fun setupMusicList() {
        // 监听歌单数据变化（首次加载/更新时触发）
        mMainMusicViewModel.playlist.observe(viewLifecycleOwner) { list ->
            if (list.isNotEmpty()) {
                musicList = list
                mMusicRecycleViewAdapter.infoList = list
                // 延迟滚动，确保布局测量完成
                mRecyclerView.post {
                    mRecyclerView.scrollToPosition(0)
                    // 初始化第一首播放逻辑...
                    val firstMusic = list.first()
                    mMainMusicViewModel.setCurrentMusicId(firstMusic.songId.toString())
                    if(firstMusic.isVideo==false){
                        mMainMusicViewModel.playMusic(firstMusic)
                        _currentPlayingSongId.value = firstMusic.songId.toString()
                    }else{
                        currentCenterPosition = 0
                        _currentPlayingSongId.value = firstMusic.songId.toString()
                        playVideo(currentCenterPosition)
                    }
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


        // 设置进度更新监听,这是adaapter的拖动回调处理逻辑
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
                        // 暂停当前播放（无论是音乐还是视频）
                        if (currentCenterPosition != -1) {
                            val currentMusic = musicList[currentCenterPosition]
                            if (currentMusic.isVideo) {
                                mMainMusicViewModel.pauseVideo()
                            } else {
                                mMainMusicViewModel.pauseMusic()
                            }
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
//                            mMainMusicViewModel.pauseVideo()
                            playVideo(currentCenterPosition)
                        }
                        // 更新当前播放ID
                        _currentPlayingSongId.value = newMusic.songId.toString()
                        mMusicRecycleViewAdapter.currentCenterPosition = currentCenterPosition
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


    // 播放视频时更新状态
    private fun playVideo(position: Int) {
        mMainMusicViewModel.pauseVideo() // 停止音乐播放

        // 2. 解绑上一个页面的 PlayerView
        val oldHolder = mRecyclerView.findViewHolderForAdapterPosition(currentCenterPosition)
        if (oldHolder is MusicRecycleViewAdapter.VideoViewHolder) {
            oldHolder.playerView.player = null
            Log.d("PlayVideo", "解绑上一个页面的 PlayerView: $currentCenterPosition")
        }
        // 3. 获取当前页面 ViewHolder
        val viewHolder = mRecyclerView.findViewHolderForAdapterPosition(position)
        if (viewHolder is MusicRecycleViewAdapter.VideoViewHolder) {
            // 1. 重置共享播放器状态
            val player = mMainMusicViewModel.sharedPlayer
            player.stop() // 停止当前播放
            player.clearMediaItems() // 清除旧媒体
//            player.removeAllListeners() // 移除所有旧监听

            // 2. 绑定新页面的 PlayerView
            viewHolder.playerView.visibility = View.VISIBLE
            viewHolder.thumbnailImageView.visibility = View.GONE
            viewHolder.playerView.player = player // 绑定新视图

            // 3. 加载新视频
            val currentMusic = musicList[position]
            val videoUrl = mMainMusicViewModel.getVideoUrlBySongId(currentMusic.songId)
            val mediaItem = MediaItem.fromUri(videoUrl)
            player.setMediaItem(mediaItem)


            // 4. 准备并播放
            player.prepare()
            player.playWhenReady = true
            // 更新当前播放位置
            currentCenterPosition = position
            mMainMusicViewModel.setCurrentMusicId(currentMusic.songId.toString())

            // 标记当前播放的是视频
            mMainMusicViewModel.startVideoPlayback()

            // 开始监听进度
            mMainMusicViewModel.startProgressUpdates()
        }
    }


    // 监听进度更新
    private fun observeProgressUpdates() {

        mMainMusicViewModel.progressLiveData.observe(viewLifecycleOwner) { (currentPosition, duration) ->
            if (duration <= 0) {
                Log.d("ProgressUpdate", "无效时长: $duration")
                return@observe
            }
            val progressPercent = if (duration > 0) ((currentPosition * 100) / duration).toInt() else 0
            val formattedTime = formatTime(currentPosition.toInt())
            val currentPlayingPosition = mMusicRecycleViewAdapter.currentCenterPosition
            mMusicRecycleViewAdapter.updateItemProgress(
                currentPlayingPosition,
                progressPercent,
                formattedTime
            )

            // 获取当前播放的ViewHolder
//            val viewHolder = mRecyclerView.findViewHolderForAdapterPosition(currentCenterPosition)
//            if (viewHolder is MusicRecycleViewAdapter.VideoViewHolder) {
//                // 更新进度条
//                val progressPercent = if (duration > 0) ((currentPosition * 100) / duration).toInt() else 0
//                val formattedTime = formatTime(currentPosition.toInt())
//                viewHolder.updateProgress(progressPercent, formattedTime)
////                Log.d("ProgressUpdate", "更新进度: $progressPercent%，位置: $currentCenterPosition")
//            }
        }
    }

    // 格式化时间辅助方法
    private fun formatTime(millis: Int): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }




    override fun setListener() {
        // 可添加其他全局监听（如顶部标题栏点击等）
    }

    override fun initData() {
        // 额外初始化数据（如本地缓存读取等）
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onPause() {
        super.onPause()
        // 切换到别的页面-》暂停播放（保留状态）--一直播放
//        if (currentCenterPosition != -1) {
//            val currentMusic = musicList[currentCenterPosition]
//            if (currentMusic.isVideo) {
//                pauseVideo(currentCenterPosition)
//            }
//        }
    }


}
