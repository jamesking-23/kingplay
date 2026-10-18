package com.kingplay.data.library.search

import java.util.Locale

/**
 * Turns what the user typed into a safe SQLite FTS4 query: every word becomes a quoted prefix term and the
 * terms are ANDed. Quoting stops words like NOT, OR and NEAR, or stray punctuation, from being read as operators.
 * Returns null when there is nothing searchable, so callers can skip the query.
 */
object FtsQuery {
    fun build(raw: String): String? {
        val terms = tokenize(raw)
        if (terms.isEmpty()) return null
        return terms.joinToString(" ") { "\"$it*\"" }
    }

    internal fun tokenize(raw: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        for (ch in raw.lowercase(Locale.ROOT)) {
            if (Character.isLetterOrDigit(ch)) {
                current.append(ch)
            } else if (current.isNotEmpty()) {
                tokens += current.toString()
                current.clear()
            }
        }
        if (current.isNotEmpty()) tokens += current.toString()
        return tokens.take(MAX_TERMS)
    }

    private const val MAX_TERMS = 8
}
