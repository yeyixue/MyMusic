package com.example.mymusic.viewmodel.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.example.mymusic.BuildConfig
import com.example.mymusic.adapter.MusicRecycleViewAdapter
import com.example.mymusic.repo.entity.MusicInfo
import com.example.mymusic.repo.playlist.PlaylistChangeListener
import com.example.mymusic.repo.playlist.PlaylistRepository
import com.example.mymusic.repo.remote.ApiService
import com.example.mymusic.repo.remote.RetrofitConnection
import com.example.mymusic.viewmodel.BaseViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class MainMusicViewModel : BaseViewModel() {

    private val apiService: ApiService = RetrofitConnection.apiService

    companion object {
        private const val TAG = "MyMusic"
        internal var isUpdatingSingleSong = false
        internal var isUpdatingByNotification = false

    }

    // 标记是否正在更新单首歌曲信息
    var mPlaylistRepository: PlaylistRepository = PlaylistRepository

    private val disposables = CompositeDisposable()
    private val _playlist = MutableLiveData<List<MusicInfo>>(emptyList())
    val playlist: LiveData<List<MusicInfo>> = _playlist
    private val playlistListener = object : PlaylistChangeListener {
        override fun onPlaylistChanged(newPlaylist: List<MusicInfo>) {
            Log.d("MainMusicViewModel", "旧歌单: ${_playlist.value}")
            Log.d("MainMusicViewModel", "新歌单: $newPlaylist")
            _playlist.value = newPlaylist
            Log.d("MainMusicViewModel", "收到新的歌单: ${newPlaylist.size} 首")
        }
    }

    init {
        // 仅注册一次
        PlaylistRepository.addListener(playlistListener)
    }

    // 当前选中的音乐id（使用LiveData便于UI观察变化）
    private val _currentMusicId = MutableLiveData<String?>(null)
    val currentMusicId: LiveData<String?> = _currentMusicId

    private val _isPlaying = MutableLiveData<Boolean>(false)
    val isPlaying: LiveData<Boolean> = _isPlaying

    // 播放器核心
//    lateinit var mediaPlayer: ExoPlayer

//    internal val mediaPlayer = MediaPlayer()
//    private var isMediaPrepared = false // 标记播放器是否准备完成
    // 缓冲进度 LiveData
    private val _bufferProgress = MutableLiveData<Int>(0)
    val bufferProgress: LiveData<Int> = _bufferProgress

    private val progressHandler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null // 用变量保存当前任务

    // 通知Fragment滚动到指定位置的LiveData
    private val _scrollToPosition = MutableLiveData<Int?>()
    val scrollToPosition: LiveData<Int?> = _scrollToPosition

    var coverResId: Int? = null // 音乐封面资源ID（mipmap/img*）
    var coverBitmap: Bitmap? = null // 视频封面（第一帧截图）
    // 初始化播放器时，确保播放完成监听正确绑定


    // 更新当前音乐ID的方法
    fun setCurrentMusicId(id: String?) {
        _currentMusicId.value = id
    }

    // 更新播放状态的方法（配合Fragment中的播放/暂停逻辑）
    fun setPlayingState(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }


    // 播放指定歌曲
//    fun playMusic(music: MusicInfo) {
//        try {
//            // 关键：无论当前状态如何，强制停止并重置播放器（确保新歌曲从0开始）
//            if (mediaPlayer.isPlaying) {
//                mediaPlayer.stop()
//            }
//
//            mediaPlayer.reset()
//            isMediaPrepared = false // 重置准备状态
//
//            // 设置新歌曲数据源
//            val audioUrl = getAudioUrlBySongId(music.songId)
//            mediaPlayer.setDataSource(audioUrl)
//            // setDataSource之后再prepareAsync
//            mediaPlayer.prepareAsync()
//
//            // 1. 准备完成监听器
//            mediaPlayer.setOnPreparedListener {
//                isMediaPrepared = true
//                // 标记当前播放的是音乐
//                isPlayingVideo = false
//
//                // 从0开始播放
//                mediaPlayer.start()
//                setPlayingState(true)
//                setCurrentMusicId(music.songId.toString())
//
//                // 启动统一进度更新 --start之后启动
//                startProgressUpdates()
//            }
//            // 2. 缓冲进度监听器
//            mediaPlayer.setOnBufferingUpdateListener { _, percent ->
//                Log.d("Music"," mediaPlayer.setOnBufferingUpdateListener 更新缓冲进度是$percent")
//                _bufferProgress.postValue(percent)
//            }
//
//            // 3. 播放完成监听器（确保下一首能正常切换）
//            mediaPlayer.setOnCompletionListener {
//                Log.d("PlayCompletion", "音乐正常播放完成，切到下一首")
//                playNextSong()
//            }
//
//            // 4. 错误监听器
//            mediaPlayer.setOnErrorListener { mp, what, extra ->
//                Log.e("PlayError", "音乐播放错误: what=$what, extra=$extra")
//                true // 消费错误
//            }
//
//        } catch (e: Exception) {
//            Log.e(TAG, "播放失败: ${e.message}", e)
//            setPlayingState(false)
//        }
//    }

    // ViewModel 中播放音乐的方法（示例）
    fun playMusic(music: MusicInfo) {
        val audioUrl = getAudioUrlBySongId(music.songId)
        val mediaItem = MediaItem.Builder()
            .setUri(audioUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(music.title)        // 通知标题
                    .setArtist(music.singer)      // 通知歌手
                    .build()
            )
            .build()
        sharedPlayer.setMediaItem(mediaItem)
        sharedPlayer.prepare()
        sharedPlayer.playWhenReady = true
        isPlayingVideo = false
        setPlayingState(true)
        setCurrentMusicId(music.songId.toString())
        startProgressUpdates() // 启动进度更新
    }



    // 根据百分比跳转进度（适配SmartSeekBar的0-100进度，统一调用新的seekTo）
    fun seekToPercent(percent: Int) {
        // 视频按百分比跳转
        if (::sharedPlayer.isInitialized) {
            val duration = sharedPlayer.duration
            val targetPosition = (percent * duration / 100f).toLong()
            seekTo(targetPosition) // 调用统一方法
        }
    }

    /**
     * 统一的音频进度跳转方法（同时支持音乐和视频）
     * 还需要处理动画逻辑--放在seekBar的Event事件了！！！
     * @param position 目标进度（毫秒，Long类型兼容大时长视频）
     */
    fun seekTo(position: Long) {

        // 视频进度跳转（使用ExoPlayer）
        if (::sharedPlayer.isInitialized&&
            sharedPlayer.playbackState != Player.STATE_IDLE &&
            sharedPlayer.duration >= 0) {
            // 确保进度在有效范围内（0 <= position <= 总时长）
            val validPosition = position.coerceIn(0, sharedPlayer.duration)
            sharedPlayer.seekTo(validPosition)
            Log.d("SeekTo", "视频跳转至: $validPosition 毫秒")
        }

    }
    private val _playNextEvent = MutableLiveData<Int?>()
    val playNextEvent: LiveData<Int?> = _playNextEvent
    // 播放下一首（完善逻辑）
    internal fun playNextSong() {
        val currentList = _playlist.value ?: return
        val currentId = _currentMusicId.value ?: return

        // 1. 计算当前歌曲位置
        val currentIndex = currentList.indexOfFirst { it.songId.toString() == currentId }
        if (currentIndex == -1) return // 未找到当前歌曲，直接返回

        // 2. 计算下一首位置（循环播放：最后一首的下一首是第一首）
        val nextIndex = (currentIndex.toInt() + 1) % currentList.size
        val nextMusic = currentList[nextIndex]

        // 3. 更新当前播放ID并播放下一首
        setCurrentMusicId(nextMusic.songId.toString())

        // 4. 通知Fragment滚动到下一首的位置（触发自动翻页）--自动播放
        _scrollToPosition.value = nextIndex
    }
    // 播放上一首
    internal fun playLastSong() {
        val currentList = _playlist.value ?: return
        val currentId = _currentMusicId.value ?: return

        // 1. 计算当前歌曲位置
        val currentIndex = currentList.indexOfFirst { it.songId.toString() == currentId }
//        if (currentIndex == -1) return // 未找到当前歌曲，直接返回

        // 2. 计算下一首位置（循环播放：最后一首的下一首是第一首）
        val nextIndex = (currentIndex.toInt() - 1+currentList.size) % currentList.size
        val nextMusic = currentList[nextIndex]

        // 3. 更新当前播放ID并播放下一首
        setCurrentMusicId(nextMusic.songId.toString())

        // 4. 通知Fragment滚动到下一首的位置（触发自动翻页）--自动播放
        _scrollToPosition.value = nextIndex
    }


    // 清除滚动指令（避免重复触发）
    fun clearScrollCommand() {
        _scrollToPosition.value = null
    }

    // 时间格式化（毫秒转mm:ss）
    fun formatTime(millis: Int): String {
        if (millis < 0) return "00:00"
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis.toLong()).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    // 获取音频地址（模拟）
    private fun getAudioUrlBySongId(songId: Int): String {
        // 实际项目替换为真实接口返回的音频URL
        return "http://${RetrofitConnection.ip}:8000/audio/$songId.mp3"
    }

    // 初始化歌单
    fun setPlayListDefault() {
        val disposable = apiService.getSong()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ resp ->
                if (resp.code == 200) {
                    val newList = resp.content
                    // 遍历歌单，为音频生成封面（视频后续预加载）
                    val processedList = newList.map { music ->
                        if (!music.isVideo) {
                            // 音频：生成随机封面资源ID并保存
//                            val randomIndex = (0..22).random() // 与适配器中一致的随机逻辑
//                            val resName = "img$randomIndex"
//                            val context = getApplication<Application>().applicationContext
//                            val resId = context.resources.getIdentifier(
//                                resName, "mipmap", context.packageName
//                            ).takeIf { it != 0 } ?: R.mipmap.img0
//                            music.copy(coverResId = resId) // 保存封面资源ID
                        } else {
                            // 视频：初始封面为null，后续预加载
                            music.copy(coverBitmap = null, isCoverLoaded = false)
                        }
                    }
                    _playlist.value = newList // 更新歌单
                    PlaylistRepository.mPlaylist= newList as MutableList<MusicInfo>// 更新仓库
                } else {
                    Log.e(TAG, "setPlayListDefault: server returned code=${resp.code}, message=${resp.message}")
                }
            }, { error ->
                Log.e(TAG, "setPlayListDefault error: ", error)
            })

        disposables.add(disposable)
    }


    /**
     * 视频处理逻辑
     * 在 ViewModel 中封装方法：根据 songId 返回视频 URL
     * 在 Adapter.VideoViewHolder 中调用并播放视频
     * 绑定数据后自动播放视频
     */
    // 播放进度LiveData，Pair的第一个值是当前位置(ms)，第二个值是总时长(ms)
    val progressLiveData = MutableLiveData<Pair<Long, Long>>()
    // 标记当前是否在播放视频（默认播放音乐）--用来区分是否在播放视频而不是标记视频的播放状态
    internal var isPlayingVideo = false
    lateinit var sharedPlayer: ExoPlayer

    fun initPlayer(context: Context) {
        if (!::sharedPlayer.isInitialized) { // 确保只初始化一次
            sharedPlayer = ExoPlayer.Builder(context).build()
            // 添加基础监听（如播放完成）
            sharedPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        playNextSong()
                    }
                }
                // 错误监听
                override fun onPlayerError(error: PlaybackException) {
                    Log.e("PlayError", "视频播放错误: ${error.message}", error)
                    // 错误时不切歌，可暂停播放
                    pauseMusicOrVideo()
                }
            })
        }
    }
    // 切换播放状态（统一控制：音乐/视频）
    fun togglePlaying() {
        val currentState = _isPlaying.value == true
        _isPlaying.value = !currentState // 切换状态

        if (currentState) {
           pauseMusicOrVideo() // 暂停音乐
        } else {
            // 当前是暂停状态 → 继续播放
           resumeMusicOrVideo() // 继续音乐
        }
    }

    fun resumeMusicOrVideo() {
        if (::sharedPlayer.isInitialized && !sharedPlayer.isPlaying) {
            sharedPlayer.play()
            _isPlaying.value = true
        }
    }


    // 视频开始播放时调用（在Adapter或Fragment播放视频时调用）
    fun startVideoPlayback() {
        isPlayingVideo = true // 标记当前播放的是视频
        _isPlaying.value = true
    }


    fun getVideoUrlBySongId(songId: Int): String {
        val ip = BuildConfig.SERVER_IP
        return "http://$ip:8000/video/$songId.mp4"
    }


    fun pauseMusicOrVideo() {
        if (::sharedPlayer.isInitialized && sharedPlayer.isPlaying) {
            sharedPlayer.pause() // 暂停而非停止，保留播放进度
            _isPlaying.value = false
        }
    }


    /**
     * 修改startProgressUpdates()，同时支持音频和视频
     *
     */
    fun startProgressUpdates() {
        // 关键：停止旧任务，避免重复发送
        stopProgressUpdates()

        progressRunnable = object : Runnable {
            override fun run() {
                if (::sharedPlayer.isInitialized) {
                    // 视频进度
                    val currentPos = sharedPlayer.currentPosition
                    val duration = sharedPlayer.duration
                    // 过滤无效时长（duration < 0 时不发送更新）
                    if (duration >= 0) {
                        progressLiveData.postValue(Pair(currentPos, duration))
                    } else {
                        Log.d("ProgressUpdate", "视频时长无效: $duration，跳过更新")
                    }
                }
                // 仅在播放状态下继续更新--删除&& mediaPlayer.isPlaying 让暂停状态也能更新进度
                progressHandler.postDelayed(this, 1000)
            }
        }
        progressHandler.post(progressRunnable!!)
    }

    // 停止进度更新（必须调用）
    fun stopProgressUpdates() {
        progressRunnable?.let {
            progressHandler.removeCallbacks(it)
            progressRunnable = null
        }
    }
    // 一个统一的方法来切换播放类型
    fun switchToVideoPlayback() {
        // 停止当前所有播放
        if (sharedPlayer.isPlaying) {
            sharedPlayer.pause()
        }

        // 重置视频播放器状态
        sharedPlayer.stop()
        sharedPlayer.clearMediaItems()

        // 标记当前为视频播放状态
        isPlayingVideo = true
    }

    // 一个统一的方法来切换播放类型
    fun switchToAudioPlayback() {
        // 停止当前视频播放
        sharedPlayer.stop()
        // 标记当前为音频播放状态
        isPlayingVideo = false
    }


    // 播放视频时更新状态
    fun playVideo(position: Int, currentMusic:MusicInfo,  mRecyclerView: RecyclerView,currentCenterPosition:Int) {

        // 防止视频出现在别的页面
        val oldHolder = mRecyclerView.findViewHolderForAdapterPosition(currentCenterPosition)
        if (oldHolder is MusicRecycleViewAdapter.VideoViewHolder) {
            oldHolder.playerView.player = null // 强制解除旧视图绑定
            Log.d("PlayVideo", "解绑上一个页面的 PlayerView: $currentCenterPosition")
        }
        // // 1. 获取当前可见的ViewHolder（必须是可见的，未被回收）---因为要处理视频封面
        val viewHolder = mRecyclerView.findViewHolderForAdapterPosition(position)
        if (viewHolder !is MusicRecycleViewAdapter.VideoViewHolder) {
            Log.w("PlayVideo", "ViewHolder已被回收或不可见，无法播放")
            return
        }
        switchToVideoPlayback() // 切换到视频

        // 1. 重置共享播放器状态
        val player = sharedPlayer
        player.stop() // 停止当前播放
        player.clearMediaItems() // 清除旧媒体

        // 2. 绑定新页面的 PlayerView
        viewHolder.playerView.visibility = View.VISIBLE
        viewHolder.thumbnailImageView.visibility = View.GONE
        viewHolder.playerView.player = player // 绑定新视图

        // 3. 加载新视频
        val videoUrl = getVideoUrlBySongId(currentMusic.songId)
        val mediaItem = MediaItem.fromUri(videoUrl)
        player.setMediaItem(mediaItem)

        // 4. 准备并播放
        player.prepare()
        player.playWhenReady = true
        // 更新当前播放位置
        setCurrentMusicId(currentMusic.songId.toString())

        // 标记当前播放的是视频
        startVideoPlayback()

        // 开始监听进度
        startProgressUpdates()
    }


    override fun onCleared() {
        super.onCleared()
        disposables.clear()
        // 释放播放器资源
        if (::sharedPlayer.isInitialized) {
            sharedPlayer.release()
        }
        PlaylistRepository.removeListener(@SuppressLint("ImplicitSamInstance")
        object : PlaylistChangeListener {
            override fun onPlaylistChanged(newPlaylist: List<MusicInfo>) {
                // 空实现，仅用于移除
            }
        })

    }

    // 新增：更新播放列表中指定歌曲的信息（如点赞状态）
    fun updateMusicInfo(updatedMusic: MusicInfo) {
        isUpdatingSingleSong=true
        val currentList = _playlist.value?.toMutableList() ?: return
        val index = currentList.indexOfFirst { it.songId == updatedMusic.songId }
        if (index != -1) {
            currentList[index] = updatedMusic
            _playlist.value = currentList  // 触发 LiveData 通知
            mPlaylistRepository.updateMusicInfo(updatedMusic)
        }
    }



}

