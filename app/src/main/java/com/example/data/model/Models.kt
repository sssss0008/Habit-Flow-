package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

// --- 1. User ---
@IgnoreExtraProperties
data class User(
    @DocumentId val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val profileImage: String? = null,
    val currentLevel: Int = 1,
    val currentXP: Int = 0,
    val lifetimeXP: Int = 0,
    val currentRank: String = "Beginner",
    val rewardPoints: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalCompletedHabits: Int = 0,
    val timezone: String? = null,
    val accountStatus: String = "Active",
    val equippedTitle: String? = "Beginner",
    val equippedBadge: String? = "first_habit",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// Backwards-compatible alias for UserProfile
typealias UserProfile = User

// --- 2. Habit ---
@IgnoreExtraProperties
data class Habit(
    @DocumentId val habitId: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String? = null,
    val category: String = "Other",
    val icon: String = "Star",
    val color: String = "#7C3AED",
    val scheduleType: String = "Daily",
    val scheduledDays: String = "ALL",
    val dailyTarget: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalCompleted: Int = 0,
    val isActive: Boolean = true,
    val isArchived: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderTime: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// --- 3. HabitCompletion ---
@IgnoreExtraProperties
data class HabitCompletion(
    @DocumentId val completionId: String = "",
    val userId: String = "",
    val habitId: String = "",
    val completionDate: String = "", // YYYY-MM-DD
    val date: String = "", // Synchronized date alias
    val completionTime: String? = null,
    val completionStatus: String = "COMPLETED",
    val completed: Boolean = true,
    val xpAwarded: Int = 10,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

// --- 4. Streak ---
@IgnoreExtraProperties
data class Streak(
    @DocumentId val streakId: String = "",
    val userId: String = "",
    val habitId: String = "",
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedDate: String? = null,
    val totalCompletedDays: Int = 0,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// --- 5. Goal ---
@IgnoreExtraProperties
data class Goal(
    @DocumentId val goalId: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String? = null,
    val category: String = "Personal Development",
    val targetValue: Int = 30,
    val currentValue: Int = 0,
    val targetDate: String? = null,
    val completionPercentage: Float = 0f,
    val status: String = "In Progress", // "In Progress", "Completed"
    @ServerTimestamp val completedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// --- 6. XPTransaction ---
@IgnoreExtraProperties
data class XPTransaction(
    @DocumentId val transactionId: String = "",
    val userId: String = "",
    val sourceType: String = "habit", // habit, streak, goal, challenge
    val sourceId: String = "",
    val transactionType: String = "EARNED",
    val xpAmount: Int = 10,
    val description: String = "",
    val idempotencyKey: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

// Backwards-compatible alias for XpTransaction
typealias XpTransaction = XPTransaction

// --- Supporting Entities (Challenges, Rewards, Badges) ---
@IgnoreExtraProperties
data class Challenge(
    @DocumentId val challengeId: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val challengeType: String = "Daily", // Daily, Weekly
    val targetValue: Int = 3,
    val currentProgress: Int = 0,
    val xpReward: Int = 50,
    val isCompleted: Boolean = false,
    val rewardClaimed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserReward(
    @DocumentId val rewardId: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val rewardType: String = "title", // title, theme, avatar_frame, badge
    val cost: Int = 100,
    @ServerTimestamp val unlockedAt: Timestamp? = null
)

enum class BadgeRarity {
    COMMON, RARE, EPIC, LEGENDARY
}

data class AchievementBadge(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val rarity: BadgeRarity,
    val xpReward: Int,
    val iconName: String,
    val isUnlocked: Boolean = false,
    val progress: Float = 0f
)
