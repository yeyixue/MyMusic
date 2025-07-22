package com.example.mymusic.Service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.example.mymusic.R
import com.example.mymusic.repo.entity.MusicInfo
import com.example.mymusic.view.activity.MyMusicActivity
import com.example.mymusic.viewmodel.fragment.MainMusicViewModel
import androidx.core.graphics.createBitmap

@UnstableApi
class NotificationService : Service() {
    private val CHANNEL_ID = "music_notification_channel"
    private val NOTIFICATION_ID = 10086

    // 动作常量（与按钮对应）
    private val ACTION_PREV = "action_prev"
    private val ACTION_PLAY_PAUSE = "action_play_pause"
    private val ACTION_NEXT = "action_next"
    private val ACTION_LIKE = "action_like"
    private val ACTION_SEEK = "action_seek" // 进度拖动动作
    private val EXTRA_SEEK_POSITION = "extra_seek_position" // 进度值参数

    // Media3 核心组件
    private lateinit var mediaSession: MediaSession
    private lateinit var notificationManager: NotificationManagerCompat

    // 业务变量
    private var mMainMusicViewModel: MainMusicViewModel? = null
    private var currentMusic: MusicInfo? = null
    private var albumArtBitmap: Bitmap? = null // 专辑封面

    // 进度相关变量
    private var currentProgressMs: Long = 0L // 当前进度（毫秒）
    private var totalDurationMs: Long = 0L   // 总时长（毫秒）
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (mMainMusicViewModel?.isPlaying?.value == true) {
                updateNotification()
                handler.postDelayed(this, 1000) // 每秒更新一次
            }
        }
    }

    inner class NotificationBinder : Binder() {
        fun setViewModel(viewModel: MainMusicViewModel) {
            mMainMusicViewModel = viewModel
//            initMediaSession() // 初始化 MediaSession
            initObservers()    // 监听播放状态和歌曲变化
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        createNotificationChannel() // 创建通知渠道
    }

    // 初始化 MediaSession（关联 ExoPlayer）
    private fun initMediaSession() {
        val player = mMainMusicViewModel?.sharedPlayer ?: return
        mediaSession = MediaSession.Builder(this, player).build()
    }

    // 监听播放状态和当前歌曲变化-暂停播放
    private fun initObservers() {
        mMainMusicViewModel?.apply {
            // 监听播放状态变化（更新播放/暂停按钮）
            isPlaying.observeForever { isPlaying ->
                updateNotification()
                if (isPlaying) {
                    handler.post(updateRunnable) // 开始播放时启动更新
                } else {
                    handler.removeCallbacks(updateRunnable) // 暂停时停止更新
                }
            }

            // 监听当前播放歌曲变化（更新曲目信息和封面）
            currentMusicId.observeForever { musicId ->
                musicId?.let { id ->
                    currentMusic = playlist.value?.find { it.songId.toString() == id }
                    currentMusic?.let { loadAlbumArt(it) } // 加载专辑封面---这里可能会有问题，因为封面加载可能需要时间，所以需要在loadAlbumArt中判断是否加载完成
                    updateNotification() // 刷新通知
                }
            }

            // 监听进度变化
            progressLiveData.observeForever { (currentMs, totalMs) ->
                currentProgressMs = currentMs
                totalDurationMs = totalMs
                // 实时更新通知进度
//                if (isPlaying.value == true) {
                updateNotification()
//                }
            }

            // 监听歌单变化（响应封面加载完成事件）
            playlist.observeForever { newPlaylist ->
                val currentId = currentMusicId.value // 获取当前播放的歌曲ID
                // 重新获取当前歌曲（此时可能已更新封面）
                val updatedMusic = newPlaylist.find { it.songId.toString() == currentId }
                if (updatedMusic != null) {
                    currentMusic = updatedMusic
                    loadAlbumArt(updatedMusic) // 重新加载封面（此时可能已加载完成）
                    updateNotification() // 刷新通知
                }
            }
        }
    }

    // 初始第二个视频封面可能没有加载完成，所以需要在 监听playList变化中又调用loadAlbumArt
    private fun loadAlbumArt(music: MusicInfo) {
        albumArtBitmap = if (music.isVideo) {
            if (music.isCoverLoaded) {
                // 封面已加载：使用实际封面或默认图
                music.coverBitmap ?: BitmapFactory.decodeResource(resources, R.mipmap.img2)
            } else {
                // 封面未加载：使用默认图并等待更新
                Log.w("NotificationService", "视频封面未加载完成，使用默认图 ${music.songId}")
                BitmapFactory.decodeResource(resources, R.mipmap.img2)
            }
        } else {
            val resId = music.coverResId ?: R.mipmap.img8
            BitmapFactory.decodeResource(resources, resId)
        }
    }


    // 更新通知（核心实现）
    private fun updateNotification() {
        currentMusic ?: return // 无当前歌曲时不更新

        // 计算进度百分比（0-100）
        val progress = if (totalDurationMs > 0) {
            (currentProgressMs * 100 / totalDurationMs).toInt()
        } else 0

        // 时间文本（当前时间 / 总时长）
        val currentTimeText = formatTime(currentProgressMs)
        val totalTimeText = formatTime(totalDurationMs)
        val isPlaying = mMainMusicViewModel?.isPlaying?.value ?: false

        // 加载自定义布局
        val remoteViews = RemoteViews(packageName, R.layout.music_notification).apply {
            // 更新歌曲信息
            setTextViewText(R.id.SongNameNotification, currentMusic?.title)
            setTextViewText(R.id.SongSingerNotification, currentMusic?.singer)
            setTextViewText(R.id.tvCurrentTimeNotification, currentTimeText)
            setTextViewText(R.id.totalTimeNotification, totalTimeText)
            setProgressBar(R.id.ssbNotification, 100, progress, false)
            setImageViewResource(R.id.lottieNotification,
                if (currentMusic?.liked == true) R.drawable.notificationlike else R.drawable.notificationdislike)

            // 更新播放/暂停图标
            val playPauseRes = if (isPlaying)
                R.drawable.notificationstart else R.drawable.notificationstop
            setImageViewResource(R.id.ImageViewNotificationStart, playPauseRes)

            // 更新专辑封面
            albumArtBitmap?.let { setImageViewBitmap(R.id.imageviewNotification, it) }

            // 设置按钮点击事件
            setOnClickPendingIntent(R.id.ImageViewNotificationLast, getPendingIntent(ACTION_PREV))
            setOnClickPendingIntent(R.id.ImageViewNotificationNext, getPendingIntent(ACTION_NEXT))
            setOnClickPendingIntent(R.id.ImageViewNotificationStart, getPendingIntent(ACTION_PLAY_PAUSE))
            setOnClickPendingIntent(R.id.lottieNotification, getPendingIntent(ACTION_LIKE))

            // 设置进度条拖动事件
            val seekIntent = Intent(this@NotificationService, NotificationService::class.java).apply {
                action = ACTION_SEEK
            }
            val seekPendingIntent = PendingIntent.getService(
                this@NotificationService,
                ACTION_SEEK.hashCode(),
                seekIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            setPendingIntentTemplate(R.id.ssbNotification, seekPendingIntent)
        }

        // 构建通知
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.notificationicon)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setCustomContentView(remoteViews) // 设置自定义布局（折叠状态）
            .setCustomBigContentView(remoteViews) // 设置自定义布局（展开状态）
            .setStyle(NotificationCompat.DecoratedCustomViewStyle()) // 启用装饰样式
            .setContentIntent(getActivityPendingIntent())
            .build()

        // 启动前台服务
        startForeground(NOTIFICATION_ID, notification)
    }

    // 创建带有特定动作的 PendingIntent
    private fun getPendingIntent(action: String, seekPosition: Int = -1): PendingIntent {
        val intent = Intent(this, NotificationService::class.java).apply {
            this.action = action
            // 若为进度拖动，附加进度参数
            if (action == ACTION_SEEK && seekPosition != -1) {
                putExtra(EXTRA_SEEK_POSITION, seekPosition)
            }
        }

        return PendingIntent.getService(
            this,
            action.hashCode() + seekPosition, // 加入进度值确保唯一性
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    // 点击通知跳转至 MyMusicActivity
    private fun getActivityPendingIntent(): PendingIntent {
        val intent = Intent(this, MyMusicActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("FROM_NOTIFICATION", true)
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    // 处理按钮点击事件
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            mMainMusicViewModel?.let { viewModel ->
                when (action) {
                    ACTION_PREV -> viewModel.playLastSong()
                    ACTION_PLAY_PAUSE -> viewModel.togglePlaying()
                    ACTION_NEXT -> viewModel.playNextSong()
                    ACTION_LIKE -> toggleLike()
                    ACTION_SEEK -> {
                        // 处理进度条拖动事件
                        val seekPosition = intent.getIntExtra(EXTRA_SEEK_POSITION, -1)
                        if (seekPosition != -1 && totalDurationMs > 0) {
                            val positionMs = (seekPosition * totalDurationMs / 100).coerceIn(0, totalDurationMs)
                            viewModel.sharedPlayer.seekTo(positionMs) // 跳转到指定位置
                            currentProgressMs = positionMs // 更新本地进度
                            updateNotification() // 刷新通知
                        }
                    }
                }
            }
        }
        return START_STICKY
    }

    // 切换点赞状态
    private fun toggleLike() {
        currentMusic?.let { music ->
            val updatedMusic = music.copy(liked = !music.liked)
            mMainMusicViewModel?.updateMusicInfo(updatedMusic)
            currentMusic = updatedMusic
            updateNotification() // 刷新通知
        }
    }

    // 创建通知渠道
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "媒体播放控制",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "显示音乐播放控件和曲目信息"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    // 格式化时间（毫秒 → mm:ss）
    private fun formatTime(millis: Long): String {
        val totalSeconds = (millis / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onBind(intent: Intent): IBinder = NotificationBinder()

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        handler.removeCallbacks(updateRunnable)
    }
}