package sophon.desktop.ui.theme

import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.Density

object MaaIcons {
    val CheckCircle = readIconPainter("check_circle_18dp.svg")
    val Error = readIconPainter("error_18dp.svg")
    val DragClick = readIconPainter("drag_click_24dp.svg")
    val UnfoldMore = readIconPainter("unfold_more_48dp.svg")
    val UnfoldLess = readIconPainter("unfold_less_48dp.svg")

    private fun readIconPainter(resourcePath: String): Painter {
        return useResource(resourcePath) {
            loadSvgPainter(it, Density(1f))
        }
    }
}