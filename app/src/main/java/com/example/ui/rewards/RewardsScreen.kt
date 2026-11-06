package com.example.ui.rewards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AchievementBadge
import com.example.data.model.BadgeRarity
import com.example.data.model.UserProfile
import com.example.data.model.UserReward
import com.example.data.repository.HabitRepository
import com.example.data.util.GamificationConfig
import com.example.ui.components.XpBanner
import com.example.ui.theme.*
import kotlinx.coroutines.launch

val ALL_ACHIEVEMENTS = listOf(
    AchievementBadge("first_habit", "First Habit", "Complete your first habit", "Getting Started", BadgeRarity.COMMON, 20, "CheckCircle"),
    AchievementBadge("first_steps", "First Steps", "Complete 5 habits in total", "Getting Started", BadgeRarity.COMMON, 50, "DirectionsRun"),
    AchievementBadge("week_warrior", "Week Warrior", "Reach a 7-day streak on any habit", "Streaks", BadgeRarity.RARE, 100, "LocalFireDepartment"),
    AchievementBadge("two_week_champ", "Two-Week Champion", "Reach a 14-day streak", "Streaks", BadgeRarity.RARE, 150, "Shield"),
    AchievementBadge("monthly_master", "Monthly Master", "Maintain a 30-day streak", "Streaks", BadgeRarity.EPIC, 300, "EmojiEvents"),
    AchievementBadge("unstoppable", "Unstoppable", "Reach an epic 100-day streak", "Streaks", BadgeRarity.LEGENDARY, 750, "Whatshot"),
    AchievementBadge("goal_crusher", "Goal Crusher", "Achieve your personal goals", "Productivity", BadgeRarity.RARE, 150, "MilitaryTech"),
    AchievementBadge("rising_star", "Rising Star", "Accumulate 1,000 Lifetime XP", "XP", BadgeRarity.EPIC, 250, "AutoAwesome"),
    AchievementBadge("legendary_rank", "Legendary Rank", "Attain Level 10 Legend status", "XP", BadgeRarity.LEGENDARY, 1000, "Diamond")
)

data class ShopItem(val id: String, val title: String, val type: String, val cost: Int, val description: String)

val SHOP_ITEMS = listOf(
    ShopItem("title_disciplined", "The Disciplined", "title", 50, "Displays exclusive 'The Disciplined' profile title"),
    ShopItem("title_streak_master", "Streak Titan", "title", 100, "Badge title honoring relentless habit dedication"),
    ShopItem("title_grandmaster", "Grandmaster Mind", "title", 250, "Elite title for habit masters reaching high consistency"),
    ShopItem("theme_neon_cyber", "Cyber Neon Theme", "theme", 150, "Unlocks vibrant glowing cyber neon highlights"),
    ShopItem("theme_sunset_glow", "Sunset Aura Theme", "theme", 180, "Warm glowing amber and coral aesthetic theme"),
    ShopItem("frame_gold_laurel", "Golden Laurel Frame", "avatar_frame", 200, "Gilded avatar frame celebrating consistency"),
    ShopItem("frame_cosmic_flame", "Cosmic Flame Aura", "avatar_frame", 300, "Radiant animated aura around your profile"),
    ShopItem("sound_zen_chime", "Zen Bell Completion FX", "sound", 80, "Soothing Tibetan chime on habit completion"),
    ShopItem("shield_rest_token", "Emergency Streak Freeze", "powerup", 120, "Shields your streak if an unexpected busy day occurs")
)

