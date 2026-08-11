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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.UpdateDialog
import com.example.data.update.UpdateState
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
import com.example.ui.screens.AiDoubtScreen
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.CreateTestScreen
import com.example.ui.screens.FlashcardsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PerformanceScreen
import com.example.ui.screens.QuestionBankScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StudyNotesScreen
import com.example.ui.screens.TestHistoryScreen
import com.example.ui.screens.WeakTopicsScreen
import com.example.ui.screens.WrongQuestionsScreen
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistryOwner
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.util.LocaleHelper
import com.example.ui.theme.AnshuExamTheme

@Composable
fun AnshuExamApp(
    viewModel: MainViewModel,
    initialRoute: String? = null
) {
    var showSplash by rememberSaveable { mutableStateOf(true) }
    val appLanguage by viewModel.appLanguage.collectAsState()
    val context = LocalContext.current
    val localizedContext = remember(appLanguage, context) {
        LocaleHelper.createLocalizedContext(context, appLanguage)
    }
    val currentActivityResultOwner = LocalActivityResultRegistryOwner.current
    val activityResultOwner = remember(context, currentActivityResultOwner) {
        currentActivityResultOwner ?: run {
            var current: Context? = context
            while (current != null) {
                if (current is ActivityResultRegistryOwner) return@run current
                if (current is ContextWrapper) {
                    current = current.baseContext
                } else {
                    break
                }
            }
            null
        }
    }

    ProvideLocals(
        localizedContext = localizedContext,
        activityResultOwner = activityResultOwner
    ) {
        AnshuExamTheme(darkTheme = false) {
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
                    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

                    UpdateDialog(
                        updateState = updateState,
                        onDismiss = {
                            (updateState as? UpdateState.Available)?.let {
                                viewModel.updateManager.dismissUpdate(it.updateInfo)
                            } ?: viewModel.updateManager.resetState()
                        },
                        onDownload = {
                            (updateState as? UpdateState.Available)?.let {
                                viewModel.updateManager.startDownload(it.updateInfo)
                            }
                        },
                        onAllowPermission = {
                            viewModel.updateManager.openSettingsForPermission()
                        },
                        onRetryInstall = {
                            (updateState as? UpdateState.Downloaded)?.let {
                                viewModel.updateManager.retryInstallation(it.updateInfo, it.apkFile)
                            } ?: (updateState as? UpdateState.PermissionRequired)?.let {
                                viewModel.updateManager.retryInstallation(it.updateInfo, it.apkFile)
                            }
                        },
                        onRetryDownload = {
                            (updateState as? UpdateState.Error)?.updateInfo?.let {
                                viewModel.updateManager.startDownload(it)
                            }
                        }
                    )

                LaunchedEffect(initialRoute, isOnboardingCompleted) {
                    if (!initialRoute.isNullOrBlank() && initialRoute != "home" && isOnboardingCompleted) {
                        try {
                            navController.navigate(initialRoute)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                val topLevelRoutes = listOf("home", "study_notes", "flashcards", "ai_doubt", "settings")
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
                                    icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.nav_home)) },
                                    label = { Text(stringResource(R.string.nav_home), maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentRoute == "study_notes",
                                    onClick = {
                                        if (currentRoute != "study_notes") {
                                            navController.navigate("study_notes") {
                                                popUpTo("home")
                                            }
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Description, contentDescription = stringResource(R.string.nav_ai_notes)) },
                                    label = { Text(stringResource(R.string.nav_ai_notes), maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentRoute == "flashcards",
                                    onClick = {
                                        if (currentRoute != "flashcards") {
                                            navController.navigate("flashcards") {
                                                popUpTo("home")
                                            }
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Style, contentDescription = stringResource(R.string.nav_flashcards)) },
                                    label = { Text(stringResource(R.string.nav_flashcards), maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentRoute == "ai_doubt",
                                    onClick = {
                                        if (currentRoute != "ai_doubt") {
                                            navController.navigate("ai_doubt") {
                                                popUpTo("home")
                                            }
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Psychology, contentDescription = stringResource(R.string.nav_ai_solver)) },
                                    label = { Text(stringResource(R.string.nav_ai_solver), maxLines = 1) },
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
                                    icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings)) },
                                    label = { Text(stringResource(R.string.nav_settings), maxLines = 1) },
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

                        composable("study_notes") {
                            StudyNotesScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("flashcards") {
                            FlashcardsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("ai_doubt") {
                            AiDoubtScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun ProvideLocals(
    localizedContext: Context,
    activityResultOwner: ActivityResultRegistryOwner?,
    content: @Composable () -> Unit
) {
    if (activityResultOwner != null) {
        CompositionLocalProvider(
            LocalContext provides localizedContext,
            LocalActivityResultRegistryOwner provides activityResultOwner,
            content = content
        )
    } else {
        CompositionLocalProvider(
            LocalContext provides localizedContext,
            content = content
        )
    }
}

