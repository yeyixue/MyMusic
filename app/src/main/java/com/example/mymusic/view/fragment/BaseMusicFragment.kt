package com.example.mymusic.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

abstract class BaseMusicFragment : Fragment() {

    protected lateinit var rootView: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //将 XML 布局文件转化为 View 的工具类
        rootView = inflater.inflate(getLayoutResId(), container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        setListener()
        initData()
        initExtra() // 可选钩子
    }

    protected open fun initExtra() {
        // 可选的扩展方法，默认不做事
    }
    /**
     * 加载布局文件
     */
    protected abstract fun getLayoutResId(): Int

    /**
     * 初始化控件
     */
    protected abstract fun initView()

    /**
     * 监听事件
     */
    protected abstract fun setListener()

    /**
     * 初始化数据
     */
    protected abstract fun initData()

}