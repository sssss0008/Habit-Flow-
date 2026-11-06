package com.example.data.repository

import android.content.Context
import com.example.data.model.*
import com.example.data.room.*
import com.example.data.util.GamificationConfig
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class HabitRepository(
    private val db: FirebaseFirestore,
    context: Context? = null
) {
    private val auth = Firebase.auth
    private val roomDb = context?.let { HabitFlowDatabase.getDatabase(it) }
    private val roomDao = roomDb?.habitFlowDao()
    private val ioScope = CoroutineScope(Dispatchers.IO)

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun todayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // --- User Profile ---
    fun observeUserProfile(userId: String): Flow<User?> {
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots().map { snapshot ->
            if (snapshot.exists()) {
                val user = snapshot.toObject(User::class.java)
                user?.let { u ->
                    ioScope.launch {
                        roomDao?.insertOrUpdateUser(
                            UserEntity(
                                userId = u.userId,
                                displayName = u.displayName,
                                email = u.email,
                                profileImage = u.profileImage,
                                currentLevel = u.currentLevel,
                                currentXP = u.currentXP,
                                lifetimeXP = u.lifetimeXP,
                                currentRank = u.currentRank,
                                rewardPoints = u.rewardPoints,
                                currentStreak = u.currentStreak,
                                longestStreak = u.longestStreak,
                                totalCompletedHabits = u.totalCompletedHabits
                            )
                        )
                    }
                }
                user
            } else {
                null
            }
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun getOrCreateUserProfile(userId: String, displayName: String, email: String?): User {
        val userDoc = db.collection("users").document(userId)
        val snapshot = userDoc.get().await()
        if (snapshot.exists()) {
            val user = snapshot.toObject(User::class.java) ?: User(userId = userId, displayName = displayName)
            roomDao?.insertOrUpdateUser(
                UserEntity(
                    userId = user.userId,
                    displayName = user.displayName,
                    email = user.email,
                    currentLevel = user.currentLevel,
                    currentXP = user.currentXP,
                    lifetimeXP = user.lifetimeXP,
                    currentRank = user.currentRank,
                    rewardPoints = user.rewardPoints,
                    currentStreak = user.currentStreak,
                    longestStreak = user.longestStreak,
                    totalCompletedHabits = user.totalCompletedHabits
                )
            )
            return user
        }

        val newProfile = User(
            userId = userId,
            displayName = displayName.ifBlank { "HabitFlow Champion" },
            email = email,
            currentLevel = 1,
            currentXP = 0,
            lifetimeXP = 0,
            currentRank = "Beginner",
            rewardPoints = 50,
            currentStreak = 0,
            longestStreak = 0,
            totalCompletedHabits = 0,
            equippedTitle = "Beginner",
            equippedBadge = "first_habit"
        )
        val payload = mapOf(
            "userId" to userId,
            "displayName" to newProfile.displayName,
            "email" to email,
            "currentLevel" to 1,
            "currentXP" to 0,
            "lifetimeXP" to 0,
            "currentRank" to "Beginner",
            "rewardPoints" to 50,
            "currentStreak" to 0,
            "longestStreak" to 0,
            "totalCompletedHabits" to 0,
            "equippedTitle" to "Beginner",
            "equippedBadge" to "first_habit",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        userDoc.set(payload).await()
        roomDao?.insertOrUpdateUser(
            UserEntity(
                userId = userId,
                displayName = newProfile.displayName,
                email = email,
                currentLevel = 1,
                currentXP = 0,
                lifetimeXP = 0,
                currentRank = "Beginner",
                rewardPoints = 50,
                currentStreak = 0,
                longestStreak = 0,
                totalCompletedHabits = 0
            )
        )
        return newProfile
    }

    // --- Habits ---
    fun observeHabits(userId: String): Flow<List<Habit>> {
        val collection = db.collection("users").document(userId).collection("habits")
        return collection.snapshots().map { snapshot ->
            val list = snapshot.toObjects(Habit::class.java)
            ioScope.launch {
                val entities = list.map { h ->
                    HabitEntity(
                        habitId = h.habitId,
                        userId = h.userId,
                        title = h.title,
                        description = h.description,
                        category = h.category,
                        icon = h.icon,
                        color = h.color,
                        scheduleType = h.scheduleType,
                        dailyTarget = h.dailyTarget,
                        currentStreak = h.currentStreak,
                        longestStreak = h.longestStreak,
                        totalCompleted = h.totalCompleted,
                        isActive = h.isActive,
                        isArchived = h.isArchived,
                        reminderTime = h.reminderTime
                    )
                }
                roomDao?.insertHabits(entities)
            }
            list
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, collection.path)
            throw e
        }
    }

    suspend fun createHabit(
        title: String,
        description: String?,
        category: String,
        icon: String,
        color: String,
        dailyTarget: Int = 1,
        reminderTime: String? = null
    ): Result<String> {
        val uid = requireUserId()
        val habitId = UUID.randomUUID().toString()
        val habitsRef = db.collection("users").document(uid).collection("habits")
        val docRef = habitsRef.document(habitId)

        val payload = mapOf(
            "habitId" to habitId,
            "userId" to uid,
            "title" to title,
            "description" to description,
            "category" to category,
            "icon" to icon,
            "color" to color,
            "scheduleType" to "Daily",
            "dailyTarget" to dailyTarget,
            "currentStreak" to 0,
            "longestStreak" to 0,
            "totalCompleted" to 0,
            "isActive" to true,
            "isArchived" to false,
            "reminderTime" to reminderTime,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        return try {
            docRef.set(payload).await()
            roomDao?.insertOrUpdateHabit(
                HabitEntity(
                    habitId = habitId,
                    userId = uid,
                    title = title,
                    description = description,
                    category = category,
                    icon = icon,
                    color = color,
                    dailyTarget = dailyTarget,
                    reminderTime = reminderTime
                )
            )
            Result.success(habitId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createHabit(habit: Habit): Result<String> {
        return createHabit(
            title = habit.title,
            description = habit.description,
            category = habit.category,
            icon = habit.icon,
            color = habit.color,
            dailyTarget = habit.dailyTarget,
            reminderTime = habit.reminderTime
        )
    }

    suspend fun updateHabit(habit: Habit): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("habits").document(habit.habitId)
        return try {
            val payload = mapOf(
                "title" to habit.title,
                "description" to habit.description,
                "category" to habit.category,
                "icon" to habit.icon,
                "color" to habit.color,
                "dailyTarget" to habit.dailyTarget,
                "isActive" to habit.isActive,
                "isArchived" to habit.isArchived,
                "reminderTime" to habit.reminderTime,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.update(payload).await()
            roomDao?.insertOrUpdateHabit(
                HabitEntity(
                    habitId = habit.habitId,
                    userId = uid,
                    title = habit.title,
                    description = habit.description,
                    category = habit.category,
                    icon = habit.icon,
                    color = habit.color,
                    dailyTarget = habit.dailyTarget,
                    isActive = habit.isActive,
                    isArchived = habit.isArchived,
                    reminderTime = habit.reminderTime
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteHabit(habitId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("habits").document(habitId)
        return try {
            docRef.delete().await()
            roomDao?.deleteHabitById(habitId)
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Completions & XP ---
    fun observeCompletionsForDate(userId: String, date: String): Flow<List<HabitCompletion>> {
        val collection = db.collection("users").document(userId).collection("completions")
        return collection.whereEqualTo("date", date).snapshots().map { snapshot ->
            val list = snapshot.toObjects(HabitCompletion::class.java)
            ioScope.launch {
                list.forEach { c ->
                    roomDao?.insertCompletion(
                        HabitCompletionEntity(
                            completionId = c.completionId,
                            userId = c.userId,
                            habitId = c.habitId,
                            completionDate = if (c.completionDate.isNotBlank()) c.completionDate else c.date,
                            completed = c.completed,
                            xpAwarded = c.xpAwarded,
                            notes = c.notes
                        )
                    )
                }
            }
            list
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, collection.path)
            throw e
        }
    }

    fun observeAllCompletions(userId: String): Flow<List<HabitCompletion>> {
        val collection = db.collection("users").document(userId).collection("completions")
        return collection.snapshots().map { snapshot ->
            val list = snapshot.toObjects(HabitCompletion::class.java)
            ioScope.launch {
                list.forEach { c ->
                    roomDao?.insertCompletion(
                        HabitCompletionEntity(
                            completionId = c.completionId,
                            userId = c.userId,
                            habitId = c.habitId,
                            completionDate = if (c.completionDate.isNotBlank()) c.completionDate else c.date,
                            completed = c.completed,
                            xpAwarded = c.xpAwarded,
                            notes = c.notes
                        )
                    )
                }
            }
            list
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, collection.path)
            throw e
        }
    }

    suspend fun toggleHabitCompletion(
        habit: Habit,
        date: String = todayDateString(),
        notes: String? = null
    ): Result<Boolean> {
        val uid = requireUserId()
        val completionId = "${habit.habitId}_$date"
        val completionDoc = db.collection("users").document(uid).collection("completions").document(completionId)
        val habitDoc = db.collection("users").document(uid).collection("habits").document(habit.habitId)
        val userDoc = db.collection("users").document(uid)

        return try {
            val existing = completionDoc.get().await()
            val isNowCompleted = if (existing.exists() && existing.getBoolean("completed") == true) {
                completionDoc.update("completed", false).await()
                roomDao?.insertCompletion(
                    HabitCompletionEntity(
                        completionId = completionId,
                        userId = uid,
                        habitId = habit.habitId,
                        completionDate = date,
                        completed = false
                    )
                )
                false
            } else {
                val completionPayload = mapOf(
                    "completionId" to completionId,
                    "userId" to uid,
                    "habitId" to habit.habitId,
                    "completionDate" to date,
                    "date" to date,
                    "completed" to true,
                    "xpAwarded" to GamificationConfig.XP_HABIT_COMPLETE,
                    "notes" to notes,
                    "createdAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }

                completionDoc.set(completionPayload).await()
                roomDao?.insertCompletion(
                    HabitCompletionEntity(
                        completionId = completionId,
                        userId = uid,
                        habitId = habit.habitId,
                        completionDate = date,
                        completed = true,
                        xpAwarded = GamificationConfig.XP_HABIT_COMPLETE,
                        notes = notes
                    )
                )

                awardXP(uid, GamificationConfig.XP_HABIT_COMPLETE, "Completed: ${habit.title}", "habit", habit.habitId)

                val newStreak = habit.currentStreak + 1
                val newLongest = maxOf(newStreak, habit.longestStreak)
                habitDoc.update(
                    mapOf(
                        "currentStreak" to newStreak,
                        "longestStreak" to newLongest,
                        "totalCompleted" to habit.totalCompleted + 1,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                // Cache Streak in Room
                roomDao?.insertOrUpdateStreak(
                    StreakEntity(
                        streakId = habit.habitId,
                        userId = uid,
                        habitId = habit.habitId,
                        currentStreak = newStreak,
                        longestStreak = newLongest,
                        lastCompletedDate = date,
                        totalCompletedDays = habit.totalCompleted + 1
                    )
                )

                GamificationConfig.STREAK_MILESTONE_XP[newStreak]?.let { milestoneBonus ->
                    awardXP(uid, milestoneBonus, "Hit $newStreak-day streak milestone on ${habit.title}!", "streak", habit.habitId)
                }

                userDoc.update(
                    mapOf(
                        "totalCompletedHabits" to FieldValue.increment(1),
                        "rewardPoints" to FieldValue.increment(5),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                true
            }
            Result.success(isNowCompleted)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, completionDoc.path)
            Result.failure(e)
        }
    }

    private suspend fun awardXP(
        userId: String,
        amount: Int,
        description: String,
        sourceType: String,
        sourceId: String
    ) {
        val userDoc = db.collection("users").document(userId)
        val userSnapshot = userDoc.get().await()
        val currentLifetimeXP = userSnapshot.getLong("lifetimeXP")?.toInt() ?: 0
        val newLifetimeXP = currentLifetimeXP + amount
        val currentTier = GamificationConfig.calculateLevel(newLifetimeXP)

        val txId = UUID.randomUUID().toString()
        val txDoc = db.collection("users").document(userId).collection("xp_transactions").document(txId)

        val txPayload = mapOf(
            "transactionId" to txId,
            "userId" to userId,
            "sourceType" to sourceType,
            "sourceId" to sourceId,
            "xpAmount" to amount,
            "description" to description,
            "createdAt" to FieldValue.serverTimestamp()
        )
        txDoc.set(txPayload).await()

        roomDao?.insertXpTransaction(
            XpTransactionEntity(
                transactionId = txId,
                userId = userId,
                sourceType = sourceType,
                sourceId = sourceId,
                xpAmount = amount,
                description = description
            )
        )

        userDoc.update(
            mapOf(
                "currentXP" to FieldValue.increment(amount.toLong()),
                "lifetimeXP" to newLifetimeXP,
                "currentLevel" to currentTier.level,
                "currentRank" to currentTier.rank,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    // --- Goals ---
    fun observeGoals(userId: String): Flow<List<Goal>> {
        val collection = db.collection("users").document(userId).collection("goals")
        return collection.snapshots().map { snapshot ->
            val list = snapshot.toObjects(Goal::class.java)
            ioScope.launch {
                list.forEach { g ->
                    val pct = if (g.targetValue > 0) (g.currentValue.toFloat() / g.targetValue) * 100f else 0f
                    roomDao?.insertOrUpdateGoal(
                        GoalEntity(
                            goalId = g.goalId,
                            userId = g.userId,
                            title = g.title,
                            description = g.description,
                            category = g.category,
                            targetValue = g.targetValue,
                            currentValue = g.currentValue,
                            targetDate = g.targetDate,
                            completionPercentage = pct,
                            status = g.status
                        )
                    )
                }
            }
            list
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, collection.path)
            throw e
        }
    }

    suspend fun createGoal(
        title: String,
        description: String?,
        category: String,
        targetValue: Int,
        targetDate: String?
    ): Result<String> {
        val uid = requireUserId()
        val goalId = UUID.randomUUID().toString()
        val goalDoc = db.collection("users").document(uid).collection("goals").document(goalId)

        val payload = mapOf(
            "goalId" to goalId,
            "userId" to uid,
            "title" to title,
            "description" to description,
            "category" to category,
            "targetValue" to targetValue,
            "currentValue" to 0,
            "targetDate" to targetDate,
            "status" to "In Progress",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        return try {
            goalDoc.set(payload).await()
            roomDao?.insertOrUpdateGoal(
                GoalEntity(
                    goalId = goalId,
                    userId = uid,
                    title = title,
                    description = description,
                    category = category,
                    targetValue = targetValue,
                    currentValue = 0,
                    targetDate = targetDate,
                    status = "In Progress"
                )
            )
            Result.success(goalId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, goalDoc.path)
            Result.failure(e)
        }
    }

    suspend fun incrementGoalProgress(goal: Goal, increment: Int = 1): Result<Unit> {
        val uid = requireUserId()
        val goalDoc = db.collection("users").document(uid).collection("goals").document(goal.goalId)
        val newValue = minOf(goal.targetValue, goal.currentValue + increment)
        val isNowCompleted = newValue >= goal.targetValue && goal.status != "Completed"

        return try {
            goalDoc.update(
                mapOf(
                    "currentValue" to newValue,
                    "status" to if (isNowCompleted) "Completed" else goal.status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val pct = if (goal.targetValue > 0) (newValue.toFloat() / goal.targetValue) * 100f else 0f
            roomDao?.insertOrUpdateGoal(
                GoalEntity(
                    goalId = goal.goalId,
                    userId = uid,
                    title = goal.title,
                    description = goal.description,
                    category = goal.category,
                    targetValue = goal.targetValue,
                    currentValue = newValue,
                    targetDate = goal.targetDate,
                    completionPercentage = pct,
                    status = if (isNowCompleted) "Completed" else goal.status
                )
            )

            if (isNowCompleted) {
                awardXP(uid, GamificationConfig.XP_GOAL_COMPLETE, "Achieved Goal: ${goal.title}", "goal", goal.goalId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, goalDoc.path)
            Result.failure(e)
        }
    }

    // --- Challenges ---
    fun observeChallenges(userId: String): Flow<List<Challenge>> {
        val collection = db.collection("users").document(userId).collection("challenges")
        return collection.snapshots().map { snapshot ->
            snapshot.toObjects(Challenge::class.java)
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, collection.path)
            throw e
        }
    }

    suspend fun seedDefaultChallengesIfEmpty(userId: String) {
        val collection = db.collection("users").document(userId).collection("challenges")
        val existing = collection.get().await()
        if (!existing.isEmpty) return

        val defaults = listOf(
            Challenge(
                challengeId = "daily_3_habits",
                userId = userId,
                title = "Daily Focus",
                description = "Complete 3 habits today",
                challengeType = "Daily",
                targetValue = 3,
                currentProgress = 0,
                xpReward = 50
            ),
            Challenge(
                challengeId = "weekly_consistency",
                userId = userId,
                title = "Iron Consistency",
                description = "Complete 10 habits this week",
                challengeType = "Weekly",
                targetValue = 10,
                currentProgress = 0,
                xpReward = 150
            ),
            Challenge(
                challengeId = "streak_starter",
                userId = userId,
                title = "Streak Prodigy",
                description = "Reach a 3-day habit streak",
                challengeType = "Weekly",
                targetValue = 3,
                currentProgress = 0,
                xpReward = 100
            )
        )

        for (challenge in defaults) {
            collection.document(challenge.challengeId).set(
                mapOf(
                    "challengeId" to challenge.challengeId,
                    "userId" to userId,
                    "title" to challenge.title,
                    "description" to challenge.description,
                    "challengeType" to challenge.challengeType,
                    "targetValue" to challenge.targetValue,
                    "currentProgress" to challenge.currentProgress,
                    "xpReward" to challenge.xpReward,
                    "isCompleted" to false,
                    "rewardClaimed" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun claimChallengeReward(challenge: Challenge): Result<Unit> {
        val uid = requireUserId()
        if (!challenge.isCompleted || challenge.rewardClaimed) return Result.success(Unit)

        val docRef = db.collection("users").document(uid).collection("challenges").document(challenge.challengeId)
        return try {
            docRef.update("rewardClaimed", true).await()
            awardXP(uid, challenge.xpReward, "Claimed Challenge: ${challenge.title}", "challenge", challenge.challengeId)
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Rewards Shop ---
    fun observeUserRewards(userId: String): Flow<List<UserReward>> {
        val collection = db.collection("users").document(userId).collection("rewards")
        return collection.snapshots().map { snapshot ->
            snapshot.toObjects(UserReward::class.java)
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, collection.path)
            throw e
        }
    }

    suspend fun purchaseReward(userId: String, rewardId: String, title: String, cost: Int, rewardType: String): Result<Unit> {
        val userDoc = db.collection("users").document(userId)
        val rewardDoc = userDoc.collection("rewards").document(rewardId)

        return try {
            val userSnapshot = userDoc.get().await()
            val points = userSnapshot.getLong("rewardPoints")?.toInt() ?: 0
            if (points < cost) {
                return Result.failure(IllegalStateException("Not enough reward points. Earn more by completing habits!"))
            }

            rewardDoc.set(
                mapOf(
                    "rewardId" to rewardId,
                    "userId" to userId,
                    "title" to title,
                    "description" to "Cosmetic unlock in HabitFlow",
                    "rewardType" to rewardType,
                    "cost" to cost,
                    "unlockedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            userDoc.update(
                mapOf(
                    "rewardPoints" to FieldValue.increment(-cost.toLong()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, rewardDoc.path)
            Result.failure(e)
        }
    }

    // --- Seed Default Habits For New Users ---
    suspend fun seedDefaultHabitsIfEmpty(userId: String) {
        val habitsRef = db.collection("users").document(userId).collection("habits")
        val existing = habitsRef.get().await()
        if (!existing.isEmpty) return

        val defaultList = listOf(
            Habit(
                habitId = "read_20_pages",
                userId = userId,
                title = "Read 20 pages",
                description = "Expand knowledge and focus daily",
                category = "Reading",
                icon = "Book",
                color = "#3B82F6",
                dailyTarget = 1
            ),
            Habit(
                habitId = "exercise_30_min",
                userId = userId,
                title = "Exercise for 30 minutes",
                description = "Workout, run or stretch to energize",
                category = "Fitness",
                icon = "Fitness",
                color = "#EF4444",
                dailyTarget = 1
            ),
            Habit(
                habitId = "drink_2l_water",
                userId = userId,
                title = "Drink 2 liters of water",
                description = "Stay hydrated and healthy throughout the day",
                category = "Health",
                icon = "Water",
                color = "#06B6D4",
                dailyTarget = 2
            ),
            Habit(
                habitId = "practice_coding",
                userId = userId,
                title = "Practice programming",
                description = "Code algorithms or build features",
                category = "Education",
                icon = "Code",
                color = "#8B5CF6",
                dailyTarget = 1
            ),
            Habit(
                habitId = "meditate_10_min",
                userId = userId,
                title = "Meditate for 10 minutes",
                description = "Calm breathing and mindfulness",
                category = "Meditation",
                icon = "SelfImprovement",
                color = "#10B981",
                dailyTarget = 1
            )
        )

        for (habit in defaultList) {
            habitsRef.document(habit.habitId).set(
                mapOf(
                    "habitId" to habit.habitId,
                    "userId" to userId,
                    "title" to habit.title,
                    "description" to habit.description,
                    "category" to habit.category,
                    "icon" to habit.icon,
                    "color" to habit.color,
                    "scheduleType" to "Daily",
                    "dailyTarget" to habit.dailyTarget,
                    "currentStreak" to 0,
                    "longestStreak" to 0,
                    "totalCompleted" to 0,
                    "isActive" to true,
                    "isArchived" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }
}
