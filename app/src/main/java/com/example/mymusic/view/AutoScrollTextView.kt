package com.example.mymusic.view

import android.R.attr.focusableInTouchMode
import android.content.Context
import android.os.Build
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi

// AutoScrollTextView.kt
@RequiresApi(Build.VERSION_CODES.O)
class AutoScrollTextView(context: Context, attrs: AttributeSet? = null) : androidx.appcompat.widget.AppCompatTextView(context, attrs) {

    init {
        // 设置必要属性
        ellipsize = TextUtils.TruncateAt.MARQUEE
        isSingleLine = true
        marqueeRepeatLimit = -1  // 无限循环
        focusable = View.FOCUSABLE
    }

    // 强制获取焦点
    override fun isFocused(): Boolean = true

    // 控制开始滚动的时机
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 延迟启动，确保布局完成
        postDelayed({ requestFocus() }, 100)
    }
}