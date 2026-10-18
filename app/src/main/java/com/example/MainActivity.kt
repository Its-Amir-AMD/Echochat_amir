package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.EchoChatViewModel
import com.example.ui.screens.EchoAuthScreen
import com.example.ui.screens.FirebaseSetupRequiredScreen
import com.example.ui.screens.MainScreen
import com.example.ui.theme.EchoChatTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val firebaseConfigured = FirebaseApp.getApps(applicationContext).isNotEmpty()
        setContent {
            if (!firebaseConfigured) {
                EchoChatTheme(darkTheme = true) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        CompositionLocalProvider(
                            LocalLayoutDirection provides LayoutDirection.Rtl
                        ) {
                            FirebaseSetupRequiredScreen()
                        }
                    }
                }
            } else {
                val auth = remember { FirebaseAuth.getInstance() }
                var signedInUid by remember(auth) {
                    mutableStateOf(auth.currentUser?.uid)
                }
                DisposableEffect(auth) {
                    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                        signedInUid = firebaseAuth.currentUser?.uid
                    }
                    auth.addAuthStateListener(listener)
                    onDispose { auth.removeAuthStateListener(listener) }
                }

                val uid = signedInUid
                if (uid.isNullOrBlank()) {
                    EchoChatTheme(darkTheme = true) {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            CompositionLocalProvider(
                                LocalLayoutDirection provides LayoutDirection.Rtl
                            ) {
                                EchoAuthScreen()
                            }
                        }
                    }
                } else {
                    // Key the ViewModel by Firebase UID so switching accounts also switches
                    // the Room cache and never leaks the previous account's local conversations.
                    val viewModel: EchoChatViewModel = viewModel(key = uid)
                    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
                    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
                    val effectiveDarkTheme = isDarkMode ?: isSystemInDarkTheme()

                    CompositionLocalProvider(
                        LocalLayoutDirection provides currentLanguage.layoutDirection
                    ) {
                        EchoChatTheme(darkTheme = effectiveDarkTheme) {
                            Surface(modifier = Modifier.fillMaxSize()) {
                                MainScreen(
                                    viewModel = viewModel,
                                    onSignOut = { auth.signOut() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
