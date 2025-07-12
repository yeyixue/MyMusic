package com.example.mymusic.repo.entity

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
    val shareCount: Int     // 分享数
)
