package com.example.mymusic.adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.mymusic.R

/**
 * RecyclerView的适配器，用于展示NoteInfo数据列表
 * @param infoList 要显示的NoteInfo数据列表
 */
class MusicRecycleViewAdapter(
//    private var infoList: List<NoteInfo>
    initialList: List<NoteInfo> = emptyList()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // 定义属性并实现自定义 setter
    var infoList: List<NoteInfo> = initialList
        set(value) {
            val diffResult = DiffUtil.calculateDiff(MyDiffCallback(field, value))
            field = value  // field 指代旧值
            // 分发差异刷新（必须在主线程）
            diffResult.dispatchUpdatesTo(this)
        }

    // 定义视图类型常量（Kotlin中用伴生对象存放）
    companion object {
        const val TYPE_ITEM1 = 1  // 第一种视图类型
        const val TYPE_ITEM2 = 2  // 第二种视图类型
    }

    // 添加 getItemViewType 方法，区分视图类型
    override fun getItemViewType(position: Int): Int {
        // 根据数据中的 type 字段返回对应类型
        return infoList[position].type
    }

    /**
     * 创建ViewHolder实例，用于显示列表项
     * @param viewGroup 父视图组
     * @param viewType 视图类型
     * @return 返回MyHolder实例
     */
    override fun onCreateViewHolder(
        viewGroup: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        // 加载列表项布局文件

        val inflater = LayoutInflater.from(viewGroup.context)
        return when (viewType) {
            TYPE_ITEM1 -> {
                // 加载第一种布局
                val view = inflater.inflate(R.layout.item_main_music, viewGroup, false)
                MyHolderMusic(view)
            }
            TYPE_ITEM2 -> {
                // 加载第二种布局
                val view = inflater.inflate(R.layout.item_main_video, viewGroup, false)
                MyHolderVideo(view)
            }
            else -> throw IllegalArgumentException("未知视图类型")
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        // 从数据列表中获取对应位置的数据项
        val info = infoList[position]

        // 调用ViewHolder的绑定方法设置数据
        when (holder) {
            is MyHolderMusic -> holder.bind(info)  // 绑定到第一种ViewHolder
            is MyHolderVideo -> holder.bind(info)  // 绑定到第二种ViewHolder
        }

        // 设置列表项的点击事件
        // 调用接口的方法时，实际运行的是 “实现了该接口的类” 所重写的方法
        // 在activity中实现了接口方法
        holder.itemView.setOnClickListener {
            // 回调监听器的点击方法，传递位置和数据项
            onItemClickListener?.onItemClick(info)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>  // 新增 payload 参数
    ) {
        // 如果有 payload，优先处理局部刷新
        if (payloads.isNotEmpty()) {
            val payload = payloads[0] as? Map<*, *> ?: return
            val info = infoList[position]
            when (holder) {
                is MyHolderMusic -> {
                    // 处理 color 变化
                    payload["color"]?.let { color ->
                        val drawable = holder.itemView.context.getDrawable(color as Int)
                        holder.imageView.background = drawable
                    }
                }
                is MyHolderVideo -> {
                    // 处理 imgRes 变化
                    payload["imgRes"]?.let { imgRes ->
                        val drawable = holder.itemView.context.getDrawable((imgRes as Int).takeIf { it != 0 }
                            ?: R.drawable.ic_launcher_background)
                        holder.imageView.setImageDrawable(drawable)
                    }
                }
            }
            return  // 局部刷新后直接返回，无需全量绑定
        }
        // 没有 payload → 全量绑定（调用原来的 onBindViewHolder）
        super.onBindViewHolder(holder, position, payloads)
    }





    /**
     * 将数据绑定到ViewHolder上
     * @param holder 要绑定数据的ViewHolder
     * @param position 数据在列表中的位置
     */
//    override fun onBindViewHolder(holder: MusicRecycleViewAdapter.MyHolder, position: Int) {
//
//    }

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
    inner class MyHolderMusic(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 缓存列表项中的TextView控件引用
        val imageView: ImageView = itemView.findViewById(R.id.imageView)

        /**
         * 将NoteInfo数据绑定到视图上
         * @param info 要绑定的NoteInfo数据
         */
        fun bind(info: NoteInfo) {
            // 通过 itemView 的上下文获取颜色对应的 Drawable
            val drawable = itemView.context.getDrawable(info.color)
            imageView.background = drawable
        }
    }

    // 修改 MyHolderVideo 的 bind 方法，使用动态数据
    inner class MyHolderVideo(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)

        fun bind(info: NoteInfo) {
            // 使用数据中的 imgRes 字段（而非硬编码）
            val drawable = itemView.context.getDrawable(info.imgRes ?: R.drawable.ic_launcher_background)
            imageView.setImageDrawable(drawable)
        }
    }





    class MyDiffCallback(
        private val oldList: List<NoteInfo>, private val newList: List<NoteInfo>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            // 把这里想成是比较holder的类型, 比如纯文本的holder和纯图片的holder的type肯定不同
            return oldList[oldItemPosition].type == newList[newItemPosition].type
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            // 把这里想成是同一种holder的比较,比如都是纯文本holder,但是title不一致
//            return oldList[oldItemPosition].color == newList[newItemPosition].color
            return oldList.equals(newList)
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]

            // 构建差异数据的 payload
            val diffPayload = mutableMapOf<String, Any>()
            if (oldItem.color != newItem.color) {
                diffPayload["someField"] = newItem.color
            }
            // ... 其他需要差异的字段

            return diffPayload


        }
    }


    /**
     * 列表项点击事件的回调接口
     * 由Activity具体实现这个接口声明
     */
    interface OnItemClickListener {
        /**
         * 当列表项被点击时调用
         * @param noteInfo 被点击的列表项对应的数据
         */
        fun onItemClick(noteInfo: NoteInfo)
    }

    // 点击事件监听器变量
    private var onItemClickListener: OnItemClickListener? = null

    /**
     * 设置列表项点击事件监听器
     * @param listener 实现了OnItemClickListener接口的监听器
     */
    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }
}

/**
 * 数据类，用于存储笔记信息
 * @param name 笔记名称
 */
// 新增 type 字段，标识当前item是音乐类型还是视频类型
data class NoteInfo(
    val color: Int,    // 颜色资源（TYPE_ITEM1）
    val type: Int,     // 视图类型
    val imgRes: Int? = null  // 图片资源（TYPE_ITEM2，可选）
)
