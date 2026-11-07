package com.example.data.engine

import com.example.data.model.Resolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.random.Random

class ImageGenerationEngine {

    /**
     * Resolves the final text with prefix, suffix, and curly choices, and returns
     * the image URL for the generated image.
     */
    suspend fun generateImage(
        promptText: String,
        prefix: String,
        suffix: String,
        resolution: Resolution,
        seed: Long = Random.nextLong(100000L, 999999999L)
    ): GenerationResult = withContext(Dispatchers.Default) {
        val resolvedPrompt = CurlyChoiceEvaluator.rollCurlyChoices(promptText.trim())
        val resolvedPrefix = CurlyChoiceEvaluator.rollCurlyChoices(prefix.trim())
        val resolvedSuffix = CurlyChoiceEvaluator.rollCurlyChoices(suffix.trim())

        val prefixPart = if (resolvedPrefix.isNotBlank() && !resolvedPrompt.startsWith(resolvedPrefix)) {
            "$resolvedPrefix "
        } else ""

        val suffixPart = if (resolvedSuffix.isNotBlank() && !resolvedPrompt.endsWith(resolvedSuffix)) {
            " $resolvedSuffix"
        } else ""

        val fullResolvedText = "$prefixPart$resolvedPrompt$suffixPart".trim()

        val encoded = URLEncoder.encode(fullResolvedText, StandardCharsets.UTF_8.toString())
        val imageUrl = "https://image.pollinations.ai/prompt/$encoded?width=${resolution.width}&height=${resolution.height}&seed=$seed&nologo=true"

        GenerationResult(
            resolvedText = fullResolvedText,
            imageUrl = imageUrl,
            seed = seed,
            resolution = resolution
        )
    }

    data class GenerationResult(
        val resolvedText: String,
        val imageUrl: String,
        val seed: Long,
        val resolution: Resolution
    )
}
