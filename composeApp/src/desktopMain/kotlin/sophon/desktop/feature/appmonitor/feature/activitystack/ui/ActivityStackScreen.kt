package sophon.desktop.feature.appmonitor.feature.activitystack.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sophon.desktop.feature.appmonitor.feature.activitystack.domain.model.LifecycleComponent

/**
 * Activity栈主界面
 * 
 * @param packageName 应用包名，由主页面传入
 * @param refreshTrigger 刷新触发器，每次变化时重新加载数据
 * @param viewModel ViewModel实例
 */
@Composable
fun ActivityStackScreen(
    packageName: String?,
    refreshTrigger: Long,
    viewModel: ActivityStackViewModel = viewModel { ActivityStackViewModel() }
) {
    val activities by viewModel.uiState.collectAsState()
    var selectedComponent by remember { mutableStateOf<LifecycleComponent?>(null) }

    // 监听包名和刷新触发器变化，加载对应的Activity栈信息
    LaunchedEffect(packageName, refreshTrigger) {
        if (packageName != null) {
            viewModel.loadActivityStack(packageName)
        }
    }


    Row(modifier = Modifier.fillMaxSize()) {
        // 左列 - 组件列表 (1/2宽度)
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 添加标题和分隔线
                Text(
                    text = "组件列表",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp)
                )

                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(activities.size) {
                        val component = activities[it]

                        // 使用自定义扩展函数来渲染组件树
                        ComponentTreeRenderer(
                            component = component,
                            level = 0,
                            selectedComponent = selectedComponent,
                            onItemClick = { clickedComponent ->
                                selectedComponent = clickedComponent
                            }
                        )
                    }
                }
            }
        }

        // 右列 - 详细信息 (1/2宽度)
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
            if (selectedComponent != null) {
                LifecycleDetailCard(
                    component = selectedComponent!!,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                // 未选择组件时的提示
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "请从左侧列表选择一个组件查看详情",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
