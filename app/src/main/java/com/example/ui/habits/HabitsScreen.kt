package com.example.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.data.model.Habit
import com.example.data.repository.HabitRepository
import com.example.ui.home.HabitItemRow
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val HABIT_CATEGORIES = listOf(
    "All", "Health", "Fitness", "Education", "Reading", "Productivity",
    "Finance", "Personal Development", "Meditation", "Sleep", "Work", "Other"
)

val CATEGORY_COLORS = listOf(
    "#7C3AED", "#EF4444", "#3B82F6", "#10B981", "#F59E0B",
    "#06B6D4", "#EC4899", "#8B5CF6", "#14B8A6"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    userId: String,
    repository: HabitRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val habits by repository.observeHabits(userId).collectAsState(initial = emptyList())
    val todayDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val todayCompletions by repository.observeCompletionsForDate(userId, todayDate).collectAsState(initial = emptyList())

    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var habitToEdit by remember { mutableStateOf<Habit?>(null) }
    var habitToDelete by remember { mutableStateOf<Habit?>(null) }

    val filteredHabits = habits.filter { habit ->
        val matchesCategory = selectedCategory == "All" || habit.category.equals(selectedCategory, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() || habit.title.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch && !habit.isArchived
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text(
                    text = "Habit Management",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search your habits...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_habits_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HabitViolet,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = true
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(HABIT_CATEGORIES) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HabitViolet,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

            // Habits List
            if (filteredHabits.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.ListAlt,
                            contentDescription = null,
                            tint = HabitViolet.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No habits found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Add new habits or adjust filters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredHabits, key = { it.habitId }) { habit ->
                        val isCompleted = todayCompletions.any { it.habitId == habit.habitId && it.completed }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                HabitItemRow(
                                    habit = habit,
                                    isCompleted = isCompleted,
                                    onToggle = {
                                        coroutineScope.launch {
                                            repository.toggleHabitCompletion(habit, todayDate)
                                        }
                                    }
                                )
                            }
                            // Options Menu Trigger
                            IconButton(
                                onClick = { habitToEdit = habit },
                                modifier = Modifier.testTag("edit_habit_${habit.habitId}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "Edit Habit",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Habit Sheet
    if (habitToEdit != null) {
        val currentHabit = habitToEdit!!
        var editedTitle by remember { mutableStateOf(currentHabit.title) }
        var editedDescription by remember { mutableStateOf(currentHabit.description ?: "") }
        var editedCategory by remember { mutableStateOf(currentHabit.category) }
        var editedColor by remember { mutableStateOf(currentHabit.color) }

        AlertDialog(
            onDismissRequest = { habitToEdit = null },
            title = { Text("Edit Habit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        label = { Text("Habit Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editedDescription,
                        onValueChange = { editedDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Category", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(HABIT_CATEGORIES.filter { it != "All" }) { cat ->
                            SuggestionChip(
                                onClick = { editedCategory = cat },
                                label = { Text(cat, fontSize = 12.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (editedCategory == cat) HabitViolet else MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.updateHabit(
                                currentHabit.copy(
                                    title = editedTitle,
                                    description = editedDescription,
                                    category = editedCategory,
                                    color = editedColor
                                )
                            )
                            habitToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HabitViolet)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        habitToDelete = currentHabit
                        habitToEdit = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            }
        )
    }

    // Delete Confirmation
    if (habitToDelete != null) {
        AlertDialog(
            onDismissRequest = { habitToDelete = null },
            title = { Text("Delete Habit?") },
            text = { Text("Are you sure you want to delete '${habitToDelete?.title}'? Your previous streak records will be removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            habitToDelete?.let { repository.deleteHabit(it.habitId) }
                            habitToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { habitToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
