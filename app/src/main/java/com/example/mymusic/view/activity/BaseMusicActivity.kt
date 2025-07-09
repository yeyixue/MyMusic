package com.example.mymusic.view.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class BaseMusicActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutResId())
        initViews()
        setListener()
        initData()
    }

    /**
     * 获取布局资源 ID
     */
    protected abstract fun getLayoutResId(): Int

    /**
     * 初始化控件
     */
    protected abstract fun initViews()

    /**
     * 设置点击事件监听
     */
    protected abstract fun setListener()

    /**
     * 初始化数据
     */
    protected abstract fun initData()


}
