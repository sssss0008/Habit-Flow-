package com.example.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "room_users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val displayName: String,
    val email: String?,
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
    val createdAtEpoch: Long = System.currentTimeMillis(),
    val updatedAtEpoch: Long = System.currentTimeMillis()
)

@Entity(tableName = "room_habits")
data class HabitEntity(
    @PrimaryKey val habitId: String,
    val userId: String,
    val title: String,
    val description: String?,
    val category: String,
    val icon: String,
    val color: String,
    val scheduleType: String = "Daily",
    val scheduledDays: String = "ALL", // Comma-delimited (e.g. MON,TUE)
    val dailyTarget: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalCompleted: Int = 0,
    val isActive: Boolean = true,
    val isArchived: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderTime: String? = null,
    val createdAtEpoch: Long = System.currentTimeMillis(),
    val updatedAtEpoch: Long = System.currentTimeMillis()
)

@Entity(tableName = "room_habit_completions")
data class HabitCompletionEntity(
    @PrimaryKey val completionId: String,
    val userId: String,
    val habitId: String,
    val completionDate: String,
    val completionTime: String? = null,
    val completionStatus: String = "COMPLETED",
    val completed: Boolean = true,
    val xpAwarded: Int = 10,
    val notes: String? = null,
    val createdAtEpoch: Long = System.currentTimeMillis()
)

@Entity(tableName = "room_streaks")
data class StreakEntity(
    @PrimaryKey val streakId: String,
    val userId: String,
    val habitId: String,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedDate: String? = null,
    val totalCompletedDays: Int = 0,
    val updatedAtEpoch: Long = System.currentTimeMillis()
)

@Entity(tableName = "room_goals")
data class GoalEntity(
    @PrimaryKey val goalId: String,
    val userId: String,
    val title: String,
    val description: String?,
    val category: String = "Personal Development",
    val targetValue: Int = 30,
    val currentValue: Int = 0,
    val targetDate: String? = null,
    val completionPercentage: Float = 0f,
    val status: String = "In Progress",
    val completedAtEpoch: Long? = null,
    val createdAtEpoch: Long = System.currentTimeMillis(),
    val updatedAtEpoch: Long = System.currentTimeMillis()
)

@Entity(tableName = "room_xp_transactions")
data class XpTransactionEntity(
    @PrimaryKey val transactionId: String,
    val userId: String,
    val sourceType: String, // habit, streak, goal, challenge
    val sourceId: String,
    val transactionType: String = "EARNED",
    val xpAmount: Int,
    val description: String,
    val idempotencyKey: String? = null,
    val createdAtEpoch: Long = System.currentTimeMillis()
)
