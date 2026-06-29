package com.goprex.data.repository

import com.goprex.data.model.CreateCheckoutSessionRequest
import com.goprex.data.model.CreateCheckoutSessionResponse
import com.goprex.data.model.AdminContaRecebimentoRequest
import com.goprex.data.model.CreateCardPaymentRequest
import com.goprex.data.model.CreateCardPaymentIntentRequest
import com.goprex.data.model.CreateCardPaymentIntentResponse
import com.goprex.data.model.CreateCardSetupIntentResponse
import com.goprex.data.model.CreateCardSetupSessionResponse
import com.goprex.data.model.CreatePixPaymentRequest
import com.goprex.data.model.CreatePixPaymentResponse
import com.goprex.data.model.CardPaymentResponse
import com.goprex.data.model.DeleteCardRequest
import com.goprex.data.model.ListCardsResponse
import com.goprex.data.model.OkResponse
import com.goprex.data.model.StripeClienteRequest
import com.goprex.data.model.UpdateCardAliasRequest
import com.goprex.data.model.AdminContaDiagnosticoResponse
import com.goprex.data.model.VerificarSenhaContaAdminRequest
import com.goprex.data.model.VerificarSenhaContaAdminResponse
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.HttpException

private const val GOPREX_BACKEND_URL = "https://goprex-backend-1.onrender.com/"

interface StripeApiService {
    @POST("stripe/create-checkout-session")
    suspend fun createCheckoutSession(
        @Body request: CreateCheckoutSessionRequest
    ): CreateCheckoutSessionResponse

    @POST("stripe/create-pix-payment")
    suspend fun createPixPayment(
        @Body request: CreatePixPaymentRequest
    ): CreatePixPaymentResponse

    @POST("stripe/create-card-setup-session")
    suspend fun createCardSetupSession(
        @Body request: StripeClienteRequest
    ): CreateCardSetupSessionResponse

    @POST("stripe/create-card-setup-intent")
    suspend fun createCardSetupIntent(
        @Body request: StripeClienteRequest
    ): CreateCardSetupIntentResponse

    @POST("stripe/list-cards")
    suspend fun listCards(
        @Body request: StripeClienteRequest
    ): ListCardsResponse

    @POST("stripe/update-card-alias")
    suspend fun updateCardAlias(
        @Body request: UpdateCardAliasRequest
    ): OkResponse

    @POST("stripe/delete-card")
    suspend fun deleteCard(
        @Body request: DeleteCardRequest
    ): OkResponse

    @POST("stripe/create-card-payment")
    suspend fun createCardPayment(
        @Body request: CreateCardPaymentRequest
    ): CardPaymentResponse

    @POST("stripe/create-card-payment-intent")
    suspend fun createCardPaymentIntent(
        @Body request: CreateCardPaymentIntentRequest
    ): CreateCardPaymentIntentResponse

    @POST("admin/conta-recebimento")
    suspend fun salvarContaAdmin(
        @Body request: AdminContaRecebimentoRequest
    ): OkResponse

    @POST("admin/verificar-senha-conta")
    suspend fun verificarSenhaContaAdmin(
        @Body request: VerificarSenhaContaAdminRequest
    ): VerificarSenhaContaAdminResponse

    @GET("health")
    suspend fun diagnosticoContaAdmin(): AdminContaDiagnosticoResponse
}

class StripeRepository {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val api = Retrofit.Builder()
        .baseUrl(GOPREX_BACKEND_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(StripeApiService::class.java)

    suspend fun criarCheckout(request: CreateCheckoutSessionRequest): Result<CreateCheckoutSessionResponse> {
        return try {
            Result.success(api.createCheckoutSession(request))
        } catch (e: Exception) {
            Result.failure(e.toApiException("Erro ao iniciar pagamento"))
        }
    }

    suspend fun criarPix(request: CreatePixPaymentRequest): Result<CreatePixPaymentResponse> {
        return try {
            Result.success(api.createPixPayment(request))
        } catch (e: Exception) {
            Result.failure(e.toApiException("Erro ao gerar Pix"))
        }
    }

    suspend fun criarSessaoCadastroCartao(request: StripeClienteRequest): Result<CreateCardSetupSessionResponse> {
        return try {
            Result.success(api.createCardSetupSession(request))
        } catch (e: Exception) {
            Result.failure(e.toApiException("Erro ao cadastrar cartao"))
        }
    }

    suspend fun criarSetupIntentCartao(request: StripeClienteRequest): Result<CreateCardSetupIntentResponse> {
        return try {
            Result.success(api.createCardSetupIntent(request))
        } catch (e: Exception) {
            Result.failure(e.toApiException("Erro ao preparar cadastro do cartao"))
        }
    }

    suspend fun listarCartoes(request: StripeClienteRequest): Result<ListCardsResponse> {
        return try {
            Result.success(api.listCards(request))
        } catch (e: Exception) {
            Result.failure(e.toApiException("Erro ao listar cartoes"))
        }
    }

    suspend fun atualizarApelidoCartao(request: UpdateCardAliasRequest): Result<OkResponse> {
        return try {
            Result.success(api.updateCardAlias(request))
        } catch (e: Exception) {
            Result.failure(e.toApiException("Erro ao atualizar cartao"))
        }
    }

    suspend fun removerCartao(request: DeleteCardRequest): Result<OkResponse> {
        return try {
            Result.success(api.deleteCard(request))
        } catch (e: Exception) {
            Result.failure(e.toApiException("Erro ao remover cartao"))
        }
    }

    suspend fun pagarComCartao(request: CreateCardPaymentRequest): Result<CardPaymentResponse> {
        return try {
            Result.success(api.createCardPayment(request))
        } catch (e: Exception) {
            Result.failure(e.toApiException("Erro ao cobrar cartao"))
        }
    }

    suspend fun criarPaymentIntentCartao(request: CreateCardPaymentIntentRequest): Result<CreateCardPaymentIntentResponse> {
        return try {
            Result.success(api.createCardPaymentIntent(request))
        } catch (e: Exception) {
            Result.failure(e.toApiException("Erro ao iniciar pagamento no app"))
        }
    }

    suspend fun salvarContaAdmin(request: AdminContaRecebimentoRequest): Result<OkResponse> {
        return try {
            Result.success(api.salvarContaAdmin(request))
        } catch (e: Exception) {
            Result.failure(e.toApiException("Erro ao salvar conta do administrador"))
        }
    }

    suspend fun verificarSenhaContaAdmin(request: VerificarSenhaContaAdminRequest): Result<VerificarSenhaContaAdminResponse> {
        return try {
            Result.success(api.verificarSenhaContaAdmin(request))
        } catch (e: Exception) {
            Result.failure(e.toApiException("Senha do administrador invalida"))
        }
    }

    suspend fun diagnosticoContaAdmin(): Result<AdminContaDiagnosticoResponse> {
        return try {
            Result.success(api.diagnosticoContaAdmin())
        } catch (e: Exception) {
            Result.failure(e.toApiException("Nao foi possivel diagnosticar a conta do administrador"))
        }
    }

    private fun Exception.toApiException(fallback: String): Exception {
        val httpException = this as? HttpException ?: return Exception(message ?: fallback)
        val body = runCatching { httpException.response()?.errorBody()?.string() }.getOrNull().orEmpty()
        val errorMessage = Regex("\"error\"\\s*:\\s*\"([^\"]+)\"")
            .find(body)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace("\\\"", "\"")
        return Exception(errorMessage ?: "$fallback (HTTP ${httpException.code()})")
    }
}
