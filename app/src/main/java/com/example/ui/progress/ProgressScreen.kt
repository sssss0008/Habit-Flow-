package com.example.ui.progress

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Goal
import com.example.data.model.Habit
import com.example.data.model.HabitCompletion
import com.example.data.model.UserProfile
import com.example.data.repository.HabitRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ProgressScreen(
    userId: String,
    repository: HabitRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val userProfile by repository.observeUserProfile(userId).collectAsState(initial = null)
    val habits by repository.observeHabits(userId).collectAsState(initial = emptyList())
    val completions by repository.observeAllCompletions(userId).collectAsState(initial = emptyList())
    val goals by repository.observeGoals(userId).collectAsState(initial = emptyList())

    var showAddGoalDialog by remember { mutableStateOf(false) }

    val totalCompletedHabits = completions.count { it.completed }
    val maxStreak = habits.maxOfOrNull { it.longestStreak } ?: 0
    val currentActiveStreaks = habits.count { it.currentStreak > 0 }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        item {
            Text(
                text = "Progress & Analytics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Visual breakdown of your consistency and achievements",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Summary Metric Grid (4 Cards)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Completed Habits",
                        value = "$totalCompletedHabits",
                        icon = Icons.Filled.CheckCircle,
                        tint = HabitEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Best Streak",
                        value = "$maxStreak Days",
                        icon = Icons.Filled.LocalFireDepartment,
                        tint = HabitAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Active Streaks",
                        value = "$currentActiveStreaks Habits",
                        icon = Icons.Filled.TrendingUp,
                        tint = HabitCyan,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Total Lifetime XP",
                        value = "${userProfile?.lifetimeXP ?: 0}",
                        icon = Icons.Filled.ElectricBolt,
                        tint = HabitGold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Interactive Weekly Habit Completion Rate Chart
        item {
            val chartPoints = remember(completions, habits) {
                calculateLast7DaysChartPoints(completions, habits.filter { it.isActive && !it.isArchived }.size)
            }
            com.example.ui.components.WeeklyProgressChart(points = chartPoints)
        }

        // Personal Goals Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Personal Goals",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { showAddGoalDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HabitViolet),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Goal")
                }
            }
        }

        // Goals List
        if (goals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Flag, contentDescription = null, tint = HabitGold, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("No personal goals set yet.", fontWeight = FontWeight.SemiBold)
                        Text("Set goals like 'Exercise 30 days' or 'Read 12 books' to earn 150 bonus XP!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(goals, key = { it.goalId }) { goal ->
                GoalItemCard(
                    goal = goal,
                    onIncrement = {
                        coroutineScope.launch {
                            repository.incrementGoalProgress(goal, 1)
                        }
                    }
                )
            }
        }
    }

    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onSave = { title, target ->
                coroutineScope.launch {
                    repository.createGoal(
                        title = title,
                        description = null,
                        category = "Personal Development",
                        targetValue = target,
                        targetDate = null
                    )
                    showAddGoalDialog = false
                }
            }
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun GoalItemCard(
    goal: Goal,
    onIncrement: () -> Unit
) {
    val progress = if (goal.targetValue > 0) (goal.currentValue.toFloat() / goal.targetValue).coerceIn(0f, 1f) else 0f
    val isCompleted = goal.status == "Completed"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) HabitEmerald.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${goal.currentValue} / ${goal.targetValue} completed (${(progress * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!isCompleted) {
                    IconButton(
                        onClick = onIncrement,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(HabitViolet.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Log progress", tint = HabitViolet)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(HabitEmerald.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("COMPLETED (+150 XP)", color = HabitEmerald, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isCompleted) HabitEmerald else HabitViolet,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var targetStr by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Personal Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Title") },
                    placeholder = { Text("e.g. Read 12 Books") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetStr,
                    onValueChange = { targetStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Target Count") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val count = targetStr.toIntOrNull() ?: 10
                    if (title.isNotBlank() && count > 0) {
                        onSave(title.trim(), count)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HabitViolet)
            ) {
                Text("Set Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun calculateLast7DaysChartPoints(
    completions: List<HabitCompletion>,
    activeHabitCount: Int
): List<com.example.ui.components.DailyChartPoint> {
    val result = mutableListOf<com.example.ui.components.DailyChartPoint>()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -6)

    val safeTotal = if (activeHabitCount > 0) activeHabitCount else 5

    for (i in 0..6) {
        val dateStr = sdf.format(cal.time)
        val label = dayFormat.format(cal.time)
        val count = completions.count { (it.completionDate == dateStr || it.date == dateStr) && it.completed }
        val rate = (count.toFloat() / safeTotal.toFloat()).coerceIn(0f, 1f)
        result.add(
            com.example.ui.components.DailyChartPoint(
                dayLabel = label,
                completedCount = count,
                totalScheduled = safeTotal,
                completionRate = rate
            )
        )
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return result
}
