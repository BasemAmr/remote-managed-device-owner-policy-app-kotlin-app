package com.selfcontrol.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.selfcontrol.presentation.navigation.NavGraph
import com.selfcontrol.presentation.theme.SelfControlTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SelfControlTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}
