package com.goprex.data.repository

import android.util.Base64
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.goprex.data.model.ImgBBResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URL
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ImgBBRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val client = OkHttpClient()
    private val gson = Gson()

    /**
     * Busca a chave da API do ImgBB no Firestore
     */
    private suspend fun getImgBBKey(): String {
        return try {
            val document = firestore.collection("config")
                .document("api_keys")
                .get()
                .await()

            val key = document.getString("imgbb_key") ?: ""
            Log.d("ImgBBRepository", "Chave ImgBB obtida: ${key.take(10)}...")
            key
        } catch (e: Exception) {
            Log.e("ImgBBRepository", "Erro ao obter chave ImgBB: ${e.message}")
            ""
        }
    }

    /**
     * Faz upload de uma imagem para o ImgBB
     * @param imageBytes Bytes da imagem
     * @param imageName Nome opcional para a imagem
     * @return URL da imagem no ImgBB
     */
    suspend fun uploadImage(imageBytes: ByteArray, imageName: String = "goprex_image"): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = getImgBBKey()

                if (apiKey.isEmpty()) {
                    return@withContext Result.failure(Exception("Chave da API ImgBB não encontrada"))
                }

                // Converter bytes para Base64
                val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

                // Construir o corpo da requisição
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("key", apiKey)
                    .addFormDataPart("image", base64Image)
                    .addFormDataPart("name", imageName)
                    .build()

                // Fazer a requisição
                val request = Request.Builder()
                    .url("https://api.imgbb.com/1/upload")
                    .post(requestBody)
                    .build()

                Log.d("ImgBBRepository", "Iniciando upload para ImgBB...")
                val response = client.newCall(request).await()
                val responseBody = response.body?.string() ?: ""

                Log.d("ImgBBRepository", "Resposta ImgBB: ${responseBody.take(200)}...")

                // Parse da resposta
                val imgBBResponse = gson.fromJson(responseBody, ImgBBResponse::class.java)

                if (imgBBResponse.success && imgBBResponse.data?.url != null) {
                    val imageUrl = imgBBResponse.data.url
                    Log.d("ImgBBRepository", "✅ Upload concluído: $imageUrl")
                    Result.success(imageUrl)
                } else {
                    Log.e("ImgBBRepository", "❌ Falha no upload: $responseBody")
                    Result.failure(Exception("Falha ao fazer upload da imagem"))
                }
            } catch (e: Exception) {
                Log.e("ImgBBRepository", "❌ Erro no upload: ${e.message}")
                Result.failure(e)
            }
        }
    }

    /**
     * Faz upload de imagem a partir de uma URL (InputStream)
     */
    suspend fun uploadImageFromUri(inputStream: InputStream, imageName: String = "goprex_image"): Result<String> {
        return try {
            val bytes = inputStream.readBytes()
            inputStream.close()
            uploadImage(bytes, imageName)
        } catch (e: Exception) {
            Log.e("ImgBBRepository", "Erro ao ler imagem: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Faz upload de múltiplas imagens
     */
    suspend fun uploadMultipleImages(
        imagesBytes: List<ByteArray>,
        baseName: String = "produto"
    ): Result<List<String>> {
        return try {
            val urls = mutableListOf<String>()

            imagesBytes.forEachIndexed { index, bytes ->
                val imageName = "${baseName}_${index + 1}"
                val result = uploadImage(bytes, imageName)

                result.fold(
                    onSuccess = { url ->
                        urls.add(url)
                        Log.d("ImgBBRepository", "Imagem ${index + 1}/${imagesBytes.size} enviada")
                    },
                    onFailure = { e ->
                        Log.e("ImgBBRepository", "Falha na imagem ${index + 1}: ${e.message}")
                    }
                )
            }

            if (urls.isNotEmpty()) {
                Result.success(urls)
            } else {
                Result.failure(Exception("Nenhuma imagem foi enviada com sucesso"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Extensão para aguardar chamadas OkHttp em corrotinas
private suspend fun Call.await(): Response {
    return suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }

            override fun onFailure(call: Call, e: java.io.IOException) {
                if (continuation.isCancelled) return
                continuation.resumeWithException(e)
            }
        })

        continuation.invokeOnCancellation {
            try {
                cancel()
            } catch (ex: Exception) {
                // Ignorar
            }
        }
    }
}