package com.example.mymusic.viewmodel.fragment

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymusic.BuildConfig
import com.example.mymusic.adapter.MusicRecycleViewAdapter
import com.example.mymusic.repo.entity.MusicInfo
import com.example.mymusic.repo.remote.ApiService
import com.example.mymusic.repo.remote.RetrofitConnection
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class MainMusicViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService: ApiService = RetrofitConnection.apiService

    companion object {
        private const val TAG = "MyMusic"
    }
    //背景图片
    private val _randomBgName = MutableLiveData<String>()
    val randomBgName: LiveData<String> = _randomBgName

    private val disposables = CompositeDisposable()
    private val _playlist = MutableLiveData<List<MusicInfo>>(emptyList())
    val playlist: LiveData<List<MusicInfo>> = _playlist

    // 当前选中的音乐id（使用LiveData便于UI观察变化）
    private val _currentMusicId = MutableLiveData<String?>(null)
    val currentMusicId: LiveData<String?> = _currentMusicId

    private val _isPlaying = MutableLiveData<Boolean>(false)
    val isPlaying: LiveData<Boolean> = _isPlaying

    // 播放器核心
    private val mediaPlayer = MediaPlayer()
    private var isMediaPrepared = false // 标记播放器是否准备完成
    // 缓冲进度 LiveData
    private val _bufferProgress = MutableLiveData<Int>(0)
    val bufferProgress: LiveData<Int> = _bufferProgress

    // 进度与时间相关LiveData（供UI层观察）
    private val _totalDuration = MutableLiveData<Int>(0) // 总时长（毫秒）
    val totalDuration: LiveData<Int> = _totalDuration

    private val _currentPosition = MutableLiveData<Int>(0) // 当前进度（毫秒）
    val currentPosition: LiveData<Int> = _currentPosition

    private val _currentProgressPercent = MutableLiveData<Int>(0) // 当前进度（0-100百分比）
    val currentProgressPercent: LiveData<Int> = _currentProgressPercent

    private val _formattedCurrentTime = MutableLiveData<String>("00:00") // 格式化当前时间（mm:ss）
    val formattedCurrentTime: LiveData<String> = _formattedCurrentTime

    private val _formattedTotalTime = MutableLiveData<String>("00:00") // 格式化总时间（mm:ss）
    val formattedTotalTime: LiveData<String> = _formattedTotalTime
    // 进度更新定时器
    private var progressTimer: CountDownTimer? = null

    // 新增：通知Fragment滚动到指定位置的LiveData
    private val _scrollToPosition = MutableLiveData<Int?>()
    val scrollToPosition: LiveData<Int?> = _scrollToPosition


    // 初始化播放器时，确保播放完成监听正确绑定
    init {
        mediaPlayer.setOnCompletionListener {
            playNextSong() // 播放完成后自动触发下一首
        }
        // 设置缓冲监听
        mediaPlayer.setOnBufferingUpdateListener { _, percent ->
            Log.d("Music"," mediaPlayer.setOnBufferingUpdateListener 更新缓冲进度是$percent")
            _bufferProgress.postValue(percent)
        }
        mediaPlayer.setOnCompletionListener {
            Log.d("PlayCompletion", "音乐正常播放完成，切到下一首")
            playNextSong()
        }
        // 新增错误监听
        mediaPlayer.setOnErrorListener { mp, what, extra ->
            Log.e("PlayError", "音乐播放错误: what=$what, extra=$extra")
            true // 消费错误，不触发完成监听
        }
    }
    // 添加方法来更新播放状态
    fun setPlaying(playing: Boolean) {
        _isPlaying.value = playing
    }

    // 切换播放状态
