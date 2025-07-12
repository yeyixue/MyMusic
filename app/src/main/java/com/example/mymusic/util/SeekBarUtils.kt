package com.example.mymusic.util

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 进度条工具类，提供高级进度条功能，包括平滑滑动、缩放效果和预加载等
 */
class SeekBarUtils private constructor() {
    companion object {
        // 单例实现
        @Volatile
        private var instance: SeekBarUtils? = null

        fun getInstance() = instance ?: synchronized(this) {
            instance ?: SeekBarUtils().also { instance = it }
        }

        // 进度条配置参数
        private const val DRAG_THRESHOLD = 8f // 点击/拖动判定阈值（像素）
        private const val SEEK_BAR_ZOOM_INSET = 17 // 进度条缩放时的内边距
        private const val SEEK_BAR_NORMAL_INSET = 18 // 进度条正常状态的内边距
    }

    // 设置高级进度条功能（包含平滑滑动、缩放效果和预加载）
    fun setupAdvancedSeekBar(
        context: Context,
        seekBar: SeekBar,
        tvCurrentTime: TextView,
        tvTotalTime: TextView,
        totalDuration: Int,
        onProgressChanged: ((Int, String) -> Unit)? = null,
        onSeekComplete: ((Int) -> Unit)? = null,
        constraintLayoutClickArea:ConstraintLayout
    ) {
        // 触摸状态标记
        var isUserTouching = false
        var isDragging = false
        // 触摸相关变量
        var initialX = 0f
        var initialProgress = 0
        var currentTouchX = 0f
        // 进度条尺寸相关
        var seekBarWidth = 0
        var scaledTouchFactor = 0f

        // 记录原始的thumb和progressDrawable
        val normalThumb = seekBar.thumb
        val progressDrawable = seekBar.progressDrawable

        // 初始设置进度条样式
        if (progressDrawable is LayerDrawable) {
            progressDrawable.setLayerInset(0, 0, SEEK_BAR_NORMAL_INSET, 0, SEEK_BAR_NORMAL_INSET) // background
            progressDrawable.setLayerInset(1, 0, SEEK_BAR_NORMAL_INSET, 0, SEEK_BAR_NORMAL_INSET) // secondaryProgress
            progressDrawable.setLayerInset(2, 0, SEEK_BAR_NORMAL_INSET, 0, SEEK_BAR_NORMAL_INSET) // progress
        }

        // 测量进度条尺寸，用于计算触摸位置对应的进度
        seekBar.post {
            seekBarWidth = seekBar.width - seekBar.paddingLeft - seekBar.paddingRight
            scaledTouchFactor = 100f / seekBarWidth // 将像素距离转换为进度百分比的因子
        }

        // 设置自定义触摸监听器
        seekBar.setOnTouchListener { _, event ->
            if (seekBarWidth <= 0) return@setOnTouchListener false

            val x = event.x.coerceIn(0f, seekBarWidth.toFloat())

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isUserTouching = true
                    isDragging = false
                    initialX = x
                    initialProgress = seekBar.progress
                    currentTouchX = x

                    // 显示时间控件
                    tvCurrentTime.visibility = View.VISIBLE
                    tvTotalTime.visibility = View.VISIBLE
                    constraintLayoutClickArea.visibility = View.INVISIBLE

                    // 放大滑块和进度条
                    if (progressDrawable is LayerDrawable) {
                        progressDrawable.setLayerInset(0, 0, SEEK_BAR_ZOOM_INSET, 0, SEEK_BAR_ZOOM_INSET)
                        progressDrawable.setLayerInset(1, 0, SEEK_BAR_ZOOM_INSET, 0, SEEK_BAR_ZOOM_INSET)
                        progressDrawable.setLayerInset(2, 0, SEEK_BAR_ZOOM_INSET, 0, SEEK_BAR_ZOOM_INSET)
                    }

                    return@setOnTouchListener true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (!isDragging) {
                        // 首次检查是否达到拖动阈值
                        val deltaX = abs(x - initialX)
                        if (deltaX >= DRAG_THRESHOLD) {
                            isDragging = true
                        } else {
                            // 未达到阈值，不处理移动事件
                            return@setOnTouchListener true
                        }
                    }

                    if (isDragging) {
                        currentTouchX = x
                        // 计算从初始位置开始的进度变化量
                        val dragDeltaX = currentTouchX - initialX
                        val deltaProgress = (dragDeltaX * scaledTouchFactor).roundToInt()
                        // 新进度 = 初始进度 + 变化量，限制在0-100范围内
                        val newProgress = (initialProgress + deltaProgress).coerceIn(0, 100)

                        // 立即设置进度
                        seekBar.progress = newProgress
                        val seekTo = newProgress * totalDuration / 100
                        val formattedTime = formatTime(seekTo)
                        tvCurrentTime.text = formattedTime

                        // 通知监听器进度变化
                        onProgressChanged?.invoke(newProgress, formattedTime)
                    }

                    return@setOnTouchListener true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // 恢复滑块和进度条大小
                    if (progressDrawable is LayerDrawable) {
                        progressDrawable.setLayerInset(0, 0, SEEK_BAR_NORMAL_INSET, 0, SEEK_BAR_NORMAL_INSET)
                        progressDrawable.setLayerInset(1, 0, SEEK_BAR_NORMAL_INSET, 0, SEEK_BAR_NORMAL_INSET)
                        progressDrawable.setLayerInset(2, 0, SEEK_BAR_NORMAL_INSET, 0, SEEK_BAR_NORMAL_INSET)
                    }

                    val targetProgress = if (isDragging) {
                        // 如果是拖动结束，使用最终进度
                        seekBar.progress
                    } else {
                        // 如果是点击，计算点击位置对应的进度
                        (x * scaledTouchFactor).roundToInt()
                    }

                    // 平滑动画到目标进度
                    animateSeekBarProgress(seekBar, from = seekBar.progress, to = targetProgress)

                    // 计算毫秒并回调
                    val seekToMillis = targetProgress * totalDuration / 100
                    onSeekComplete?.invoke(seekToMillis)

                    // 更新时间显示
                    tvCurrentTime.text = formatTime(seekToMillis)
                    tvCurrentTime.visibility = View.GONE
                    tvTotalTime.visibility = View.GONE
                    constraintLayoutClickArea.visibility = View.VISIBLE

                    isUserTouching = false
                    isDragging = false

                    return@setOnTouchListener true
                }
            }

            false
        }
    }

    // 进度条平滑动画
    private fun animateSeekBarProgress(seekBar: SeekBar, from: Int, to: Int) {
        val animator = ValueAnimator.ofInt(from, to).apply {
            duration = 200
            addUpdateListener {
                val animatedValue = it.animatedValue as Int
                seekBar.progress = animatedValue
            }
        }
        animator.start()
    }

    // 时间格式化方法
    fun formatTime(ms: Int): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    // 更新进度条进度（带平滑动画）
    fun updateProgress(seekBar: SeekBar, progress: Int, withAnimation: Boolean = true) {
        if (withAnimation) {
            animateSeekBarProgress(seekBar, from = seekBar.progress, to = progress)
        } else {
            seekBar.progress = progress
        }
    }

    // 更新预加载进度
    fun updateBufferProgress(seekBar: SeekBar, bufferProgress: Int) {
        seekBar.secondaryProgress = bufferProgress
    }
}
