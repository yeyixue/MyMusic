package com.example.mymusic.view.mymusicfragment
import android.util.Log
import android.widget.LinearLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymusic.R
import com.example.mymusic.adapter.FindRecycleViewAdapter
import com.example.mymusic.adapter.MusicFindStyleInfo
import com.example.mymusic.adapter.MyMusicPlayListAdapter
import com.example.mymusic.adapter.PlayListInfo
import com.example.mymusic.view.fragment.BaseMusicFragment

class PlayListFragment : BaseMusicFragment() {

    // 视图控件
    private lateinit var recyclerView: RecyclerView
    private lateinit var linearInsert: LinearLayout
    private lateinit var linearCreate: LinearLayout

    // 适配器和数据
    private lateinit var playListAdapter: MyMusicPlayListAdapter
    private val playListData = mutableListOf<PlayListInfo>()

    // 返回当前Fragment的布局ID
    override fun getLayoutResId(): Int {
        return R.layout.fragment_play_list // 对应你提供的布局文件ID（请确保布局文件名正确，此处假设为fragment_playlist.xml）
    }

    // 初始化视图
    override fun initView() {
        // 获取RecyclerView并初始化
        setupRecyclerView()



        // 获取按钮控件
        linearInsert = rootView.findViewById(R.id.linearLayoutInsert)
        linearCreate = rootView.findViewById(R.id.linearLayoutCreate)
    }

    // 设置事件监听
    override fun setListener() {
        // 导入按钮点击事件
        linearInsert.setOnClickListener {
            // 处理导入逻辑
            Log.d("MyMusic","导入歌单")
        }

        // 创建按钮点击事件
        linearCreate.setOnClickListener {
            // 处理创建逻辑（例如跳转到创建歌单页面）
            Log.d("MyMusic","导入歌单")
        }


    }

    // 初始化数据
    override fun initData() {
        loadMockPlayListData()
    }


    private fun setupRecyclerView() {
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recycleViewPlayList)

        // 设置垂直布局管理器，并启用页面切换模式
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        recyclerView.layoutManager = layoutManager

        // 创建适配器并设置数据
        playListAdapter = MyMusicPlayListAdapter(playListData)
        recyclerView.adapter = playListAdapter

        // 设置点击事件监听
        playListAdapter.setOnItemClickListener(object : MyMusicPlayListAdapter.OnItemClickListener{
            override fun onItemClick(noteInfo: PlayListInfo) {
                Log.d("mFindRecycleViewAdapter","  点击了$noteInfo  ")

            }
        })

        // 监听滚动事件，实现超过中间位置自动切换
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

            }
        })
    }

    // 模拟加载歌单数据
    private fun loadMockPlayListData() {
        val mockData = listOf(
            PlayListInfo(R.mipmap.img0, "我喜欢的音乐", true,18),
            PlayListInfo(R.mipmap.tiktok, "抖音收藏的音乐", true,128),
            PlayListInfo(R.mipmap.tiktok, "歌单1", true,82),
            PlayListInfo(R.mipmap.tiktok, "其他歌单", true,81),
            PlayListInfo(R.mipmap.tiktok, "其他歌单2", true,81),

        )
        // 添加数据并刷新适配器
        playListData.addAll(mockData)
        playListAdapter.notifyDataSetChanged()
    }

}