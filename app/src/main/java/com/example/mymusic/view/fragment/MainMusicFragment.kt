package com.example.mymusic.view.fragment

import android.media.AudioManager
import android.os.Handler
import android.os.Looper
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
import com.example.mymusic.adapter.MusicRecycleViewAdapter.ProgressListenerManager
import com.example.mymusic.repo.entity.MusicInfo
import com.example.mymusic.view.SmartSeekBar
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
    // 在 setupRecyclerView() 的滚动监听中添加
    // 记录当前播放的歌曲ID和中心页面位置
    private val _currentPlayingSongId = MutableLiveData<String?>()
    var currentPlayingSongId: LiveData<String?> = _currentPlayingSongId
    private var currentCenterPosition: Int = 0

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
        // 启动进度监听
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
                        mMainMusicViewModel.playVideo(currentCenterPosition,firstMusic,mRecyclerView)
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


        // 设置进度更新监听,这是adapter的拖动回调处理逻辑--播放进度更新接口
        mMusicRecycleViewAdapter.setOnPlayProgressListener(object : ProgressListenerManager.OnPlayProgressListener {
            override fun onProgressUpdate(position: Int, progress: Int, currentTime: String, totalTime: String) {
                // 用户拖动进度条时，更新ViewModel  pos
                val musicInfo = musicList[position]
                mMainMusicViewModel.setCurrentMusicId(musicInfo.songId.toString())
                // 滑动开始时暂停播放
//                if (musicInfo.isVideo) {
//                    mMainMusicViewModel.pauseVideo()
//                } else {
//                    mMainMusicViewModel.pauseMusic()
//                }
                // 无论播放/暂停，都强制跳转进度
                mMainMusicViewModel.seekToPercent(progress)
                Log.d("MyMusic","无论播放/暂停，都强制跳转进度mMainMusicViewModel.seekToPercent(progress)")

                // 手动更新进度条UI（避免被ViewModel的旧进度覆盖）
//                mMusicRecycleViewAdapter.updateItemProgress(position, progress, currentTime)
//                if (musicInfo.isVideo) {
//                    mMainMusicViewModel.resumeVideo()
//                } else {
//                    mMainMusicViewModel.resumeMusic()
//                }

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
                        val viewHolder = mRecyclerView.findViewHolderForAdapterPosition(currentCenterPosition)

                        val newMusic = musicList[newCenterPosition]
                        Log.d("PositionUpdate", "滚动完成，旧位置: $currentCenterPosition → 新位置: $newCenterPosition")
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
                        // 更新当前播放ID
                        _currentPlayingSongId.value = newMusic.songId.toString()
//                        mMusicRecycleViewAdapter.currentCenterPosition = currentCenterPosition
                        if(newMusic.isVideo==false){
                            mMainMusicViewModel.switchToAudioPlayback()
                            //播放音乐
                            // 只有当新页面的歌曲ID与当前播放的不同时才切换播放
                            if (newMusic.songId.toString() != _currentPlayingSongId.value) {
                                // 暂停当前播放
//                                mMainMusicViewModel.pauseMusic()
                                // 播放新歌曲
                                mMainMusicViewModel.setCurrentMusicId(newMusic.songId.toString())
                                mMainMusicViewModel.playMusic(newMusic)

//                                mMainMusicViewModel.startProgressUpdates()
                            }
                        }else{
                            //处理播放视频
//                            mMainMusicViewModel.pauseVideo()
//                            Log.d("Mymusic","playVideo(currentCenterPosition) currentCenterPosition是 $currentCenterPosition ")
                            mMainMusicViewModel.playVideo(currentCenterPosition,newMusic,mRecyclerView)
//                            mMainMusicViewModel.startProgressUpdates()
                        }

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
            // 格式化当前时间（复用ViewModel的格式化方法）
            val formattedTime = mMainMusicViewModel.formatTime(currentPosition.toInt())
            // 获取当前中心位置（音频和视频共用同一个位置标记）
//            val currentPlayingPosition = mMusicRecycleViewAdapter.currentCenterPosition

            // 日志调试：区分音频/视频进度
            val mediaType = if (mMainMusicViewModel.isPlayingVideo) "视频" else "音频"
//            Log.d("ProgressUpdate", "$mediaType 进度: $progressPercent%，位置: $currentPlayingPosition")
//            Log.d("ProgressUpdate", "currentPlayingPosition: $currentPlayingPosition，progressPercent: $progressPercent")

            // 更新进度条（统一调用适配器方法）
            mMusicRecycleViewAdapter.updateItemProgress(
                currentCenterPosition,
                progressPercent,
                formattedTime
            )
        }
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
