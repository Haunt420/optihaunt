package com.example.data.engine

import kotlin.random.Random

object CurlyChoiceEvaluator {

    /**
     * Rolls curly alternatives like "{golden hour|blue hour|overcast}" randomly.
     * Empty choices like "{huge heads|}" are supported.
     */
    fun rollCurlyChoices(input: String): String {
        if (!input.contains('{')) return input
        val regex = Regex("""\{([^{}]*)\}""")
        var result = input
        while (regex.containsMatchIn(result)) {
            result = regex.replace(result) { matchResult ->
                val inner = matchResult.groupValues[1]
                val options = inner.split('|').map { it.trim() }
                if (options.isNotEmpty()) {
                    options[Random.nextInt(options.size)]
                } else {
                    ""
                }
            }
        }
        return result.replace(Regex("""\s{2,}"""), " ").trim()
    }

    /**
     * Checks if curly brackets in text are properly balanced and not nested.
     */
    fun isValidCurlyBlocks(text: String): Boolean {
        var depth = 0
        for (ch in text) {
            when (ch) {
                '{' -> {
                    depth++
                    if (depth > 1) return false // No nested blocks allowed
                }
                '}' -> {
                    depth--
                    if (depth < 0) return false
                }
            }
        }
        return depth == 0
    }
}
