package com.example.data.engine

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.CreativityLevel
import com.example.data.model.PromptEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class PromptBrainstormEngine(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    private val styles = listOf(
        "cinematic lighting, ultra detailed 8k, photorealistic",
        "analog film 35mm grain, vintage color grade, nostalgic",
        "volumetric fog, dramatic chiaroscuro, moody atmospheric",
        "vibrant synthwave neon, glowing accents, cybernetic detail",
        "oil on canvas, visible textured brushstrokes, classical masterpiece",
        "minimalist graphic editorial, bold negative space, high contrast",
        "macro tilt-shift focus, miniature depth of field, delicate lighting",
        "ethereal fantasy watercolor, pastel wash, dreamlike soft edges",
        "cyberpunk street photography, wet asphalt reflections, hyper-detailed",
        "concept art, matte painting, epic scale composition"
    )

    private val compositions = listOf(
        "close-up portrait with intense depth of field",
        "wide-angle cinematic establishing shot",
        "low-angle heroic perspective, dynamic framing",
        "overhead flat-lay geometric arrangement",
        "candid street-level snapshot, spontaneous motion",
        "symmetrical central composition, balanced harmony"
    )

    private val lightingModifiers = listOf(
        "golden hour warm rim lighting",
        "dramatic moody twilight with deep shadows",
        "diffused overcast morning glow",
        "bioluminescent soft neon illumination",
        "harsh midday sun with sharp stark shadows",
        "studio softbox key lighting with subtle fill"
    )

    suspend fun brainstormBatch(
        instruction: String,
        effectiveInstruction: String,
        creativity: CreativityLevel,
        steerText: String?,
        batchRule: String?,
        likedExamples: List<PromptEntity>,
        batchCount: Int = 5
    ): List<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!apiKey.isNullOrBlank()) {
            try {
                val result = callGeminiForPrompts(
                    apiKey = apiKey,
                    effectiveInstruction = effectiveInstruction,
                    creativity = creativity,
                    steerText = steerText,
                    batchRule = batchRule,
                    likedExamples = likedExamples,
                    count = batchCount
                )
                if (result.isNotEmpty()) {
                    return@withContext result
                }
            } catch (e: Exception) {
                Log.w("PromptBrainstormEngine", "Gemini call failed, using creative procedural engine", e)
            }
        }

        // Procedural intelligent brainstorm engine adhering to exact creativity and rules
        generateProceduralBatch(
            effectiveInstruction = effectiveInstruction,
            creativity = creativity,
            steerText = steerText,
            batchRule = batchRule,
            likedExamples = likedExamples,
            count = batchCount
        )
    }

    private fun getApiKey(): String? {
        return try {
            val key = BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String
            if (!key.isNullOrBlank() && key != "MY_GEMINI_API_KEY") key else null
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun callGeminiForPrompts(
        apiKey: String,
        effectiveInstruction: String,
        creativity: CreativityLevel,
        steerText: String?,
        batchRule: String?,
        likedExamples: List<PromptEntity>,
        count: Int
    ): List<String> {
        val rules = when (creativity) {
            CreativityLevel.CREATIVE -> """
                - One prompt per line (no commentary or numbering).
                - Use the instruction as a creative springboard, not a checklist. Reinterpret or expand freely.
                - Push for genuine variety: different subjects, moods, compositions, art styles, color palettes.
                - Spread outputs across a creative spectrum: some faithful with a fresh twist, some bold creative departures.
                - Avoid near-duplicates.
            """.trimIndent()
            CreativityLevel.BALANCED -> """
                - One prompt per line (no commentary or numbering).
                - Keep the core subject and explicit asks intact, but explore fresh angles, varied moods, lighting, and styles.
                - Across outputs, vary mood, lighting, composition, and art style while keeping the subject steady.
                - Avoid near-duplicates.
            """.trimIndent()
            CreativityLevel.FAITHFUL -> """
                - One prompt per line (no commentary or numbering).
                - Keep the user's style and constraints; do not add conflicting styles.
                - Include ALL details specified in the instruction, creatively rephrased and expanded into rich visual prompts.
                - Push for exploratory diversity within the user's explicit constraints.
            """.trimIndent()
        }

        val steerSection = if (!steerText.isNullOrBlank()) {
            "\n<theme_steer>\n$steerText\n</theme_steer>\nBlend this theme heavily into every one of the $count prompts."
        } else ""

        val ruleSection = if (!batchRule.isNullOrBlank()) {
            "\n<every_batch_rule>\n$batchRule\n</every_batch_rule>\nFollow this rule in all generated prompts."
        } else ""

        val examplesSection = if (likedExamples.isNotEmpty()) {
            val topPicks = likedExamples.take(3).joinToString("\n") { "- ${it.text}" }
            "\n<highly_rated_prompts>\n$topPicks\n</highly_rated_prompts>\nTake creative inspiration from these liked styles."
        } else ""

        val promptBody = """
            <user_instruction>
            $effectiveInstruction
            </user_instruction>
            
            Generate $count NEW image prompts inspired by the user_instruction.
            $steerSection$ruleSection$examplesSection
            
            Rules:
            $rules
        """.trimIndent()

        val json = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", promptBody)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", if (creativity == CreativityLevel.CREATIVE) 1.0 else if (creativity == CreativityLevel.BALANCED) 0.7 else 0.4)
                put("maxOutputTokens", 1024)
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw RuntimeException("Gemini HTTP ${response.code}: ${response.body?.string()}")
        }

        val bodyString = response.body?.string() ?: ""
        val responseJson = JSONObject(bodyString)
        val candidate = responseJson.optJSONArray("candidates")?.optJSONObject(0)
        val textPart = candidate?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: ""

        val lines = textPart.lines()
            .map { it.trim() }
            .map { it.removePrefix("- ").removePrefix("* ") }
            .map { it.replace(Regex("""^\d+[\.\)]\s*"""), "") }
            .map { it.replace("[", "(").replace("]", ")") }
            .filter { it.isNotBlank() && !it.startsWith("Midjourney", ignoreCase = true) }

        return lines.take(count)
    }

    private fun generateProceduralBatch(
        effectiveInstruction: String,
        creativity: CreativityLevel,
        steerText: String?,
        batchRule: String?,
        likedExamples: List<PromptEntity>,
        count: Int
    ): List<String> {
        val baseSubject = CurlyChoiceEvaluator.rollCurlyChoices(effectiveInstruction.trim())
        val steer = steerText?.let { CurlyChoiceEvaluator.rollCurlyChoices(it.trim()) }
        val rule = batchRule?.let { CurlyChoiceEvaluator.rollCurlyChoices(it.trim()) }

        val results = mutableListOf<String>()
        val usedStyles = styles.shuffled().toMutableList()
        val usedComps = compositions.shuffled().toMutableList()
        val usedLights = lightingModifiers.shuffled().toMutableList()

        for (i in 0 until count) {
            val style = usedStyles.getOrNull(i % usedStyles.size) ?: styles.random()
            val comp = usedComps.getOrNull(i % usedComps.size) ?: compositions.random()
            val light = usedLights.getOrNull(i % usedLights.size) ?: lightingModifiers.random()

            val parts = mutableListOf<String>()

            // Steer influence
            if (!steer.isNullOrBlank()) {
                parts.add("$steer themed")
            }

            // Subject core
            when (creativity) {
                CreativityLevel.FAITHFUL -> {
                    parts.add(baseSubject)
                    parts.add(comp)
                    parts.add(light)
                }
                CreativityLevel.BALANCED -> {
                    parts.add(baseSubject)
                    parts.add(comp)
                    parts.add(light)
                    parts.add(style)
                }
                CreativityLevel.CREATIVE -> {
                    val creativeTwist = listOf(
                        "surreal and reimagined",
                        "hyper-detailed avant-garde vision",
                        "ethereal dreamscape concept",
                        "dramatic cinematic interpretation",
                        "experimental artistic expression"
                    ).random()
                    parts.add("$baseSubject, $creativeTwist")
                    parts.add(comp)
                    parts.add(style)
                    parts.add(light)
                }
            }

            // Mix-in from liked prompt if available
            if (likedExamples.isNotEmpty() && Random.nextFloat() < 0.4f) {
                val exampleText = likedExamples.random().text
                val exampleSnippet = exampleText.split(",").map { it.trim() }.filter { it.length > 5 }.randomOrNull()
                if (exampleSnippet != null && !parts.contains(exampleSnippet)) {
                    parts.add(exampleSnippet)
                }
            }

            // Batch rule
            if (!rule.isNullOrBlank()) {
                parts.add(rule)
            }

            results.add(parts.joinToString(", "))
        }

        return results
    }
}
