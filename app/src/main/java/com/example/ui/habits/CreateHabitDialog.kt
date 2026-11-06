package com.example.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.HabitCoachService
import com.example.data.repository.HabitRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch

val HABIT_ICONS = listOf(
    "Star" to Icons.Filled.Star,
    "Fitness" to Icons.Filled.FitnessCenter,
    "Book" to Icons.Filled.MenuBook,
    "Water" to Icons.Filled.WaterDrop,
    "Code" to Icons.Filled.Terminal,
    "SelfImprovement" to Icons.Filled.SelfImprovement,
    "Meditation" to Icons.Filled.Spa,
    "Bed" to Icons.Filled.Bedtime,
    "DirectionsRun" to Icons.Filled.DirectionsRun,
    "Savings" to Icons.Filled.Savings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHabitDialog(
    repository: HabitRepository,
    onDismiss: () -> Unit,
    onHabitCreated: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val aiCoach = remember { HabitCoachService() }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Health") }
    var selectedColor by remember { mutableStateOf(CATEGORY_COLORS.first()) }
    var selectedIcon by remember { mutableStateOf("Star") }
    var isSaving by remember { mutableStateOf(false) }

    // AI suggestions section
    var aiTopic by remember { mutableStateOf("") }
    var aiSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var isAiGenerating by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AddCircle,
                    contentDescription = null,
                    tint = HabitViolet
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create New Habit", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Habit Name*") },
                    placeholder = { Text("e.g. Read 20 pages") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_habit_name_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Why is this habit meaningful to you?") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                // Category Selection
                Text("Category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(HABIT_CATEGORIES.filter { it != "All" }) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HabitViolet,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Color Selection
                Text("Theme Color", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(CATEGORY_COLORS) { hexColor ->
                        val color = try { Color(android.graphics.Color.parseColor(hexColor)) } catch (e: Exception) { HabitViolet }
                        val isSelected = selectedColor == hexColor
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hexColor }
                                .then(
                                    if (isSelected) Modifier.border(3.dp, Color.White, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Icon Selection
                Text("Icon Symbol", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(HABIT_ICONS) { (iconKey, vector) ->
                        val isSelected = selectedIcon == iconKey
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) HabitViolet else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedIcon = iconKey },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = vector,
                                contentDescription = iconKey,
                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // AI Assist Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = HabitPurpleDark.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = HabitViolet, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI Habit Ideas", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = HabitVioletLight)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = aiTopic,
                                onValueChange = { aiTopic = it },
                                placeholder = { Text("e.g. Better sleep or fitness", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    if (aiTopic.isNotBlank()) {
                                        isAiGenerating = true
                                        coroutineScope.launch {
                                            aiSuggestions = aiCoach.suggestHabits(aiTopic)
                                            isAiGenerating = false
                                        }
                                    }
                                }
                            ) {
                                if (isAiGenerating) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Filled.Send, contentDescription = "Suggest", tint = HabitViolet)
                                }
                            }
                        }

                        if (aiSuggestions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            aiSuggestions.forEach { suggestion ->
                                Text(
                                    text = "💡 $suggestion",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HabitCyan,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { title = suggestion }
                                        .padding(vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        isSaving = true
                        coroutineScope.launch {
                            repository.createHabit(
                                title = title.trim(),
                                description = description.trim().ifEmpty { null },
                                category = selectedCategory,
                                icon = selectedIcon,
                                color = selectedColor
                            )
                            isSaving = false
                            onHabitCreated()
                        }
                    }
                },
                enabled = title.isNotBlank() && !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = HabitViolet),
                modifier = Modifier.testTag("submit_create_habit_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Add Habit (+10 XP)")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
