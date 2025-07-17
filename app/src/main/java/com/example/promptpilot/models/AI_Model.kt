package com.example.promptpilot.models

enum class AI_Model(
    val model: String,
    isChatCompletion1: Int,
    val isChatCompletion: Boolean = false
) {
    Deepseek("deepseek/deepseek-r1:free", 500, isChatCompletion = true),
    KimiVL("moonshotai/kimi-vl-a3b-thinking:free", 500, isChatCompletion = true),
    Maverick("meta-llama/llama-4-maverick:free", 500, isChatCompletion = true)
}
