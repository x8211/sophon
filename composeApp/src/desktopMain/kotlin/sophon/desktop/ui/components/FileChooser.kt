package sophon.desktop.ui.components

import androidx.annotation.IntDef
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import java.awt.datatransfer.DataFlavor
import java.io.File
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import sophon.desktop.ui.theme.MaaIcons
import sophon.desktop.ui.theme.Dimens
import javax.swing.JFileChooser
import javax.swing.filechooser.FileFilter
import javax.swing.filechooser.FileSystemView

/**
 * 可拖动、可点选的文件选择器
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun FileChooser(
    title: String = "",
    content: String = "",
    @FileSelectionMode fileSelectionMode: Int = JFileChooser.FILES_AND_DIRECTORIES,
    fileFilter: FileFilter? = null,
    modifier: Modifier = Modifier.fillMaxSize(),
    onFileSelected: (String?) -> Unit,
) {
    val isDragging = remember { mutableStateOf(false) }
    val borderColor = if (isDragging.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val backgroundColor = if (isDragging.value) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent
    
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.cornerRadiusLarge))
            .border(
                width = if (isDragging.value) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(Dimens.cornerRadiusLarge)
            )
            .dragAndDropTarget(
                shouldStartDragAndDrop = { true },
                target = remember {
                    object : DragAndDropTarget {
                        override fun onDrop(event: DragAndDropEvent): Boolean {
                            isDragging.value = false
                            try {
                                val nativeEvent = event.nativeEvent
                                if (nativeEvent is java.awt.dnd.DropTargetDropEvent) {
                                    nativeEvent.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY)
                                    val transferable = nativeEvent.transferable
                                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                                        @Suppress("UNCHECKED_CAST")
                                        val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                                        files.firstOrNull()?.absolutePath?.let { 
                                            onFileSelected(it) 
                                        }
                                        nativeEvent.dropComplete(true)
                                        return true
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            return false
                        }

                        override fun onEntered(event: DragAndDropEvent) {
                            isDragging.value = true
                        }

                        override fun onExited(event: DragAndDropEvent) {
                            isDragging.value = false
                        }
                        
                        override fun onEnded(event: DragAndDropEvent) {
                            isDragging.value = false
                        }
                    }
                }
            )
            .onClick {
                onFileSelected.invoke(openFileChooser(fileSelectionMode, fileFilter))
            },
        color = backgroundColor
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(Dimens.paddingExtraLarge)
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = MaaIcons.DragClick,
                    contentDescription = "上传文件",
                    modifier = Modifier.size(Dimens.iconSizeLarge),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(Dimens.spacerMedium))
                
                if (title.isNotBlank()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(Dimens.spacerSmall))
                }
                
                Text(
                    text = content.ifBlank { "点击选择文件或将文件拖放到此处" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

fun openFileChooser(
    @FileSelectionMode fileSelectionMode: Int = JFileChooser.FILES_AND_DIRECTORIES,
    fileFilter: FileFilter? = null
): String? {
    val fileChooser = JFileChooser(FileSystemView.getFileSystemView()).apply {
        dialogTitle = "选择文件"
        approveButtonText = "确定"
        dragEnabled = true
        this.fileSelectionMode = fileSelectionMode
        this.fileFilter = fileFilter
    }
    fileChooser.showOpenDialog(ComposeWindow() /* OR null */)
    return fileChooser.selectedFile?.absolutePath
}

@IntDef(JFileChooser.FILES_ONLY, JFileChooser.DIRECTORIES_ONLY, JFileChooser.FILES_AND_DIRECTORIES)
annotation class FileSelectionMode