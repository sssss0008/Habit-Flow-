package com.example.ui.navigation

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.data.repository.HabitRepository
import com.example.ui.ai.AiCoachDialog
import com.example.ui.components.SyncStatusDialog
import com.example.ui.habits.CreateHabitDialog
import com.example.ui.habits.HabitsScreen
import com.example.ui.home.HomeScreen
import com.example.ui.onboarding.StarterPackDialog
import com.example.ui.profile.ProfileScreen
import com.example.ui.progress.ProgressScreen
import com.example.ui.rewards.RewardsScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.GamificationViewModel
import kotlinx.coroutines.launch

enum class HabitFlowTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    HABITS("Habits", Icons.Filled.Checklist),
    PROGRESS("Progress", Icons.Filled.BarChart),
    REWARDS("Rewards", Icons.Filled.EmojiEvents),
    PROFILE("Profile", Icons.Filled.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    userId: String,
    repository: HabitRepository,
    gamificationViewModel: GamificationViewModel,
    onSignOut: () -> Unit,
    onReplayOnboarding: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var currentTab by remember { mutableStateOf(HabitFlowTab.HOME) }
    var showCreateHabitDialog by remember { mutableStateOf(false) }
    var showAiCoachDialog by remember { mutableStateOf(false) }
    var showStarterPackDialog by remember { mutableStateOf(false) }
    var showSyncStatusDialog by remember { mutableStateOf(false) }

    // Check SharedPreferences for first sign-in feature prompt
    val prefs = remember(context) { context.getSharedPreferences("habitflow_prefs", Context.MODE_PRIVATE) }
    var hasPromptedStarterPack by remember {
        mutableStateOf(prefs.getBoolean("prompted_starter_pack_$userId", false))
    }

    LaunchedEffect(userId) {
        if (!hasPromptedStarterPack) {
            // First time sign-in: ask user to claim free feature starter pack
            showStarterPackDialog = true
            prefs.edit().putBoolean("prompted_starter_pack_$userId", true).apply()
            hasPromptedStarterPack = true
        }
    }

    // Reactive states for drawer profile header
    val roomUser by gamificationViewModel.roomUser.collectAsState()
    val cloudUser by repository.observeUserProfile(userId).collectAsState(initial = null)
    val userProfile = roomUser?.let {
        User(
            userId = it.userId,
            displayName = it.displayName,
            email = it.email,
            currentLevel = it.currentLevel,
            currentXP = it.currentXP,
            lifetimeXP = it.lifetimeXP,
            currentRank = it.currentRank
        )
    } ?: cloudUser ?: User(userId = userId, displayName = "Habit Champion")

    val roomHabits by gamificationViewModel.roomHabits.collectAsState()
    val cloudHabits by repository.observeHabits(userId).collectAsState(initial = emptyList())

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(320.dp)
                    .testTag("app_navigation_drawer"),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerTonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Drawer Header Profile Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(HabitVioletDark, HabitPurpleDark)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(HabitViolet),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = "Avatar",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = HabitGold.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, HabitGold)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.WorkspacePremium,
                                            contentDescription = null,
                                            tint = HabitGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "ALL FREE",
                                            color = HabitGold,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = userProfile.displayName.ifBlank { "HabitFlow Champion" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            userProfile.email?.let { email ->
                                Text(
                                    text = email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HabitVioletLight
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Level & Rank row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Level ${userProfile.currentLevel} • ${userProfile.currentRank}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = HabitCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${userProfile.currentXP} XP",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = HabitGold,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            val nextLevelXp = userProfile.currentLevel * 100
                            val xpProgress = (userProfile.currentXP % 100).toFloat() / 100f
                            LinearProgressIndicator(
                                progress = { xpProgress.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = HabitGold,
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "NAVIGATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )

                    HabitFlowTab.values().forEach { tab ->
                        NavigationDrawerItem(
                            label = { Text(tab.label, fontWeight = FontWeight.Medium) },
                            icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                            selected = currentTab == tab,
                            onClick = {
                                currentTab = tab
                                coroutineScope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = HabitViolet.copy(alpha = 0.15f),
                                selectedTextColor = HabitViolet,
                                selectedIconColor = HabitViolet
                            ),
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .testTag("drawer_item_${tab.name.lowercase()}")
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Text(
                        text = "FREE FEATURES & TOOLS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )

                    // Free Starter Pack
                    NavigationDrawerItem(
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Starter Habit Packs", fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = HabitGreen.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "FREE",
                                        color = HabitGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        },
                        icon = { Icon(Icons.Filled.Bolt, contentDescription = null, tint = HabitOrange) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch {
                                drawerState.close()
                                showStarterPackDialog = true
                            }
                        },
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .testTag("drawer_starter_packs")
                    )

                    // Free AI Coach
                    NavigationDrawerItem(
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("FlowBot AI Coach", fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = HabitViolet.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "AI",
                                        color = HabitViolet,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        },
                        icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = HabitViolet) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch {
                                drawerState.close()
                                showAiCoachDialog = true
                            }
                        },
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .testTag("drawer_ai_coach")
                    )

                    // Sync Status
                    NavigationDrawerItem(
                        label = { Text("Room SQLite & Cloud Sync", fontWeight = FontWeight.Medium) },
                        icon = { Icon(Icons.Filled.CloudDone, contentDescription = null, tint = HabitCyan) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch {
                                drawerState.close()
                                showSyncStatusDialog = true
                            }
                        },
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .testTag("drawer_sync_status")
                    )

                    // Replay Onboarding
                    NavigationDrawerItem(
                        label = { Text("App Tour & Guide", fontWeight = FontWeight.Medium) },
                        icon = { Icon(Icons.Filled.HelpOutline, contentDescription = null) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch {
                                drawerState.close()
                                onReplayOnboarding()
                            }
                        },
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .testTag("drawer_replay_onboarding")
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Sign Out
                    NavigationDrawerItem(
                        label = { Text("Sign Out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch {
                                drawerState.close()
                                onSignOut()
                            }
                        },
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("drawer_sign_out")
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.ElectricBolt,
                                contentDescription = null,
                                tint = HabitGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "HabitFlow",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { coroutineScope.launch { drawerState.open() } },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("nav_drawer_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Open Navigation Menu",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showAiCoachDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("top_ai_coach_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "Free AI Coach",
                                tint = HabitViolet
                            )
                        }

                        IconButton(
                            onClick = { showSyncStatusDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("top_sync_status_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudDone,
                                contentDescription = "Sync Status",
                                tint = HabitGreen
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.testTag("main_top_app_bar")
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    HabitFlowTab.values().forEach { tab ->
                        val selected = currentTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    tint = if (selected) HabitViolet else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = HabitViolet.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateHabitDialog = true },
                    containerColor = HabitViolet,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("fab_create_habit")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Create Habit",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    HabitFlowTab.HOME -> HomeScreen(
                        userId = userId,
                        repository = repository,
                        gamificationViewModel = gamificationViewModel,
                        onNavigateToHabits = { currentTab = HabitFlowTab.HABITS },
                        onNavigateToRewards = { currentTab = HabitFlowTab.REWARDS }
                    )
                    HabitFlowTab.HABITS -> HabitsScreen(
                        userId = userId,
                        repository = repository
                    )
                    HabitFlowTab.PROGRESS -> ProgressScreen(
                        userId = userId,
                        repository = repository
                    )
                    HabitFlowTab.REWARDS -> RewardsScreen(
                        userId = userId,
                        repository = repository
                    )
                    HabitFlowTab.PROFILE -> ProfileScreen(
                        userId = userId,
                        repository = repository,
                        onSignOut = onSignOut
                    )
                }

                if (showCreateHabitDialog) {
                    CreateHabitDialog(
                        repository = repository,
                        onDismiss = { showCreateHabitDialog = false },
                        onHabitCreated = { showCreateHabitDialog = false }
                    )
                }

                if (showStarterPackDialog) {
                    StarterPackDialog(
                        userId = userId,
                        repository = repository,
                        gamificationViewModel = gamificationViewModel,
                        onDismiss = { showStarterPackDialog = false }
                    )
                }

                if (showAiCoachDialog) {
                    AiCoachDialog(
                        userId = userId,
                        repository = repository,
                        onDismiss = { showAiCoachDialog = false }
                    )
                }

                if (showSyncStatusDialog) {
                    SyncStatusDialog(
                        roomHabitCount = roomHabits.size,
                        cloudHabitCount = cloudHabits.size,
                        onDismiss = { showSyncStatusDialog = false }
                    )
                }
            }
        }
    }
}
