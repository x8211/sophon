package sophon.desktop.feature.i18n.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import sophon.desktop.feature.i18n.domain.model.I18nProject

@Composable
fun I18NStep3Screen(
    project: I18nProject,
    uiState: UiState,
    modifier: Modifier = Modifier,
    onSelectModule: (String) -> Unit,
) {
    Column(
        modifier = modifier
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(8.dp)
            )
            .verticalScroll(rememberScrollState())
            .padding(8.dp)
    ) {
        project.modules.forEach { module ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onSelectModule(module.absolutePath) },
                color = if (module.absolutePath == uiState.modulePath)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Text(
                    module.name,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (module.absolutePath == uiState.modulePath)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
