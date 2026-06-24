package com.goprex.data.repository

import com.goprex.data.model.CreateCheckoutSessionRequest
import com.goprex.data.model.CreateCheckoutSessionResponse
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
}
