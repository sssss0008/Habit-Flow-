package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Habit
import com.example.data.model.HabitCompletion
import com.example.data.model.User
import com.example.data.model.XPTransaction
import com.example.data.repository.HabitRepository
import com.example.data.room.*
import com.example.data.util.GamificationConfig
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class LevelUpEvent(
    val newLevel: Int,
    val newRank: String,
    val totalXp: Int
)

data class XpRewardEvent(
    val habitTitle: String,
    val xpEarned: Int,
    val milestoneStreak: Int? = null,
    val allCompletedBonus: Boolean = false
)

class GamificationViewModel(
    private val repository: HabitRepository,
    private val roomDao: HabitFlowDao,
    private val userId: String
) : ViewModel() {

    private val _levelUpEvent = MutableSharedFlow<LevelUpEvent>()
    val levelUpEvent: SharedFlow<LevelUpEvent> = _levelUpEvent.asSharedFlow()

    private val _xpRewardEvent = MutableSharedFlow<XpRewardEvent>()
    val xpRewardEvent: SharedFlow<XpRewardEvent> = _xpRewardEvent.asSharedFlow()

    // Room-backed daily habits due today
    val roomHabits: StateFlow<List<HabitEntity>> = roomDao.getActiveHabits(userId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val todayDateString: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // Room-backed today's habit completions
    val roomCompletionsToday: StateFlow<List<HabitCompletionEntity>> = roomDao.getCompletionsByDate(userId, todayDateString)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Room-backed user entity
    val roomUser: StateFlow<UserEntity?> = roomDao.getUserById(userId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Room-backed XP transactions history
    val roomXpTransactions: StateFlow<List<XpTransactionEntity>> = roomDao.getXpTransactions(userId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Records or toggles habit completion, executing complete XP calculation,
     * milestone check, all-habits bonus, and user level progression.
     * Updates both Room local SQLite and Cloud Firestore.
     */
    fun toggleHabitCompletion(habitEntity: HabitEntity) {
        viewModelScope.launch {
            val completionId = "${habitEntity.habitId}_$todayDateString"
            val existing = roomCompletionsToday.value.find { it.habitId == habitEntity.habitId }
            val isCurrentlyCompleted = existing?.completed == true

            if (isCurrentlyCompleted) {
                // Undo completion: update Room and cloud without reducing lifetime XP level
                val uncompletedEntity = HabitCompletionEntity(
                    completionId = completionId,
                    userId = userId,
                    habitId = habitEntity.habitId,
                    completionDate = todayDateString,
                    completed = false,
                    xpAwarded = 0
                )
                roomDao.insertCompletion(uncompletedEntity)

                // Sync with repository
                val habitModel = Habit(
                    habitId = habitEntity.habitId,
                    userId = userId,
                    title = habitEntity.title,
                    category = habitEntity.category,
                    icon = habitEntity.icon,
                    color = habitEntity.color,
                    currentStreak = habitEntity.currentStreak,
                    longestStreak = habitEntity.longestStreak,
                    totalCompleted = habitEntity.totalCompleted
                )
                repository.toggleHabitCompletion(habitModel, todayDateString)
            } else {
                // New completion: calculate base XP + streak milestone + all-habits bonus
                val baseXP = GamificationConfig.XP_HABIT_COMPLETE
                val newStreak = habitEntity.currentStreak + 1
                val newLongest = maxOf(newStreak, habitEntity.longestStreak)
                val milestoneBonus = GamificationConfig.STREAK_MILESTONE_XP[newStreak] ?: 0

                val updatedHabits = roomHabits.value
                val willAllBeCompleted = updatedHabits.isNotEmpty() &&
                        updatedHabits.all { h ->
                            if (h.habitId == habitEntity.habitId) true
                            else roomCompletionsToday.value.any { it.habitId == h.habitId && it.completed }
                        }
                val allBonus = if (willAllBeCompleted) GamificationConfig.XP_ALL_HABITS_BONUS else 0

                val totalEarnedXp = baseXP + milestoneBonus + allBonus

                // 1. Record completion in Room
                val completionEntity = HabitCompletionEntity(
                    completionId = completionId,
                    userId = userId,
                    habitId = habitEntity.habitId,
                    completionDate = todayDateString,
                    completionStatus = "COMPLETED",
                    completed = true,
                    xpAwarded = totalEarnedXp,
                    notes = "Completed via daily checklist"
                )
                roomDao.insertCompletion(completionEntity)

                // 2. Update Habit Streak in Room
                val updatedHabit = habitEntity.copy(
                    currentStreak = newStreak,
                    longestStreak = newLongest,
                    totalCompleted = habitEntity.totalCompleted + 1,
                    updatedAtEpoch = System.currentTimeMillis()
                )
                roomDao.insertOrUpdateHabit(updatedHabit)

                // 3. Record Streak Entity in Room
                roomDao.insertOrUpdateStreak(
                    StreakEntity(
                        streakId = habitEntity.habitId,
                        userId = userId,
                        habitId = habitEntity.habitId,
                        currentStreak = newStreak,
                        longestStreak = newLongest,
                        lastCompletedDate = todayDateString,
                        totalCompletedDays = habitEntity.totalCompleted + 1
                    )
                )

                // 4. Create XP Transaction in Room
                val txId = UUID.randomUUID().toString()
                val txDesc = buildString {
                    append("Completed habit: ${habitEntity.title}")
                    if (milestoneBonus > 0) append(" (+$milestoneBonus XP Streak Milestone!)")
                    if (allBonus > 0) append(" (+$allBonus XP All-Habits Bonus!)")
                }
                val xpTxEntity = XpTransactionEntity(
                    transactionId = txId,
                    userId = userId,
                    sourceType = "habit",
                    sourceId = habitEntity.habitId,
                    transactionType = "EARNED",
                    xpAmount = totalEarnedXp,
                    description = txDesc
                )
                roomDao.insertXpTransaction(xpTxEntity)

                // 5. Update User Level & Rank in Room
                val currentUser = roomUser.value ?: UserEntity(
                    userId = userId,
                    displayName = "HabitFlow Champion",
                    email = null
                )
                val oldLevel = currentUser.currentLevel
                val newLifetimeXp = currentUser.lifetimeXP + totalEarnedXp
                val newCurrentXp = currentUser.currentXP + totalEarnedXp
                val levelTier = GamificationConfig.calculateLevel(newLifetimeXp)
                val newPoints = currentUser.rewardPoints + 5 + (if (allBonus > 0) 10 else 0)

                val updatedUser = currentUser.copy(
                    currentXP = newCurrentXp,
                    lifetimeXP = newLifetimeXp,
                    currentLevel = levelTier.level,
                    currentRank = levelTier.rank,
                    rewardPoints = newPoints,
                    totalCompletedHabits = currentUser.totalCompletedHabits + 1,
                    longestStreak = maxOf(currentUser.longestStreak, newLongest),
                    currentStreak = newStreak,
                    updatedAtEpoch = System.currentTimeMillis()
                )
                roomDao.insertOrUpdateUser(updatedUser)

                // 6. Sync with Cloud Firestore
                val habitModel = Habit(
                    habitId = habitEntity.habitId,
                    userId = userId,
                    title = habitEntity.title,
                    category = habitEntity.category,
                    icon = habitEntity.icon,
                    color = habitEntity.color,
                    currentStreak = habitEntity.currentStreak,
                    longestStreak = habitEntity.longestStreak,
                    totalCompleted = habitEntity.totalCompleted
                )
                repository.toggleHabitCompletion(habitModel, todayDateString)

                // 7. Fire Events for UI celebrations (XP snackbar & Level up dialog)
                _xpRewardEvent.emit(
                    XpRewardEvent(
                        habitTitle = habitEntity.title,
                        xpEarned = totalEarnedXp,
                        milestoneStreak = if (milestoneBonus > 0) newStreak else null,
                        allCompletedBonus = allBonus > 0
                    )
                )

                if (levelTier.level > oldLevel) {
                    _levelUpEvent.emit(
                        LevelUpEvent(
                            newLevel = levelTier.level,
                            newRank = levelTier.rank,
                            totalXp = newLifetimeXp
                        )
                    )
                }
            }
        }
    }
}
