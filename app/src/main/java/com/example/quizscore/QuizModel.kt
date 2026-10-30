package com.example.quizscore

import android.content.Context
import org.json.JSONObject

/** 一道题：只有「是 / 否」两个选项。 */
data class Question(
    /** 题干 */
    val text: String,
    /** 正确答案：true 表示选「是」 */
    val answerIsYes: Boolean,
    /** 答对加分 */
    val correctScore: Int,
    /** 答错扣分（正数，扣分时减掉） */
    val wrongPenalty: Int,
)

/** 一套题。 */
data class Quiz(
    val title: String,
    /** 通过分；JSON 中不写则为 null（不判定通过与否） */
    val passScore: Int?,
    /** 「是」按钮文案 */
    val yesText: String,
    /** 「否」按钮文案 */
    val noText: String,
    val questions: List<Question>,
) {
    /** 满分 = 所有题答对得分之和 */
    val maxScore: Int get() = questions.sumOf { it.correctScore }
}

/**
 * 从 assets/questions.json 读取题库。
 * 增删题目只需要修改这个 JSON 文件，无需改代码。
 */
object QuizLoader {

    private const val ASSET_FILE = "questions.json"

    fun load(context: Context): Result<Quiz> = runCatching {
        val raw = context.assets.open(ASSET_FILE).bufferedReader().use { it.readText() }
        parse(raw)
    }

    fun parse(raw: String): Quiz {
        val root = JSONObject(raw)
        val array = root.getJSONArray("questions")

        val questions = buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val answer = item.optString("answer", "yes").trim().lowercase()
                require(answer == "yes" || answer == "no") {
                    "第 ${i + 1} 题的 answer 只能是 yes 或 no，当前为「$answer」"
                }
                require(!item.optString("text").isNullOrBlank()) {
                    "第 ${i + 1} 题缺少 text（题干）"
                }
                add(
                    Question(
                        text = item.getString("text").trim(),
                        answerIsYes = answer == "yes",
                        correctScore = item.optInt("correct_score", 1),
                        wrongPenalty = item.optInt("wrong_penalty", 1),
                    )
                )
            }
        }

        return Quiz(
            title = root.optString("title", "是/否打分"),
            passScore = if (root.has("pass_score")) root.optInt("pass_score") else null,
            yesText = root.optString("yes_text", "是"),
            noText = root.optString("no_text", "否"),
            questions = questions,
        )
    }
}
