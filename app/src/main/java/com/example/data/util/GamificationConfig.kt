package com.example.data.util

object GamificationConfig {
    data class LevelTier(val level: Int, val requiredLifetimeXP: Int, val rank: String)

    val LEVEL_TIERS = listOf(
        LevelTier(1, 0, "Beginner"),
        LevelTier(2, 100, "Learner"),
        LevelTier(3, 250, "Explorer"),
        LevelTier(4, 500, "Disciplined"),
        LevelTier(5, 1000, "Achiever"),
        LevelTier(6, 1750, "Focused"),
        LevelTier(7, 2750, "Advanced"),
        LevelTier(8, 4000, "Expert"),
        LevelTier(9, 5500, "Master"),
        LevelTier(10, 7500, "Legend"),
        LevelTier(11, 10000, "Grandmaster"),
        LevelTier(12, 13000, "Mythic"),
        LevelTier(13, 17000, "Immortal"),
        LevelTier(14, 22000, "Titan"),
        LevelTier(15, 28000, "Ascended")
    )

    fun calculateLevel(lifetimeXP: Int): LevelTier {
        var current = LEVEL_TIERS.first()
        for (tier in LEVEL_TIERS) {
            if (lifetimeXP >= tier.requiredLifetimeXP) {
                current = tier
            } else {
                break
            }
        }
        return current
    }

    fun nextLevel(currentLevel: Int): LevelTier? {
        return LEVEL_TIERS.find { it.level == currentLevel + 1 }
    }

    const val XP_HABIT_COMPLETE = 10
    const val XP_ALL_HABITS_BONUS = 30
    const val XP_GOAL_COMPLETE = 150
    const val XP_CHALLENGE_COMPLETE = 100

    val STREAK_MILESTONE_XP = mapOf(
        3 to 15,
        7 to 50,
        14 to 100,
        30 to 200,
        60 to 350,
        100 to 500,
        365 to 1500
    )
}