//    fun togglePlaying() {
//        val current = _isPlaying.value ?: false
//        _isPlaying.value = !current
//
//        // 控制实际的音乐播放
//        if (_isPlaying.value == true) mediaPlayer.start()
//        else mediaPlayer.pause()
//
//        Log.d("MainMusicViewModel", "Toggle playing to: ${_isPlaying.value}")
//    }
    // 传递名称字符串
    fun generateRandomBgName() {
        val randomIndex = Random.nextInt(0, 5)
        val bgName = "bg$randomIndex"
        _randomBgName.value = bgName // 传递"bg0"等字符串
    }
    // 新增：更新当前音乐ID的方法
    fun setCurrentMusicId(id: String?) {
        _currentMusicId.value = id
    }

    // 新增：更新播放状态的方法（配合Fragment中的播放/暂停逻辑）
    fun setPlayingState(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }


    // 播放指定歌曲
    fun playMusic(music: MusicInfo) {
        try {
            // 关键：无论当前状态如何，强制停止并重置播放器（确保新歌曲从0开始）
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.reset()
            isMediaPrepared = false // 重置准备状态

            // 设置新歌曲数据源
            val audioUrl = getAudioUrlBySongId(music.songId)
            mediaPlayer.setDataSource(audioUrl)

            // 异步准备并播放
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnPreparedListener {
                isMediaPrepared = true
                // 标记当前播放的是音乐
                isPlayingVideo = false
                // 新歌曲总时长
                val duration = mediaPlayer.duration
                _totalDuration.value = duration
                _formattedTotalTime.value = formatTime(duration)

                // 从0开始播放
                mediaPlayer.start()
                setPlayingState(true)

                // 重置进度相关数据（确保从0开始）
                _currentPosition.value = 0
                _currentProgressPercent.value = 0
                _formattedCurrentTime.value = "00:00"

                // 启动进度更新
                startProgressTimer()
            }

        } catch (e: Exception) {
            Log.e(TAG, "播放失败: ${e.message}", e)
            setPlayingState(false)
        }
    }

    // 暂停播放
    fun pauseMusic() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            setPlayingState(false)
            stopProgressTimer() // 暂停时停止进度更新
        }
    }

    // 继续播放
    fun resumeMusic() {
        if (!mediaPlayer.isPlaying && isMediaPrepared) {
            mediaPlayer.start()
            setPlayingState(true)
            startProgressTimer() // 恢复进度更新
        }
    }

    // 根据百分比跳转进度（适配SmartSeekBar的0-100进度，统一调用新的seekTo）
    fun seekToPercent(percent: Int) {
        if (isPlayingVideo) {
            // 视频按百分比跳转
            if (::sharedPlayer.isInitialized) {
                val duration = sharedPlayer.duration
                val targetPosition = (percent * duration / 100f).toLong()
                seekTo(targetPosition) // 调用统一方法
            }
        } else {
            // 音乐按百分比跳转
            if (!isMediaPrepared) return
            val targetMillis = percentToMillis(percent)
            seekTo(targetMillis.toLong()) // 调用统一方法
        }
    }

    /**
     * 统一的进度跳转方法（同时支持音乐和视频）
     * 还需要处理动画逻辑
     * @param position 目标进度（毫秒，Long类型兼容大时长视频）
     */
    fun seekTo(position: Long) {
        if (isPlayingVideo) {
            // 视频进度跳转（使用ExoPlayer）
            if (::sharedPlayer.isInitialized) {
                // 确保进度在有效范围内（0 <= position <= 总时长）
                val validPosition = position.coerceIn(0, sharedPlayer.duration)
                sharedPlayer.seekTo(validPosition)
                Log.d("SeekTo", "视频跳转至: $validPosition 毫秒")
            }
        } else {
            // 音乐进度跳转（使用MediaPlayer）
            if (isMediaPrepared) {
                // 转换为Int（音乐时长通常不超过Int范围，超出则取最大值）
                val validPosition = position.coerceIn(0, _totalDuration.value?.toLong() ?: 0L).toInt()
                if (validPosition in 0..(_totalDuration.value ?: 0)) {
                    mediaPlayer.seekTo(validPosition)
                    // 同步更新音乐进度相关LiveData
                    _currentPosition.value = validPosition
                    _currentProgressPercent.value = millisToPercent(validPosition)
                    _formattedCurrentTime.value = formatTime(validPosition)
                    Log.d("SeekTo", "音乐跳转至: $validPosition 毫秒")
                }
            } else {
                Log.w("SeekTo", "音乐播放器未准备完成，无法跳转")
            }
        }
    }

    // 播放下一首（完善逻辑）
    private fun playNextSong() {
        val currentList = _playlist.value ?: return
        val currentId = _currentMusicId.value ?: return

        // 1. 计算当前歌曲位置
        val currentIndex = currentList.indexOfFirst { it.songId.toString() == currentId }
        if (currentIndex == -1) return // 未找到当前歌曲，直接返回

        // 2. 计算下一首位置（循环播放：最后一首的下一首是第一首）
        val nextIndex = (currentIndex + 1) % currentList.size
        val nextMusic = currentList[nextIndex]

        // 3. 更新当前播放ID并播放下一首
        setCurrentMusicId(nextMusic.songId.toString())
        playMusic(nextMusic)

        // 4. 通知Fragment滚动到下一首的位置（触发自动翻页）
        _scrollToPosition.value = nextIndex
    }
    // 清除滚动指令（避免重复触发）
    fun clearScrollCommand() {
        _scrollToPosition.value = null
    }

    // 启动进度更新定时器（每秒更新一次）
    private fun startProgressTimer() {
        stopProgressTimer() // 先停止已有定时器
        progressTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (mediaPlayer.isPlaying) {
                    val currentMillis = mediaPlayer.currentPosition
                    // 更新毫秒进度
                    _currentPosition.value = currentMillis
                    // 计算并更新百分比进度（0-100）
                    _currentProgressPercent.value = millisToPercent(currentMillis)
                    // 格式化当前时间并更新
                    _formattedCurrentTime.value = formatTime(currentMillis)
                }
            }

            override fun onFinish() {}
        }.start()
    }

    // 停止进度更新定时器
    private fun stopProgressTimer() {
        progressTimer?.cancel()
        progressTimer = null
    }

    // 毫秒转百分比（0-100）
    private fun millisToPercent(millis: Int): Int {
        val total = _totalDuration.value ?: 0
        return if (total == 0) 0 else (millis * 100f / total).toInt().coerceIn(0, 100)
    }

    // 百分比转毫秒
    private fun percentToMillis(percent: Int): Int {
        val total = _totalDuration.value ?: 0
        return (percent * total / 100f).toInt().coerceIn(0, total)
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
                    _playlist.value = resp.content
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
    // 新增：标记当前是否在播放视频（默认播放音乐）
    private var isPlayingVideo = false
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
                // 新增错误监听
                override fun onPlayerError(error: PlaybackException) {
                    Log.e("PlayError", "视频播放错误: ${error.message}", error)
                    // 错误时不切歌，可暂停播放
                    pauseVideo()
                }
            })
        }
    }
    // 切换播放状态（统一控制：音乐/视频）
    fun togglePlaying() {
        val currentState = _isPlaying.value ?: false
        _isPlaying.value = !currentState // 切换状态

        if (currentState) {
            // 当前是播放状态 → 暂停
            if (isPlayingVideo) {
                pauseVideo() // 暂停视频
            } else {
                pauseMusic() // 暂停音乐
            }
        } else {
            // 当前是暂停状态 → 继续播放
            if (isPlayingVideo) {
                resumeVideo() // 继续视频
            } else {
                resumeMusic() // 继续音乐
            }
        }
    }


    // 继续播放视频
    fun resumeVideo() {
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
    fun pauseVideo() {
        if (::sharedPlayer.isInitialized && sharedPlayer.isPlaying) {
            sharedPlayer.pause() // 暂停而非停止，保留播放进度
            _isPlaying.value = false
        }
    }
    // 开始监听播放器进度
    fun startProgressUpdates() {
        if (!::sharedPlayer.isInitialized) return

        // 使用Handler定期更新进度
        val handler = Handler(Looper.getMainLooper())
        val updateProgressRunnable = object : Runnable {
            override fun run() {
                if (sharedPlayer.isPlaying) {
                    val currentPosition = sharedPlayer.currentPosition
                    val duration = sharedPlayer.duration
//                    Log.d("ProgressUpdate", "当前进度: $currentPosition, 总时长: $duration")
                    // 通过LiveData通知Fragment更新进度条
                    progressLiveData.postValue(Pair(currentPosition, duration))
                }
                handler.postDelayed(this, 1000) // 每秒更新一次
            }
        }

        // 开始更新
        handler.post(updateProgressRunnable)
    }

    // 跳转到指定位置
//    fun seekTo(position: Long) {
//        if (::sharedPlayer.isInitialized && sharedPlayer.isPlaying) {
//            sharedPlayer.seekTo(position)
//        }
//    }
















    override fun onCleared() {
        super.onCleared()
        disposables.clear()
        // 释放播放器资源
        if (::sharedPlayer.isInitialized) {
            sharedPlayer.release()
        }
    }



}
