package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.repository.HabitRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.navigation.MainAppScaffold
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.splash.HabitFlowSplashScreen
import com.example.ui.theme.HabitFlowTheme
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

enum class AppScreenState {
    SPLASH,
    ONBOARDING,
    MAIN
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory App Check intent extra wiring before setContent
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            HabitFlowTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRootContent(this)
                }
            }
        }
    }
}

@Composable
fun AppRootContent(activity: ComponentActivity) {
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember(activity) {
        activity.getSharedPreferences("habitflow_prefs", Context.MODE_PRIVATE)
    }

    var screenState by remember { mutableStateOf(AppScreenState.SPLASH) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    when (screenState) {
        AppScreenState.SPLASH -> {
            HabitFlowSplashScreen(
                onSplashFinished = {
                    val hasCompletedOnboarding = prefs.getBoolean("has_completed_onboarding", false)
                    screenState = if (hasCompletedOnboarding) {
                        AppScreenState.MAIN
                    } else {
                        AppScreenState.ONBOARDING
                    }
                }
            )
        }
        AppScreenState.ONBOARDING -> {
            OnboardingScreen(
                onFinishOnboarding = {
                    prefs.edit().putBoolean("has_completed_onboarding", true).apply()
                    screenState = AppScreenState.MAIN
                }
            )
        }
        AppScreenState.MAIN -> {
            val user = currentUser
            if (user == null) {
                AuthScreen(
                    onAuthSuccess = {
                        currentUser = Firebase.auth.currentUser
                    }
                )
            } else {
                val databaseId = activity.getString(R.string.firestore_database_id)
                val firestore = remember(databaseId) {
                    FirebaseFirestore.getInstance(databaseId)
                }
                val repository = remember(firestore) {
                    HabitRepository(firestore, activity.applicationContext)
                }

                val roomDatabase = remember(activity) {
                    com.example.data.room.HabitFlowDatabase.getDatabase(activity.applicationContext)
                }
                val gamificationViewModel = remember(user.uid) {
                    com.example.ui.viewmodel.GamificationViewModel(
                        repository = repository,
                        roomDao = roomDatabase.habitFlowDao(),
                        userId = user.uid
                    )
                }

                // Initialize user profile document on first sign in
                LaunchedEffect(user.uid) {
                    coroutineScope.launch {
                        repository.getOrCreateUserProfile(
                            userId = user.uid,
                            displayName = user.displayName ?: "HabitFlow Champion",
                            email = user.email
                        )
                    }
                }

                MainAppScaffold(
                    userId = user.uid,
                    repository = repository,
                    gamificationViewModel = gamificationViewModel,
                    onSignOut = {
                        currentUser = null
                    },
                    onReplayOnboarding = {
                        screenState = AppScreenState.ONBOARDING
                    }
                )
            }
        }
    }
}
