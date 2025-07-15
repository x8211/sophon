package sophon.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import sophon.desktop.ui.theme.MaaIcons

@Composable
fun OutputConsole(
    output: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF212121)
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF333333))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "输出结果",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onClear,
                    enabled = output.isNotBlank()
                ) {
                    Icon(
                        painter = MaaIcons.Error,
                        contentDescription = "清除",
                        tint = if (output.isNotBlank()) Color.White else Color.Gray
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))
            }

            // 输出内容
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp)
            ) {
                if (output.isBlank()) {
                    Text(
                        "请选择APK文件进行安装",
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    Text(
                        output,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}