package com.reelflow.platform

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.facebook.CallbackManager
import com.reelflow.platform.data.service.FacebookLoginHelper
import com.reelflow.platform.data.service.RealMetaPublishingService
import com.reelflow.platform.data.service.SecureTokenStore
import com.reelflow.platform.ui.navigation.BOTTOM_NAV_ITEMS
import com.reelflow.platform.ui.navigation.Screen
import com.reelflow.platform.ui.screens.*
import com.reelflow.platform.ui.theme.ReelFlowTheme
import com.reelflow.platform.ui.theme.Slate950
import com.reelflow.platform.viewmodel.PlatformViewModel

class MainActivity : ComponentActivity() {
    private lateinit var callbackManager: CallbackManager

    @Deprecated("Facebook SDK still routes login results through onActivityResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // NOTE: do NOT call FacebookSdk.sdkInitialize() here — it crashed
        // launch on the old app. Init happens lazily inside the login/token
        // try blocks, where a failure shows as a message, never a crash.
        callbackManager = CallbackManager.Factory.create()
        val realPublisher by lazy { RealMetaPublishingService(applicationContext) }
        setContent {
            ReelFlowTheme {
                val viewModel: PlatformViewModel = viewModel()
                LaunchedEffect(Unit) {
                    viewModel.attachRealPublishing(realPublisher)
                    viewModel.attachAppContext(applicationContext)
                }
                // Facebook Login requests from Settings: run the SDK login flow here.
                LaunchedEffect(Unit) {
                    viewModel.fbLoginRequest.collect {
                        FacebookLoginHelper(
                            activity = this@MainActivity,
                            tokenStore = SecureTokenStore(applicationContext),
                            callbackManager = callbackManager,
                            appId = getString(R.string.facebook_app_id),
                            onResult = { viewModel.onFacebookLoginResult(it) }
                        ).login()
                    }
                }
                // Token-connect requests from Settings: same helper, pasted token.
                LaunchedEffect(Unit) {
                    viewModel.tokenLoginRequest.collect { userToken ->
                        FacebookLoginHelper(
                            activity = this@MainActivity,
                            tokenStore = SecureTokenStore(applicationContext),
                            callbackManager = callbackManager,
                            appId = getString(R.string.facebook_app_id),
                            onResult = { viewModel.onFacebookLoginResult(it) }
                        ).loginWithToken(userToken)
                    }
                }
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val statusMessage by viewModel.statusMessage.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(statusMessage) {
                    statusMessage?.let {
                        snackbarHostState.showSnackbar(it)
                        viewModel.clearMessage()
                    }
                }

                Scaffold(
                    containerColor = Slate950,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        NavigationBar(containerColor = Slate950) {
                            BOTTOM_NAV_ITEMS.forEach { screen ->
                                NavigationBarItem(
                                    selected = currentRoute == screen.route,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(iconFor(screen), contentDescription = screen.label) },
                                    label = { Text(screen.label) }
                                )
                            }
                        }
                    }
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.padding(padding)
                    ) {
                        composable(Screen.Home.route) {
                            DashboardScreen(viewModel) { route -> navController.navigate(route) }
                        }
                        composable(Screen.Library.route) {
                            VideoLibraryScreen(
                                viewModel,
                                onOpenVideo = { id -> navController.navigate(Screen.VideoDetail.routeFor(id)) },
                                onOpenThumbnail = { id -> navController.navigate(Screen.ThumbnailEditor.routeFor(id)) }
                            )
                        }
                        composable(Screen.Create.route) { CreateVideoScreen(viewModel) }
                        composable(Screen.Review.route) {
                            ApprovalScreen(
                                viewModel,
                                onOpenThumbnail = { id -> navController.navigate(Screen.ThumbnailEditor.routeFor(id)) }
                            )
                        }
                        composable(Screen.Pages.route) { PagesScreen(viewModel) }
                        composable(Screen.Schedule.route) { ScheduleScreen(viewModel) }
                        composable(Screen.Analytics.route) { AnalyticsScreen(viewModel) }
                        composable(Screen.Settings.route) { SettingsScreen(viewModel) }
                        composable(Screen.ThumbnailEditor.route) { backStack ->
                            val jobId = backStack.arguments?.getString("jobId").orEmpty()
                            ThumbnailEditorScreen(viewModel, jobId) { navController.popBackStack() }
                        }
                        composable(Screen.VideoDetail.route) { backStack ->
                            val jobId = backStack.arguments?.getString("jobId").orEmpty()
                            VideoDetailScreen(
                                viewModel, jobId,
                                onOpenThumbnail = { id -> navController.navigate(Screen.ThumbnailEditor.routeFor(id)) },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun iconFor(screen: Screen): ImageVector = when (screen) {
    Screen.Home -> Icons.Filled.Home
    Screen.Library -> Icons.Filled.VideoLibrary
    Screen.Create -> Icons.Filled.AddCircle
    Screen.Review -> Icons.Filled.CheckCircle
    Screen.Pages -> Icons.Filled.Group
    else -> Icons.Filled.MoreHoriz
}
