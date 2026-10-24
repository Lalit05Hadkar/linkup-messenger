package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.chat.ChatDetailScreen
import com.example.ui.screens.edit_profile.EditProfileScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.profile_setup.ProfileSetupScreen
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.screens.voice_live.LiveVoiceScreen
import com.example.ui.theme.LinkUpTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.LinkUpViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: LinkUpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.repository.themeMode.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()
            val toastMessage by viewModel.toastMessage.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(toastMessage) {
                toastMessage?.let {
                    snackbarHostState.showSnackbar(it)
                    viewModel.clearToast()
                }
            }

            LinkUpTheme(themeMode = themeMode) {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        Crossfade(
                            targetState = currentScreen,
                            label = "ScreenTransition"
                        ) { screen ->
                            when (screen) {
                                is AppScreen.Splash -> SplashScreen()
                                is AppScreen.Auth -> AuthScreen(viewModel = viewModel)
                                is AppScreen.ProfileSetup -> ProfileSetupScreen(viewModel = viewModel)
                                is AppScreen.Home -> HomeScreen(viewModel = viewModel)
                                is AppScreen.ChatDetail -> ChatDetailScreen(
                                    friendUid = screen.friendUid,
                                    chatId = screen.chatId,
                                    viewModel = viewModel
                                )
                                is AppScreen.EditProfile -> EditProfileScreen(viewModel = viewModel)
                                is AppScreen.LiveVoice -> LiveVoiceScreen(
                                    onDismiss = { viewModel.handleBack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
