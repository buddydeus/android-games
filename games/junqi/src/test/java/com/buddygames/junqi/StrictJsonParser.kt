package com.buddygames.junqi

internal object StrictJsonParser {
    fun parseObject(source: String): Map<String, StrictJsonValue> {
        val parser = Parser(source)
        val value = parser.parseValue()
        require(value is StrictJsonValue.Object) { "JSON root must be an object" }
        parser.requireEnd()
        return value.values
    }

    private class Parser(private val source: String) {
        private var index = 0

        fun parseValue(): StrictJsonValue {
            skipWhitespace()
            return when (peek()) {
                '{' -> parseObject()
                '"' -> StrictJsonValue.StringValue(parseString())
                't' -> parseLiteral("true", StrictJsonValue.BooleanValue(true))
                'f' -> parseLiteral("false", StrictJsonValue.BooleanValue(false))
                'n' -> parseLiteral("null", StrictJsonValue.NullValue)
                '-', in '0'..'9' -> parseNumber()
                else -> fail("Expected a JSON value")
            }
        }

        fun requireEnd() {
            skipWhitespace()
            require(index == source.length) { "Unexpected data at character $index" }
        }

        private fun parseObject(): StrictJsonValue.Object {
            expect('{')
            skipWhitespace()
            val values = linkedMapOf<String, StrictJsonValue>()
            if (consume('}')) return StrictJsonValue.Object(values)
            while (true) {
                skipWhitespace()
                require(peek() == '"') { "Expected an object key at character $index" }
                val key = parseString()
                skipWhitespace()
                expect(':')
                val value = parseValue()
                require(values.put(key, value) == null) { "Duplicate object key '$key'" }
                skipWhitespace()
                when {
                    consume('}') -> return StrictJsonValue.Object(values)
                    consume(',') -> Unit
                    else -> fail("Expected ',' or '}' in object")
                }
            }
        }

        private fun parseString(): String {
            expect('"')
            return buildString {
                while (true) {
                    val character = take()
                    when (character) {
                        '"' -> return@buildString
                        '\\' -> append(parseEscape())
                        in '\u0000'..'\u001f' -> fail("Unescaped control character in string")
                        else -> append(character)
                    }
                }
            }
        }

        private fun parseEscape(): Char = when (val escape = take()) {
            '"', '\\', '/' -> escape
            'b' -> '\b'
            'f' -> '\u000c'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'u' -> parseUnicodeEscape()
            else -> fail("Invalid escape sequence at character ${index - 1}")
        }

        private fun parseUnicodeEscape(): Char {
            var value = 0
            repeat(4) {
                val character = take()
                val digit = character.digitToIntOrNull(16)
                    ?: fail("Invalid unicode escape at character ${index - 1}")
                value = value * 16 + digit
            }
            return value.toChar()
        }

        private fun parseNumber(): StrictJsonValue.NumberValue {
            val start = index
            consume('-')
            when (peek()) {
                '0' -> index++
                in '1'..'9' -> {
                    index++
                    while (peek() in '0'..'9') index++
                }
                else -> fail("Invalid number at character $index")
            }
            if (consume('.')) {
                require(peek() in '0'..'9') { "Expected fraction digits at character $index" }
                while (peek() in '0'..'9') index++
            }
            if (peek() == 'e' || peek() == 'E') {
                index++
                if (peek() == '+' || peek() == '-') index++
                require(peek() in '0'..'9') { "Expected exponent digits at character $index" }
                while (peek() in '0'..'9') index++
            }
            return StrictJsonValue.NumberValue(source.substring(start, index))
        }

        private fun parseLiteral(literal: String, value: StrictJsonValue): StrictJsonValue {
            require(source.regionMatches(index, literal, 0, literal.length)) {
                "Expected '$literal' at character $index"
            }
            index += literal.length
            return value
        }

        private fun expect(expected: Char) {
            require(take() == expected) { "Expected '$expected' at character ${index - 1}" }
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
            while (peek() in JSON_WHITESPACE) index++
        }

        private fun fail(message: String): Nothing = throw IllegalArgumentException(message)
    }

    private val JSON_WHITESPACE = setOf(' ', '\t', '\n', '\r')
}

internal sealed interface StrictJsonValue {
    data class Object(val values: Map<String, StrictJsonValue>) : StrictJsonValue
    data class StringValue(val value: String) : StrictJsonValue
    data class NumberValue(val value: String) : StrictJsonValue
    data class BooleanValue(val value: Boolean) : StrictJsonValue
    object NullValue : StrictJsonValue
}

internal fun Map<String, StrictJsonValue>.string(name: String): String =
    (requireNotNull(this[name]) { "Missing '$name'" } as? StrictJsonValue.StringValue)?.value
        ?: error("'$name' must be a JSON string")

internal fun Map<String, StrictJsonValue>.int(name: String): Int =
    (requireNotNull(this[name]) { "Missing '$name'" } as? StrictJsonValue.NumberValue)?.value
        ?.toIntOrNull()
        ?: error("'$name' must be a JSON integer")
