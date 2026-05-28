package com.example.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.AddEditScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.VaultListScreen

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    vaultViewModel: VaultViewModel
) {
    val navController = rememberNavController()
    val authState by authViewModel.uiState.collectAsState()

    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (authState.screenState == AuthScreenState.AUTHENTICATED) "vault" else "auth",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("auth") {
                LoginScreen(
                    viewModel = authViewModel,
                    onAuthenticated = {
                        navController.navigate("vault") {
                            popUpTo("auth") { inclusive = true }
                        }
                    }
                )
            }
            composable("vault") {
                if (authState.screenState != AuthScreenState.AUTHENTICATED) {
                    navController.navigate("auth") {
                        popUpTo("vault") { inclusive = true }
                    }
                } else {
                    VaultListScreen(
                        viewModel = vaultViewModel,
                        onLogout = {
                            authViewModel.logout()
                        },
                        onNavigateToAdd = {
                            navController.navigate("addEdit/-1")
                        },
                        onNavigateToEdit = { itemId ->
                            navController.navigate("addEdit/$itemId")
                        }
                    )
                }
            }
            composable(
                route = "addEdit/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.IntType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getInt("itemId") ?: -1
                AddEditScreen(
                    itemId = itemId,
                    viewModel = vaultViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
