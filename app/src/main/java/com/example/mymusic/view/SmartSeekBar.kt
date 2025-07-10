package com.example.mymusic.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.content.ContextCompat
import com.example.mymusic.R
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
    private val insetDragging = 17

    // 状态变量
    private var isDragging = false
    private var isUserInteracting = false // 控件内部管理用户交互状态
    private var initialX = 0f
    private var initialProgress = 0
    // 1px ≈ 100 / seekBarWidth 个进度单位
    private var scaledTouchFactor = 1f
    private var seekBarWidth = 0
    private var totalDuration = 0 // 总时长（ms），由Activity设置

    // 动画相关
    private var progressAnimator: ValueAnimator? = null

    // 扩展回调接口（包含时间信息，减少Activity计算）
    interface OnProgressActionListener {
        // 进度变化（包含进度、对应时间ms、格式化时间）
        fun onProgressChanged(seekBar: SmartSeekBar, progress: Int, timeMs: Int, formattedTime: String, fromUser: Boolean) {}
        // 用户开始/结束交互
        fun onStartTrackingTouch(seekBar: SmartSeekBar) {}
        fun onStopTrackingTouch(seekBar: SmartSeekBar) {}
    }

    private var actionListener: OnProgressActionListener? = null


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


    // 对外提供API：设置总时长（由Activity调用，用于进度-时间转换）
    fun setTotalDuration(durationMs: Int) {
        this.totalDuration = durationMs
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


    // 进度-时间转换（控件内部处理）
    private fun progressToTimeMs(progress: Int): Int {
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


    // 平滑更新进度（内部动画逻辑）
    private fun smoothSetProgress(progress: Int) {
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofInt(this.progress, progress).apply {
            duration = 200
            addUpdateListener { anim ->
                val newProgress = anim.animatedValue as Int
                this@SmartSeekBar.progress = newProgress
                // 触发回调（包含时间信息）
                val timeMs = progressToTimeMs(newProgress)
                actionListener?.onProgressChanged(this@SmartSeekBar, newProgress, timeMs, formatTime(timeMs), false)
            }
            start()
        }
    }

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

        val touchX = event.x - paddingLeft
        val validTouchX = touchX.coerceIn(0f, seekBarWidth.toFloat())

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = validTouchX
                initialProgress = progress
                isDragging = false
                isUserInteracting = true // 标记用户开始交互
                thumb = thumbDragging
                updateLayerInset(insetDragging)
                actionListener?.onStartTrackingTouch(this)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isDragging) {
                    if (abs(validTouchX - initialX) < dragThreshold) return true
                    isDragging = true
                }

                val deltaProgress = ((validTouchX - initialX) * scaledTouchFactor).roundToInt()
                val newProgress = (initialProgress + deltaProgress).coerceIn(0, 100)
                if (newProgress != progress) {
                    progress = newProgress
                    // 触发回调（包含时间信息）
                    val timeMs = progressToTimeMs(newProgress)
                    actionListener?.onProgressChanged(this, newProgress, timeMs, formatTime(timeMs), true)
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                //validTouchX 用户当前手指触摸点  scaledTouchFactor 每个像素对应多少进度百分比
                //当前手指 X 坐标 × 每像素代表的进度 → 当前进度百分比
                val finalProgress = if (isDragging) progress else (validTouchX * scaledTouchFactor).roundToInt().coerceIn(0, 100)
                // 触发回调（包含时间信息）
                val timeMs = progressToTimeMs(finalProgress)
                actionListener?.onProgressChanged(this, finalProgress, timeMs, formatTime(timeMs), true)
                actionListener?.onStopTrackingTouch(this)

                smoothSetProgress(finalProgress)
                thumb = thumbNormal
                updateLayerInset(insetNormal)
                isDragging = false
                isUserInteracting = false // 标记用户结束交互
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
        progressAnimator?.cancel()
        progressAnimator = null
        actionListener = null
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
