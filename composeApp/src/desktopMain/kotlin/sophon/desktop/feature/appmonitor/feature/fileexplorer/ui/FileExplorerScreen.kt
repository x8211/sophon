package sophon.desktop.feature.appmonitor.feature.fileexplorer.ui

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sophon.desktop.feature.appmonitor.feature.fileexplorer.domain.model.AppDirectoryInfo
import sophon.desktop.feature.appmonitor.feature.fileexplorer.domain.model.FileItem
import sophon.desktop.feature.device.ui.simpleVerticalScrollbar
import sophon.desktop.ui.theme.Dimens
import java.awt.Cursor
import java.util.Locale

/**
 * 文件浏览器主界面
 *
 * 显示应用的目录结构,支持目录导航
 *
 * @param packageName 应用包名，由主页面传入
 * @param viewModel ViewModel实例
 */
@Composable
fun FileExplorerScreen(
    packageName: String?,
    viewModel: FileExplorerViewModel = viewModel { FileExplorerViewModel() }
) {
    // 监听包名和刷新触发器变化，加载对应的应用目录信息
    LaunchedEffect(packageName) {
        if (packageName != null) {
            viewModel.loadAppDirectories(packageName)
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(Dimens.paddingMedium)
    ) {
        when (val state = uiState) {
            is FileExplorerUiState.Loading -> {
                LoadingView()
            }

            is FileExplorerUiState.ShowDirectories -> {
                DirectorySelectionView(
                    appInfo = state.appInfo,
                    onDirectoryClick = { viewModel.browseDirectory(it) }
                )
            }

            is FileExplorerUiState.ShowFiles -> {
                FileListView(
                    currentPath = state.currentPath,
                    files = state.files,
                    onFileClick = { file ->
                        if (file.isDirectory) {
                            viewModel.browseDirectory(file.path)
                        } else {
                            viewModel.viewFileContent(file)
                        }
                    },
                    onBackClick = { viewModel.navigateBack() },
                    onRefreshClick = { viewModel.refresh() }
                )
            }

            is FileExplorerUiState.ShowFileContent -> {
                FileContentView(
                    fileName = state.fileName,
                    filePath = state.filePath,
                    content = state.content,
                    isXml = state.isXml,
                    onBackClick = { viewModel.backToFileList() }
                )
            }

            is FileExplorerUiState.Error -> {
                ErrorView(
                    message = state.message,
                    onRetry = { viewModel.refresh() }
                )
            }
        }
    }
}

/**
 * 加载中视图
 */
@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "加载中...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 目录选择视图
 *
 * 显示应用的各个目录入口
 */
@Composable
private fun DirectorySelectionView(
    appInfo: AppDirectoryInfo,
    onDirectoryClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacerMedium)
    ) {
        // 权限提示
        if (!appInfo.isDebuggable) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFEF3C7) // 浅黄色背景
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "权限受限提示",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B)
                        )
                        Text(
                            text = "此应用为Release版本,在Android高版本系统中可能无法访问私有目录。建议使用Debug版本以获得完整访问权限。",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 数据目录卡片
        DirectoryCard(
            title = "数据目录",
            description = "应用的私有数据存储目录",
            path = appInfo.dataDir,
            icon = Icons.Default.Storage,
            onClick = { onDirectoryClick(appInfo.dataDir) }
        )

        // 缓存目录卡片
        DirectoryCard(
            title = "内部缓存目录",
            description = "应用的内部缓存存储目录",
            path = appInfo.cacheDir,
            icon = Icons.Default.FolderOpen,
            onClick = { onDirectoryClick(appInfo.cacheDir) }
        )

        // 外部缓存目录卡片
        appInfo.externalCacheDir?.let { externalCache ->
            DirectoryCard(
                title = "外部缓存目录",
                description = "应用的外部缓存存储目录(SD卡)",
                path = externalCache,
                icon = Icons.Default.Folder,
                onClick = { onDirectoryClick(externalCache) }
            )
        }
    }
}

