package com.goprex.data.repository

import android.util.Base64
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.goprex.data.model.ImgBBResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ImgBBRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val gson = Gson()

    // OkHttpClient configurado com timeouts adequados
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)  // Timeout maior para upload
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    companion object {
        private const val TAG = "ImgBBRepository"
        private const val IMGBB_UPLOAD_URL = "https://api.imgbb.com/1/upload"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    /**
     * Busca a chave da API do ImgBB no Firestore
     * Com cache para evitar múltiplas chamadas
     */
    private var cachedApiKey: String? = null
    private var lastFetchTime: Long = 0
    private val cacheDuration = TimeUnit.MINUTES.toMillis(30) // Cache de 30 minutos

    private suspend fun getImgBBKey(): String {
        val now = System.currentTimeMillis()

        // Retorna cache se ainda válido
        if (cachedApiKey != null && (now - lastFetchTime) < cacheDuration) {
            return cachedApiKey!!
        }

        return try {
            val document = firestore.collection("config")
                .document("api_keys")
                .get()
                .await()

            val key = document.getString("imgbb_key") ?: ""

            if (key.isNotEmpty()) {
                cachedApiKey = key
                lastFetchTime = now
                Log.d(TAG, "✅ Chave ImgBB obtida do Firestore: ${key.take(10)}...")
            } else {
                Log.e(TAG, "❌ Chave ImgBB não encontrada no Firestore")
            }

            key
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao obter chave ImgBB do Firestore: ${e.message}")
            // Retorna cache mesmo expirado em caso de erro (fallback)
            cachedApiKey ?: ""
        }
    }

    /**
     * Força a atualização do cache da API Key
     */
    suspend fun refreshApiKey() {
        cachedApiKey = null
        lastFetchTime = 0
        getImgBBKey()
    }

    /**
     * Comprime a imagem se for muito grande
     * ImgBB tem limite de 32MB para Base64
     */
    private fun compressImageIfNeeded(imageBytes: ByteArray): ByteArray {
        val maxSizeBytes = 20 * 1024 * 1024 // 20MB para segurança

        if (imageBytes.size <= maxSizeBytes) {
            return imageBytes
        }

        Log.w(TAG, "⚠️ Imagem muito grande (${imageBytes.size / 1024}KB). Tentando reduzir...")

        // Para imagens muito grandes, poderíamos implementar compressão aqui
        // Por enquanto, retorna os bytes originais e deixa o ImgBB processar
        return imageBytes
    }

    /**
     * Faz upload de uma imagem para o ImgBB
     * @param imageBytes Bytes da imagem
     * @param imageName Nome opcional para a imagem
     * @param tentarCompressao Se deve tentar comprimir imagens grandes
     * @return Result com URL da imagem no ImgBB ou erro
     */
    suspend fun uploadImage(
        imageBytes: ByteArray,
        imageName: String = "goprex_image",
        tentarCompressao: Boolean = true
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = getImgBBKey()

                if (apiKey.isEmpty()) {
                    return@withContext Result.failure(
                        Exception("Chave da API ImgBB não configurada. Contate o suporte.")
                    )
                }

                // Comprime se necessário e habilitado
                val bytesParaUpload = if (tentarCompressao) {
                    compressImageIfNeeded(imageBytes)
                } else {
                    imageBytes
                }

                // Validar tamanho máximo (32MB em Base64 = ~24MB em bytes)
                if (bytesParaUpload.size > 24 * 1024 * 1024) {
                    return@withContext Result.failure(
                        Exception("Imagem muito grande. Máximo permitido: 24MB")
                    )
                }

                // Converter bytes para Base64
                val base64Image = Base64.encodeToString(bytesParaUpload, Base64.NO_WRAP)

                Log.d(TAG, "📤 Iniciando upload: $imageName (${bytesParaUpload.size / 1024}KB)")

                // Construir o corpo da requisição
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("key", apiKey)
                    .addFormDataPart("image", base64Image)
                    .addFormDataPart("name", sanitizeFileName(imageName))
                    .build()

                // Fazer a requisição
                val request = Request.Builder()
                    .url(IMGBB_UPLOAD_URL)
                    .post(requestBody)
                    .header("Accept", "application/json")
                    .build()

                val response = client.newCall(request).await()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    Log.e(TAG, "❌ Erro HTTP ${response.code}: ${responseBody.take(300)}")
                    return@withContext Result.failure(
                        Exception("Erro no servidor de imagens (${response.code}). Tente novamente.")
                    )
                }

                Log.d(TAG, "📥 Resposta recebida: ${responseBody.take(200)}...")

                // Parse da resposta
                val imgBBResponse = try {
                    gson.fromJson(responseBody, ImgBBResponse::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erro ao parsear resposta: ${e.message}")
                    return@withContext Result.failure(Exception("Resposta inválida do servidor"))
                }

                if (imgBBResponse.success && imgBBResponse.data?.url != null) {
                    val imageUrl = imgBBResponse.data.url
                    Log.d(TAG, "✅ Upload concluído com sucesso: $imageUrl")
                    Result.success(imageUrl)
                } else {
                    val errorMsg = imgBBResponse.data?.let {
                        "Erro desconhecido"
                    } ?: "Falha ao fazer upload"

                    Log.e(TAG, "❌ Falha no upload: $errorMsg - $responseBody")
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: java.net.SocketTimeoutException) {
                Log.e(TAG, "❌ Timeout no upload: ${e.message}")
                Result.failure(Exception("Tempo esgotado ao enviar imagem. Verifique sua conexão."))
            } catch (e: java.net.UnknownHostException) {
                Log.e(TAG, "❌ Sem conexão: ${e.message}")
                Result.failure(Exception("Sem conexão com a internet. Verifique sua rede."))
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro inesperado no upload: ${e.message}", e)
                Result.failure(Exception("Erro ao enviar imagem: ${e.message}"))
            }
        }
    }

    /**
     * Upload com sistema de retry automático
     * @param imageBytes Bytes da imagem
     * @param imageName Nome da imagem
     * @param maxRetries Número máximo de tentativas
     * @param onProgress Callback de progresso (tentativa atual, total)
     * @return Result com URL da imagem
     */
    suspend fun uploadImageWithRetry(
        imageBytes: ByteArray,
        imageName: String = "goprex_image",
        maxRetries: Int = MAX_RETRIES,
        onProgress: ((attempt: Int, total: Int) -> Unit)? = null
    ): Result<String> {
        var lastError: Throwable? = null

        for (attempt in 1..maxRetries) {
            Log.d(TAG, "🔄 Tentativa $attempt de $maxRetries para $imageName")

            onProgress?.invoke(attempt, maxRetries)

            val result = uploadImage(imageBytes, imageName)

            if (result.isSuccess) {
                return result
            }

            lastError = result.exceptionOrNull()

            if (attempt < maxRetries) {
                val delayMs = RETRY_DELAY_MS * attempt // Backoff exponencial
                Log.d(TAG, "⏳ Aguardando ${delayMs}ms antes da próxima tentativa...")
                delay(delayMs)
            }
        }

        return Result.failure(
            lastError ?: Exception("Falha após $maxRetries tentativas")
        )
    }

    /**
     * Faz upload de imagem a partir de um InputStream
     */
    suspend fun uploadImageFromStream(
        inputStream: InputStream,
        imageName: String = "goprex_image"
    ): Result<String> {
        return try {
            val bytes = inputStream.use { it.readBytes() }
            uploadImage(bytes, imageName)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao ler stream: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Faz upload de imagem a partir de uma URL
     */
    suspend fun uploadImageFromUrl(
        imageUrl: String,
        imageName: String = "goprex_image"
    ): Result<String> {
        return try {
            val url = java.net.URL(imageUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val inputStream = connection.getInputStream()
            val bytes = inputStream.use { it.readBytes() }

            uploadImage(bytes, imageName)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao baixar imagem da URL: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Faz upload de múltiplas imagens com progresso detalhado
     * @param imagesBytes Lista de bytes das imagens
     * @param baseName Nome base para as imagens
     * @param usarRetry Se deve usar retry em cada imagem
     * @param onProgress Callback de progresso (atual, total, nomeImagem)
     * @return Result com lista de URLs
     */
    suspend fun uploadMultipleImages(
        imagesBytes: List<ByteArray>,
        baseName: String = "produto",
        usarRetry: Boolean = true,
        onProgress: ((current: Int, total: Int, imageName: String) -> Unit)? = null
    ): Result<List<String>> {
        return try {
            val urls = mutableListOf<String>()
            val erros = mutableListOf<String>()

            imagesBytes.forEachIndexed { index, bytes ->
                val imageName = "${baseName}_${index + 1}"

                onProgress?.invoke(index + 1, imagesBytes.size, imageName)

                val result = if (usarRetry) {
                    uploadImageWithRetry(bytes, imageName, maxRetries = 2)
                } else {
                    uploadImage(bytes, imageName)
                }

                result.fold(
                    onSuccess = { url ->
                        urls.add(url)
                        Log.d(TAG, "✅ Imagem ${index + 1}/${imagesBytes.size} enviada: $url")
                    },
                    onFailure = { e ->
                        val errorMsg = "Imagem ${index + 1} falhou: ${e.message}"
                        erros.add(errorMsg)
                        Log.e(TAG, "❌ $errorMsg")
                    }
                )
            }

            if (urls.isNotEmpty()) {
                if (erros.isNotEmpty()) {
                    Log.w(TAG, "⚠️ Upload parcial: ${urls.size} sucesso, ${erros.size} falhas")
                }
                Result.success(urls)
            } else {
                Result.failure(
                    Exception("Nenhuma imagem foi enviada. ${erros.joinToString("; ")}")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no upload múltiplo: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Sanitiza o nome do arquivo para evitar caracteres inválidos
     */
    private fun sanitizeFileName(fileName: String): String {
        return fileName
            .replace(" ", "_")
            .replace("[^a-zA-Z0-9_.-]".toRegex(), "")
            .take(100) // Limita tamanho do nome
            .ifEmpty { "goprex_image" }
    }

    /**
     * Verifica se a API Key está configurada e é válida
     */
    suspend fun isApiKeyValid(): Boolean {
        return try {
            val key = getImgBBKey()
            key.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Limpa o cache da API Key
     */
    fun clearCache() {
        cachedApiKey = null
        lastFetchTime = 0
        Log.d(TAG, "Cache da API Key limpo")
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
                // Ignora erros de cancelamento
            }
        }
    }
}