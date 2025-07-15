package com.example.mymusic.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mymusic.R

class MyMusicPlayListAdapter(
    private val playList: List<PlayListInfo>
) : RecyclerView.Adapter<MyMusicPlayListAdapter.MusicViewHolder>() {

    // 点击事件监听器
    /**
     * 列表项点击事件的回调接口
     * 由Activity具体实现这个接口声明
     */
    interface OnItemClickListener {
        /**
         * 当列表项被点击时调用
         * @param noteInfo 被点击的列表项对应的数据
         */
        fun onItemClick(noteInfo: PlayListInfo)
    }

    // 点击事件监听器变量
    private var onItemClickListener: OnItemClickListener? = null

    /**
     * 设置列表项点击事件监听器
     * @param listener 实现了OnItemClickListener接口的监听器
     */
    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mymusic_playlist, parent, false)
        return MusicViewHolder(view)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        val music = playList[position]
        holder.bind(music)
        // 设置点击事件
        holder.itemView.setOnClickListener {
            onItemClickListener?.onItemClick(music)
        }
    }

    override fun getItemCount(): Int = playList.size

    inner class MusicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPlaylist: ImageView = itemView.findViewById(R.id.ivplaylist)
        private val ivLock: ImageView = itemView.findViewById(R.id.ivLock)
        private val playlistTitle: TextView = itemView.findViewById(R.id.playlisttitle)
        private val songNumText: TextView = itemView.findViewById(R.id.songNumText)

        fun bind(music: PlayListInfo) {
            // 设置音乐封面
            ivPlaylist.setImageResource(music.coverResId)
            ivLock.visibility=if (music.isLock) View.VISIBLE else View.GONE
            // 设置音乐标题
            playlistTitle.text = music.title

            // 设置歌曲数量
            songNumText.text = "${music.songCount} 首"
        }
    }
}


data class PlayListInfo(
    val coverResId: Int,  // 封面资源ID
    val title: String,    // 标题
    val isLock: Boolean,   //是否隐私
    val songCount: Int    // 歌曲数量
)