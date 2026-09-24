package dev.viniciuscole.nudge.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.viniciuscole.nudge.NudgeApp
import dev.viniciuscole.nudge.ui.add.AddReminderScreen
import dev.viniciuscole.nudge.ui.add.AddReminderViewModel
import dev.viniciuscole.nudge.ui.builder.MealBuilderScreen
import dev.viniciuscole.nudge.ui.builder.MealBuilderViewModel
import dev.viniciuscole.nudge.ui.diet.DietScreen
import dev.viniciuscole.nudge.ui.diet.DietViewModel
import dev.viniciuscole.nudge.ui.home.HomeScreen
import dev.viniciuscole.nudge.ui.home.HomeViewModel

object Routes {
    const val HOME = "home"
    const val ADD = "add"
    const val EDIT = "edit/{reminderId}"
    fun edit(id: Long) = "edit/$id"
    const val BUILDER = "builder/{reminderId}"
    fun builder(id: Long) = "builder/$id"
    const val DIET = "diet"
}

@Composable
fun NudgeNavHost() {
    val nav = rememberNavController()
    val app = NudgeApp.from(LocalContext.current)

    NavHost(nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel(factory = viewModelFactory { initializer { HomeViewModel(app) } })
            HomeScreen(
                vm = vm,
                onAdd = { nav.navigate(Routes.ADD) },
                onOpenBuilder = { id -> nav.navigate(Routes.builder(id)) },
            )
        }
        composable(Routes.ADD) {
            val vm: AddReminderViewModel = viewModel(factory = viewModelFactory { initializer { AddReminderViewModel(app) } })
            AddReminderScreen(vm = vm, onBack = { nav.popBackStack() })
        }
        composable(
            Routes.EDIT,
            arguments = listOf(navArgument("reminderId") { type = NavType.LongType }),
        ) { entry ->
            val reminderId = entry.arguments?.getLong("reminderId") ?: -1L
            val vm: AddReminderViewModel = viewModel(
                key = "edit-$reminderId",
                factory = viewModelFactory { initializer { AddReminderViewModel(app, reminderId) } },
            )
            AddReminderScreen(vm = vm, onBack = { nav.popBackStack() })
        }
        composable(
            Routes.BUILDER,
            arguments = listOf(navArgument("reminderId") { type = NavType.LongType }),
        ) { entry ->
            val reminderId = entry.arguments?.getLong("reminderId") ?: -1L
            val vm: MealBuilderViewModel = viewModel(
                key = "builder-$reminderId",
                factory = viewModelFactory { initializer { MealBuilderViewModel(app, reminderId) } },
            )
            MealBuilderScreen(vm = vm, onBack = { nav.popBackStack() })
        }
        composable(Routes.DIET) {
            val vm: DietViewModel = viewModel(factory = viewModelFactory { initializer { DietViewModel(app) } })
            DietScreen(vm = vm, onBack = { nav.popBackStack() }, onOpenBuilder = { id -> nav.navigate(Routes.builder(id)) })
        }
    }
}
