package sophon.desktop.ui.theme

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.NavigationRailItemColors
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun outlinedTextFieldColorsMd3(): TextFieldColors {
    return OutlinedTextFieldDefaults.colors().copy(
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun filledTonalButtonColorsMd3(): ButtonColors {
    return ButtonDefaults.filledTonalButtonColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun inputChipColorsMd3(): SelectableChipColors {
    return InputChipDefaults.inputChipColors(
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun menuItemColorsMd3(): MenuItemColors {
    return MenuDefaults.itemColors(
        textColor = MaterialTheme.colorScheme.onSurface,
        leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun listItemColorsMd3(): ListItemColors {
    return ListItemDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surface,
        headlineColor = MaterialTheme.colorScheme.onSurface,
        supportingColor = MaterialTheme.colorScheme.onSurfaceVariant,
        trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        overlineColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun navigationRailItemColorsMd3(): NavigationRailItemColors {
    return NavigationRailItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onSurface,
        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}