package com.z.wakeywakey.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.z.wakeywakey.ui.screens.AddAlarmScreen
import com.z.wakeywakey.ui.screens.HomeScreen
import com.z.wakeywakey.viewmodel.AlarmViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AddAlarm : Screen("add_alarm")
    object EditAlarm : Screen("edit_alarm/{alarmId}") {
        fun route(id: Int) = "edit_alarm/$id"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: AlarmViewModel
) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onAddAlarm = { navController.navigate(Screen.AddAlarm.route) },
                onEditAlarm = { id -> navController.navigate(Screen.EditAlarm.route(id)) }
            )
        }

        composable(Screen.AddAlarm.route) {
            AddAlarmScreen(
                viewModel = viewModel,
                alarmId = null,
                onDone = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditAlarm.route,
            arguments = listOf(navArgument("alarmId") { type = NavType.IntType })
        ) { back ->
            AddAlarmScreen(
                viewModel = viewModel,
                alarmId = back.arguments?.getInt("alarmId"),
                onDone = { navController.popBackStack() }
            )
        }
    }
}
