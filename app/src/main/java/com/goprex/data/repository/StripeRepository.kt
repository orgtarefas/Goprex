package com.goprex.data.repository

import com.goprex.data.model.CreateCheckoutSessionRequest
import com.goprex.data.model.CreateCheckoutSessionResponse
import com.goprex.data.model.CreateCardPaymentRequest
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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

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
}

class StripeRepository {
    private val api = Retrofit.Builder()
        .baseUrl(GOPREX_BACKEND_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(StripeApiService::class.java)

    suspend fun criarCheckout(request: CreateCheckoutSessionRequest): Result<CreateCheckoutSessionResponse> {
        return try {
            Result.success(api.createCheckoutSession(request))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun criarPix(request: CreatePixPaymentRequest): Result<CreatePixPaymentResponse> {
        return try {
            Result.success(api.createPixPayment(request))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun criarSessaoCadastroCartao(request: StripeClienteRequest): Result<CreateCardSetupSessionResponse> {
        return try {
            Result.success(api.createCardSetupSession(request))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun criarSetupIntentCartao(request: StripeClienteRequest): Result<CreateCardSetupIntentResponse> {
        return try {
            Result.success(api.createCardSetupIntent(request))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listarCartoes(request: StripeClienteRequest): Result<ListCardsResponse> {
        return try {
            Result.success(api.listCards(request))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun atualizarApelidoCartao(request: UpdateCardAliasRequest): Result<OkResponse> {
        return try {
            Result.success(api.updateCardAlias(request))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removerCartao(request: DeleteCardRequest): Result<OkResponse> {
        return try {
            Result.success(api.deleteCard(request))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pagarComCartao(request: CreateCardPaymentRequest): Result<CardPaymentResponse> {
        return try {
            Result.success(api.createCardPayment(request))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
