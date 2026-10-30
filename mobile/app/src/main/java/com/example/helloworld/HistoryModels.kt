package com.example.helloworld

import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Короткий запис для списку історії (без повної відповіді backend). */
data class HistoryItem(
    val id: Long,
    val createdAt: Long,
    val inputSummary: String,
    val resultSummary: String,
    val status: String
)

fun formatTimestamp(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))

/** Короткий опис вхідних даних для списку, наприклад «Natus Vincere vs FaZe Clan». */
fun PredictResponse.inputSummary(): String = "$team_a_name vs $team_b_name"

/** Короткий опис результату для списку, наприклад «53% / 47%». */
fun PredictResponse.resultSummary(): String =
    "${(team_a_win_probability * 100).roundToInt()}% / ${(team_b_win_probability * 100).roundToInt()}%"

/** Повна відповідь backend одним JSON-рядком, щоб показати деталі без повторного запиту. */
fun PredictResponse.toJsonString(): String = JSONObject().apply {
    put("team_a_name", team_a_name)
    put("team_b_name", team_b_name)
    put("team_a_win_probability", team_a_win_probability)
    put("team_b_win_probability", team_b_win_probability)
    put("explanation", explanation)
    put("team_a_win_rate", team_a_win_rate)
    put("team_a_rating", team_a_rating)
    put("team_a_win_rate_contribution", team_a_win_rate_contribution)
    put("team_a_rating_contribution", team_a_rating_contribution)
    put("team_a_score", team_a_score)
    put("team_b_win_rate", team_b_win_rate)
    put("team_b_rating", team_b_rating)
    put("team_b_win_rate_contribution", team_b_win_rate_contribution)
    put("team_b_rating_contribution", team_b_rating_contribution)
    put("team_b_score", team_b_score)
}.toString()

/** Повертає null, якщо збережений рядок пошкоджений або неповний. */
fun predictResponseFromJson(text: String): PredictResponse? = try {
    val json = JSONObject(text)
    PredictResponse(
        team_a_name = json.getString("team_a_name"),
        team_b_name = json.getString("team_b_name"),
        team_a_win_probability = json.getDouble("team_a_win_probability"),
        team_b_win_probability = json.getDouble("team_b_win_probability"),
        explanation = json.getString("explanation"),
        team_a_win_rate = json.getDouble("team_a_win_rate"),
        team_a_rating = json.getDouble("team_a_rating"),
        team_a_win_rate_contribution = json.getDouble("team_a_win_rate_contribution"),
        team_a_rating_contribution = json.getDouble("team_a_rating_contribution"),
        team_a_score = json.getDouble("team_a_score"),
        team_b_win_rate = json.getDouble("team_b_win_rate"),
        team_b_rating = json.getDouble("team_b_rating"),
        team_b_win_rate_contribution = json.getDouble("team_b_win_rate_contribution"),
        team_b_rating_contribution = json.getDouble("team_b_rating_contribution"),
        team_b_score = json.getDouble("team_b_score")
    )
} catch (e: JSONException) {
    null
}
