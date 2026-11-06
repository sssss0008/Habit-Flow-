package com.example.data.ai

import android.util.Log
import com.example.data.model.Habit
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout

class HabitCoachService {
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.7f
                maxOutputTokens = 600
            }
        )
    }

    suspend fun getMotivationalFeedback(
        user: UserProfile,
        habits: List<Habit>,
        todayCompletedCount: Int
    ): String {
        return try {
            withTimeout(30_000L) {
                val prompt = """
                    You are HabitFlow AI, an energetic, empathetic gamified habit coach.
                    User: ${user.displayName}
                    Level: ${user.currentLevel} (${user.currentRank})
                    Lifetime XP: ${user.lifetimeXP}
                    Habits completed today: $todayCompletedCount out of ${habits.size}
                    Active habits: ${habits.take(5).joinToString(", ") { it.title }}
                    
                    Give a concise, punchy 2-3 sentence motivational message with an actionable tip and gamified encouragement. Keep it engaging, cheerful, and motivating.
                """.trimIndent()

                val response = model.generateContent(prompt)
                response.text ?: "You're building greatness step by step! Complete your habits today to earn XP and level up."
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("HabitCoachService", "AI generation error", e)
            "Consistency is your superpower! Every habit completed today unlocks tomorrow's potential. Keep flowing!"
        }
    }

    suspend fun suggestHabits(goalTopic: String): List<String> {
        return try {
            withTimeout(30_000L) {
                val prompt = """
                    Suggest 3 high-impact daily habits for someone who wants to achieve: "$goalTopic".
                    Format your response strictly as 3 bullet points, each under 8 words. No extra text.
                """.trimIndent()
                val response = model.generateContent(prompt)
                val lines = response.text?.lines()
                    ?.map { it.trim().removePrefix("-").removePrefix("*").trim() }
                    ?.filter { it.isNotBlank() }
                    ?.take(3)
                if (!lines.isNullOrEmpty()) lines else listOf("Read for 15 minutes", "Hydrate first thing in morning", "10-minute focus session")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("HabitCoachService", "AI habit suggestion failed", e)
            listOf("Dedicate 20 minutes daily", "Log your milestones each evening", "Review your weekly streak")
        }
    }
}
