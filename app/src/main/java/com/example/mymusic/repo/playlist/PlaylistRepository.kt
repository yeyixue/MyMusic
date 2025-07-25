package com.example.mymusic.repo.playlist

import com.example.mymusic.repo.entity.MusicInfo
import com.example.mymusic.repo.remote.ApiService
import com.example.mymusic.repo.remote.RetrofitConnection
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlin.collections.mutableListOf
import androidx.lifecycle.MutableLiveData

interface PlaylistChangeListener {
    fun onPlaylistChanged(newPlaylist: List<MusicInfo>)
}

object PlaylistRepository : PlaylistInterface, BaseRepository() {
    private val apiService: ApiService = RetrofitConnection.apiService
    private val disposables = CompositeDisposable()
    private val listeners = mutableListOf<PlaylistChangeListener>()

    lateinit var  mPlaylist: MutableList<MusicInfo>


    override fun getPlaylist(): MutableList<MusicInfo> {
        return mPlaylist
    }

    override fun updatePlaylist(playlist: List<MusicInfo>) {
        if (mPlaylist != playlist) {
            mPlaylist.clear()
            mPlaylist.addAll(playlist)
            notifyListeners()
        }
    }

    override fun updateMusicInfo(updatedMusic: MusicInfo) {
        val index = mPlaylist.indexOfFirst { it.songId == updatedMusic.songId }
        if (index != -1) {
            mPlaylist[index] = updatedMusic
            notifyListeners()
        }
    }

    override fun setPlayListDefault() {
        apiService.getSong()
            .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
            .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
            .subscribe({ resp ->
                if (resp.code == 200) {
                    val newList = resp.content
                    updatePlaylist(newList)
                }
            }, { error ->
                // 处理错误
            })
            .also { disposables.add(it) }
    }

    fun addListener(listener: Any) {
        listeners.add(listener as PlaylistChangeListener)
    }

    fun removeListener(listener: PlaylistChangeListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        mPlaylist.let { newPlaylist ->
            listeners.forEach { it.onPlaylistChanged(newPlaylist) }
        }
    }
}