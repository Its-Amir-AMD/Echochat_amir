package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun EchoAuthScreen() {
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()
    var isRegistering by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isBusy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (maxWidth < 420.dp) 16.dp else 28.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(if (maxWidth < 420.dp) 22.dp else 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forum,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                    Text(
                        text = "EchoChat",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isRegistering)
                            "حساب بسازید و به گفت‌وگوهای واقعی متصل شوید."
                        else
                            "وارد حساب خود شوید و گفت‌وگوهایتان را ادامه دهید.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isRegistering) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it.take(60); errorMessage = null },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("نام نمایشی") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            enabled = !isBusy
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.trim().take(254); errorMessage = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("ایمیل") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        enabled = !isBusy,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it.take(128); errorMessage = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("گذرواژه") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        singleLine = true,
                        enabled = !isBusy,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    statusMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = {
                            errorMessage = null
                            statusMessage = null
                            val cleanEmail = email.trim().lowercase()
                            if (!cleanEmail.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
                                errorMessage = "یک نشانی ایمیل معتبر وارد کنید."
                                return@Button
                            }
                            if (password.length < 8) {
                                errorMessage = "گذرواژه باید حداقل ۸ نویسه داشته باشد."
                                return@Button
                            }
                            if (isRegistering && displayName.trim().length < 2) {
                                errorMessage = "نام نمایشی باید حداقل ۲ نویسه داشته باشد."
                                return@Button
                            }

                            scope.launch {
                                isBusy = true
                                try {
                                    if (isRegistering) {
                                        auth.createUserWithEmailAndPassword(cleanEmail, password).await()
                                    } else {
                                        auth.signInWithEmailAndPassword(cleanEmail, password).await()
                                    }
                                    val user = requireNotNull(auth.currentUser) {
                                        "ورود انجام نشد؛ دوباره تلاش کنید."
                                    }
                                    val chosenName = if (isRegistering) {
                                        displayName.trim()
                                    } else {
                                        user.displayName?.takeIf(String::isNotBlank)
                                            ?: cleanEmail.substringBefore('@')
                                    }
                                    if (user.displayName != chosenName) {
                                        user.updateProfile(
                                            UserProfileChangeRequest.Builder()
                                                .setDisplayName(chosenName)
                                                .build()
                                        ).await()
                                    }
                                    saveAccountProfile(firestore, user, chosenName)
                                } catch (exception: Exception) {
                                    errorMessage = authErrorMessage(exception)
                                } finally {
                                    isBusy = false
                                }
                            }
                        },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                text = if (isRegistering) "ایجاد حساب" else "ورود",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (!isRegistering) {
                        TextButton(
                            onClick = {
                                errorMessage = null
                                statusMessage = null
                                val cleanEmail = email.trim().lowercase()
                                if (!cleanEmail.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
                                    errorMessage = "ابتدا ایمیل معتبر خود را وارد کنید."
                                } else {
                                    scope.launch {
                                        isBusy = true
                                        try {
                                            auth.sendPasswordResetEmail(cleanEmail).await()
                                            statusMessage = "در صورت وجود حساب، پیام بازیابی برای ایمیل شما ارسال شد."
                                        } catch (exception: Exception) {
                                            errorMessage = authErrorMessage(exception)
                                        } finally {
                                            isBusy = false
                                        }
                                    }
                                }
                            },
                            enabled = !isBusy,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("بازیابی گذرواژه")
                        }
                    }

                    Spacer(Modifier.height(2.dp))
                    TextButton(
                        onClick = {
                            isRegistering = !isRegistering
                            errorMessage = null
                            statusMessage = null
                        },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (isRegistering) "حساب دارید؟ وارد شوید"
                            else "حساب ندارید؟ ثبت‌نام کنید"
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "ارتباطات ابری با احراز هویت و قواعد دسترسی Firebase محافظت می‌شوند.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun FirebaseSetupRequiredScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Forum,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("پیکربندی Firebase لازم است", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "برای جلوگیری از نمایش گفت‌وگوهای ساختگی یا ذخیره‌سازی ناخواستهٔ محلی، " +
                        "ابتدا فایل app/google-services.json را برای پروژهٔ Firebase خود اضافه کنید، " +
                        "ورود با ایمیل/گذرواژه را فعال کنید و قواعد Firestore موجود در مخزن را منتشر کنید.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "راهنمای کامل در README.md قرار دارد.",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private suspend fun saveAccountProfile(
    firestore: FirebaseFirestore,
    user: FirebaseUser,
    displayName: String
) {
    val now = System.currentTimeMillis()
    firestore.collection("users").document(user.uid).set(
        mapOf(
            "uid" to user.uid,
            "email" to (user.email ?: ""),
            "displayName" to displayName,
            "updatedAt" to now,
            "createdAt" to now
        ),
        SetOptions.merge()
    ).await()
    firestore.collection("publicProfiles").document(user.uid).set(
        mapOf(
            "uid" to user.uid,
            "displayName" to displayName,
            "avatarColor" to 0xFF4F46E5L,
            "updatedAt" to now
        ),
        SetOptions.merge()
    ).await()
}

private fun authErrorMessage(exception: Exception): String = when (exception) {
    is FirebaseAuthWeakPasswordException -> "گذرواژه ضعیف است؛ یک گذرواژهٔ قوی‌تر انتخاب کنید."
    is FirebaseAuthUserCollisionException -> "برای این ایمیل قبلاً حسابی ساخته شده است."
    is FirebaseAuthInvalidCredentialsException -> "ایمیل یا گذرواژه معتبر نیست."
    else -> "عملیات انجام نشد. اتصال اینترنت و تنظیمات Firebase را بررسی کنید."
}
