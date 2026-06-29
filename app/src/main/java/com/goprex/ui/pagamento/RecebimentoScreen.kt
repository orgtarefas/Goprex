package com.goprex.ui.pagamento

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goprex.data.model.AdminContaRecebimentoRequest
import com.goprex.data.model.ContaRecebimentoPayload
import com.goprex.data.model.Login
import com.goprex.data.repository.AuthRepository
import com.goprex.data.repository.StripeRepository
import com.goprex.ui.theme.GoPrexDark
import com.goprex.ui.theme.GoPrexOrange
import com.goprex.ui.theme.SuccessGreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

data class RecebimentoUiState(
    val tipoChavePix: String = "",
    val chavePix: String = "",
    val banco: String = "",
    val agencia: String = "",
    val conta: String = "",
    val titular: String = "",
    val documentoTitular: String = "",
    val senhaAlteracaoAdmin: String = "",
    val senhaAdminAutorizada: String = "",
    val carregado: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null
)

data class BancoOpcao(
    val codigo: String,
    val nome: String
) {
    val label: String get() = "$codigo - $nome"
}

private val bancosRecebimento = listOf(
    BancoOpcao("001", "Banco do Brasil"),
    BancoOpcao("033", "Santander"),
    BancoOpcao("041", "Banrisul"),
    BancoOpcao("070", "BRB"),
    BancoOpcao("077", "Banco Inter"),
    BancoOpcao("104", "Caixa Economica Federal"),
    BancoOpcao("208", "Banco BTG Pactual"),
    BancoOpcao("212", "Banco Original"),
    BancoOpcao("237", "Bradesco"),
    BancoOpcao("260", "Nu Pagamentos"),
    BancoOpcao("290", "PagBank"),
    BancoOpcao("318", "Banco BMG"),
    BancoOpcao("323", "Mercado Pago"),
    BancoOpcao("336", "Banco C6"),
    BancoOpcao("341", "Itau"),
    BancoOpcao("380", "PicPay"),
    BancoOpcao("422", "Banco Safra"),
    BancoOpcao("655", "Banco Votorantim"),
    BancoOpcao("756", "Sicoob")
)

class RecebimentoViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    private val stripeRepository = StripeRepository()
    private val _uiState = MutableStateFlow(RecebimentoUiState())
    val uiState: StateFlow<RecebimentoUiState> = _uiState.asStateFlow()

    fun carregar(loginData: Login) {
        aplicarConta(loginData)
        viewModelScope.launch {
            authRepository.getLoginDataByUid(loginData.documentoId).fold(
                onSuccess = { loginAtualizado -> aplicarConta(loginAtualizado) },
                onFailure = { }
            )
        }
    }

    private fun aplicarConta(loginData: Login) {
        val conta = loginData.getMap("contaRecebimento").orEmpty()
        if (conta.isEmpty() && _uiState.value.carregado) return
        _uiState.value = _uiState.value.copy(
            tipoChavePix = conta["tipoChavePix"]?.toString().orEmpty(),
            chavePix = conta["chavePix"]?.toString().orEmpty(),
            banco = conta["banco"]?.toString().orEmpty(),
            agencia = conta["agencia"]?.toString().orEmpty(),
            conta = conta["conta"]?.toString().orEmpty(),
            titular = conta["titular"]?.toString().orEmpty(),
            documentoTitular = conta["documentoTitular"]?.toString().orEmpty(),
            carregado = true
        )
    }

    fun updateTipoChavePix(value: String) = update { it.copy(tipoChavePix = value, error = null, success = null) }
    fun updateChavePix(value: String) = update { it.copy(chavePix = value, error = null, success = null) }
    fun updateBanco(value: String) = update { it.copy(banco = value, error = null, success = null) }
    fun updateAgencia(value: String) = update { it.copy(agencia = value, error = null, success = null) }
    fun updateConta(value: String) = update { it.copy(conta = value, error = null, success = null) }
    fun updateTitular(value: String) = update { it.copy(titular = value, error = null, success = null) }
    fun updateDocumentoTitular(value: String) = update { it.copy(documentoTitular = value, error = null, success = null) }
    fun updateSenhaAlteracaoAdmin(value: String) = update { it.copy(senhaAlteracaoAdmin = value, error = null, success = null) }
    fun limparSenhaAdminAutorizada() = update {
        it.copy(
            senhaAdminAutorizada = "",
            senhaAlteracaoAdmin = "",
            error = null,
            success = null
        )
    }

    fun verificarSenhaAdmin(loginData: Login) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, success = null)
            stripeRepository.verificarSenhaContaAdmin(
                com.goprex.data.model.VerificarSenhaContaAdminRequest(
                    adminLogin = loginData.documentoId,
                    senhaAlteracao = _uiState.value.senhaAlteracaoAdmin
                )
            ).fold(
                onSuccess = { resposta ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        senhaAdminAutorizada = _uiState.value.senhaAlteracaoAdmin,
                        senhaAlteracaoAdmin = "",
                        success = "Senha validada"
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Senha invalida"
                    )
                }
            )
        }
    }

    fun salvar(loginData: Login) {
        val state = _uiState.value
        val isAdmin = loginData.getString("perfil").contains("admin", ignoreCase = true) ||
                loginData.getString("descricaoPerfil").contains("admin", ignoreCase = true)
        if (state.titular.isBlank()) {
            _uiState.value = state.copy(error = "Informe o nome do titular")
            return
        }
        if (state.chavePix.isBlank() && (state.banco.isBlank() || state.agencia.isBlank() || state.conta.isBlank())) {
            _uiState.value = state.copy(error = "Informe uma chave PIX ou os dados bancarios")
            return
        }
        if (isAdmin && state.senhaAdminAutorizada.isBlank()) {
            _uiState.value = state.copy(error = "Informe a senha de alteracao da conta Admin")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null, success = null)
            val payload = ContaRecebimentoPayload(
                tipoChavePix = state.tipoChavePix.trim(),
                chavePix = state.chavePix.trim(),
                banco = state.banco.trim(),
                agencia = state.agencia.trim(),
                conta = state.conta.trim(),
                titular = state.titular.trim(),
                documentoTitular = state.documentoTitular.trim()
            )
            val result = if (isAdmin) {
                stripeRepository.salvarContaAdmin(
                    AdminContaRecebimentoRequest(
                        adminLogin = loginData.documentoId,
                        senhaAlteracao = state.senhaAdminAutorizada,
                        contaRecebimento = payload
                    )
                ).map { Unit }
            } else {
                val dados = mapOf(
                    "tipoChavePix" to payload.tipoChavePix,
                    "chavePix" to payload.chavePix,
                    "banco" to payload.banco,
                    "agencia" to payload.agencia,
                    "conta" to payload.conta,
                    "titular" to payload.titular,
                    "documentoTitular" to payload.documentoTitular,
                    "atualizadoEm" to System.currentTimeMillis()
                )
                authRepository.atualizarCampo(loginData.documentoId, "contaRecebimento", dados)
            }

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        senhaAlteracaoAdmin = "",
                        senhaAdminAutorizada = if (isAdmin) state.senhaAdminAutorizada else "",
                        tipoChavePix = payload.tipoChavePix,
                        chavePix = payload.chavePix,
                        banco = payload.banco,
                        agencia = payload.agencia,
                        conta = payload.conta,
                        titular = payload.titular,
                        documentoTitular = payload.documentoTitular,
                        carregado = true,
                        success = "Conta de recebimento salva"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Erro ao salvar conta"
                    )
                }
            )
        }
    }

    private fun update(block: (RecebimentoUiState) -> RecebimentoUiState) {
        _uiState.value = block(_uiState.value)
    }
}

