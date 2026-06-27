package com.portfello.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.portfello.ui.assets.AddEditAssetScreen
import com.portfello.ui.assets.AssetDetailScreen
import com.portfello.ui.assets.AssetListScreen
import com.portfello.ui.dashboard.DashboardScreen
import com.portfello.ui.settings.SettingsScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val ASSET_LIST = "assets"
    const val ADD_ASSET = "assets/add"
    const val EDIT_ASSET = "assets/edit/{id}"
    const val ASSET_DETAIL = "assets/{id}"
    const val SETTINGS = "settings"

    fun editAsset(id: Long) = "assets/edit/$id"
    fun assetDetail(id: Long) = "assets/$id"
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onNavigateToAssets = { navController.navigate(Routes.ASSET_LIST) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onAssetClick = { navController.navigate(Routes.assetDetail(it)) }
            )
        }
        composable(Routes.ASSET_LIST) {
            AssetListScreen(
                onAddAsset = { navController.navigate(Routes.ADD_ASSET) },
                onAssetClick = { navController.navigate(Routes.assetDetail(it)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.ADD_ASSET) {
            AddEditAssetScreen(
                assetId = null,
                onDone = { navController.popBackStack() }
            )
        }
        composable(
            Routes.EDIT_ASSET,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) {
            AddEditAssetScreen(
                assetId = it.arguments?.getLong("id"),
                onDone = { navController.popBackStack() }
            )
        }
        composable(
            Routes.ASSET_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) {
            AssetDetailScreen(
                assetId = it.arguments?.getLong("id") ?: return@composable,
                onEdit = { id -> navController.navigate(Routes.editAsset(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
