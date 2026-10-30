package com.example.quizscore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** 记录答案的编码：'1' = 是，'0' = 否 */
private const val YES_CHAR = '1'
private const val NO_CHAR = '0'

@Composable
fun QuizScreen(quiz: Quiz) {
    // 已答题目按顺序拼成的字符串，如 "11 0"；长度即已答题数
    var answered by rememberSaveable { mutableStateOf("") }
    // 是否正在显示上一题的对错反馈
    var showFeedback by rememberSaveable { mutableStateOf(false) }

    val questions = quiz.questions
    val total = questions.size
    val answeredCount = answered.length
    val finished = total > 0 && answeredCount == total && !showFeedback

    // 得分 = 每题答对加 correct_score，答错扣 wrong_penalty
    val score = remember(answered, quiz) {
        answered.mapIndexed { index, char ->
            val question = questions[index]
            if ((char == YES_CHAR) == question.answerIsYes) question.correctScore
            else -question.wrongPenalty
        }.sum()
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = quiz.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "共 $total 题 · 满分 ${quiz.maxScore} 分",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            when {
                total == 0 -> EmptyView()
                finished -> ResultView(
                    quiz = quiz,
                    answers = answered,
                    score = score,
                    onRestart = {
                        answered = ""
                        showFeedback = false
                    },
                )
                else -> {
                    val currentIndex = if (showFeedback) answeredCount - 1 else answeredCount
                    val question = questions[currentIndex]

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "第 ${currentIndex + 1} / $total 题",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "当前得分 $score",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { answeredCount.toFloat() / total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(24.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Text(
                            text = question.text,
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Spacer(Modifier.height(24.dp))

                    if (!showFeedback) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Button(
                                onClick = {
                                    answered += YES_CHAR
                                    showFeedback = true
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(quiz.yesText)
                            }
                            OutlinedButton(
                                onClick = {
                                    answered += NO_CHAR
                                    showFeedback = true
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(quiz.noText)
                            }
                        }
                    } else {
                        val userSaysYes = answered.last() == YES_CHAR
                        val isCorrect = userSaysYes == question.answerIsYes
                        FeedbackView(
                            isCorrect = isCorrect,
                            delta = if (isCorrect) question.correctScore else -question.wrongPenalty,
                            correctText = if (question.answerIsYes) quiz.yesText else quiz.noText,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { showFeedback = false },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (answeredCount == total) "查看结果" else "下一题")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackView(isCorrect: Boolean, delta: Int, correctText: String) {
    val container = if (isCorrect) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val content = if (isCorrect) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = if (isCorrect) "回答正确，${signed(delta)} 分" else "回答错误，${signed(delta)} 分",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (!isCorrect) {
                Spacer(Modifier.height(4.dp))
                Text("正确答案：$correctText", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ResultView(
    quiz: Quiz,
    answers: String,
    score: Int,
    onRestart: () -> Unit,
) {
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
            Text("最终得分", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "$score",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "满分 ${quiz.maxScore} 分",
                style = MaterialTheme.typography.bodyMedium,
            )
            quiz.passScore?.let { pass ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (score >= pass) "通过（通过分 $pass）" else "未通过（通过分 $pass）",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (score >= pass) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
        Text("重新开始")
    }
    Spacer(Modifier.height(24.dp))

    Text("答题明细", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))

    quiz.questions.forEachIndexed { index, question ->
        val userSaysYes = answers[index] == YES_CHAR
        val isCorrect = userSaysYes == question.answerIsYes
        val delta = if (isCorrect) question.correctScore else -question.wrongPenalty

        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(text = "${index + 1}. ${question.text}", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "你的回答：${if (userSaysYes) quiz.yesText else quiz.noText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "正确答案：${if (question.answerIsYes) quiz.yesText else quiz.noText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = signed(delta),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCorrect) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
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
        Text(
            text = "题库中没有题目",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "请在 assets/questions.json 的 questions 数组中添加题目",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 把分值格式化为 "+3" / "-2" */
private fun signed(value: Int): String = if (value >= 0) "+$value" else "$value"
