// MyApplication.kt
package com.example.mymusic

import android.app.Application
import com.facebook.drawee.backends.pipeline.Fresco

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化Fresco（必须在使用SimpleDraweeView之前调用）
        Fresco.initialize(this)
    }
}