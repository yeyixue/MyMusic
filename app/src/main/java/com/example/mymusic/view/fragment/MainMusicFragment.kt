package com.example.mymusic.view.fragment

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat.startForegroundService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.example.mymusic.R
import com.example.mymusic.Service.NotificationService
import com.example.mymusic.adapter.MusicRecycleViewAdapter
import com.example.mymusic.adapter.MusicRecycleViewAdapter.ProgressListenerManager
import com.example.mymusic.repo.entity.MusicInfo
import com.example.mymusic.view.SmartSeekBar
import com.example.mymusic.view.activity.MyMusicActivity
import com.example.mymusic.viewmodel.fragment.MainMusicViewModel
import kotlin.compareTo
import kotlin.div
import kotlin.text.toInt
import kotlin.times


class MainMusicFragment : BaseMusicFragment() {

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
    /*
        bindService 是异步操作，系统需要时间建立连接并回调 onServiceConnected。
        而 mMainMusicViewModel 的初始化是同步的（在 initView 的同步代码中提前触发），因此时间上必然早于 serviceBinder 的赋值和使用。
     */
    private var serviceBinder: NotificationService.NotificationBinder? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // 绑定成功，传递 ViewModel 给 Service
            serviceBinder = service as NotificationService.NotificationBinder
            serviceBinder?.setViewModel(mMainMusicViewModel)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            serviceBinder = null
        }

    }



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
        // 监听ViewModel的滚动指令，自动滚动到下一页 --自动播放在这
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
        // 启动进度监听
        observeProgressUpdates()

        // 启动并绑定服务
        val intent = Intent(requireContext(), NotificationService::class.java)
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        // 启动前台服务（Android 8.0+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
    }


    private fun setupMusicList() {
        var isFirstLoad = true // 首次加载标记
        // 监听歌单数据变化（首次加载/更新时触发）
        mMainMusicViewModel.playlist.observe(viewLifecycleOwner) { newList ->
            if (newList.isNotEmpty()) {
                musicList = newList
                mMusicRecycleViewAdapter.infoList = newList // 只更新适配器，不重新播放

                // 仅在首次加载时执行播放初始化
                if (isFirstLoad) {

                    mRecyclerView.post {
                        mRecyclerView.scrollToPosition(0)
                        val firstMusic = newList.first()
                        mMainMusicViewModel.setCurrentMusicId(firstMusic.songId.toString())

                        if (!firstMusic.isVideo) {
                            mMainMusicViewModel.playMusic(firstMusic)
                            _currentPlayingSongId.value = firstMusic.songId.toString()
                        } else {
                            currentCenterPosition = 0
                            _currentPlayingSongId.value = firstMusic.songId.toString()
                            mMainMusicViewModel.playVideo(
                                currentCenterPosition,
                                firstMusic,
                                mRecyclerView,
                                currentCenterPosition
                            )
                        }
                        isFirstLoad = false // 首次加载完成，标记为 false
                    }
                    val videoList = newList.filter { it.isVideo }
                    videoList.take(3).forEach { videoMusic -> // 预加载前3个视频
                        preloadVideoCover(videoMusic)
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
    }


    private fun setupRecyclerView() {
        // 使用已初始化的mRecyclerView
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        mRecyclerView.layoutManager = layoutManager

        // 初始化适配器
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


        // 设置进度更新监听,这是adapter的拖动回调处理逻辑--播放进度更新接口
        mMusicRecycleViewAdapter.setOnPlayProgressListener(object : ProgressListenerManager.OnPlayProgressListener {
            override fun onProgressUpdate(position: Int, progress: Int, currentTime: String, totalTime: String) {
                // 用户拖动进度条时，复用viewmodel这里会设置错误的id
//                val musicInfo = musicList[position]
//                mMainMusicViewModel.setCurrentMusicId(musicInfo.songId.toString())

                // 无论播放/暂停，都强制跳转进度
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
//                        val viewHolder = mRecyclerView.findViewHolderForAdapterPosition(currentCenterPosition)



                        val newMusic = musicList[newCenterPosition]
                        Log.d("PositionUpdate", "滚动完成，旧位置: $currentCenterPosition → 新位置: $newCenterPosition")
                        // 暂停当前播放（无论是音乐还是视频）
//                        if (currentCenterPosition != -1) {
//                            val currentMusic = musicList[currentCenterPosition]
//                            if (currentMusic.isVideo) {
//                                mMainMusicViewModel.pauseVideo()
//                            } else {
//                                mMainMusicViewModel.pauseMusic()
//                            }
//                        }
                        // 更新当前中心位置
                        currentCenterPosition = newCenterPosition
                        if(newMusic.isVideo==false){
                            mMainMusicViewModel.switchToAudioPlayback()
                            //播放音乐
                            // 只有当新页面的歌曲ID与当前播放的不同时才切换播放  --这个影响自动播放！
//                            if (newMusic.songId.toString() != _currentPlayingSongId.value) {
                                // 暂停当前播放
//                                mMainMusicViewModel.pauseMusic()
                                // 播放新歌曲
                                mMainMusicViewModel.setCurrentMusicId(newMusic.songId.toString())
                                mMainMusicViewModel.playMusic(newMusic)

//                                mMainMusicViewModel.startProgressUpdates()
//                            }
                        }else{
                            //处理播放视频
//                            mMainMusicViewModel.pauseVideo()
//                            mMainMusicViewModel.resetPlayer()
                            Log.d("Mymusic","playVideo(currentCenterPosition) currentCenterPosition是 $currentCenterPosition ")
                            mMainMusicViewModel.playVideo(currentCenterPosition,newMusic,mRecyclerView,currentCenterPosition)
//                            mMainMusicViewModel.startProgressUpdates()
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


    // 监听进度更新
    private fun observeProgressUpdates() {
        mMainMusicViewModel.progressLiveData.observe(viewLifecycleOwner) { (currentPosition, duration) ->
            if (duration <= 0) {
                Log.d("ProgressUpdate", "无效时长: $duration，可能是音频/视频未准备好")
                return@observe
            }
            // 计算进度百分比（0-100）
            val progressPercent = ((currentPosition * 100) / duration).toInt().coerceIn(0, 100)
            // 格式化当前时间
            val formattedTime = mMainMusicViewModel.formatTime(currentPosition.toInt())
            // 获取当前中心位置（音频和视频共用同一个位置标记）
            val currentPlayingPosition = mMusicRecycleViewAdapter.currentCenterPosition

            // 日志调试：区分音频/视频进度
            val mediaType = if (mMainMusicViewModel.isPlayingVideo) "视频" else "音频"
//            Log.d("ProgressUpdate", "$mediaType 进度: $progressPercent%，位置: $currentPlayingPosition")
//            Log.d("ProgressUpdate", "currentPlayingPosition: $currentPlayingPosition，progressPercent: $progressPercent")

            // 更新进度条（统一调用适配器方法）
            mMusicRecycleViewAdapter.updateItemProgress(
                currentPlayingPosition,
                progressPercent,
                formattedTime
            )
        }
    }



    // 预加载视频封面并保存到MusicInfo
    private fun preloadVideoCover(musicInfo: MusicInfo) {
        val videoUrl = mMainMusicViewModel.getVideoUrlBySongId(musicInfo.songId)
        Glide.with(requireContext())
            .asBitmap()
            .load(videoUrl)
            .frame(0) // 取第一帧
            .listener(object : RequestListener<Bitmap> {

                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<Bitmap?>?,
                    isFirstResource: Boolean
                ): Boolean {
                    // 加载失败：使用默认封面
                    val defaultCover = BitmapFactory.decodeResource(
                        resources, R.mipmap.img2
                    )
                    val updatedMusic = musicInfo.copy(
                        coverBitmap = defaultCover,
                        isCoverLoaded = true
                    )
                    mMainMusicViewModel.updateMusicInfo(updatedMusic)
                    return false
                }

                override fun onResourceReady(
                    resource: Bitmap?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<Bitmap?>?,
                    dataSource: com.bumptech.glide.load.DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    // 保存封面到MusicInfo
                    val updatedMusic = musicInfo.copy(
                        coverBitmap = resource,
                        isCoverLoaded = true
                    )
                    mMainMusicViewModel.updateMusicInfo(updatedMusic) // 更新歌单
                    return false
                }
            })
            .preload() // 预加载（或使用into()强制加载，此处用preload更轻量）
    }


    override fun setListener() {
        // 可添加其他全局监听（如顶部标题栏点击等）
    }

    override fun initData() {
        // 额外初始化数据（如本地缓存读取等）
    }

    override fun onResume() {
        super.onResume()
        //限制只有在播放页才能切换viewpager2
        (requireActivity() as? MyMusicActivity)?.onPlaybackPageVisible(true)
    }

    override fun onPause() {
        super.onPause()
        (requireActivity() as? MyMusicActivity)?.onPlaybackPageVisible(false)
        // 切换到别的页面-》暂停播放（保留状态）--一直播放
//        if (currentCenterPosition != -1) {
//            val currentMusic = musicList[currentCenterPosition]
//            if (currentMusic.isVideo) {
//                pauseVideo(currentCenterPosition)
//            }
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 解绑服务
        requireContext().unbindService(serviceConnection)
    }


}
