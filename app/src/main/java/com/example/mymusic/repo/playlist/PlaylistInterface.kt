package com.example.mymusic.repo.playlist

import com.example.mymusic.repo.entity.ApiResponse
import com.example.mymusic.repo.entity.MusicInfo
import io.reactivex.rxjava3.core.Single

interface PlaylistInterface {


    fun getPlaylist(): MutableList<MusicInfo>
    fun updatePlaylist(playlist: List<MusicInfo>)
    fun updateMusicInfo(updatedMusic: MusicInfo)
    fun setPlayListDefault()
}