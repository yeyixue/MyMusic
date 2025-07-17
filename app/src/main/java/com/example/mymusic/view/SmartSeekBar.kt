package com.example.mymusic.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.mymusic.R
import com.example.mymusic.adapter.MusicRecycleViewAdapter.ProgressListenerManager
import kotlin.math.abs
import kotlin.math.roundToInt

class SmartSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatSeekBar(context, attrs, defStyleAttr) {

    // 样式配置
    private val dragThreshold = 8f
    private val thumbNormal: Drawable? = ContextCompat.getDrawable(context, R.drawable.seekbar_thumb)
    private val thumbDragging: Drawable? = ContextCompat.getDrawable(context, R.drawable.seekbar_thumb_large)
    private val insetNormal = 18
    private val insetDragging = 16

    // 状态变量
    var isDragging = false
    private var isUserInteracting = false // 控件内部管理用户交互状态
    private var initialX = 0f
    private var initialProgress = 0
    // 1px ≈ 100 / seekBarWidth 个进度单位
    private var scaledTouchFactor = 1f
    private var seekBarWidth = 0
    private var totalDuration = 0 // 总时长（ms），由Activity设置

    // 动画相关
    private var progressAnimator: ValueAnimator? = null

    // 扩展的触摸区域高度（dp，上下各扩展该值）
    private val touchExpansion = dp2px(40f) // 可根据需求调整
    // dp转px工具方法
    private fun dp2px(dp: Float): Int {
        return (dp * context.resources.displayMetrics.density + 0.5f).toInt()
    }

