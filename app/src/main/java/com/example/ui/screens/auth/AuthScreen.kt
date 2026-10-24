package com.example.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.theme.AccentBlue
import com.example.viewmodel.LinkUpViewModel
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

private const val GOOGLE_WEB_CLIENT_ID = "1035669536043-if6aguqtm3bj6k9sui2v3g9u3227kgn0.apps.googleusercontent.com"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: LinkUpViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val authLoading by viewModel.authLoading.collectAsState()
    val backendAuthError by viewModel.authErrorMessage.collectAsState()

    var showEmailSheet by remember { mutableStateOf(false) }
    var isSignUpMode by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var isGoogleLoading by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Trigger authentic Google Sign-In with Credential Manager
    fun launchGoogleSignIn() {
        if (isGoogleLoading || authLoading) return
        isGoogleLoading = true
        localError = null
        viewModel.clearAuthError()

        scope.launch {
            try {
                val googleIdOption = GetSignInWithGoogleOption.Builder(GOOGLE_WEB_CLIENT_ID)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )

                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val email = googleIdTokenCredential.id
                    val displayName = googleIdTokenCredential.displayName
                    val idToken = googleIdTokenCredential.idToken

                    if (email.isNotBlank()) {
                        viewModel.signInWithGoogle(
                            email = email,
                            displayName = displayName,
                            idToken = idToken
                        )
                    } else {
                        localError = "Failed to obtain email from Google account."
                    }
                } else {
                    localError = "Unexpected authentication credential from Google."
                }
            } catch (e: GetCredentialCancellationException) {
                // User cancelled the Google account chooser
                localError = "Google Sign-In was cancelled."
            } catch (e: Exception) {
                localError = e.localizedMessage ?: "Google Sign-In failed."
            } finally {
                isGoogleLoading = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            // Top Section with Logo and Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_linkup_logo),
                    contentDescription = "LinkUp Logo",
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(20.dp))
                )

                Spacer(modifier = Modifier.height(36.dp))

                Text(
                    text = "Welcome to\nLinkUp",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                        lineHeight = 42.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Chat with your friends in real time. Sign in to get started.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        lineHeight = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Bottom Actions Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Error display if Google sign-in failed
                if (localError != null && !showEmailSheet) {
                    Text(
                        text = localError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 12.dp),
                        textAlign = TextAlign.Center
                    )
                }

                // Continue with Google Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        .clickable(enabled = !isGoogleLoading && !authLoading) {
                            launchGoogleSignIn()
                        }
                        .testTag("google_signin_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isGoogleLoading || (authLoading && !showEmailSheet)) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AccentBlue,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "G",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = AccentBlue
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Continue with Google",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Continue with Email Button
                Button(
                    onClick = {
                        localError = null
                        viewModel.clearAuthError()
                        showEmailSheet = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("email_signin_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentBlue,
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Mail,
                            contentDescription = "Email",
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Continue with Email",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Legal text
                Text(
                    text = "By continuing, you agree to our Terms and Privacy Policy.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }

    // Email Bottom Sheet for Sign In / Sign Up
    if (showEmailSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                if (!authLoading) {
                    showEmailSheet = false
                    localError = null
                    viewModel.clearAuthError()
                }
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = if (isSignUpMode) "Create an Account" else "Sign In with Email",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isSignUpMode) "Enter your email and create a password." else "Enter your LinkUp credentials.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = {
                        emailInput = it
                        localError = null
                        viewModel.clearAuthError()
                    },
                    label = { Text("Email address") },
                    placeholder = { Text("you@example.com") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("email_input_field"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    enabled = !authLoading
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = {
                        passwordInput = it
                        localError = null
                        viewModel.clearAuthError()
                    },
                    label = { Text("Password") },
                    placeholder = { Text("••••••••") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input_field"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    enabled = !authLoading
                )

                val displayError = localError ?: backendAuthError
                if (displayError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = displayError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = {
                        val trimmedEmail = emailInput.trim()
                        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
                            localError = "Please enter a valid email address."
                            return@Button
                        }
                        if (passwordInput.length < 6) {
                            localError = "Password must be at least 6 characters."
                            return@Button
                        }

                        localError = null
                        viewModel.clearAuthError()

                        if (isSignUpMode) {
                            viewModel.createAccountWithEmail(trimmedEmail, passwordInput)
                        } else {
                            viewModel.signInWithEmail(trimmedEmail, passwordInput)
                        }
                    },
                    enabled = !authLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_auth_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentBlue,
                        contentColor = Color.White
                    )
                ) {
                    if (authLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isSignUpMode) "Sign Up" else "Sign In",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSignUpMode) "Already have an account?" else "Don't have an account?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = {
                            isSignUpMode = !isSignUpMode
                            localError = null
                            viewModel.clearAuthError()
                        },
                        enabled = !authLoading
                    ) {
                        Text(
                            text = if (isSignUpMode) "Sign In" else "Sign Up",
                            color = AccentBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
