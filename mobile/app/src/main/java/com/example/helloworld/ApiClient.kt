package com.example.helloworld

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {

    private const val BASE_URL = "http://127.0.0.1:8000"

    suspend fun getTeams(): List<TeamInfo> = withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/teams")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        val responseCode = connection.responseCode
        if (responseCode != 200) {
            throw Exception("Помилка сервера: $responseCode")
        }

        val response = connection.inputStream.bufferedReader().use(BufferedReader::readText)
        parseTeamsArray(JSONArray(response))
    }

    suspend fun getTeamsRanking(): List<TeamInfo> = withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/teams/ranking")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        val responseCode = connection.responseCode
        if (responseCode != 200) {
            throw Exception("Помилка завантаження рейтингу: $responseCode")
        }

        val response = connection.inputStream.bufferedReader().use(BufferedReader::readText)
        parseTeamsArray(JSONArray(response))
    }

    private fun parseTeamsArray(jsonArray: JSONArray): List<TeamInfo> {
        val teams = mutableListOf<TeamInfo>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val rosterArray = obj.optJSONArray("roster")
            val roster = mutableListOf<PlayerInfo>()
            if (rosterArray != null) {
                for (j in 0 until rosterArray.length()) {
                    val pObj = rosterArray.getJSONObject(j)
                    roster.add(
                        PlayerInfo(
                            nickname = pObj.getString("nickname"),
                            rating = pObj.getDouble("rating")
                        )
                    )
                }
            }
            teams.add(
                TeamInfo(
                    name = obj.getString("name"),
                    win_rate = obj.getDouble("win_rate"),
                    rating = obj.getDouble("rating"),
                    points = obj.optInt("points", 0),
                    roster = roster
                )
            )
        }
        return teams
    }

    suspend fun getMaps(): List<String> = withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/maps")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        val responseCode = connection.responseCode
        if (responseCode != 200) {
            throw Exception("Помилка завантаження карт: $responseCode")
        }

        val response = connection.inputStream.bufferedReader().use(BufferedReader::readText)
        val jsonArray = JSONArray(response)
        val maps = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            maps.add(jsonArray.getString(i))
        }
        maps
    }

    suspend fun predictMatch(
        teamA: String,
        teamB: String,
        mapName: String? = null,
        useAi: Boolean = false
    ): PredictResponse = withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/predict")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 5000
        connection.readTimeout = 30000
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        val requestBody = JSONObject().apply {
            put("team_a_name", teamA)
            put("team_b_name", teamB)
            if (mapName != null) {
                put("map_name", mapName)
            }
            put("use_ai", useAi)
        }

        OutputStreamWriter(connection.outputStream).use { writer ->
            writer.write(requestBody.toString())
            writer.flush()
        }

        val responseCode = connection.responseCode

        if (responseCode == 200) {
            val response = connection.inputStream.bufferedReader().use(BufferedReader::readText)
            val json = JSONObject(response)
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
                team_b_score = json.getDouble("team_b_score"),
                map_name = if (json.isNull("map_name")) null else json.optString("map_name"),
                ai_status = if (json.isNull("ai_status")) null else json.optString("ai_status"),
                ai_analysis = if (json.isNull("ai_analysis")) null else json.optString("ai_analysis"),
                ai_model = if (json.isNull("ai_model")) null else json.optString("ai_model")
            )
        } else {
            val errorStream = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
            val errorJson = errorStream?.let { JSONObject(it) }
            val detail = errorJson?.optString("detail") ?: "Невідома помилка"
            throw Exception(detail)
        }
    }
}