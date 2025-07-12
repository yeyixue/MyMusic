package com.example.mymusic.viewmodel.fragment

import android.media.MediaPlayer
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymusic.adapter.MusicRecycleViewAdapter
import com.example.mymusic.repo.entity.MusicInfo
import com.example.mymusic.repo.remote.ApiService
import com.example.mymusic.repo.remote.RetrofitConnection
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class MainMusicViewModel : ViewModel() {

    private val apiService: ApiService = RetrofitConnection.apiService

    companion object {
        private const val TAG = "MyMusic"
    }

    private val disposables = CompositeDisposable()
    private val _playlist = MutableLiveData<List<MusicInfo>>(emptyList())
    val playlist: LiveData<List<MusicInfo>> = _playlist

    // 当前选中的音乐id（使用LiveData便于UI观察变化）
    private val _currentMusicId = MutableLiveData<String?>(null)
    val currentMusicId: LiveData<String?> = _currentMusicId

    private val _isPlaying = MutableLiveData<Boolean>(false)
    val isPlaying: LiveData<Boolean> = _isPlaying

    // 播放器实例（简化示例，实际需封装为单例或使用服务）
    private val mediaPlayer = MediaPlayer()
    // 歌曲总时长（毫秒）
    private val _totalDuration = MutableLiveData<Int>(0)
    val totalDuration: LiveData<Int> = _totalDuration
    // 当前播放进度（毫秒）
    private val _currentPosition = MutableLiveData<Int>(0)
    val currentPosition: LiveData<Int> = _currentPosition
    // 进度更新定时器
    private var progressTimer: CountDownTimer? = null


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
            // 停止当前播放
            mediaPlayer.stop()
            mediaPlayer.reset()
            // 模拟歌曲地址（实际需替换为真实音频URL）
            val audioUrl = getAudioUrlBySongId(music.songId) // 需实现：根据ID获取音频地址
            mediaPlayer.setDataSource(audioUrl)
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnPreparedListener {
                // 准备完成后开始播放
                _totalDuration.value = mediaPlayer.duration // 设置总时长
                mediaPlayer.start()
                setPlayingState(true)
                // 启动进度更新定时器（每秒刷新一次）
                startProgressTimer()
            }
            // 监听播放完成（自动播放下一首）
            mediaPlayer.setOnCompletionListener {
                playNextSong()
            }
        } catch (e: Exception) {
            Log.e(TAG, "播放失败：${e.message}")
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
        if (!mediaPlayer.isPlaying && mediaPlayer.duration > 0) {
            mediaPlayer.start()
            setPlayingState(true)
            startProgressTimer()
        }
    }

    // 拖动进度条更新播放位置
    fun seekTo(position: Int) {
        if (position in 0..(mediaPlayer.duration)) {
            mediaPlayer.seekTo(position)
            _currentPosition.value = position
        }
    }

    // 播放下一首
    private fun playNextSong() {
        val currentList = _playlist.value ?: return
        val currentId = _currentMusicId.value ?: return
        // 找到当前歌曲在列表中的位置
        val currentIndex = currentList.indexOfFirst { it.songId.toString() == currentId }
        if (currentIndex != -1) {
            // 切换到下一首（循环播放）
            val nextIndex = (currentIndex + 1) % currentList.size
            val nextMusic = currentList[nextIndex]
            setCurrentMusicId(nextMusic.songId.toString())
            playMusic(nextMusic)
        }
    }

    // 启动进度更新定时器
    private fun startProgressTimer() {
        stopProgressTimer() // 先停止已有定时器
        progressTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) { // 每秒更新一次
            override fun onTick(millisUntilFinished: Long) {
                if (mediaPlayer.isPlaying) {
                    _currentPosition.value = mediaPlayer.currentPosition
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

    // 根据歌曲ID获取音频地址（模拟实现）
    private fun getAudioUrlBySongId(songId: Int): String {
        // 实际项目中需替换为真实接口返回的音频URL
        return "http://example.com/audio/$songId.mp3"
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


    // 处理页面切换逻辑
    fun handlePageSwitch(recyclerView: RecyclerView, layoutManager: LinearLayoutManager) {
        // 获取第一个完全可见的 item 位置
        val firstCompletelyVisibleItemPosition = layoutManager.findFirstCompletelyVisibleItemPosition()

        // 如果没有完全可见的 item，获取第一个可见 item 的位置
        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
        val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

        // 如果列表为空，直接返回
        if (firstVisibleItemPosition == RecyclerView.NO_POSITION) return

        // 获取RecyclerView 的高度（用于计算中间位置）
        val recyclerViewHeight = recyclerView.height

        // 如果有完全可见的 item，直接滚动到该位置
        if (firstCompletelyVisibleItemPosition != RecyclerView.NO_POSITION) {
            return
        }

        // 如果没有完全可见的 item，计算哪个 item 超过了中间位置
        for (i in firstVisibleItemPosition..lastVisibleItemPosition) {
            val view = layoutManager.findViewByPosition(i) ?: continue

            // 计算 item 顶部距离RecyclerView 顶部的距离
            val top = recyclerView.getChildAt(0).top

            // 如果 item 顶部超过了RecyclerView 的中间位置，滚动到下一个 item
            if (top < -recyclerViewHeight / 2) {
                recyclerView.smoothScrollToPosition(i + 1)
                return
            }

            // 如果 item 底部超过了RecyclerView 的中间位置，滚动到当前 item
            val bottom = recyclerView.getChildAt(0).bottom
            if (bottom > recyclerViewHeight / 2) {
                recyclerView.smoothScrollToPosition(i)
                return
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

}
