package sophon.desktop.feature.taskrecord

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sophon.desktop.ui.theme.MaaIcons

/**
 * 递归渲染组件树
 */
@Composable
fun ComponentTreeRenderer(
    component: LifecycleComponent,
    level: Int,
    selectedComponent: LifecycleComponent?,
    onItemClick: (LifecycleComponent) -> Unit,
) {
    LifecycleListItem(
        component = component,
        level = level,
        isSelected = component == selectedComponent,
        selectedComponent = selectedComponent,
        onItemClick = { onItemClick(it) }
    )
}

@Composable
fun LifecycleListItem(
    component: LifecycleComponent,
    level: Int,
    isSelected: Boolean = false,
    selectedComponent: LifecycleComponent? = null,
    onItemClick: (LifecycleComponent) -> Unit = {},
) {
    // 如果组件被选中，自动展开
    var isExpanded by remember { mutableStateOf(true) }
    // 当选中状态变化时更新展开状态
    LaunchedEffect(isSelected) {
        if (isSelected && component.children().isNotEmpty()) {
            isExpanded = true
        }
    }

    val hasChildren = component.children().isNotEmpty()

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onItemClick(component) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 缩进
            Spacer(modifier = Modifier.width((level * 15).dp))

            // 展开/收起图标
            if (hasChildren) {
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = if (isExpanded) MaaIcons.UnfoldLess else MaaIcons.UnfoldMore,
                        contentDescription = if (isExpanded) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(24.dp))
            }

            // 组件卡片
            Card(
                colors = CardDefaults.cardColors(
                    containerColor =
                        if (component.isRunning()) MaterialTheme.colorScheme.inversePrimary
                        else MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (component.isRunning()) 4.dp else 1.dp
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = component.name(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color =
                        if (component.isRunning()) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                )
            }
        }

        // 子组件列表
        if (isExpanded && hasChildren) {
            component.children().forEach { child ->
                LifecycleListItem(
                    component = child,
                    level + 1,
                    isSelected = child == selectedComponent,
                    selectedComponent = selectedComponent,
                    onItemClick = { onItemClick(it) }  // 直接传递原始的 onItemClick 函数，这样子组件的点击会被正确处理
                )
            }
        }
    }
}

@Composable
fun StateTag(lifecycleComponent: LifecycleComponent, modifier: Modifier = Modifier) {
    val (backgroundColor, textColor) = when (lifecycleComponent) {
        is ActivityInfo -> {
            if (lifecycleComponent.resumed) MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
            else if (lifecycleComponent.stopped) MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
            else MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
        }

        is FragmentInfo -> {
            when (lifecycleComponent.state) {
                FragmentInfo.Companion.State.RESUMED -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
                FragmentInfo.Companion.State.ATTACHED,
                FragmentInfo.Companion.State.CREATED,
                FragmentInfo.Companion.State.VIEW_CREATED,
                FragmentInfo.Companion.State.AWAITING_EXIT_EFFECTS,
                FragmentInfo.Companion.State.ACTIVITY_CREATED,
                    -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary

                FragmentInfo.Companion.State.STARTED,
                FragmentInfo.Companion.State.AWAITING_ENTER_EFFECTS,
                    -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary

                else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
            }
        }

        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = lifecycleComponent.stateText(),
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = if (lifecycleComponent.isRunning()) MaterialTheme.colorScheme.onPrimary.copy(
                    alpha = 0.3f
                ) else backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = if (lifecycleComponent.isRunning()) FontWeight.Bold else FontWeight.Normal
        ),
        color = textColor
    )
}

@Composable
fun LifecycleDetailCard(
    component: LifecycleComponent,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(scrollState)) {
            // 标题
            Text(
                text = "组件详情",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 类名
            DetailItem(title = "类名", value = component.name())

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // 状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "生命周期状态: ",
                    style = MaterialTheme.typography.titleMedium
                )

                Box(modifier = Modifier.padding(start = 8.dp)) {
                    StateTag(component)
                }
            }

            // 根据组件类型显示额外信息
            when (component) {
                is ActivityInfo -> ActivityDetailInfo(component)
                is FragmentInfo -> FragmentDetailInfo(component)
            }
        }
    }
}

@Composable
private fun DetailItem(title: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "$title: ",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ActivityDetailInfo(activity: ActivityInfo) {
    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(8.dp))
    DetailItem(title = "包名", value = activity.packageName)
    Spacer(modifier = Modifier.height(8.dp))
    DetailItem(title = "是否处于Resumed状态", value = activity.resumed.toString())
    Spacer(modifier = Modifier.height(8.dp))
    DetailItem(title = "是否处于Stopped状态", value = activity.stopped.toString())
    Spacer(modifier = Modifier.height(8.dp))
    DetailItem(title = "是否已销毁", value = activity.finished.toString())
}

@Composable
private fun FragmentDetailInfo(fragment: FragmentInfo) {
    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(8.dp))

    DetailItem(title = "Tag", value = fragment.tag ?: "null")
    Spacer(modifier = Modifier.height(8.dp))

    DetailItem(title = "状态码", value = "${fragment.state.code}")
    Spacer(modifier = Modifier.height(8.dp))

    DetailItem(title = "Who", value = fragment.who)
    Spacer(modifier = Modifier.height(8.dp))

    DetailItem(title = "是否隐藏", value = fragment.hidden.toString())
    Spacer(modifier = Modifier.height(8.dp))

    DetailItem(title = "用户可见提示", value = fragment.userVisibleHint.toString())
    Spacer(modifier = Modifier.height(8.dp))

    DetailItem(title = "容器信息", value = fragment.container.ifEmpty { "无" })
    Spacer(modifier = Modifier.height(8.dp))

}