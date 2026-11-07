package com.example.data.engine

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AiHelpMode
import com.example.data.model.InfluenceMarkEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiHelpEngine(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    suspend fun suggestInstruction(
        currentInstruction: String,
        pendingDraft: String?,
        userNote: String?,
        marks: List<InfluenceMarkEntity>,
        mode: AiHelpMode
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!apiKey.isNullOrBlank()) {
            try {
                val suggestion = callGeminiForSuggestion(
                    apiKey = apiKey,
                    currentInstruction = currentInstruction,
                    pendingDraft = pendingDraft,
                    userNote = userNote,
                    marks = marks,
                    mode = mode
                )
                if (suggestion.isNotBlank()) {
                    return@withContext cleanOutput(suggestion)
                }
            } catch (e: Exception) {
                Log.w("AiHelpEngine", "Gemini suggestion failed, using rule-based synthesis", e)
            }
        }

        // Procedural rewrite synthesis
        generateProceduralSuggestion(currentInstruction, pendingDraft, userNote, marks, mode)
    }

    private fun getApiKey(): String? {
        return try {
            val key = BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String
            if (!key.isNullOrBlank() && key != "MY_GEMINI_API_KEY") key else null
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun callGeminiForSuggestion(
        apiKey: String,
        currentInstruction: String,
        pendingDraft: String?,
        userNote: String?,
        marks: List<InfluenceMarkEntity>,
        mode: AiHelpMode
    ): String {
        val liked = marks.filter { !it.isAnti }.takeLast(12)
        val disliked = marks.filter { it.isAnti }.takeLast(12)

        val likedBullets = liked.joinToString("\n") { "- ${it.text.trim()}" }
        val dislikedBullets = disliked.joinToString("\n") { "- ${it.text.trim()}" }

        val parts = mutableListOf(
            "You are helping a user rewrite the instruction they give to an AI image-prompt writer. The user looks at generated images and marked some as positive influences (liked aspects) or anti-influences (unwanted aspects).",
            "<current_instruction>\n${currentInstruction.ifBlank { "(none yet)" }}\n</current_instruction>"
        )

        if (!pendingDraft.isNullOrBlank() && pendingDraft != currentInstruction) {
            parts.add("<draft_in_progress>\n$pendingDraft\n</draft_in_progress>\nThe user has already started rewriting it. Build on this draft rather than starting over.")
        }
        if (likedBullets.isNotBlank()) {
            parts.add("<liked_image_prompts>\n$likedBullets\n</liked_image_prompts>\nThe user liked these aspects. Synthesize what they have in common into the instruction.")
        }
        if (dislikedBullets.isNotBlank()) {
            parts.add("<disliked_image_prompts>\n$dislikedBullets\n</disliked_image_prompts>\nThe user did NOT want these aspects. Steer the instruction away from them.")
        }
        if (!userNote.isNullOrBlank()) {
            parts.add("<user_request>\n$userNote\n</user_request>\nThis is what the user wants changed. Follow this request above everything else.")
        }

        parts.add("""
            Rules:
            - Write a new version of the instruction itself (a description of the images the user wants), not an image prompt.
            - Mode: ${mode.title}. ${mode.rule}
            - Keep any {option a|option b} choice blocks well-formed.
            - Reply with the new instruction only, between <new_instruction> and </new_instruction>: nothing before or after it — no preamble, no commentary, no markdown quotes.
        """.trimIndent())

        val json = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", parts.joinToString("\n\n"))
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.6)
                put("maxOutputTokens", 512)
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val bodyString = response.body?.string() ?: ""
        val responseJson = JSONObject(bodyString)
        val textPart = responseJson.optJSONArray("candidates")?.optJSONObject(0)
            ?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: ""

        return textPart
    }

    private fun cleanOutput(raw: String): String {
        var t = raw.trim()
        val openTag = "<new_instruction>"
        val closeTag = "</new_instruction>"

        if (t.contains(openTag, ignoreCase = true)) {
            val start = t.indexOf(openTag, ignoreCase = true) + openTag.length
            val end = if (t.contains(closeTag, ignoreCase = true)) t.indexOf(closeTag, ignoreCase = true) else t.length
            t = t.substring(start, end).trim()
        }

        // Strip leading wrappers
        t = t.replace(Regex("""^(?:here(?:'s| is) (?:the )?(?:new |revised )?instruction:?\s*)""", RegexOption.IGNORE_CASE), "")
        t = t.replace(Regex("""^```[a-z]*\n([\s\S]*?)\n?```$"""), "$1").trim()
        if (t.startsWith("\"") && t.endsWith("\"") && t.length > 2) {
            t = t.substring(1, t.length - 1).trim()
        }
        return t
    }

    private fun generateProceduralSuggestion(
        currentInstruction: String,
        pendingDraft: String?,
        userNote: String?,
        marks: List<InfluenceMarkEntity>,
        mode: AiHelpMode
    ): String {
        val base = (pendingDraft?.ifBlank { null } ?: currentInstruction).trim()
        val likedSnippets = marks.filter { !it.isAnti }
            .flatMap { it.text.split(",") }
            .map { it.trim() }
            .filter { it.length > 4 }
            .distinct()
            .take(3)

        val dislikedSnippets = marks.filter { it.isAnti }
            .flatMap { it.text.split(",") }
            .map { it.trim() }
            .filter { it.length > 4 }
            .distinct()
            .take(2)

        val elements = mutableListOf<String>()
        if (base.isNotBlank()) elements.add(base)

        if (!userNote.isNullOrBlank()) {
            elements.add(userNote.trim())
        }

        if (likedSnippets.isNotEmpty()) {
            elements.add("accentuating ${likedSnippets.joinToString(" and ")}")
        }

        if (dislikedSnippets.isNotEmpty()) {
            elements.add("avoiding ${dislikedSnippets.joinToString(", ")}")
        }

        return when (mode) {
            AiHelpMode.TIGHTEN -> {
                // Remove repeated words, keep concise
                base.split(",").map { it.trim() }.distinct().joinToString(", ")
            }
            AiHelpMode.EXPAND -> {
                val expandedDetails = listOf(
                    "rich volumetric depth",
                    "exquisite textured details",
                    "nuanced atmospheric lighting"
                ).shuffled().take(2).joinToString(", ")
                "$base, with $expandedDetails" + if (userNote?.isNotBlank() == true) ", ${userNote.trim()}" else ""
            }
            AiHelpMode.FREE -> {
                "Artistic reimagining of $base" +
                        (if (likedSnippets.isNotEmpty()) " highlighting ${likedSnippets.first()}" else "") +
                        (if (userNote?.isNotBlank() == true) ", ${userNote.trim()}" else "")
            }
            AiHelpMode.KEEP -> {
                elements.joinToString(", ")
            }
        }
    }
}
