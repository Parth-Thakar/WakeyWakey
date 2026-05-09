package com.z.wakey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.z.wakey.ui.navigation.NavGraph
import com.z.wakey.ui.theme.DeepBlack
import com.z.wakey.ui.theme.WakeyWakeyTheme
import com.z.wakey.viewmodel.AlarmViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WakeyWakeyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DeepBlack
                ) {
                    val navController = rememberNavController()
                    val viewModel: AlarmViewModel = viewModel()
                    NavGraph(navController = navController, viewModel = viewModel)
                }
            }
        }
    }
}
