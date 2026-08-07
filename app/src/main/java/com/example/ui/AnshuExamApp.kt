package com.example.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.viewmodel.MainViewModel
import com.example.model.TestConfig
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.CreateTestScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PerformanceScreen
import com.example.ui.screens.QuestionBankScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TestHistoryScreen
import com.example.ui.screens.WeakTopicsScreen
import com.example.ui.screens.WrongQuestionsScreen
import com.example.ui.theme.AnshuExamTheme

@Composable
fun AnshuExamApp(
    viewModel: MainViewModel,
    initialRoute: String? = null
) {
    val isDark = isSystemInDarkTheme()
    var showSplash by rememberSaveable { mutableStateOf(true) }

    AnshuExamTheme(darkTheme = isDark) {
        Crossfade(
            targetState = showSplash,
            animationSpec = tween(400),
            label = "splash_crossfade"
        ) { isSplashShowing ->
            if (isSplashShowing) {
                SplashScreen(
                    onSplashFinished = { showSplash = false }
                )
            } else {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val isOnboardingCompleted by viewModel.onboardingCompleted.collectAsState()

                LaunchedEffect(initialRoute, isOnboardingCompleted) {
                    if (!initialRoute.isNullOrBlank() && initialRoute != "home" && isOnboardingCompleted) {
                        try {
                            navController.navigate(initialRoute)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                val topLevelRoutes = listOf("home", "test_history", "settings")
                val showBottomBar = currentRoute in topLevelRoutes

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 3.dp
                            ) {
                                NavigationBarItem(
                                    selected = currentRoute == "home",
                                    onClick = {
                                        if (currentRoute != "home") {
                                            navController.navigate("home") {
                                                popUpTo("home") { inclusive = true }
                                            }
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Home") },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentRoute == "test_history",
                                    onClick = {
                                        if (currentRoute != "test_history") {
                                            navController.navigate("test_history") {
                                                popUpTo("home")
                                            }
                                        }
                                    },
                                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                                    label = { Text("History") },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentRoute == "settings",
                                    onClick = {
                                        if (currentRoute != "settings") {
                                            navController.navigate("settings") {
                                                popUpTo("home")
                                            }
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text("Settings") },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = if (isOnboardingCompleted) "home" else "onboarding",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        enterTransition = {
                            fadeIn(animationSpec = tween(220)) + slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(220)
                            )
                        },
                        exitTransition = {
                            fadeOut(animationSpec = tween(180)) + slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(220)
                            )
                        },
                        popEnterTransition = {
                            fadeIn(animationSpec = tween(220)) + slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(220)
                            )
                        },
                        popExitTransition = {
                            fadeOut(animationSpec = tween(180)) + slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(220)
                            )
                        }
                    ) {
                        composable("onboarding") {
                            OnboardingScreen(
                                viewModel = viewModel,
                                onOnboardingFinished = {
                                    navController.navigate("home") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigate = { route -> navController.navigate(route) }
                            )
                        }

                        composable("create_test") {
                            CreateTestScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onStartQuiz = { navController.navigate("quiz") }
                            )
                        }

                        composable("quiz") {
                            QuizScreen(
                                viewModel = viewModel,
                                onNavigateHome = {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                onNavigateToResult = {
                                    navController.navigate("result") {
                                        popUpTo("home")
                                    }
                                }
                            )
                        }

                        composable("result") {
                            ResultScreen(
                                viewModel = viewModel,
                                onNavigateHome = {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                onNavigateWrongQuestions = {
                                    navController.navigate("wrong_questions")
                                },
                                onNavigateToQuiz = {
                                    navController.navigate("quiz")
                                }
                            )
                        }

                        composable("question_bank") {
                            QuestionBankScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToQuiz = { navController.navigate("quiz") },
                                onNavigateToCreateTest = { navController.navigate("create_test") }
                            )
                        }

                        composable("wrong_questions") {
                            WrongQuestionsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("weak_topics") {
                            WeakTopicsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onPracticeTopic = { topic ->
                                    val config = TestConfig(
                                        naturalPrompt = "Practice $topic topic questions",
                                        strictSourceMode = false
                                    )
                                    viewModel.startNewTest(config)
                                    navController.navigate("quiz")
                                }
                            )
                        }

                        composable("test_history") {
                            TestHistoryScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onOpenResult = { record ->
                                    viewModel.reopenTestRecord(record)
                                    navController.navigate("result")
                                }
                            )
                        }

                        composable("bookmarks") {
                            BookmarksScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("performance") {
                            PerformanceScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onRerunOnboarding = {
                                    viewModel.rerunOnboarding()
                                    navController.navigate("onboarding") {
                                        popUpTo("home")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

