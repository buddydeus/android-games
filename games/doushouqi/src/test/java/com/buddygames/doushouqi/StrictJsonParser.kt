package com.buddygames.doushouqi

internal object StrictJsonParser {
    fun parseObject(source: String): Map<String, StrictJsonValue> {
        val parser = Parser(source)
        val result = parser.parseObject()
        parser.requireEnd()
        return result
    }

    private class Parser(private val source: String) {
        private var index = 0

        fun parseObject(): Map<String, StrictJsonValue> {
            skipWhitespace()
            expect('{')
            val values = linkedMapOf<String, StrictJsonValue>()
            skipWhitespace()
            if (consume('}')) return values
            while (true) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                expect(':')
                skipWhitespace()
                val value = when (peek()) {
                    '"' -> StrictJsonValue.StringValue(parseString())
                    '-', in '0'..'9' -> StrictJsonValue.NumberValue(parseInteger())
                    else -> error("Unsupported JSON value at character $index")
                }
                require(values.put(key, value) == null) { "Duplicate key '$key'" }
                skipWhitespace()
                if (consume('}')) return values
                expect(',')
            }
        }

        fun requireEnd() {
            skipWhitespace()
            require(index == source.length) { "Unexpected data at character $index" }
        }

        private fun parseString(): String {
            expect('"')
            return buildString {
                while (true) {
                    when (val character = take()) {
                        '"' -> return@buildString
                        '\\' -> append(
                            when (val escaped = take()) {
                                '"', '\\', '/' -> escaped
                                'b' -> '\b'
                                'f' -> '\u000c'
                                'n' -> '\n'
                                'r' -> '\r'
                                't' -> '\t'
                                else -> error("Unsupported JSON escape")
                            },
                        )
                        else -> append(character)
                    }
                }
            }
        }

        private fun parseInteger(): String {
            val start = index
            consume('-')
            require(peek() in '0'..'9')
            while (peek() in '0'..'9') index++
            return source.substring(start, index)
        }

        private fun expect(expected: Char) {
            require(take() == expected) { "Expected '$expected'" }
        }

        private fun consume(expected: Char): Boolean {
            if (peek() != expected) return false
            index++
            return true
        }

        private fun take(): Char {
            require(index < source.length) { "Unexpected end of JSON input" }
            return source[index++]
        }

        private fun peek(): Char? = source.getOrNull(index)

        private fun skipWhitespace() {
            while (peek() in setOf(' ', '\t', '\n', '\r')) index++
        }
    }
}

internal sealed interface StrictJsonValue {
    data class StringValue(val value: String) : StrictJsonValue
    data class NumberValue(val value: String) : StrictJsonValue
}

internal fun Map<String, StrictJsonValue>.string(name: String): String =
    (requireNotNull(this[name]) { "Missing '$name'" } as? StrictJsonValue.StringValue)?.value
        ?: error("'$name' must be a JSON string")

internal fun Map<String, StrictJsonValue>.int(name: String): Int =
    (requireNotNull(this[name]) { "Missing '$name'" } as? StrictJsonValue.NumberValue)?.value
        ?.toIntOrNull()
        ?: error("'$name' must be a JSON integer")
