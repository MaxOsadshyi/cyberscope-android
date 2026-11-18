package com.example.helloworld


data class PredictRequest(
    val team_a_name: String,
    val team_b_name: String,
    val map_name: String? = null,
    val use_ai: Boolean = false
)

data class PredictResponse(
    val team_a_name: String,
    val team_b_name: String,
    val team_a_win_probability: Double,
    val team_b_win_probability: Double,
    val explanation: String,
    val team_a_win_rate: Double,
    val team_a_rating: Double,
    val team_a_win_rate_contribution: Double,
    val team_a_rating_contribution: Double,
    val team_a_score: Double,
    val team_b_win_rate: Double,
    val team_b_rating: Double,
    val team_b_win_rate_contribution: Double,
    val team_b_rating_contribution: Double,
    val team_b_score: Double,
    val map_name: String? = null,
    val ai_status: String? = null,
    val ai_analysis: String? = null,
    val ai_model: String? = null
)

data class TeamInfo(
    val name: String,
    val win_rate: Double,
    val rating: Double
)
