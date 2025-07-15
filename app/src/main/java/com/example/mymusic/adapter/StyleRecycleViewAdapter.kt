package com.example.mymusic.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mymusic.R


class StyleRecycleViewAdapter (
    private val infoList: List<MusicStyleInfo>
) : RecyclerView.Adapter<StyleRecycleViewAdapter.MyHolder>() {

    /**
     * 创建ViewHolder实例，用于显示列表项
     * @param viewGroup 父视图组
     * @param viewType 视图类型
     * @return 返回MyHolder实例
     */
    override fun onCreateViewHolder(
        viewGroup: ViewGroup,
        viewType: Int
    ): StyleRecycleViewAdapter.MyHolder {
        // 加载列表项布局文件
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_music_style, viewGroup, false)

        return MyHolder(view)
    }

    /**
     * 将数据绑定到ViewHolder上
     * @param holder 要绑定数据的ViewHolder
     * @param position 数据在列表中的位置
     */
    override fun onBindViewHolder(holder: StyleRecycleViewAdapter.MyHolder, position: Int) {
        // 从数据列表中获取对应位置的数据项
        val note = infoList[position]
        // 调用ViewHolder的绑定方法设置数据
        holder.bind(note)

        // 设置列表项的点击事件
        // 调用接口的方法时，实际运行的是 “实现了该接口的类” 所重写的方法
        // 在activity中实现了接口方法
        holder.itemView.setOnClickListener {
            // 回调监听器的点击方法，传递位置和数据项
            onItemClickListener?.onItemClick(note)
        }
    }

    /**
     * 获取数据列表的大小
     * @return 数据列表的大小
     */
    override fun getItemCount(): Int {
        return infoList.size
    }

    /**
     * 列表项的ViewHolder类，用于缓存视图引用
     * @param itemView 列表项的根视图
     */
    inner class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageViewIcon: ImageView = itemView.findViewById(R.id.ivSongStyleIcon)
        private val textViewContent: TextView = itemView.findViewById(R.id.contentStyleMusic)

        fun bind(info: MusicStyleInfo) {
            imageViewIcon.setImageResource(info.coverResId)
            textViewContent.text = info.content
        }
    }


    /**
     * 列表项点击事件的回调接口
     * 由Activity具体实现这个接口声明
     */
    fun interface OnItemClickListener {
        /**
         * 当列表项被点击时调用
         * @param noteInfo 被点击的列表项对应的数据
         */
        fun onItemClick(noteInfo: MusicStyleInfo)
    }

    // 点击事件监听器变量
    private var onItemClickListener: OnItemClickListener? = null

    /**
     * 设置列表项点击事件监听器
     * @param listener 实现了OnItemClickListener接口的监听器
     */
    fun setOnItemClickListener(listener: OnItemClickListener?) {
        this.onItemClickListener = listener
    }
}

/**
 * 数据类，用于存储笔记信息
 * @param name 笔记名称
 */
    data class MusicStyleInfo(
        val coverResId: Int,  // 封面资源ID
        val content: String
    )
// 可以添加更多属性，如content, date等
