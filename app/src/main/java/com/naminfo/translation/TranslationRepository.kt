package com.naminfo.translation

import com.naminfo.DiyaOneApplication.Companion.corePreferences
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.linphone.core.tools.Log
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors as JavaExecutors

object TranslationRepository {
    private const val TAG = "[Translation Repository]"
    private const val API_URL = "http://203.95.216.68:8020/translate"
    private const val API_KEY = "qhyvM1rr4RE2vqnvAAdfoDDC9Wlbylv-XXiUYF0HaZE"

    private val inFlightRequests = ConcurrentHashMap.newKeySet<String>()
    private val memoryCache = ConcurrentHashMap<String, String>()
    private val translationDispatcher = JavaExecutors.newFixedThreadPool(4).asCoroutineDispatcher()

    fun getCacheKey(messageId: String, targetLanguage: ChatTranslationLanguage): String {
        return "${messageId}_${targetLanguage.name}"
    }

    fun getCachedTranslation(messageId: String, targetLanguage: ChatTranslationLanguage): String? {
        if (targetLanguage == ChatTranslationLanguage.NONE) return null
        val cacheKey = getCacheKey(messageId, targetLanguage)
        memoryCache[cacheKey]?.let { return it }
        val persisted = corePreferences.getPersistedTranslation(cacheKey)
        if (persisted != null) {
            memoryCache[cacheKey] = persisted
            return persisted
        }
        return null
    }

    suspend fun translateMessage(
        messageId: String,
        toNumber: String,
        originalText: String,
        targetLanguage: ChatTranslationLanguage
    ): Result<String> = withContext(translationDispatcher) {
        if (targetLanguage == ChatTranslationLanguage.NONE || originalText.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Invalid target language or empty text"))
        }

        val cached = getCachedTranslation(messageId, targetLanguage)
        if (cached != null) {
            Log.i("$TAG Returning cached translation for messageId [$messageId], lang [${targetLanguage.name}]")
            return@withContext Result.success(cached)
        }

        val cacheKey = getCacheKey(messageId, targetLanguage)
        if (!inFlightRequests.add(cacheKey)) {
            Log.w("$TAG Translation already in flight for messageId [$messageId], lang [${targetLanguage.name}]")
            return@withContext Result.failure(IllegalStateException("Translation already in progress"))
        }

        try {
            Log.i("$TAG Calling Translation API for messageId [$messageId], targetLang [${targetLanguage.apiValue}]")
            val requestJson = JSONObject().apply {
                put("to_number", toNumber)
                put("language", targetLanguage.apiValue)
                put("message", originalText)
            }

            val url = URL(API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("x-api-key", API_KEY)
                setRequestProperty("accept", "*/*")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connectTimeout = 10000
                readTimeout = 10000
                doOutput = true
            }

            connection.outputStream.use { os ->
                val input = requestJson.toString().toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val responseJson = JSONObject(responseText)
                val translatedMessage = if (responseJson.has("translated_message") && !responseJson.isNull("translated_message")) {
                    responseJson.getString("translated_message")
                } else {
                    null
                }

                if (!translatedMessage.isNullOrEmpty()) {
                    Log.i("$TAG Translation successful for messageId [$messageId]")
                    memoryCache[cacheKey] = translatedMessage
                    corePreferences.setPersistedTranslation(cacheKey, translatedMessage)
                    return@withContext Result.success(translatedMessage)
                } else {
                    Log.e("$TAG 'translated_message' field missing in API response: $responseText")
                    return@withContext Result.failure(IllegalStateException("Empty translated_message in response"))
                }
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                Log.e("$TAG API HTTP Error [$responseCode]: $errorStream")
                return@withContext Result.failure(IllegalStateException("HTTP $responseCode: $errorStream"))
            }
        } catch (e: Exception) {
            Log.e("$TAG Exception during translation: $e")
            return@withContext Result.failure(e)
        } finally {
            inFlightRequests.remove(cacheKey)
        }
    }
}
