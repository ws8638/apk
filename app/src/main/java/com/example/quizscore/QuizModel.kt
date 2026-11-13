package com.example.quizscore

import android.content.Context
import android.net.Uri
import android.util.Log
import org.json.JSONObject
import java.io.File

/** 一个选项。correct=true 表示它是本题唯一正确答案。 */
data class Option(
    val text: String,
    val correct: Boolean,
)

/** 一道单选题：题干 + 2~4 个选项，其中有且仅有一个 correct=true。 */
data class Question(
    val text: String,
    val options: List<Option>,
) {
    /** 正确选项的下标（parse 已保证恰有一个，故不会为 -1）。 */
    val correctIndex: Int get() = options.indexOfFirst { it.correct }
}

/** 一套题。评分系统已移除：只判对错、不打分。 */
data class Quiz(
    val title: String,
    val questions: List<Question>,
)

/**
 * 题库读取 / SAF 导入 / JSON 解析。
 * 增删题目只需修改 questions.json，无需改代码。
 */
object QuizLoader {

    private const val TAG = "QuizLoader"
    private const val FILE_NAME = "questions.json"

    /**
     * 读取题库。优先使用「外部文件」，找不到再回退到 APK 内置 assets。
     *
     * 外部文件位于应用专属外部目录（Android 10+ 免存储权限）：
     *   /sdcard/Android/data/<applicationId>/files/questions.json
     * 用「导入题库」按钮或 `adb push` 替换它，即可在【不重新打包、不重装 APK】
     * 的情况下更新题库。
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
                val qText = item.optString("text").trim()
                require(qText.isNotEmpty()) { "第 ${i + 1} 题缺少 text（题干）" }

                val optArray = item.optJSONArray("options")
                    ?: throw IllegalArgumentException("第 ${i + 1} 题缺少 options（选项数组）")
                require(optArray.length() in 2..4) {
                    "第 ${i + 1} 题的选项数量必须是 2~4 个，当前为 ${optArray.length()} 个"
                }

                val options = buildList {
                    for (j in 0 until optArray.length()) {
                        val opt = optArray.getJSONObject(j)
                        val oText = opt.optString("text").trim()
                        require(oText.isNotEmpty()) { "第 ${i + 1} 题第 ${j + 1} 个选项缺少 text" }
                        add(Option(text = oText, correct = opt.optBoolean("correct", false)))
                    }
                }

                val correctCount = options.count { it.correct }
                require(correctCount == 1) {
                    if (correctCount == 0) "第 ${i + 1} 题没有任何选项标记 correct=true，单选题必须恰有一个正确答案"
                    else "第 ${i + 1} 题有 $correctCount 个 correct=true，单选题必须恰有一个正确答案"
                }

                add(Question(text = qText, options = options))
            }
        }

        return Quiz(
            title = root.optString("title", "选择题自测"),
            questions = questions,
        )
    }
}
