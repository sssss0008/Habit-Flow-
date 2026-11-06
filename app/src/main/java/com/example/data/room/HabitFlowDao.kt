package com.example.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitFlowDao {

    // --- Users ---
    @Query("SELECT * FROM room_users WHERE userId = :userId LIMIT 1")
    fun getUserById(userId: String): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: UserEntity)

    // --- Habits ---
    @Query("SELECT * FROM room_habits WHERE userId = :userId AND isArchived = 0 ORDER BY createdAtEpoch DESC")
    fun getActiveHabits(userId: String): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(habits: List<HabitEntity>)

    @Query("DELETE FROM room_habits WHERE habitId = :habitId")
    suspend fun deleteHabitById(habitId: String)

    // --- Habit Completions ---
    @Query("SELECT * FROM room_habit_completions WHERE userId = :userId ORDER BY createdAtEpoch DESC")
    fun getAllCompletions(userId: String): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM room_habit_completions WHERE userId = :userId AND completionDate = :date")
    fun getCompletionsByDate(userId: String, date: String): Flow<List<HabitCompletionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: HabitCompletionEntity)

    // --- Streaks ---
    @Query("SELECT * FROM room_streaks WHERE userId = :userId")
    fun getStreaks(userId: String): Flow<List<StreakEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStreak(streak: StreakEntity)

    // --- Goals ---
    @Query("SELECT * FROM room_goals WHERE userId = :userId ORDER BY createdAtEpoch DESC")
    fun getGoals(userId: String): Flow<List<GoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGoal(goal: GoalEntity)

    // --- XP Transactions ---
    @Query("SELECT * FROM room_xp_transactions WHERE userId = :userId ORDER BY createdAtEpoch DESC")
    fun getXpTransactions(userId: String): Flow<List<XpTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertXpTransaction(tx: XpTransactionEntity)
}
