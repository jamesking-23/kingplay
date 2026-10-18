package com.kingplay.core.model.backup

/**
 * A small JSON reader and writer for the backup file. Values are `Map<String, Any?>`, `List<Any?>`, `String`,
 * `Long`, `Double`, `Boolean` or null. Kept in the model module so it can be tested on the plain JVM.
 */
object Json {
    private const val MAX_DEPTH = 32

    fun parse(text: String): Any? {
        val reader = Reader(text.removePrefix("﻿"))
        val value = reader.value(0)
        reader.skipSpace()
        require(reader.atEnd()) { "Unexpected text after the end of the data" }
        return value
    }

    fun write(value: Any?): String = StringBuilder().also { append(it, value, 0) }.toString()

    private fun append(out: StringBuilder, value: Any?, indent: Int) {
        when (value) {
            null -> out.append("null")
            is Boolean, is Long, is Int -> out.append(value.toString())
            is Float -> out.append(finite(value.toString().toDouble()))
            is Double -> out.append(finite(value))
            is String -> quote(out, value)
            is Map<*, *> -> block(out, '{', '}', value.entries.toList(), indent) { entry ->
                quote(out, entry.key.toString())
                out.append(": ")
                append(out, entry.value, indent + 1)
            }
            is List<*> -> block(out, '[', ']', value, indent) { append(out, it, indent + 1) }
            else -> throw IllegalArgumentException("Cannot write ${value::class.simpleName}")
        }
    }

    private fun finite(d: Double): String {
        require(d.isFinite()) { "Numbers must be finite" }
        return if (d == Math.rint(d) && Math.abs(d) < 1e15) d.toLong().toString() + ".0" else d.toString()
    }

    private fun <T> block(
        out: StringBuilder,
        open: Char,
        close: Char,
        items: List<T>,
        indent: Int,
        item: (T) -> Unit,
    ) {
        out.append(open)
        if (items.isNotEmpty()) {
            items.forEachIndexed { index, element ->
                out.append(if (index == 0) "\n" else ",\n")
                repeat(indent + 1) { out.append("  ") }
                item(element)
            }
            out.append('\n')
            repeat(indent) { out.append("  ") }
        }
        out.append(close)
    }

    private fun quote(out: StringBuilder, s: String) {
        out.append('"')
        for (c in s) {
            when {
                c == '"' -> out.append("\\\"")
                c == '\\' -> out.append("\\\\")
                c == '\n' -> out.append("\\n")
                c == '\r' -> out.append("\\r")
                c == '\t' -> out.append("\\t")
                c < ' ' -> out.append("\\u").append(c.code.toString(16).padStart(4, '0'))
                else -> out.append(c)
            }
        }
        out.append('"')
    }

    private class Reader(private val s: String) {
        private var i = 0

        fun atEnd() = i >= s.length

        fun skipSpace() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        fun value(depth: Int): Any? {
            require(depth <= MAX_DEPTH) { "Data is nested too deeply" }
            skipSpace()
            require(!atEnd()) { "Unexpected end of data" }
            return when (val c = s[i]) {
                '{' -> obj(depth)
                '[' -> list(depth)
                '"' -> string()
                't' -> literal("true", true)
                'f' -> literal("false", false)
                'n' -> literal("null", null)
                else -> {
                    require(c == '-' || c.isDigit()) { "Unexpected '$c' at $i" }
                    number()
                }
            }
        }

        private fun literal(word: String, result: Any?): Any? {
            require(s.startsWith(word, i)) { "Unexpected text at $i" }
            i += word.length
            return result
        }

        private fun obj(depth: Int): Map<String, Any?> {
            i++
            val map = LinkedHashMap<String, Any?>()
            skipSpace()
            if (peek() == '}') {
                i++
                return map
            }
            while (true) {
                skipSpace()
                require(peek() == '"') { "Expected a name at $i" }
                val key = string()
                skipSpace()
                require(peek() == ':') { "Expected ':' at $i" }
                i++
                map[key] = value(depth + 1)
                skipSpace()
                when (peek()) {
                    ',' -> i++
                    '}' -> {
                        i++
                        return map
                    }
                    else -> throw IllegalArgumentException("Expected ',' or '}' at $i")
                }
            }
        }

        private fun list(depth: Int): List<Any?> {
            i++
            val list = ArrayList<Any?>()
            skipSpace()
            if (peek() == ']') {
                i++
                return list
            }
            while (true) {
                list.add(value(depth + 1))
                skipSpace()
                when (peek()) {
                    ',' -> i++
                    ']' -> {
                        i++
                        return list
                    }
                    else -> throw IllegalArgumentException("Expected ',' or ']' at $i")
                }
            }
        }

        private fun peek(): Char {
            require(!atEnd()) { "Unexpected end of data" }
            return s[i]
        }

        private fun string(): String {
            i++
            val sb = StringBuilder()
            while (true) {
                require(!atEnd()) { "Unterminated text" }
                val c = s[i++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        require(!atEnd()) { "Unterminated text" }
                        when (val e = s[i++]) {
                            '"', '\\', '/' -> sb.append(e)
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'u' -> {
                                require(i + 4 <= s.length) { "Bad escape" }
                                sb.append(s.substring(i, i + 4).toInt(16).toChar())
                                i += 4
                            }
                            else -> throw IllegalArgumentException("Bad escape '\\$e'")
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }

        private fun number(): Any {
            val start = i
            if (s[i] == '-') i++
            while (i < s.length && (s[i].isDigit() || s[i] in ".eE+-")) i++
            val token = s.substring(start, i)
            val number = token.toLongOrNull() ?: token.toDoubleOrNull()
            return requireNotNull(number) { "Bad number '$token'" }
        }
    }
}
