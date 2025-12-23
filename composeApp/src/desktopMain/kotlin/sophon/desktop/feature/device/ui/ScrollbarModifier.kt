package sophon.desktop.feature.device.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

/**
 * 为 LazyColumn 添加简易垂直滚动条的修饰符扩展函数
 *
 * 此滚动条会自动淡入淡出，并根据列表滚动位置更新滑块位置。
 * 实现原理是基于 content draw 之上绘制矩形滑块。
 *
 * @param state LazyColumn 的列表状态，用于获取滚动位置和列表信息
 * @param width 滚动条的宽度，默认为 4.dp
 * @param color 滚动条的颜色，默认为当前主题的主色
 * @param thumbHeightRatio 滚动条滑块高度占可视区域高度的比例，默认为 0.1 (10%)
 */
@Composable
fun Modifier.simpleVerticalScrollbar(
    state: LazyListState,
    width: Dp = 4.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    thumbHeightRatio: Float = 0.1f
): Modifier {
    // 使用记忆状态跟踪滚动条是否应该显示（手动滚动或正在惯性滑动）
    var isScrolling by remember { mutableStateOf(false) }
    
    // 监测滑动状态变化，并在停止滑动 1 秒后隐藏
    LaunchedEffect(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset) {
        isScrolling = true
        delay(1000) // 延迟隐藏滚动条
        isScrolling = false
    }
    
    // 计算滚动条的透明度，实现淡入淡出动画
    // 正在滚动时为 0.7f，否则为 0f
    val targetAlpha = if (isScrolling || state.isScrollInProgress) 0.7f else 0f
    val duration = if (isScrolling || state.isScrollInProgress) 150 else 500
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = duration)
    )
    
    // 使用 derivedStateOf 优化计算，判断是否已经滚动到底部
    val isAtBottom by remember {
        derivedStateOf {
            val lastVisibleItem = state.layoutInfo.visibleItemsInfo.lastOrNull()
            if (lastVisibleItem == null) {
                false
            } else {
                // 如果最后一个可见项是列表中的最后一项，并且完全可见或几乎完全可见
                lastVisibleItem.index == state.layoutInfo.totalItemsCount - 1 &&
                        (lastVisibleItem.offset + lastVisibleItem.size <= state.layoutInfo.viewportEndOffset + 10)
            }
        }
    }
    
    // 计算滚动滑块的垂直位置 (0.0f - 1.0f)
    val scrollPosition = if (state.layoutInfo.totalItemsCount > 0) {
        if (isAtBottom) {
            // 如果到达底部，强制设置为 100% 位置，避免计算误差
            1.0f
        } else if (state.layoutInfo.visibleItemsInfo.isEmpty()) {
            // 无可见项时设为 0
            0f
        } else {
            // 估算当前滚动进度
            val visibleItems = state.layoutInfo.visibleItemsInfo
            val firstVisibleItem = visibleItems.first()
            
            // 计算平均每个 Item 的高度
            val visibleItemsHeight = visibleItems.sumOf { it.size }
            val averageItemHeight = visibleItemsHeight / visibleItems.size.toFloat()
            // 估算总内容高度
            val estimatedTotalHeight = averageItemHeight * state.layoutInfo.totalItemsCount
            
            // 计算顶部已经滚过的距离
            val scrolledDistance = (firstVisibleItem.index * averageItemHeight - firstVisibleItem.offset)
            
            // 计算滚动百分比
            // 分母为：总可滚动距离 (总高度 - 视口高度)
            val totalScrollableDistance = estimatedTotalHeight - state.layoutInfo.viewportEndOffset + state.layoutInfo.viewportStartOffset
            val percent = min(scrolledDistance / totalScrollableDistance, 1f)
            max(0f, percent)
        }
    } else 0f
    
    // 对滚动位置应用弹簧动画，使其移动更平滑
    val animatedScrollPosition by animateFloatAsState(
        targetValue = scrollPosition,
        animationSpec = spring(stiffness = 300f, dampingRatio = 1f)
    )
    
    return drawWithContent {
        drawContent() // 绘制原始内容

        val needDrawScrollbar = isScrolling || state.isScrollInProgress || alpha > 0.0f
        
        if (needDrawScrollbar && state.layoutInfo.totalItemsCount > 0) {
            // 固定滚动条滑块高度为容器高度的一部分
            val scrollbarHeight = size.height * thumbHeightRatio
            
            // 计算可滚动的轨道高度(总容器高度 - 滑块高度)
            val trackHeight = size.height - scrollbarHeight
            
            // 根据滚动位置比例计算滑块的 Y 坐标偏移
            val scrollbarOffsetY = trackHeight * animatedScrollPosition
            
            drawRect(
                color = color,
                topLeft = Offset(this.size.width - width.toPx(), scrollbarOffsetY),
                size = Size(width.toPx(), scrollbarHeight),
                alpha = alpha
            )
        }
    }
}
