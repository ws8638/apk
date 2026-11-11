package com.example.quizscore

import android.content.Context
import android.net.Uri
import android.util.Log
import org.json.JSONObject
import java.io.File

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

    private const val TAG = "QuizLoader"
    private const val FILE_NAME = "questions.json"

    /**
     * 读取题库。优先使用「外部文件」，找不到再回退到 APK 内置 assets。
     *
     * 外部文件位于应用专属外部目录（Android 10+ 免存储权限）：
     *   /sdcard/Android/data/<applicationId>/files/questions.json
     * 用 `adb push` 或支持访问 Android/data 的文件管理器替换它，即可在
     * 【不重新打包、不重装 APK】的情况下更新题库。
     */
    fun load(context: Context): Result<Quiz> = runCatching {
        val external = externalFile(context)
        val (raw, source) = if (external != null && external.isFile) {
            external.readText() to external.absolutePath
        } else {
            context.assets.open(FILE_NAME).bufferedReader().use { it.readText() } to "assets/$FILE_NAME"
        }
        Log.i(TAG, "题库来源: $source")
        try {
            parse(raw)
        } catch (e: Exception) {
            throw IllegalStateException("来源「$source」解析失败：${e.message}", e)
        }
    }

    /**
     * SAF 导入：读取用户选中的 json → 校验通过后写入应用专属外部目录，
     * 之后每次启动都会自动加载这份新题库（也可用 adb push 覆盖）。
     * 校验失败则不落盘，避免用坏文件覆盖掉现有可用的题库。
     */
    fun importFromUri(context: Context, uri: Uri): Result<Quiz> = runCatching {
        val raw = context.contentResolver.openInputStream(uri)
            ?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalStateException("无法读取所选文件")
        val quiz = parse(raw)
        val target = externalFile(context)
            ?: throw IllegalStateException("外部题库目录不可用")
        target.parentFile?.mkdirs()
        target.writeText(raw)
        Log.i(TAG, "已导入题库并写入: ${target.absolutePath}")
        quiz
    }

    /** 应用专属外部目录下的题库文件；getExternalFilesDir 返回 null 时（罕见）视为无外部文件。 */
    private fun externalFile(context: Context): File? =
        context.getExternalFilesDir(null)?.let { File(it, FILE_NAME) }

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