@Composable
fun RecebimentoScreen(
    loginData: Login,
    embedded: Boolean = false,
    viewModel: RecebimentoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAdmin = loginData.getString("perfil").contains("admin", ignoreCase = true) ||
            loginData.getString("descricaoPerfil").contains("admin", ignoreCase = true)
    val adminLiberado = !isAdmin || uiState.senhaAdminAutorizada.isNotBlank()
    val contaCadastrada = uiState.titular.isNotBlank() &&
            (uiState.chavePix.isNotBlank() || uiState.banco.isNotBlank() || uiState.conta.isNotBlank())
    var editandoConta by remember { mutableStateOf(false) }
    var solicitandoSenhaAdmin by remember { mutableStateOf(false) }
    var mostrarSenhaAdmin by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(loginData.documentoId) {
        viewModel.carregar(loginData)
    }

    androidx.compose.runtime.LaunchedEffect(uiState.success) {
        if (uiState.success != null && uiState.success != "Senha validada") {
            editandoConta = false
            if (isAdmin) {
                viewModel.limparSenhaAdminAutorizada()
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(uiState.senhaAdminAutorizada) {
        if (isAdmin && uiState.senhaAdminAutorizada.isNotBlank() && solicitandoSenhaAdmin) {
            solicitandoSenhaAdmin = false
            editandoConta = true
        }
    }

    val screenModifier = if (embedded) {
        Modifier.fillMaxWidth()
    } else {
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F7F9))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    }

    Column(
        modifier = screenModifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Conta para recebimento", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GoPrexDark)
        Text(
            if (isAdmin) {
                "Cadastre a conta que recebera a tarifa de transacao."
            } else {
                "Cadastre os dados que serao usados para receber os repasses das vendas pagas com cartao."
            },
            fontSize = 13.sp,
            color = Color.Gray
        )

        if (contaCadastrada) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                        Text("Conta cadastrada", fontWeight = FontWeight.Bold, color = SuccessGreen)
                    }
                    Text("Titular: ${if (adminLiberado) uiState.titular else "********"}", fontSize = 13.sp, color = GoPrexDark)
                    if (uiState.banco.isNotBlank()) Text("Banco: ${if (adminLiberado) uiState.banco else "********"}", fontSize = 13.sp, color = GoPrexDark)
                    if (uiState.agencia.isNotBlank() || uiState.conta.isNotBlank()) {
                        Text(
                            "Agencia/Conta: ${if (adminLiberado) "${uiState.agencia} / ${uiState.conta}" else "******** / ********"}",
                            fontSize = 13.sp,
                            color = GoPrexDark
                        )
                    }
                    if (uiState.chavePix.isNotBlank()) Text("PIX: ${if (adminLiberado) uiState.chavePix else "********"}", fontSize = 13.sp, color = GoPrexDark)
                }
            }

            Button(
                onClick = {
                    if (isAdmin && !adminLiberado) {
                        solicitandoSenhaAdmin = true
                    } else {
                        editandoConta = true
                    }
                },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.Edit, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text(if (isAdmin && !adminLiberado) "Visualizar/Editar" else "Editar conta de recebimento", fontWeight = FontWeight.Bold)
            }
        } else if (isAdmin && !adminLiberado) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                        Text("Conta protegida", fontWeight = FontWeight.Bold, color = SuccessGreen)
                    }
                    Text("Titular: ********", fontSize = 13.sp, color = GoPrexDark)
                    Text("Banco: ********", fontSize = 13.sp, color = GoPrexDark)
                    Text("Agencia/Conta: ******** / ********", fontSize = 13.sp, color = GoPrexDark)
                    Text("PIX: ********", fontSize = 13.sp, color = GoPrexDark)
                }
            }

            Button(
                onClick = { solicitandoSenhaAdmin = true },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.Edit, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("Visualizar/Editar", fontWeight = FontWeight.Bold)
            }
        }

        if (!isAdmin && !contaCadastrada) {
            RecebimentoForm(
                uiState = uiState,
                isAdmin = isAdmin,
                viewModel = viewModel,
                loginData = loginData,
                submitLabel = "Salvar conta de recebimento"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    if (editandoConta) {
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isLoading) {
                    editandoConta = false
                    if (isAdmin) viewModel.limparSenhaAdminAutorizada()
                }
            },
            title = { Text("Editar conta", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RecebimentoForm(
                        uiState = uiState,
                        isAdmin = isAdmin,
                        viewModel = viewModel,
                        loginData = loginData,
                        submitLabel = "Salvar alteracoes"
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        editandoConta = false
                        if (isAdmin) viewModel.limparSenhaAdminAutorizada()
                    },
                    enabled = !uiState.isLoading
                ) {
                    Text("Fechar")
                }
            }
        )
    }

    if (solicitandoSenhaAdmin) {
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isLoading) {
                    solicitandoSenhaAdmin = false
                    viewModel.updateSenhaAlteracaoAdmin("")
                }
            },
            title = { Text("Senha de acesso", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Informe a senha para visualizar ou editar a conta do Admin.", fontSize = 13.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = uiState.senhaAlteracaoAdmin,
                        onValueChange = viewModel::updateSenhaAlteracaoAdmin,
                        label = { Text("Senha") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { mostrarSenhaAdmin = !mostrarSenhaAdmin }) {
                                Icon(
                                    if (mostrarSenhaAdmin) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (mostrarSenhaAdmin) "Ocultar senha" else "Mostrar senha"
                                )
                            }
                        },
                        visualTransformation = if (mostrarSenhaAdmin) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors()
                    )
                    uiState.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                    uiState.success?.let {
                        Text(it, color = SuccessGreen, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.verificarSenhaAdmin(loginData)
                    },
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Confirmar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        solicitandoSenhaAdmin = false
                        viewModel.updateSenhaAlteracaoAdmin("")
                    },
                    enabled = !uiState.isLoading
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun RecebimentoForm(
    uiState: RecebimentoUiState,
    isAdmin: Boolean,
    viewModel: RecebimentoViewModel,
    loginData: Login,
    submitLabel: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(10.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Payments, null, tint = GoPrexOrange, modifier = Modifier.size(26.dp))
                    Text("Dados de recebimento", fontWeight = FontWeight.Bold, color = GoPrexDark)
                }

                OutlinedTextField(
                    value = uiState.titular,
                    onValueChange = viewModel::updateTitular,
                    label = { Text("Nome do titular *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors()
                )
                OutlinedTextField(
                    value = uiState.documentoTitular,
                    onValueChange = viewModel::updateDocumentoTitular,
                    label = { Text("CPF/CNPJ do titular") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors()
                )
                OutlinedTextField(
                    value = uiState.tipoChavePix,
                    onValueChange = viewModel::updateTipoChavePix,
                    label = { Text("Tipo da chave PIX") },
                    placeholder = { Text("CPF, CNPJ, telefone, e-mail ou aleatoria") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors()
                )
                OutlinedTextField(
                    value = uiState.chavePix,
                    onValueChange = viewModel::updateChavePix,
                    label = { Text("Chave PIX") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors()
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(10.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.AccountBalance, null, tint = GoPrexOrange, modifier = Modifier.size(26.dp))
                    Text("Dados bancarios", fontWeight = FontWeight.Bold, color = GoPrexDark)
                }
                BancoRecebimentoField(
                    bancoAtual = uiState.banco,
                    onBancoChange = viewModel::updateBanco,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.agencia,
                        onValueChange = viewModel::updateAgencia,
                        label = { Text("Agencia") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = fieldColors()
                    )
                    OutlinedTextField(
                        value = uiState.conta,
                        onValueChange = viewModel::updateConta,
                        label = { Text("Conta") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = fieldColors()
                    )
                }
            }
        }

        uiState.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp)).padding(10.dp)
            )
        }

        uiState.success?.let {
            Row(
                modifier = Modifier.fillMaxWidth().background(SuccessGreen.copy(alpha = 0.08f), RoundedCornerShape(8.dp)).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                Text(it, color = SuccessGreen, fontSize = 13.sp)
            }
        }

        Button(
            onClick = { viewModel.salvar(loginData) },
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(submitLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GoPrexOrange,
    unfocusedBorderColor = GoPrexDark.copy(alpha = 0.3f),
    focusedLabelColor = GoPrexOrange,
    cursorColor = GoPrexOrange
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BancoRecebimentoField(
    bancoAtual: String,
    onBancoChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpandido by remember { mutableStateOf(false) }
    val selecionado = bancosRecebimento.firstOrNull { bancoAtual == it.label || bancoAtual == it.codigo }
    val textoBanco = selecionado?.label ?: bancoAtual

    ExposedDropdownMenuBox(
        expanded = menuExpandido,
        onExpandedChange = { menuExpandido = !menuExpandido },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = textoBanco,
            onValueChange = {},
            readOnly = true,
            label = { Text("Banco") },
            placeholder = { Text("Selecione o banco") },
            leadingIcon = { Icon(Icons.Filled.AccountBalance, null, tint = GoPrexOrange) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpandido) },
            singleLine = true,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            colors = fieldColors()
        )

        ExposedDropdownMenu(
            expanded = menuExpandido,
            onDismissRequest = { menuExpandido = false }
        ) {
            bancosRecebimento.forEach { banco ->
                DropdownMenuItem(
                    text = { Text(banco.label) },
                    onClick = {
                        onBancoChange(banco.label)
                        menuExpandido = false
                    }
                )
            }
        }
    }
}
