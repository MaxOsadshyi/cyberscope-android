package com.example.helloworld


data class PredictRequest(
    val team_a_name: String,
    val team_b_name: String
)

data class PredictResponse(
    val team_a_name: String,
    val team_b_name: String,
    val team_a_win_probability: Double,
    val team_b_win_probability: Double,
    val explanation: String
)

data class TeamInfo(
    val name: String,
    val win_rate: Double,
    val rating: Double
)