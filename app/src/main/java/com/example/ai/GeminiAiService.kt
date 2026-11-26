package com.example.ai

import com.example.data.local.AiUsageLogDao
import com.example.domain.model.AiCoachResponse
import com.example.domain.model.AiUsageLog
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

class RateLimitExceededException(message: String) : Exception(message)

class GeminiAiService(
    private val aiUsageLogDao: AiUsageLogDao
) {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun generateCoachFeedback(
        spendingSummary: String,
        isPremium: Boolean
    ): Result<AiCoachResponse> = withContext(Dispatchers.IO) {
        try {
            // 1. Rate Limiting Check
            val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
            val logsInLastHour = aiUsageLogDao.getLogsAfterTimestamp(oneHourAgo)
            val limit = if (isPremium) 30 else 5
            
            if (logsInLastHour.size >= limit) {
                return@withContext Result.failure(
                    RateLimitExceededException(
                        "Hai esaurito i tuoi crediti IA per questa ora. Riprova più tardi o passa a Premium."
                    )
                )
            }

            val prompt = """
                Analizza queste spese mensili sintetiche e restituisci JSON con: summary, topRiskCategory, savingTips[3], budgetForecast, motivationMessage.
                
                Dati Spesa:
                $spendingSummary
                
                Rispondi RIGIDAMENTE in formato JSON con la seguente struttura:
                {
                  "summary": "Riassunto analitico in italiano",
                  "topRiskCategory": "Categoria a maggior rischio",
                  "savingTips": ["Tip 1", "Tip 2", "Tip 3"],
                  "budgetForecast": "Previsione di budget in italiano",
                  "motivationMessage": "Breve incoraggiamento"
                }
            """.trimIndent()

            // 2. Token Estimation (Input)
            val inputTokens = prompt.length / 4

            // 3. Check for API key presence
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "YOUR_API_KEY_HERE") {
                return@withContext Result.failure(
                    IllegalStateException("AI Coach non configurato: nessuna analisi è stata generata.")
                )
            }
            val request = GeminiRequest(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json", temperature = 0.5f
                )
            )
            val response = apiService.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw IllegalStateException("Risposta AI assente. Nessuna analisi disponibile.")

            // 4. Token Estimation (Output)
            val outputTokens = jsonText.length / 4
            val totalTokens = inputTokens + outputTokens

            // 5. Save Log
            aiUsageLogDao.insertUsageLog(
                AiUsageLog(
                    timestamp = System.currentTimeMillis(),
                    tokensEstimated = totalTokens
                )
            )

            // 6. Parse JSON response
            val coachResponse = parseJsonResponse(jsonText)
            Result.success(coachResponse)
        } catch (e: RateLimitExceededException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseJsonResponse(jsonString: String): AiCoachResponse {
        val cleanJson = jsonString.trim().removeSurrounding("```json", "```").trim()
        val adapter = moshi.adapter(AiCoachResponse::class.java)
        return adapter.fromJson(cleanJson)
            ?: throw IllegalStateException("Formato risposta AI non valido. Nessun risultato inventato.")
    }
}

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}
