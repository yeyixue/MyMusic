package com.example.mymusic.repo.entity

import android.graphics.Bitmap

data class ApiResponse(
    val code: Int,
    val message: String,
    val content: List<MusicInfo>
)

data class MusicInfo(
    val songId: Int,
    val title: String,
    val singer: String,
    val lyricist: String,
    val composer: String,
    val duration: Int,
    var followed: Boolean,
    var liked: Boolean,
    val disliked: Boolean,
    val isVideo: Boolean,
    val likeCount: Int,     // 点赞数
    val commentCount: Int,  // 评论数
    val shareCount: Int,     // 分享数
    var coverResId: Int? = null ,// 音乐封面资源ID（mipmap/img*）
    var coverBitmap: Bitmap? = null, // 视频封面（第一帧截图）
    var isCoverLoaded: Boolean = false // 新增：封面加载状态标记
)
