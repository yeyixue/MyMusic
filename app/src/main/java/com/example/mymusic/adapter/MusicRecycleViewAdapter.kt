package com.example.mymusic.adapter

import android.animation.Animator
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.bumptech.glide.Glide
import com.example.mymusic.R
import com.example.mymusic.repo.entity.MusicInfo
import com.example.mymusic.view.AutoScrollTextView
import com.example.mymusic.view.SmartSeekBar
import com.example.mymusic.viewmodel.fragment.MainMusicViewModel

/**
 * RecyclerView的适配器，用于展示MusicInfo数据列表
 * 根据isVideo字段区分音乐/视频两种视图类型，适配复用布局中的控件
 */
class MusicRecycleViewAdapter(
    private val viewModel: MainMusicViewModel,
    initialList: List<MusicInfo> = emptyList()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // 持有RecyclerView引用
    private var mRecyclerView: RecyclerView? = null
    // 记录当前中心页
    var currentCenterPosition: Int = 0
    // 提供外部设置RecyclerView的方法（在Fragment中调用）
    fun setRecyclerView(recyclerView: RecyclerView) {
        this.mRecyclerView = recyclerView
    }


    fun updateBufferProgress(percent: Int) {
        val validPercent = percent.coerceIn(0, 100)
        Log.d("BufferProgress", "适配器更新缓冲进度: $validPercent")
        // 更新可见的 ViewHolder
        mRecyclerView?.let { recyclerView ->
            val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
            layoutManager?.let {
                val firstVisiblePosition = it.findFirstVisibleItemPosition()
                val lastVisiblePosition = it.findLastVisibleItemPosition()
                for (i in firstVisiblePosition..lastVisiblePosition) {
                    val viewHolder = recyclerView.findViewHolderForAdapterPosition(i)
                    when (viewHolder) {
                        is MusicViewHolder -> viewHolder.updateBuffer(validPercent)
                        is VideoViewHolder -> viewHolder.updateBuffer(validPercent)
                    }
                }
            }
        }
    }


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

        // 补充时间格式化方法（毫秒转mm:ss）
        fun formatTime(millis: Int): String {
            if (millis < 0) return "00:00"
            val totalSeconds = millis / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
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
        val bgResId = getRandomBgResId(holder.itemView.context, position)
        val isCurrent = position == currentCenterPosition
        when (holder) {
            is MusicViewHolder -> {
                holder.bind(musicInfo)
                holder.itemRootLayout.setBackgroundResource(bgResId)

            }
            is VideoViewHolder -> {
                holder.bind(musicInfo, isCurrent)
                holder.itemRootLayout.setBackgroundResource(bgResId)
            }
        }

    }
    //
    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
//        if (holder is VideoViewHolder) {
//            holder.setIsRecyclable(false) // 防止被系统回收
//        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            val payload = payloads[0] as? Map<*, *> ?: return
            when (holder) {
                is MusicViewHolder -> {
                    // 音乐项局部刷新
                    payload["title"]?.let { holder.tvTitle.text = it as String }
                    payload["singer"]?.let { holder.tvSinger.text = it as String }
                    payload["lyricist"]?.let { holder.tvLyricist.text = "作词：$it" }
                    payload["composer"]?.let { holder.tvComposer.text = "作曲：$it" }
                    payload["followed"]?.let { holder.tvFollow.text= it as String }
                    payload["liked"]?.let { updateLikeStatus(holder.lottieLike, it as Boolean) }
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
        val tvCurrentTime: TextView = itemView.findViewById(R.id.tvCurrentTime)  // 正在播放的进度
        val tvTotalTime: TextView = itemView.findViewById(R.id.tvTotalTime)  // 歌曲总时长
        val seekBar: SmartSeekBar = itemView.findViewById(R.id.SSB_my)  // 子项中的进度条
        val ivSongImage: ImageView = itemView.findViewById(R.id.imageViewSongIMG)
        val constraintLayoutClickArea: ConstraintLayout = itemView.findViewById(R.id.constraintLayoutClickArea)
        val itemRootLayout: ConstraintLayout = itemView.findViewById(R.id.itemMainMusic)
        // 保存动画的 composition（用于获取时长）
        private var heartComposition: LottieComposition? = null

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
            tvFollow.text = if (musicInfo.followed) "已关注" else "关注"
            //初始化时设置动画
            lottieLike?.run {
//            setAnimation("heart_start_end.json")
                LottieCompositionFactory.fromAsset(context, "heart_start_end.json")
                    .addListener { composition ->
                        // 动画加载成功，设置给 LottieAnimationView
                        heartComposition=composition
                        lottieLike?.setComposition(composition)
                        lottieLike?.setMinAndMaxFrame(25,51)
                        lottieLike?.setProgress(0f) // 初始显示第一帧}.addFailureListener { e ->
                    }
            }

            // 格式化显示点赞数、评论数、分享数
            tvLikeCount.text = formatCount(musicInfo.likeCount)
            tvCommentCount.text = formatCount(musicInfo.commentCount)
            tvShareCount.text = formatCount(musicInfo.shareCount)

            // 1. 设置进度条的文本视图（当前时间、总时间、点击区域）
            seekBar.setTextViews(tvCurrentTime, tvTotalTime, constraintLayoutClickArea)
            // 2. 设置总时长（用于进度-时间转换）
            seekBar.setTotalDuration(musicInfo.duration)
            // 3. 初始化缓冲进度
            seekBar.setBufferProgress(0)
            Log.d("SmartSeekBar","音乐类型ViewHolder初始化ActionListener 该holder的seekBar对应hashcode 是  ${seekBar.hashCode()}")

            // 4. 设置 SmartSeekBar 的核心监听（替代 SeekBarUtils） 音乐
//            seekBar.setOnProgressActionListener(object : SmartSeekBar.OnProgressActionListener {
//                // 进度变化时触发（包括拖动过程和自动更新）
//                override fun onProgressChanged(
//                    seekBar: SmartSeekBar,
//                    progress: Int,
//                    timeMs: Int,
//                    formattedTime: String,
//                    fromUser: Boolean
//                ) {
//
////                    // 仅处理用户交互导致的进度变化---这个要写 不然没反应
//                    if (fromUser) {
//                        ProgressListenerManager.progressListener?.onProgressUpdate(adapterPosition, progress,formattedTime,tvTotalTime.text.toString())
//                    }
//                }
//
//                override fun onStopTrackingTouch(seekBar: SmartSeekBar) {
//                    super.onStopTrackingTouch(seekBar)
//                    Log.e("onStopTrackingTouch", "音乐onStopTrackingTouch")
//                }
//                //不需要重写 onStartTrackingTouch和onStopTrackingTouch
//            })

            // 5. 初始化进度和时间显示
            seekBar.updateMediaProgress(0) // 初始进度0
            tvCurrentTime.text = "00:00"
            tvTotalTime.text = "/ ${formatTime(musicInfo.duration)}"
            tvCurrentTime.visibility= View.GONE
            tvTotalTime.visibility= View.GONE


            // 设置随机图片
            setRandomSongImage()

            // 使用通用方法设置关注按钮
            setupFollowButton(tvFollow, musicInfo, adapterPosition, onItemActionListener)

            // 使用通用方法设置点赞动画
            setupLikeAnimation(lottieLike, musicInfo, adapterPosition, onItemActionListener)

        }
        //处理爱心的点击动画和结束显示
        fun updateBuffer(progress: Int) {
            seekBar.setBufferProgress(progress)
        }

    }


    // 设置关注按钮的点击事件处理
    fun setupFollowButton(
        followButton: TextView,
        musicInfo: MusicInfo,
        position: Int,
        listener: OnItemActionListener?
    ) {
        followButton.text = if (musicInfo.followed) "已关注" else "关注"
        followButton.setOnClickListener {
            // 通知监听器状态变化
            listener?.onFollowStatusChanged(position, !musicInfo.followed)
            // 更新UI显示
            musicInfo.followed = !musicInfo.followed
            followButton.text = if (musicInfo.followed) "已关注" else "关注"
        }
    }

    // 设置点赞动画和状态处理
    fun setupLikeAnimation(
        likeView: LottieAnimationView,
        musicInfo: MusicInfo,
        position: Int,
        listener: OnItemActionListener?
    ) {
        // 初始化动画
        LottieCompositionFactory.fromAsset(likeView.context, "heart_start_end.json")
            .addListener { composition ->
                likeView.setComposition(composition)
                likeView.setMinAndMaxFrame(25, 51)
                likeView.progress = if (musicInfo.liked) 1f else 0f
            }
            .addFailureListener { e ->
                Log.d("Lottie", "动画加载失败2: ${e.message}")
            }

        // 设置点击事件
        likeView.setOnClickListener {
            // 通知监听器状态变化
            listener?.onLikeStatusChanged(position, !musicInfo.liked)
            // 更新UI状态
            updateLikeStatus(likeView, !musicInfo.liked)
            musicInfo.liked = !musicInfo.liked
        }
    }
    // 更新点赞状态的通用方法
// 更新点赞状态的通用方法（优化版）
    fun updateLikeStatus(likeView: LottieAnimationView, isLiked: Boolean) {
        if (isLiked) {
            // 清除之前的监听器（避免重复添加）
            likeView.removeAllAnimatorListeners()

            // 添加动画完成监听器
            likeView.addAnimatorListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    // 动画结束后，确保进度为 1f（显示最后一帧）
                    likeView.progress = 1f
                    // 移除监听器，避免内存泄漏
                    likeView.removeAnimatorListener(this)
                }
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })

            // 开始播放动画
            likeView.playAnimation()
        } else {
            // 取消点赞时，直接重置到初始状态
            likeView.cancelAnimation()
            likeView.progress = 0f
        }
    }


    // 更新进度条和时间显示的通用方法
    fun updateProgress(
        seekBar: SmartSeekBar,
        tvCurrentTime: TextView,
        progress: Int,
        currentTime: String,
        isDragging: Boolean = false // 新增参数：是否正在拖动
    ) {
//        Log.d("MyMusic","isDragging 是 $isDragging")
        if (!isDragging) {
            seekBar.updateMediaProgress(progress)
            tvCurrentTime.text = currentTime
        }
    }
    // 适配器层的updateItemProgress方法（用于外部调用）
    fun updateItemProgress(position: Int, progress: Int, currentTime: String) {
//        Log.d("updateItemProgress", "fun updateItemProgress(position: Int, progress: Int, currentTime: String) ")
        if (position < 0 || position >= infoList.size) return // 校验位置合法性
        // 通过RecyclerView获取对应位置的ViewHolder
        val viewHolder = mRecyclerView?.findViewHolderForAdapterPosition(position)
        if (viewHolder is MusicViewHolder) {
            // 传入ViewHolder中的控件实例和参数
            updateProgress(
                seekBar = viewHolder.seekBar,
                tvCurrentTime = viewHolder.tvCurrentTime,
                progress = progress,
                currentTime = currentTime,
                isDragging = viewHolder.seekBar.isDragging
            )
        }else if(viewHolder is VideoViewHolder){
            updateProgress(
                seekBar = viewHolder.seekBar,
                tvCurrentTime = viewHolder.tvCurrentTime,
                progress = progress,
                currentTime = currentTime,
                isDragging = viewHolder.seekBar.isDragging
            )
        }
    }

    // 用于生成随机背景的工具方法  mipmap/bg0-bg4
    private fun getRandomBgResId(context: Context, position: Int): Int {
        // 根据位置固定背景
        val fixedIndex = position % 8 // 0-4循环（确保每个位置对应固定图片）

        val bgName = "bg$fixedIndex"
        // 转换为资源ID
        return context.resources.getIdentifier(
            bgName,
            "mipmap",
            context.packageName
        ).takeIf { it != 0 } ?: R.mipmap.bg0 // 兜底图
    }


    // 视频类型ViewHolder
    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 绑定item_main_video布局中的控件
        val tvTitle: AutoScrollTextView = itemView.findViewById(R.id.textViewSongTitle)
        val tvSinger: AutoScrollTextView = itemView.findViewById(R.id.textViewSongSinger)
        val tvFollow: TextView = itemView.findViewById(R.id.textViewSingerFollow)
        val lottieLike: LottieAnimationView = itemView.findViewById(R.id.lottie_heart)  // 点赞动画
        val tvLikeCount: TextView = itemView.findViewById(R.id.text_like_count)  // 点赞数
        val tvCommentCount: TextView = itemView.findViewById(R.id.text_review_count)  // 评论数
        val tvShareCount: TextView = itemView.findViewById(R.id.text_share_count)  // 分享数
        val tvCurrentTime: TextView = itemView.findViewById(R.id.tvCurrentTime)  // 正在播放的进度
        val tvTotalTime: TextView = itemView.findViewById(R.id.tvTotalTime)  // 歌曲总时长
        val seekBar: SmartSeekBar = itemView.findViewById(R.id.SSB_my)  // 子项中的进度条
        val itemRootLayout: ConstraintLayout = itemView.findViewById(R.id.itemMainVideo)
        val constraintLayoutClickArea: ConstraintLayout = itemView.findViewById(R.id.constraintLayoutClickArea)
        val playerView: PlayerView = itemView.findViewById(R.id.playerView)
        val thumbnailImageView: ImageView = itemView.findViewById(R.id.thumbnailImageView)  // 封面页

        // 保存动画的 composition（用于获取时长）
        private var heartComposition: LottieComposition? = null
        var firstFrameRendered = false



        fun bind(musicInfo: MusicInfo,isCurrent: Boolean) {
            tvTitle.text = musicInfo.title
            tvSinger.text = musicInfo.singer
            tvFollow.text = if (musicInfo.followed) "已关注" else "关注"
            //初始化时设置动画
            lottieLike?.run {
                LottieCompositionFactory.fromAsset(context, "heart_start_end.json")
                    .addListener { composition ->
                        // 动画加载成功，设置给 LottieAnimationView
                        heartComposition=composition
                        lottieLike?.setComposition(composition)
                        lottieLike?.setMinAndMaxFrame(25,51)
                        lottieLike?.setProgress(0f) // 初始显示第一帧}.addFailureListener { e ->

                    }
            }

            // 格式化显示点赞数、评论数、分享数
            tvLikeCount.text = formatCount(musicInfo.likeCount)
            tvCommentCount.text = formatCount(musicInfo.commentCount)
            tvShareCount.text = formatCount(musicInfo.shareCount)

            // 1. 设置进度条的文本视图（当前时间、总时间、点击区域）
            seekBar.setTextViews(tvCurrentTime, tvTotalTime, constraintLayoutClickArea)
            // 2. 设置总时长（用于进度-时间转换）
            seekBar.setTotalDuration(musicInfo.duration)
            // 3. 初始化缓冲进度
            seekBar.setBufferProgress(0)
            Log.d("SmartSeekBar","视频类型ViewHolder初始化ActionListener 该holder的seekBar对应hashcode 是  ${seekBar.hashCode()}")

            // 4. 设置 SmartSeekBar 的核心监听，视频
//            seekBar.setOnProgressActionListener(object : SmartSeekBar.OnProgressActionListener {
//                // 进度变化时触发（包括拖动过程和自动更新）
//                override fun onProgressChanged(
//                    seekBar: SmartSeekBar,
//                    progress: Int,
//                    timeMs: Int,
//                    formattedTime: String,
//                    fromUser: Boolean
//                ) {
////                    // 仅处理用户交互导致的进度变化---这个要写 不然没反应！
//                    if (fromUser) {
//                        ProgressListenerManager.progressListener?.onProgressUpdate(adapterPosition, progress,formattedTime,tvTotalTime.text.toString())
//                    }
//                }
//
//                //不需要重写 onStartTrackingTouch和onStopTrackingTouch
//            })

            // 5. 初始化进度和时间显示
            seekBar.updateMediaProgress(0) // 初始进度0
            tvCurrentTime.text = "00:00"
            tvTotalTime.text = "/ ${formatTime(musicInfo.duration)}"
            tvCurrentTime.visibility= View.GONE
            tvTotalTime.visibility= View.GONE

            // 使用通用方法设置关注按钮
            setupFollowButton(tvFollow, musicInfo, adapterPosition, onItemActionListener)

            // 使用通用方法设置点赞动画
            setupLikeAnimation(lottieLike, musicInfo, adapterPosition, onItemActionListener)


            /**
             * 视频播放
             */
            playerView.useController = false
            val videoUrl = viewModel.getVideoUrlBySongId(musicInfo.songId)

            // 重置播放器状态
            if(!isCurrent){
                // 加载视频封面
                Glide.with(itemView.context)
                    .asBitmap()
                    .load(videoUrl)
                    .frame(0) // 加载第0毫秒的帧作为封面图
                    .into(thumbnailImageView)
            }

            if (isCurrent) {
                // 当前页：显示播放器
                thumbnailImageView.visibility = View.GONE
                playerView.visibility = View.VISIBLE

                playerView.player = viewModel.sharedPlayer

                val mediaItem = MediaItem.fromUri(videoUrl)
                viewModel.sharedPlayer.setMediaItem(mediaItem)
                // 视频第一帧渲染后隐藏缩略图，此处可优化
                viewModel.sharedPlayer.addListener(object : Player.Listener {
                    override fun onRenderedFirstFrame() {
                        //视频中途暂停后重新播放, 确保隐藏缩略图的操作只执行一次
                        if (!firstFrameRendered) {
                            thumbnailImageView.visibility = View.GONE  // 第一帧显示后隐藏封面
                            firstFrameRendered = true
                        }
                    }
                })
                viewModel.sharedPlayer.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            // 启动进度更新（确保播放时才启动）
//                            viewModel.startProgressUpdates() // 准备就绪后启动更新
                            val duration = viewModel.sharedPlayer.duration
                            Log.d("MyMusic", "视频时长：${duration}毫秒") // 检查是否正常
                            thumbnailImageView.visibility = View.GONE
                            Log.d("MyMusic", "关闭封面") // 检查是否正常
                        }
                        if(state==Player.STATE_ENDED){
                            //播放器已播放到媒体的末尾。
                        }
                        if(state==Player.STATE_BUFFERING){
                            //播放器正在缓冲数据。
                            Log.d("MyMusic", "播放器正在缓冲数据") // 检查是否正常
                        }
                    }
                })
                viewModel.sharedPlayer.prepare()
                viewModel.sharedPlayer.playWhenReady = true
                setIsRecyclable(false)

            } else {
                // 非当前页：只显示封面图
                setIsRecyclable(true)
//                viewModel.sharedPlayer.clearMediaItems() // 清除旧媒体
                playerView.player = null // 解绑视图但保留播放器状态
                thumbnailImageView.visibility = View.VISIBLE
                playerView.visibility = View.GONE
            }

        }



        fun pauseVideo() {
            viewModel.sharedPlayer.pause()
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
        fun updateBuffer(progress: Int) {
            seekBar.setBufferProgress(progress)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is VideoViewHolder) {
            holder.playerView.player = null // 强制解绑
//            holder.releasePlayer()
            holder.pauseVideo()
            Log.d("ViewRecycled", "ViewHolder回收，解绑PlayerView")
            holder.firstFrameRendered = false // 重置第一帧渲染状态
            // 隐藏playerView，显示封面图（避免复用时显示旧画面）
            holder.playerView.visibility = View.GONE
            holder.thumbnailImageView.visibility = View.VISIBLE
        }
        super.onViewRecycled(holder)
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



    // 播放进度更新接口
    object ProgressListenerManager {
        var progressListener: OnPlayProgressListener? = null

        fun interface OnPlayProgressListener {
            fun onProgressUpdate(position: Int, progress: Int, currentTime: String, totalTime: String)
        }
    }

    fun setOnPlayProgressListener(listener: ProgressListenerManager.OnPlayProgressListener) {
        ProgressListenerManager.progressListener = listener
    }



    // 交互事件接口
    interface OnItemActionListener {
        fun onFollowStatusChanged(position: Int, isFollowed: Boolean)
        fun onLikeStatusChanged(position: Int, isLiked: Boolean)
    }
    private var onItemActionListener: OnItemActionListener? = null

    fun setOnItemActionListener(listener: OnItemActionListener) {
        this.onItemActionListener = listener
    }
}
