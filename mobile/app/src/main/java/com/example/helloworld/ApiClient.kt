package com.example.helloworld

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        val jsonArray = org.json.JSONArray(response)

        val teams = mutableListOf<TeamInfo>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            teams.add(
                TeamInfo(
                    name = obj.getString("name"),
                    win_rate = obj.getDouble("win_rate"),
                    rating = obj.getDouble("rating")
                )
            )
        }
        teams
    }

    suspend fun predictMatch(teamA: String, teamB: String): PredictResponse = withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/predict")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        val requestBody = JSONObject().apply {
            put("team_a_name", teamA)
            put("team_b_name", teamB)
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
                explanation = json.getString("explanation")
            )
        } else {
            val errorStream = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
            val errorJson = errorStream?.let { JSONObject(it) }
            val detail = errorJson?.optString("detail") ?: "Невідома помилка"
            throw Exception(detail)
        }
    }
}