    // 扩展回调接口（包含时间信息，减少Activity计算）
    interface OnProgressActionListener {
        // 进度变化（包含进度、对应时间ms、格式化时间）
        fun onProgressChanged(seekBar: SmartSeekBar, progress: Int, timeMs: Int, formattedTime: String, fromUser: Boolean) {}
        // 用户开始/结束交互
        fun onStartTrackingTouch(seekBar: SmartSeekBar) {}
        fun onStopTrackingTouch(seekBar: SmartSeekBar) {}
    }
    // 需要设置setOnProgressActionListener，不然actionListener为空，导致fragment没法监听，progressListener弄成单例
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d("SmartSeekBar","setOnProgressActionListener")
        // 重置进度和缓冲进度
        progress = 0 // 当前进度重置为0
        secondaryProgress = 0 // 缓冲进度重置为0
        setOnProgressActionListener(object : SmartSeekBar.OnProgressActionListener {
            // 进度变化时触发（包括拖动过程和自动更新）
            override fun onProgressChanged(
                seekBar: SmartSeekBar,
                progress: Int,
                timeMs: Int,
                formattedTime: String,
                fromUser: Boolean
            ) {
//                    // 仅处理用户交互导致的进度变化---这个要写 不然没反应
                if (fromUser) {
                    ProgressListenerManager.progressListener?.onProgressUpdate(0, progress,formattedTime,tvTotalTime?.text.toString())
                }
            }

            override fun onStopTrackingTouch(seekBar: SmartSeekBar) {
                super.onStopTrackingTouch(seekBar)
                Log.e("onStopTrackingTouch", "音乐onStopTrackingTouch")
            }
            //不需要重写 onStartTrackingTouch和onStopTrackingTouch
        })
    }
    private var actionListener: OnProgressActionListener? = null

    // 新增文本显示相关变量
    private var tvCurrentTime: TextView? = null
    private var tvTotalTime: TextView? = null
    private var constraintLayoutClickArea: ConstraintLayout? = null

    init {
        thumb = thumbNormal
        updateLayerInset(insetNormal)
        // 初始化宽度
        viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                seekBarWidth = width - paddingLeft - paddingRight
                if (seekBarWidth > 0) {
                    scaledTouchFactor = 100f / seekBarWidth
                }
            }
        })
    }

    override fun getHitRect(outRect: Rect) {
        super.getHitRect(outRect)
        // 扩大垂直方向的触摸区域：上边界上移 touchExpansion，下边界下移 touchExpansion
        outRect.top -= touchExpansion
        outRect.bottom += touchExpansion
    }

    // 对外提供API：设置总时长（由Activity调用，用于进度-时间转换）
    fun setTotalDuration(durationMs: Int) {
        this.totalDuration = durationMs
        tvTotalTime?.text = formatTime(durationMs)
    }

    // 对外提供API：更新媒体播放进度（由Activity调用，控件内部处理动画）
    fun updateMediaProgress(progress: Int) {
        if (!isUserInteracting) { // 用户未操作时才更新
            smoothSetProgress(progress)
        }
    }

    // 对外提供API：设置缓冲进度（由Activity调用，或控件内部模拟）
    fun setBufferProgress(progress: Int) {
        secondaryProgress = progress.coerceIn(0, 100)
    }

    // 设置监听器
    fun setOnProgressActionListener(listener: OnProgressActionListener) {
        this.actionListener = listener
    }

    // 设置文本显示控件
    fun setTextViews(tvCurrentTime: TextView, tvTotalTime: TextView, constraintLayoutClickArea: ConstraintLayout) {
        this.tvCurrentTime = tvCurrentTime
        this.tvTotalTime = tvTotalTime
        this.constraintLayoutClickArea = constraintLayoutClickArea
        tvTotalTime.text = formatTime(totalDuration)
    }

    // 进度-时间转换（控件内部处理）
    fun progressToTimeMs(progress: Int): Int {
        return if (totalDuration <= 0) 0 else (progress * totalDuration / 100).coerceIn(0, totalDuration)
    }

    // 格式化时间（控件内部处理，减少Activity代码）
    fun formatTime(ms: Int): String {
        if (ms < 0) return "00:00"
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    // 平滑更新进度,媒体的跳转在viewmodel里面
    private fun smoothSetProgress(progress: Int) {
//        Log.d("SmartSeekBar","smoothSetProgress   progress  $progress")

        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofInt(this.progress, progress).apply {
            duration = 200
            addUpdateListener { anim ->
                val newProgress = anim.animatedValue as Int
                this@SmartSeekBar.progress = newProgress
                // 触发回调（包含时间信息）
                val timeMs = progressToTimeMs(newProgress)
                actionListener?.onProgressChanged(this@SmartSeekBar, newProgress, timeMs, formatTime(timeMs), false)
                tvCurrentTime?.text = formatTime(timeMs)
            }
            start()
        }
    }

    // 查找父级 ViewPager2
    private fun findParentViewPager2(): ViewPager2? {
        var parent = parent
        while (parent != null) {
            if (parent is ViewPager2) {
                return parent
            }
            parent = parent.parent
        }
        return null
    }

    // 用于记录是否在进度条交互中，避免重复设置
    private var isInterceptDisabled = false
    // 触摸事件处理（优化用户交互状态管理）
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {

        if (seekBarWidth <= 0) {
            seekBarWidth = width - paddingLeft - paddingRight
            if (seekBarWidth > 0) {
                scaledTouchFactor = 100f / seekBarWidth
            } else {
                return super.onTouchEvent(event)
            }
        }
        // 获取父级 ViewPager
        val viewPager2 = findParentViewPager2()
        val touchX = event.x - paddingLeft
        val validTouchX = touchX.coerceIn(0f, seekBarWidth.toFloat())

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                Log.d("MyMusic","SmartSeekBar的MotionEvent.ACTION_DOWN")
                viewPager2?.let {
                    it.requestDisallowInterceptTouchEvent(true)
                    isInterceptDisabled = true
                }

                initialX = validTouchX
                initialProgress = progress
                isDragging = false
                isUserInteracting = true // 标记用户开始交互
                thumb = thumbDragging
                updateLayerInset(insetDragging)
                actionListener?.onStartTrackingTouch(this)

                // 显示时间控件
                tvCurrentTime?.visibility = View.VISIBLE
                tvTotalTime?.visibility = View.VISIBLE
                constraintLayoutClickArea?.visibility = View.INVISIBLE

                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isDragging) {
                    if (abs(validTouchX - initialX) < dragThreshold) return true
                    isDragging = true
                }
                // 按下进度条时，禁止 ViewPager2 拦截事件
                viewPager2?.let {
                    it.requestDisallowInterceptTouchEvent(true)
                    isInterceptDisabled = true
                }
                val deltaProgress = ((validTouchX - initialX) * scaledTouchFactor).roundToInt()
                val newProgress = (initialProgress + deltaProgress).coerceIn(0, 100)
                if (newProgress != progress) {
                    progress = newProgress
                    // 触发回调（包含时间信息）
                    val timeMs = progressToTimeMs(newProgress)
                    actionListener?.onProgressChanged(this, newProgress, timeMs, formatTime(timeMs), true)
                    tvCurrentTime?.text = formatTime(timeMs)
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                Log.d("MyMusic","SmartSeekBar的MMotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL")

                // 触摸结束，恢复 ViewPager2 拦截事件
                viewPager2?.let {
                    it.requestDisallowInterceptTouchEvent(false)
                    isInterceptDisabled = false
                }
                //validTouchX 用户当前手指触摸点  scaledTouchFactor 每个像素对应多少进度百分比
                //当前手指 X 坐标 × 每像素代表的进度 → 当前进度百分比
                val finalProgress = if (isDragging) progress else (validTouchX * scaledTouchFactor).roundToInt().coerceIn(0, 100)
                // 触发回调（包含时间信息）
                val timeMs = progressToTimeMs(finalProgress)
                actionListener?.onProgressChanged(this, finalProgress, timeMs, formatTime(timeMs), true)
                actionListener?.onStopTrackingTouch(this)

                Log.d("MyMusic","SmartSeekBar的MMotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL")
                // 更新进度条的ui进度
                // 这个注释掉是因为在viewmodel设置了progressRunnable去监听媒体的变化，然后更新进度条的进度，只不过可能会延迟响应一秒
//                smoothSetProgress(finalProgress)
                thumb = thumbNormal
                updateLayerInset(insetNormal)
                isDragging = false
                isUserInteracting = false // 标记用户结束交互

                // 隐藏时间控件
                tvCurrentTime?.visibility = View.GONE
                tvTotalTime?.visibility = View.GONE
                constraintLayoutClickArea?.visibility = View.VISIBLE

                return true
            }
        }

        return super.onTouchEvent(event)
    }

    // 辅助方法：更新边距
    private fun updateLayerInset(inset: Int) {
        (progressDrawable as? LayerDrawable)?.apply {
            setLayerInset(0, 0, inset, 0, inset)
            setLayerInset(1, 0, inset, 0, inset)
            setLayerInset(2, 0, inset, 0, inset)
        }
    }




    // 资源释放
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.d("SmartSeekBar","onDetachedFromWindow  ${hashCode()}")
        progressAnimator?.cancel()
        progressAnimator = null
        actionListener = null

        // 重置进度和缓冲进度
        progress = 0 // 当前进度重置为0
        secondaryProgress = 0 // 缓冲进度重置为0

        // 重置状态变量
        isDragging = false
        isUserInteracting = false
        isInterceptDisabled = false


    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
