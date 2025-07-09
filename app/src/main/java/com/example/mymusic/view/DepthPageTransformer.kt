import androidx.viewpager2.widget.ViewPager2
import android.view.View
import kotlin.math.abs

class DepthPageTransformer : ViewPager2.PageTransformer {
    private val MIN_SCALE = 0.75f // 最小缩放比例

    override fun transformPage(view: View, position: Float) {
        view.apply {
            val pageWidth = width
            when {
                // 情况1：页面完全在左侧（不可见）
                position < -1 -> alpha = 0f

                // 情况2：页面从左侧滑入，或当前页滑出到左侧
                position <= 0 -> {
                    alpha = 1f
                    translationX = 0f
                    scaleX = 1f
                    scaleY = 1f
                }

                // 情况3：页面从右侧滑入，或当前页滑出到右侧
                position <= 1 -> {
                    alpha = 1 - position // 透明度渐变（1→0）
                    translationX = pageWidth * -position // 水平偏移抵消默认滑动
                    // 缩放渐变（MIN_SCALE→1）
                    val scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - abs(position))
                    scaleX = scaleFactor
                    scaleY = scaleFactor
                }

                // 情况4：页面完全在右侧（不可见）
                else -> alpha = 0f
            }
        }
    }
}