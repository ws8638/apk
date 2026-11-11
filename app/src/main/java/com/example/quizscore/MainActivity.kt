package com.example.quizscore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            ) {
                QuizApp()
            }
        }
    }
}

@Composable
private fun QuizApp() {
    val context = LocalContext.current

    // 当前题库状态：启动时用「外部优先、assets 兜底」加载；导入成功后替换。
    var state by remember { mutableStateOf(QuizLoader.load(context)) }

    // SAF 文件选择器。用 "*/*" 以兼容各文件管理器对 .json 返回的不同 MIME。
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) state = QuizLoader.importFromUri(context, uri)
    }

    fun openPicker() = picker.launch(arrayOf("*/*"))

    state.fold(
        onSuccess = { quiz ->
            // key(quiz)：题库内容变化时重建 QuizScreen，重置答题进度，
            // 避免旧进度与新题数错位导致越界。
            key(quiz) { QuizScreen(quiz, onImport = { openPicker() }) }
        },
        onFailure = { error ->
            ErrorScreen(error.message, onImport = { openPicker() })
        },
    )
}

@Composable
private fun ErrorScreen(message: String?, onImport: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("读取题库失败", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Text(
                text = message ?: "未知错误",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "点「导入题库」手动选择 questions.json，" +
                    "或把文件用 adb push 放到 Android/data/com.example.quizscore/files/。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onImport) {
                Text("导入题库")
            }
        }
    }
}
