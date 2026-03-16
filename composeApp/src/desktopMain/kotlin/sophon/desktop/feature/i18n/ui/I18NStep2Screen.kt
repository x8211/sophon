package sophon.desktop.feature.i18n.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import sophon.desktop.ui.components.FileChooser
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun I18NStep2Screen(onFileSelected: (String?) -> Unit, modifier: Modifier) {
    FileChooser(
        fileSelectionMode = JFileChooser.FILES_ONLY,
        fileFilter = FileNameExtensionFilter("CSV文件", "csv"),
        modifier = modifier,
        onFileSelected = onFileSelected
    )
}
