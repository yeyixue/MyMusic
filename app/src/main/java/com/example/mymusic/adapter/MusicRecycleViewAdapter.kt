package com.example.mymusic.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.example.mymusic.R
import com.example.mymusic.repo.entity.MusicInfo
import com.example.mymusic.view.AutoScrollTextView

/**
 * RecyclerView的适配器，用于展示MusicInfo数据列表
 * 根据isVideo字段区分音乐/视频两种视图类型，适配复用布局中的控件
 */
class MusicRecycleViewAdapter(
    initialList: List<MusicInfo> = emptyList()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var infoList: List<MusicInfo> = initialList
        set(value) {
            val diffResult = DiffUtil.calculateDiff(MyDiffCallback(field, value))
            field = value
            diffResult.dispatchUpdatesTo(this)
        }

    companion object {
        const val TYPE_MUSIC = 1
        const val TYPE_VIDEO = 2

        /**
         * 数字格式化工具：
         * @param count 原始数字
         * @return 格式化后的字符串
         */
        fun formatCount(count: Int): String {
            return when {
                count >= 10000 -> {
                    val value = (count.toFloat() / 10000).toInt()  // 向下取整（舍弃小数部分）
                    "${value}w+"
                }
                count >= 1000 -> {
                    val value = (count.toFloat() / 1000).toInt()  // 向下取整
                    "${value}k+"
                }
                else -> count.toString()
            }
        }

    }

    override fun getItemViewType(position: Int): Int {
        return if (infoList[position].isVideo) TYPE_VIDEO else TYPE_MUSIC
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_MUSIC -> {
                val view = inflater.inflate(R.layout.item_main_music, parent, false)
                MusicViewHolder(view)
            }
            TYPE_VIDEO -> {
                val view = inflater.inflate(R.layout.item_main_video, parent, false)
                VideoViewHolder(view)
            }
            else -> throw IllegalArgumentException("未知视图类型: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val musicInfo = infoList[position]
        when (holder) {
            is MusicViewHolder -> holder.bind(musicInfo)
            is VideoViewHolder -> holder.bind(musicInfo)
        }

        holder.itemView.setOnClickListener {
            onItemClickListener?.onItemClick(musicInfo)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            val payload = payloads[0] as? Map<*, *> ?: return
            val musicInfo = infoList[position]
            when (holder) {
                is MusicViewHolder -> {
                    // 音乐项局部刷新
                    payload["title"]?.let { holder.tvTitle.text = it as String }
                    payload["singer"]?.let { holder.tvSinger.text = it as String }
                    payload["lyricist"]?.let { holder.tvLyricist.text = "作词：$it" }
                    payload["composer"]?.let { holder.tvComposer.text = "作曲：$it" }
                    payload["followed"]?.let { holder.updateFollowStatus(it as Boolean) }
                    payload["liked"]?.let { holder.updateLikeStatus(it as Boolean) }
                    payload["likeCount"]?.let {
                        holder.tvLikeCount.text = formatCount(it as Int)  // 局部刷新时格式化
                    }
                    payload["commentCount"]?.let {
                        holder.tvCommentCount.text = formatCount(it as Int)
                    }
                    payload["shareCount"]?.let {
                        holder.tvShareCount.text = formatCount(it as Int)
                    }
                }
                is VideoViewHolder -> {
                    // 视频项局部刷新
                    payload["title"]?.let { holder.tvTitle.text = it as String }
                    payload["singer"]?.let { holder.tvSinger.text = it as String }
                    payload["liked"]?.let { holder.updateLikeStatus(it as Boolean) }
                    payload["likeCount"]?.let {
                        holder.tvLikeCount.text = formatCount(it as Int)  // 局部刷新时格式化
                    }
                    payload["cover"]?.let { holder.ivCover.setImageResource(it as Int) }
                }
            }
            return
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun getItemCount(): Int = infoList.size

    // 音乐类型ViewHolder
    inner class MusicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 绑定item_main_music布局中的控件
        val tvTitle: AutoScrollTextView = itemView.findViewById(R.id.textViewSongTitle)
        val tvSinger: AutoScrollTextView = itemView.findViewById(R.id.textViewSongSinger)
        val tvLyricist: TextView = itemView.findViewById(R.id.textViewLyricFirstLine)
        val tvComposer: TextView = itemView.findViewById(R.id.textViewLyricSecondLine)
        val tvFollow: TextView = itemView.findViewById(R.id.textViewSingerFollow)
        val lottieLike: LottieAnimationView = itemView.findViewById(R.id.lottie_heart)
        val tvLikeCount: TextView = itemView.findViewById(R.id.text_like_count)  // 点赞数
        val tvCommentCount: TextView = itemView.findViewById(R.id.text_review_count)  // 评论数
        val tvShareCount: TextView = itemView.findViewById(R.id.text_share_count)  // 分享数
        val ivSongImage: ImageView = itemView.findViewById(R.id.imageViewSongIMG)

        fun setRandomSongImage() {
            val randomIndex = (0..22).random()
            val resName = "img$randomIndex"
            val resId = itemView.context.resources.getIdentifier(resName, "mipmap", itemView.context.packageName)
            if (resId != 0) ivSongImage.setImageResource(resId)
            else ivSongImage.setImageResource(R.mipmap.img0)
        }

        fun bind(musicInfo: MusicInfo) {
            tvTitle.text = musicInfo.title
            tvSinger.text = musicInfo.singer
            tvLyricist.text = "作词：${musicInfo.lyricist}"
            tvComposer.text = "作曲：${musicInfo.composer}"
            updateFollowStatus(musicInfo.followed)
            updateLikeStatus(musicInfo.liked)

            // 格式化显示点赞数、评论数、分享数
            tvLikeCount.text = formatCount(musicInfo.likeCount)
            tvCommentCount.text = formatCount(musicInfo.commentCount)
            tvShareCount.text = formatCount(musicInfo.shareCount)

            setRandomSongImage()

            // 关注按钮点击
            tvFollow.setOnClickListener {
                onItemActionListener?.onFollowStatusChanged(adapterPosition, !musicInfo.followed)
            }

            // 点赞按钮点击
            lottieLike.setOnClickListener {
                onItemActionListener?.onLikeStatusChanged(adapterPosition, !musicInfo.liked)
            }
        }

        fun updateFollowStatus(isFollowed: Boolean) {
            tvFollow.text = if (isFollowed) "已关注" else "关注"
        }

        fun updateLikeStatus(isLiked: Boolean) {
            if (isLiked) {
                lottieLike.playAnimation()
                lottieLike.progress = 1f
            } else {
                lottieLike.cancelAnimation()
                lottieLike.progress = 0f
            }
        }
    }

    // 视频类型ViewHolder
    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 绑定item_main_video布局中的控件
        val tvTitle: AutoScrollTextView = itemView.findViewById(R.id.textViewSongTitle)
        val tvSinger: AutoScrollTextView = itemView.findViewById(R.id.textViewSongSinger)
        val ivCover: ImageView = itemView.findViewById(R.id.imageViewSongVideo)  // 视频封面
        val lottieLike: LottieAnimationView = itemView.findViewById(R.id.lottie_heart)  // 点赞动画
        val tvLikeCount: TextView = itemView.findViewById(R.id.text_like_count)  // 点赞数
        val tvCommentCount: TextView = itemView.findViewById(R.id.text_review_count)  // 评论数
        val tvShareCount: TextView = itemView.findViewById(R.id.text_share_count)  // 分享数

        fun bind(musicInfo: MusicInfo) {
            tvTitle.text = musicInfo.title
            tvSinger.text = musicInfo.singer
            ivCover.setImageResource(R.drawable.flower)

            // 格式化显示点赞数、评论数、分享数
            tvLikeCount.text = formatCount(musicInfo.likeCount)
            tvCommentCount.text = formatCount(musicInfo.commentCount)
            tvShareCount.text = formatCount(musicInfo.shareCount)

            updateLikeStatus(musicInfo.liked)

            // 视频点赞点击
            lottieLike.setOnClickListener {
                onItemActionListener?.onLikeStatusChanged(adapterPosition, !musicInfo.liked)
            }
        }

        fun updateLikeStatus(isLiked: Boolean) {
            if (isLiked) {
                lottieLike.playAnimation()
                lottieLike.progress = 1f
            } else {
                lottieLike.cancelAnimation()
                lottieLike.progress = 0f
            }
        }
    }

    // DiffUtil回调（补充评论数、分享数的差异检查）
    inner class MyDiffCallback(
        private val oldList: List<MusicInfo>,
        private val newList: List<MusicInfo>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].songId == newList[newItemPosition].songId
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            return old.title == new.title &&
                    old.singer == new.singer &&
                    old.liked == new.liked &&
                    old.likeCount == new.likeCount &&
                    old.commentCount == new.commentCount &&  // 检查评论数变化
                    old.shareCount == new.shareCount &&  // 检查分享数变化
                    old.isVideo == new.isVideo
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): MutableMap<String, Any>? {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            val payload = mutableMapOf<String, Any>()

            if (old.title != new.title) payload["title"] = new.title
            if (old.singer != new.singer) payload["singer"] = new.singer
            if (old.liked != new.liked) payload["liked"] = new.liked
            if (old.likeCount != new.likeCount) payload["likeCount"] = new.likeCount
            if (old.commentCount != new.commentCount) payload["commentCount"] = new.commentCount  // 评论数差异
            if (old.shareCount != new.shareCount) payload["shareCount"] = new.shareCount  // 分享数差异

            // 视频特有字段
            if (old.isVideo && new.isVideo) {
                // 假设的视频封面字段
                // if (old.videoCover != new.videoCover) payload["cover"] = new.videoCover
            }

            // 音乐特有字段
            if (!old.isVideo && !new.isVideo) {
                if (old.lyricist != new.lyricist) payload["lyricist"] = new.lyricist
                if (old.composer != new.composer) payload["composer"] = new.composer
                if (old.followed != new.followed) payload["followed"] = new.followed
            }

            return if (payload.isEmpty()) null else payload
        }
    }

    // 点击事件接口
    interface OnItemClickListener {
        fun onItemClick(musicInfo: MusicInfo)
    }

    // 交互事件接口
    interface OnItemActionListener {
        fun onFollowStatusChanged(position: Int, isFollowed: Boolean)
        fun onLikeStatusChanged(position: Int, isLiked: Boolean)
    }

    private var onItemClickListener: OnItemClickListener? = null
    private var onItemActionListener: OnItemActionListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }

    fun setOnItemActionListener(listener: OnItemActionListener) {
        this.onItemActionListener = listener
    }
}
