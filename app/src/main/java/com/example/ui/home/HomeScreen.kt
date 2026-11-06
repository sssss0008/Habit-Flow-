package com.example.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.HabitCoachService
import com.example.data.model.Habit
import com.example.data.model.User
import com.example.data.repository.HabitRepository
import com.example.data.room.HabitEntity
import com.example.ui.components.ProgressRing
import com.example.ui.components.XpBanner
import com.example.ui.theme.*
import com.example.ui.viewmodel.GamificationViewModel
import com.example.ui.viewmodel.LevelUpEvent
import com.example.ui.viewmodel.XpRewardEvent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    userId: String,
    repository: HabitRepository,
    gamificationViewModel: GamificationViewModel,
    onNavigateToHabits: () -> Unit,
    onNavigateToRewards: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Room-backed reactive states from ViewModel
    val roomHabits by gamificationViewModel.roomHabits.collectAsState()
    val roomCompletions by gamificationViewModel.roomCompletionsToday.collectAsState()
    val roomUser by gamificationViewModel.roomUser.collectAsState()
    val roomXpTransactions by gamificationViewModel.roomXpTransactions.collectAsState()

    // Firestore fallback / cloud synced states
    val userProfile by repository.observeUserProfile(userId).collectAsState(initial = null)
    val habitsCloud by repository.observeHabits(userId).collectAsState(initial = emptyList())
    val challenges by repository.observeChallenges(userId).collectAsState(initial = emptyList())
    val goals by repository.observeGoals(userId).collectAsState(initial = emptyList())

    val aiCoach = remember { HabitCoachService() }
    var aiCoachMessage by remember { mutableStateOf<String?>(null) }
    var isAiLoading by remember { mutableStateOf(false) }

    var levelUpDialogEvent by remember { mutableStateOf<LevelUpEvent?>(null) }

    // Listen for XP and Level up events from GamificationViewModel
    LaunchedEffect(gamificationViewModel) {
        launch {
            gamificationViewModel.xpRewardEvent.collect { event ->
                val extraMsg = buildString {
                    append("+${event.xpEarned} XP for ${event.habitTitle}!")
                    event.milestoneStreak?.let { append(" 🔥 $it-day streak milestone!") }
                    if (event.allCompletedBonus) append(" ⭐ All daily habits completed bonus!")
                }
                snackbarHostState.showSnackbar(extraMsg)
            }
        }
        launch {
            gamificationViewModel.levelUpEvent.collect { event ->
                levelUpDialogEvent = event
            }
        }
    }

    // Seed defaults if fresh user
    LaunchedEffect(userId) {
        repository.seedDefaultHabitsIfEmpty(userId)
        repository.seedDefaultChallengesIfEmpty(userId)
    }

    // Load AI Coach advice
    val profile = roomUser?.let {
        User(
            userId = it.userId,
            displayName = it.displayName,
            email = it.email,
            currentLevel = it.currentLevel,
            currentXP = it.currentXP,
            lifetimeXP = it.lifetimeXP,
            currentRank = it.currentRank,
            rewardPoints = it.rewardPoints,
            currentStreak = it.currentStreak,
            longestStreak = it.longestStreak,
            totalCompletedHabits = it.totalCompletedHabits
        )
    } ?: userProfile ?: User(userId = userId, displayName = "HabitFlow Champion")

    LaunchedEffect(profile, roomHabits, roomCompletions) {
        if (roomHabits.isNotEmpty() && aiCoachMessage == null && !isAiLoading) {
            isAiLoading = true
            coroutineScope.launch {
                val completedCount = roomCompletions.count { it.completed }
                val habitModels = roomHabits.map {
                    Habit(
                        habitId = it.habitId,
                        userId = it.userId,
                        title = it.title,
                        category = it.category
                    )
                }
                aiCoachMessage = aiCoach.getMotivationalFeedback(profile, habitModels, completedCount)
                isAiLoading = false
            }
        }
    }

    val completedCount = roomCompletions.count { it.completed }
    val totalCount = roomHabits.size
    val dailyProgress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
        ) {
            // Welcome Header & Quick Level Badge
            item {
                val formattedDate = remember {
                    SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hello, ${profile.displayName}!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(HabitPurple.copy(alpha = 0.2f))
                            .clickable { onNavigateToRewards() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = "Achievements",
                            tint = HabitGold,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // Gamification XP Banner
            item {
                XpBanner(
                    xp = profile.lifetimeXP,
                    level = profile.currentLevel,
                    rank = profile.currentRank
                )
            }

            // Daily Habit Momentum Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_progress_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Habit Momentum",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$completedCount of $totalCount completed today",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.ElectricBolt,
                                    contentDescription = null,
                                    tint = HabitGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+${completedCount * 10} XP earned today",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = HabitGold
                                )
                            }
                        }

                        ProgressRing(
                            progress = dailyProgress,
                            size = 88.dp,
                            strokeWidth = 9.dp
                        ) {
                            Text(
                                text = "${(dailyProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = HabitCyan
                            )
                        }
                    }
                }
            }

            // Overview Metric Badges Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardMetricBadge(
                        icon = Icons.Filled.LocalFireDepartment,
                        iconTint = HabitAmber,
                        title = "Streak",
                        value = "${profile.longestStreak} Days",
                        modifier = Modifier.weight(1f)
                    )
                    DashboardMetricBadge(
                        icon = Icons.Filled.CheckCircle,
                        iconTint = HabitEmerald,
                        title = "Habits Done",
                        value = "${profile.totalCompletedHabits}",
                        modifier = Modifier.weight(1f)
                    )
                    DashboardMetricBadge(
                        icon = Icons.Filled.Stars,
                        iconTint = HabitGold,
                        title = "Shop Points",
                        value = "${profile.rewardPoints}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Daily Habit Checklist View (ROOM ENTITY BACKED)
            item {
                DailyHabitChecklistView(
                    habitsDueToday = roomHabits,
                    completionsToday = roomCompletions,
                    onToggleCompletion = { habitEntity ->
                        gamificationViewModel.toggleHabitCompletion(habitEntity)
                    }
                )
            }

            // AI Habit Coach Insight
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = HabitPurpleDark.copy(alpha = 0.25f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(HabitViolet.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "AI Coach",
                                tint = HabitVioletLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "AI Habit Coach",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = HabitVioletLight
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = aiCoachMessage ?: "Analyzing your routine to optimize momentum...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }

            // Active Challenges Section
            if (challenges.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Active Challenges",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(challenges.take(2), key = { it.challengeId }) { challenge ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = challenge.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(HabitCyan.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "+${challenge.xpReward} XP",
                                            color = HabitCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = challenge.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                imageVector = Icons.Filled.MilitaryTech,
                                contentDescription = "Challenge",
                                tint = HabitGold,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            // Recent XP Transactions Log in Room
            if (roomXpTransactions.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Recent XP Ledger",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(roomXpTransactions.take(3), key = { it.transactionId }) { tx ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = tx.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(text = tx.sourceType.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(text = "+${tx.xpAmount} XP", color = HabitGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Animated Level Up Celebration Dialog
        levelUpDialogEvent?.let { event ->
            AlertDialog(
                onDismissRequest = { levelUpDialogEvent = null },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(HabitGold.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ElectricBolt,
                            contentDescription = "Level Up",
                            tint = HabitGold,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "LEVEL UP! LEVEL ${event.newLevel}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = HabitGold
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Congratulations! You have attained the rank of:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = event.newRank,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = HabitCyan
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Keep up the momentum to unlock even higher tiers and exclusive shop rewards!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { levelUpDialogEvent = null },
                        colors = ButtonDefaults.buttonColors(containerColor = HabitViolet),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Claim & Continue")
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }
}

@Composable
fun DashboardMetricBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun HabitItemRow(
    habit: Habit,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("habit_item_${habit.habitId}")
            .clickable { onToggle() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                HabitEmerald.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            try {
                                Color(android.graphics.Color.parseColor(habit.color)).copy(alpha = 0.2f)
                            } catch (e: Exception) {
                                HabitPurple.copy(alpha = 0.2f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (habit.icon) {
                            "Fitness" -> Icons.Filled.FitnessCenter
                            "Book" -> Icons.Filled.MenuBook
                            "Water" -> Icons.Filled.WaterDrop
                            "Code" -> Icons.Filled.Terminal
                            "SelfImprovement" -> Icons.Filled.SelfImprovement
                            else -> Icons.Filled.CheckCircleOutline
                        },
                        contentDescription = habit.category,
                        tint = try {
                            Color(android.graphics.Color.parseColor(habit.color))
                        } catch (e: Exception) {
                            HabitViolet
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None,
                        color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = habit.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (habit.currentStreak > 0) {
                            Text(text = " • ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = "Streak",
                                tint = HabitAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${habit.currentStreak}d",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = HabitAmber
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Completed",
                        tint = HabitEmerald,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.RadioButtonUnchecked,
                        contentDescription = "Mark completed",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}
