package com.example.quizscore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** 未作答的编码字符 */
private const val UNANSWERED = '.'

/** 第 index 个选项的字母标号：A / B / C / D */
private fun letterOf(index: Int): Char = 'A' + index

/**
 * 答题页。作答阶段只显示选项、不显示对错；全部作答后提交，
 * 进入结果页统一批改，错题可点开查看正确答案。评分系统已移除。
 */
@Composable
fun QuizScreen(quiz: Quiz, onImport: () -> Unit) {
    val total = quiz.questions.size
    // 每个字符对应一题：UNANSWERED 表示未作答，'0'..'3' 表示选中的下标
    var answers by rememberSaveable { mutableStateOf(UNANSWERED.toString().repeat(total)) }
    var currentIndex by rememberSaveable { mutableStateOf(0) }
    var submitted by rememberSaveable { mutableStateOf(false) }

    val answeredCount = answers.count { it != UNANSWERED }
    val allAnswered = total > 0 && answeredCount == total

    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = quiz.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = onImport) {
                    Text("导入题库")
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "共 $total 题 · 已作答 $answeredCount 题",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            when {
                total == 0 -> EmptyView()
                submitted -> ResultsView(
                    quiz = quiz,
                    answers = answers,
                    onRestart = {
                        answers = UNANSWERED.toString().repeat(total)
                        currentIndex = 0
                        submitted = false
                    },
                )
                else -> {
                    val safeIndex = currentIndex.coerceIn(0, total - 1)
                    val question = quiz.questions[safeIndex]
                    val selectedChar = answers[safeIndex]
                    val selected = if (selectedChar == UNANSWERED) -1 else selectedChar - '0'
                    val isLast = safeIndex == total - 1

                    Text(
                        text = "第 ${safeIndex + 1} / $total 题",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { answeredCount.toFloat() / total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(24.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFD7EFD9),
                        ),
                    ) {
                        Text(
                            text = question.text,
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        question.options.forEachIndexed { index, option ->
                            if (index == selected) {
                                Button(
                                    onClick = {
                                        answers = answers.replaceRange(safeIndex, safeIndex + 1, "$index")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("${letterOf(index)}  ${option.text}")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        answers = answers.replaceRange(safeIndex, safeIndex + 1, "$index")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("${letterOf(index)}  ${option.text}")
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        OutlinedButton(onClick = { currentIndex -= 1 }, enabled = safeIndex > 0) {
                            Text("上一题")
                        }
                        if (isLast) {
                            Button(onClick = { submitted = true }, enabled = allAnswered) {
                                Text("提交答卷")
                            }
                        } else {
                            Button(onClick = { currentIndex += 1 }) {
                                Text("下一题")
                            }
                        }
                    }
                    if (isLast && !allAnswered) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "还有 ${total - answeredCount} 题未作答，全部作答后才能提交",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultsView(quiz: Quiz, answers: String, onRestart: () -> Unit) {
    val questions = quiz.questions
    val correctCount = questions.countIndexed { index, q ->
        answers[index] != UNANSWERED && (answers[index] - '0') == q.correctIndex
    }
    val total = questions.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("答题结果", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "答对 $correctCount / $total 题",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
        Text("重新作答")
    }
    Spacer(Modifier.height(24.dp))

    Text(
        text = "错题可点击展开，查看正确答案",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))

    questions.forEachIndexed { index, question ->
        val selectedChar = answers[index]
        val selected = if (selectedChar == UNANSWERED) -1 else selectedChar - '0'
        val isCorrect = selected == question.correctIndex
        val wrong = !isCorrect
        var expanded by remember { mutableStateOf(wrong) }

        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clickable { expanded = !expanded },
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${index + 1}. ${question.text}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(0.dp))
                    Text(
                        text = when {
                            isCorrect -> "  ✓"
                            selected == -1 -> "  未作答"
                            else -> "  ✗"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCorrect) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
                if (expanded) {
                    Spacer(Modifier.height(12.dp))
                    question.options.forEachIndexed { optIndex, option ->
                        val isRight = optIndex == question.correctIndex
                        val isUserWrongPick = optIndex == selected && !isRight
                        Text(
                            text = buildString {
                                append("${letterOf(optIndex)}  ${option.text}")
                                if (isRight) append("   ← 正确答案")
                                if (isUserWrongPick) append("   （你的选择）")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isRight) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isRight -> MaterialTheme.colorScheme.primary
                                isUserWrongPick -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyView() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        Text("题库中没有题目", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "题库为空。请检查 questions 数组，或点「导入题库」重新选择 questions.json",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 带下标的 count，避免额外引入 index 遍历样板。 */
private inline fun <T> List<T>.countIndexed(predicate: (Int, T) -> Boolean): Int {
    var count = 0
    forEachIndexed { index, item -> if (predicate(index, item)) count++ }
    return count
}
