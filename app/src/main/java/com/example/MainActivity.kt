package com.example

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.data.viewmodel.MainViewModel
import com.example.ui.AnshuExamApp

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val alpha = ObjectAnimator.ofFloat(
                splashScreenViewProvider.view,
                View.ALPHA,
                1f,
                0f
            )
            alpha.interpolator = AccelerateInterpolator()
            alpha.duration = 200L
            alpha.doOnEnd { splashScreenViewProvider.remove() }
            alpha.start()
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialRoute = intent?.getStringExtra("EXTRA_NAVIGATE_ROUTE")
        setContent {
            AnshuExamApp(viewModel = viewModel, initialRoute = initialRoute)
        }
    }
}
