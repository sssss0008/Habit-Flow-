package com.example.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Habit
import com.example.data.repository.HabitRepository
import com.example.ui.theme.*
import com.example.ui.viewmodel.GamificationViewModel
import kotlinx.coroutines.launch

data class StarterHabitItem(
    val title: String,
    val category: String,
    val icon: String,
    val color: String,
    val description: String
)

data class StarterPack(
    val id: String,
    val name: String,
    val tagline: String,
    val icon: ImageVector,
    val accentColor: Color,
    val habits: List<StarterHabitItem>
)

val STARTER_PACKS = listOf(
    StarterPack(
        id = "fitness",
        name = "Peak Fitness & Health",
        tagline = "Boost your energy, hydration and strength",
        icon = Icons.Filled.FitnessCenter,
        accentColor = HabitOrange,
        habits = listOf(
            StarterHabitItem("Drink 2L Water", "Health", "WaterDrop", "#06B6D4", "Stay hydrated throughout the day"),
            StarterHabitItem("Exercise for 30 Mins", "Fitness", "FitnessCenter", "#F97316", "Cardio, gym, or home workout"),
            StarterHabitItem("Sleep 8 Hours", "Sleep", "Bedtime", "#8B5CF6", "Consistent restorative sleep")
        )
    ),
    StarterPack(
        id = "mindfulness",
        name = "Mind & Knowledge",
        tagline = "Sharpen your intellect and inner peace",
        icon = Icons.Filled.SelfImprovement,
        accentColor = HabitCyan,
        habits = listOf(
            StarterHabitItem("Read 20 Pages", "Reading", "MenuBook", "#3B82F6", "Daily book reading session"),
            StarterHabitItem("Meditate for 10 Mins", "Meditation", "SelfImprovement", "#06B6D4", "Calm breathing & mindfulness"),
            StarterHabitItem("Daily Gratitude Journal", "Personal Development", "EditNote", "#10B981", "Write 3 things you are grateful for")
        )
    ),
    StarterPack(
        id = "productivity",
        name = "Supercharged Productivity",
        tagline = "Conquer your goals and master high-value skills",
        icon = Icons.Filled.RocketLaunch,
        accentColor = HabitViolet,
        habits = listOf(
            StarterHabitItem("Practice Programming / Study", "Education", "Code", "#8B5CF6", "Focused skill-building hour"),
            StarterHabitItem("Clear Top 3 Priorities", "Productivity", "TaskAlt", "#EC4899", "Complete primary objectives first"),
            StarterHabitItem("Daily Evening Review", "Personal Development", "CheckCircle", "#F59E0B", "Review wins and plan tomorrow")
        )
    )
)

@Composable
fun StarterPackDialog(
    userId: String,
    repository: HabitRepository,
    gamificationViewModel: GamificationViewModel,
    onDismiss: () -> Unit
) {
    var selectedPackId by remember { mutableStateOf(STARTER_PACKS[0].id) }
    var isClaiming by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = { if (!isClaiming) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("starter_pack_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Free Badge Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(HabitGold.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.WorkspacePremium,
                        contentDescription = "Free Feature",
                        tint = HabitGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "100% FREE STARTER GIFT",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = HabitGold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Choose Your Starter Habit Pack",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Pick a focus area to kickstart your journey with 3 pre-built habits + 50 Welcome Bonus XP!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
                )

                // Starter Pack Options
                STARTER_PACKS.forEach { pack ->
                    val isSelected = selectedPackId == pack.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) pack.accentColor else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedPackId = pack.id },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) pack.accentColor.copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(pack.accentColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = pack.icon,
                                        contentDescription = pack.name,
                                        tint = pack.accentColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pack.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = pack.tagline,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedPackId = pack.id },
                                    colors = RadioButtonDefaults.colors(selectedColor = pack.accentColor)
                                )
                            }

                            if (isSelected) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = pack.accentColor.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(8.dp))
                                pack.habits.forEach { habit ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = pack.accentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = habit.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Button(
                    onClick = {
                        isClaiming = true
                        val selectedPack = STARTER_PACKS.first { it.id == selectedPackId }
                        coroutineScope.launch {
                            selectedPack.habits.forEach { item ->
                                val habit = Habit(
                                    userId = userId,
                                    title = item.title,
                                    description = item.description,
                                    category = item.category,
                                    icon = item.icon,
                                    color = item.color,
                                    scheduleType = "Daily",
                                    scheduledDays = "ALL",
                                    dailyTarget = 1
                                )
                                repository.createHabit(habit)
                            }
                            isClaiming = false
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("claim_starter_pack_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = HabitViolet),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isClaiming
                ) {
                    if (isClaiming) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.Bolt, contentDescription = null, tint = HabitGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Claim Free Pack & +50 XP",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    enabled = !isClaiming
                ) {
                    Text(
                        text = "I'll create habits manually",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
