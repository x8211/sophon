package sophon.desktop.feature.device

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
 * 为LazyColumn添加垂直滚动条的修饰符扩展函数
 */
@Composable
fun Modifier.simpleVerticalScrollbar(
    state: LazyListState,
    width: Dp = 4.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    thumbHeightRatio: Float = 0.1f  // 滚动条高度占比，默认10%
): Modifier {
    // 使用记忆状态跟踪滚动状态
    var isScrolling by remember { mutableStateOf(false) }
    
    // 监测滑动状态变化
    LaunchedEffect(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset) {
        isScrolling = true
        delay(1000) // 延迟隐藏滚动条
        isScrolling = false
    }
    
    // 计算alpha值
    val targetAlpha = if (isScrolling || state.isScrollInProgress) 0.7f else 0f
    val duration = if (isScrolling || state.isScrollInProgress) 150 else 500
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = duration)
    )
    
    // 使用derivedStateOf计算是否到达底部
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
    
    // 使用动画平滑滚动条位置
    val scrollPosition = if (state.layoutInfo.totalItemsCount > 0) {
        if (isAtBottom) {
            // 如果到达底部，设置为100%位置
            1.0f
        } else if (state.layoutInfo.visibleItemsInfo.isEmpty()) {
            // 无可见项时设为0
            0f
        } else {
            // 根据当前可见项的位置和偏移量计算百分比
            val visibleItems = state.layoutInfo.visibleItemsInfo
            val firstVisibleItem = visibleItems.first()
            val lastVisibleItem = visibleItems.last()
            
            // 可见范围占总范围的比例
            val visibleItemsHeight = visibleItems.sumOf { it.size }
            val averageItemHeight = visibleItemsHeight / visibleItems.size.toFloat()
            val estimatedTotalHeight = averageItemHeight * state.layoutInfo.totalItemsCount
            
            // 顶部已经滚过的距离
            val scrolledDistance = (firstVisibleItem.index * averageItemHeight - firstVisibleItem.offset)
            
            // 计算滚动百分比
            val percent = min(scrolledDistance / (estimatedTotalHeight - state.layoutInfo.viewportEndOffset + state.layoutInfo.viewportStartOffset), 1f)
            max(0f, percent)
        }
    } else 0f
    
    val animatedScrollPosition by animateFloatAsState(
        targetValue = scrollPosition,
        animationSpec = spring(stiffness = 300f, dampingRatio = 1f)
    )
    
    return drawWithContent {
        drawContent()

        val needDrawScrollbar = isScrolling || state.isScrollInProgress || alpha > 0.0f
        
        if (needDrawScrollbar && state.layoutInfo.totalItemsCount > 0) {
            // 固定滚动条高度为容器高度的一部分
            val scrollbarHeight = size.height * thumbHeightRatio
            
            // 计算可滚动的轨道高度(总高度减去滚动条高度)
            val trackHeight = size.height - scrollbarHeight
            
            // 根据滚动位置计算滚动条的Y偏移
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