@Composable
fun RewardsScreen(
    userId: String,
    repository: HabitRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val userProfile by repository.observeUserProfile(userId).collectAsState(initial = null)
    val unlockedRewards by repository.observeUserRewards(userId).collectAsState(initial = emptyList())
    var selectedTab by remember { mutableStateOf(0) } // 0: Badges & Levels, 1: Reward Shop
    var purchaseMessage by remember { mutableStateOf<String?>(null) }

    val profile = userProfile ?: UserProfile()
    val nextLevelTier = GamificationConfig.nextLevel(profile.currentLevel)
    val currentTier = GamificationConfig.calculateLevel(profile.lifetimeXP)
    val xpForNext = (nextLevelTier?.requiredLifetimeXP ?: profile.lifetimeXP) - currentTier.requiredLifetimeXP
    val xpInCurrent = profile.lifetimeXP - currentTier.requiredLifetimeXP
    val levelProgress = if (xpForNext > 0) (xpInCurrent.toFloat() / xpForNext).coerceIn(0f, 1f) else 1f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        item {
            Text(
                text = "Rewards & Achievements",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Track badges, level milestones, and unlock shop items",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Gamified Level Progression Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Current Rank: ${profile.currentRank}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = HabitGold
                            )
                            Text(
                                text = "Level ${profile.currentLevel}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(HabitCyan.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${profile.rewardPoints} Reward Points",
                                color = HabitCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${profile.lifetimeXP} Lifetime XP",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = nextLevelTier?.let { "Next: Level ${it.level} (${it.requiredLifetimeXP} XP)" } ?: "Max Level Achieved!",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = HabitViolet
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { levelProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = HabitViolet,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // Section Tabs (Badges vs Shop)
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = HabitViolet,
                modifier = Modifier.clip(RoundedCornerShape(14.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Achievements (${ALL_ACHIEVEMENTS.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Reward Shop", fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (selectedTab == 0) {
            // Achievement Badges
            item {
                Text(
                    text = "Achievement Badges",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(ALL_ACHIEVEMENTS) { badge ->
                // Check unlocked condition based on profile
                val isUnlocked = when (badge.id) {
                    "first_habit" -> profile.totalCompletedHabits >= 1
                    "first_steps" -> profile.totalCompletedHabits >= 5
                    "week_warrior" -> profile.longestStreak >= 7
                    "two_week_champ" -> profile.longestStreak >= 14
                    "monthly_master" -> profile.longestStreak >= 30
                    "unstoppable" -> profile.longestStreak >= 100
                    "rising_star" -> profile.lifetimeXP >= 1000
                    "legendary_rank" -> profile.currentLevel >= 10
                    else -> profile.totalCompletedHabits >= 1
                }

                AchievementItemCard(badge = badge, isUnlocked = isUnlocked)
            }
        } else {
            // Reward Shop
            item {
                if (purchaseMessage != null) {
                    Text(
                        text = purchaseMessage!!,
                        color = HabitCyan,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            items(SHOP_ITEMS) { item ->
                val alreadyPurchased = unlockedRewards.any { it.rewardId == item.id }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (alreadyPurchased) HabitEmerald.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "${item.cost} Points", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = HabitGold)
                        }

                        if (alreadyPurchased) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(HabitEmerald.copy(alpha = 0.2f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("UNLOCKED", color = HabitEmerald, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        } else {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val res = repository.purchaseReward(userId, item.id, item.title, item.cost, item.type)
                                        if (res.isSuccess) {
                                            purchaseMessage = "Successfully unlocked ${item.title}!"
                                        } else {
                                            purchaseMessage = res.exceptionOrNull()?.message ?: "Purchase failed"
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HabitViolet),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Unlock")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementItemCard(
    badge: AchievementBadge,
    isUnlocked: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) {
                when (badge.rarity) {
                    BadgeRarity.LEGENDARY -> HabitGold.copy(alpha = 0.15f)
                    BadgeRarity.EPIC -> HabitViolet.copy(alpha = 0.15f)
                    BadgeRarity.RARE -> HabitCyan.copy(alpha = 0.12f)
                    BadgeRarity.COMMON -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        if (isUnlocked) {
                            when (badge.rarity) {
                                BadgeRarity.LEGENDARY -> HabitGold.copy(alpha = 0.25f)
                                BadgeRarity.EPIC -> HabitViolet.copy(alpha = 0.25f)
                                BadgeRarity.RARE -> HabitCyan.copy(alpha = 0.25f)
                                BadgeRarity.COMMON -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        } else {
                            Color(0x33555555)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isUnlocked) Icons.Filled.MilitaryTech else Icons.Filled.Lock,
                    contentDescription = badge.title,
                    tint = if (isUnlocked) {
                        when (badge.rarity) {
                            BadgeRarity.LEGENDARY -> HabitGold
                            BadgeRarity.EPIC -> HabitVioletLight
                            BadgeRarity.RARE -> HabitCyan
                            BadgeRarity.COMMON -> HabitEmerald
                        }
                    } else {
                        Color.Gray
                    },
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = badge.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) MaterialTheme.colorScheme.onBackground else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x22FFFFFF))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge.rarity.name,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (badge.rarity) {
                                BadgeRarity.LEGENDARY -> HabitGold
                                BadgeRarity.EPIC -> HabitViolet
                                BadgeRarity.RARE -> HabitCyan
                                BadgeRarity.COMMON -> Color.LightGray
                            }
                        )
                    }
                }
                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(HabitGold.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "+${badge.xpReward} XP",
                    color = HabitGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