/**
 * 目录卡片组件
 */
@Composable
private fun DirectoryCard(
    title: String,
    description: String,
    path: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            hoveredElevation = 6.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.paddingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacerMedium)
        ) {
            // 图标
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            // 文本信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 文件列表视图
 */
@Composable
private fun FileListView(
    currentPath: String,
    files: List<FileItem>,
    onFileClick: (FileItem) -> Unit,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacerSmall)
    ) {
        // 工具栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = currentPath,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onRefreshClick) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "刷新",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        // 文件列表
        if (files.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "目录为空",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .simpleVerticalScrollbar(listState),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(files) { file ->
                    FileItemRow(
                        file = file,
                        onClick = { onFileClick(file) }
                    )
                }
            }
        }
    }
}

/**
 * 文件项行组件
 */
@Composable
private fun FileItemRow(
    file: FileItem,
    onClick: () -> Unit
) {
    val backgroundColor = if (file.isDirectory) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick) // 移除enabled限制,文件也可以点击
            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
            .padding(Dimens.paddingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 图标
        Icon(
            imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = if (file.isDirectory) "目录" else "文件",
            modifier = Modifier.size(32.dp),
            tint = if (file.isDirectory) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        // 文件信息
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (file.isDirectory) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = file.permissions,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "${file.owner}:${file.group}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                file.size?.let { size ->
                    Text(
                        text = formatFileSize(size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 修改时间
        Text(
            text = file.modifiedTime,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
    }
}

/**
 * 错误视图
 */
@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "❌ 错误",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.error
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Button(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}

/**
 * 格式化文件大小
 *
 * @param bytes 字节数
 * @return 格式化后的字符串(如 "1.5 KB", "2.3 MB")
 */
private fun formatFileSize(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024

    return when {
        bytes >= gb -> "%.2f GB".format(Locale.getDefault(), bytes / gb)
        bytes >= mb -> "%.2f MB".format(Locale.getDefault(), bytes / mb)
        bytes >= kb -> "%.2f KB".format(Locale.getDefault(), bytes / kb)
        else -> "$bytes B"
    }
}

/**
 * 文件内容查看器
 *
 * @param fileName 文件名
 * @param filePath 文件路径
 * @param content 文件内容
 * @param isXml 是否为XML文件
 * @param onBackClick 返回按钮点击回调
 */
@Composable
private fun FileContentView(
    fileName: String,
    filePath: String,
    content: String,
    isXml: Boolean,
    onBackClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacerSmall)
    ) {
        // 工具栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Column {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = filePath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 文件类型标签
            Surface(
                color = if (isXml) {
                    Color(0xFF7C3AED).copy(alpha = 0.1f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = if (isXml) "XML" else "TEXT",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isXml) {
                        Color(0xFF7C3AED)
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        // 内容显示区域
        Card(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF8F9FA)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 使用SelectionContainer支持文本选择和复制
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        if (isXml) {
                            // XML格式化显示
                            XmlFormattedText(content)
                        } else {
                            // 普通文本显示
                            Text(
                                text = content,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color(0xFF1F2937)
                            )
                        }
                    }
                }

                // 滚动条
                VerticalScrollbar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(scrollState)
                )
            }
        }

        // 底部信息栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "行数: ${content.lines().size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "字符数: ${content.length}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * XML格式化文本显示
 *
 * 简单的XML语法高亮
 */
@Composable
private fun XmlFormattedText(xmlContent: String) {
    val lines = xmlContent.lines()

    Column {
        lines.forEachIndexed { index, line ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 行号
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color(0xFF9CA3AF),
                    modifier = Modifier.width(40.dp)
                )

                // XML内容 (简单高亮)
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = when {
                        line.trim().startsWith("<?") -> Color(0xFF7C3AED) // XML声明
                        line.trim().startsWith("<!--") -> Color(0xFF059669) // 注释
                        line.trim().startsWith("<") -> Color(0xFF2563EB) // 标签
                        else -> Color(0xFF1F2937) // 普通文本
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
