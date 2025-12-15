package sophon.desktop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import sophon.desktop.core.State
import sophon.desktop.core.Context
import sophon.desktop.ui.theme.MaaIcons
import sophon.desktop.ui.theme.Dimens
import sophon.desktop.ui.theme.inputChipColorsMd3
import sophon.desktop.ui.theme.menuItemColorsMd3

/**
 * Adb路径功能区
 */
@Composable
fun AdbPath(
    state: State,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(30.dp)) {
            Text("Adb路径：${state.adbToolPath}", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(Dimens.paddingSmall))
            Icon(
                if (state.adbToolAvailable) MaaIcons.CheckCircle else MaaIcons.Error,
                if (state.adbToolAvailable) "valid" else "invalid",
                tint = if (state.adbToolAvailable) sophon.desktop.ui.theme.SuccessGreen else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
        }
        if (state.adbToolAvailable) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("已连接设备：", style = MaterialTheme.typography.labelMedium)
                AttachedDeviceDropdownMenu(state) { Context.selectDevice(it) }
            }
        }
    }
}

/**
 * 已关联设备列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachedDeviceDropdownMenu(state: State, onSelectDevice: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        InputChip(
            onClick = { expanded = true },
            selected = false,
            shape = MaterialTheme.shapes.small,
            colors = inputChipColorsMd3(),
            label = {
                Text(
                    text = state.selectedDevice,
                    style = MaterialTheme.typography.labelLarge
                )
            },
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowDropDown,
                    "展开收起按钮",
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            })

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            state.connectingDevices.forEach {
                DropdownMenuItem(text = { Text(it) }, colors = menuItemColorsMd3(), onClick = {
                    expanded = false
                    onSelectDevice.invoke(it)
                })
            }
        }
    }
}
