package com.goprex.ui.login

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goprex.R
import com.goprex.data.model.Login
import com.goprex.ui.theme.BackgroundLight
import com.goprex.ui.theme.BorderGray
import com.goprex.ui.theme.ErrorRed
import com.goprex.ui.theme.GoPrexOrange
import com.goprex.ui.theme.TextPrimary
import com.goprex.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (Login) -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    var lembrarDados by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        // Limpa estado antigo
        viewModel.resetState()

        val sharedPrefs = context.getSharedPreferences("goprex_prefs", Context.MODE_PRIVATE)
        val savedLembrar = sharedPrefs.getBoolean("lembrar_dados", false)
        val savedLogin = sharedPrefs.getString("saved_login", "") ?: ""
        val savedPassword = sharedPrefs.getString("saved_password", "") ?: ""

        if (savedLembrar && savedLogin.isNotEmpty()) {
            viewModel.updateLogin(savedLogin)
            if (savedPassword.isNotEmpty()) {
                viewModel.updatePassword(savedPassword)
            }
            lembrarDados = true
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess && uiState.loginData != null) {

            val sharedPrefs = context.getSharedPreferences(
                "goprex_prefs",
                Context.MODE_PRIVATE
            )

            if (lembrarDados) {
                sharedPrefs.edit()
                    .putString("saved_login", uiState.login)
                    .putString("saved_password", uiState.password)
                    .putBoolean("lembrar_dados", true)
                    .apply()
            } else {
                sharedPrefs.edit()
                    .remove("saved_login")
                    .remove("saved_password")
                    .putBoolean("lembrar_dados", false)
                    .apply()
            }

            val loginData = uiState.loginData!!

            viewModel.resetState()

            onLoginSuccess(loginData)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "GoPrex",
                modifier = Modifier
                    .size(200.dp)
                    .padding(bottom = 40.dp)
            )

            // Card de login
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Acessar",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    OutlinedTextField(
                        value = uiState.login,
                        onValueChange = { viewModel.updateLogin(it) },
                        label = { Text("Login") },
                        placeholder = { Text("Digite seu login") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                        isError = uiState.error != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoPrexOrange,
                            unfocusedBorderColor = BorderGray,
                            focusedLabelColor = GoPrexOrange,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = GoPrexOrange
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = { viewModel.updatePassword(it) },
                        label = { Text("Senha") },
                        placeholder = { Text("Digite sua senha") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    painter = painterResource(if (passwordVisible) R.drawable.ic_eye_visible else R.drawable.ic_eye_invisible),
                                    contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha",
                                    tint = if (passwordVisible) GoPrexOrange else TextSecondary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        isError = uiState.error != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoPrexOrange,
                            unfocusedBorderColor = BorderGray,
                            focusedLabelColor = GoPrexOrange,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = GoPrexOrange
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = lembrarDados,
                            onCheckedChange = { lembrarDados = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = GoPrexOrange,
                                uncheckedColor = BorderGray,
                                checkmarkColor = Color.White
                            )
                        )
                        Text("Lembrar meus dados", fontSize = 13.sp, color = TextSecondary)
                    }

                    if (uiState.error != null) {
                        Text(uiState.error!!, color = ErrorRed, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }

                    Button(
                        onClick = { viewModel.login() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("ENTRAR", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}