package com.endroid.codecanva.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Lightweight, fast token-based syntax highlighter.
 * Skips heavy work on very large files to keep the UI responsive.
 */
object SyntaxHighlighter {

    private const val MAX_HIGHLIGHT_CHARS = 40_000

    data class ThemeColors(
        val keyword: Color,
        val string: Color,
        val comment: Color,
        val number: Color,
        val function: Color,
        val type: Color,
        val operator: Color,
        val default: Color
    )

    val darkColors = ThemeColors(
        keyword = Color(0xFFBB9AF7),
        string = Color(0xFF9ECE6A),
        comment = Color(0xFF565F89),
        number = Color(0xFFFF9E64),
        function = Color(0xFF7AA2F7),
        type = Color(0xFF2AC3DE),
        operator = Color(0xFF89DDFF),
        default = Color(0xFFC0CAF5)
    )

    val lightColors = ThemeColors(
        keyword = Color(0xFF5C2D91),
        string = Color(0xFF0A7A0A),
        comment = Color(0xFF6A737D),
        number = Color(0xFFB35C00),
        function = Color(0xFF0550AE),
        type = Color(0xFF0550AE),
        operator = Color(0xFFCF222E),
        default = Color(0xFF24292F)
    )

    private val kotlinKeywords = hashSetOf(
        "package", "import", "class", "interface", "object", "fun", "val", "var",
        "if", "else", "when", "for", "while", "do", "return", "break", "continue",
        "try", "catch", "finally", "throw", "in", "is", "as", "by", "this", "super",
        "true", "false", "null", "typealias", "data", "sealed", "enum", "annotation",
        "companion", "init", "constructor", "override", "open", "abstract", "final",
        "private", "protected", "public", "internal", "suspend", "inline", "reified",
        "lateinit", "const", "operator", "infix", "tailrec", "external", "actual",
        "expect", "where", "get", "set", "field", "it"
    )

    private val javaKeywords = hashSetOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
        "class", "const", "continue", "default", "do", "double", "else", "enum",
        "extends", "final", "finally", "float", "for", "goto", "if", "implements",
        "import", "instanceof", "int", "interface", "long", "native", "new", "package",
        "private", "protected", "public", "return", "short", "static", "strictfp",
        "super", "switch", "synchronized", "this", "throw", "throws", "transient",
        "try", "void", "volatile", "while", "true", "false", "null", "var", "record",
        "sealed", "permits", "yield"
    )

    private val pythonKeywords = hashSetOf(
        "False", "None", "True", "and", "as", "assert", "async", "await", "break",
        "class", "continue", "def", "del", "elif", "else", "except", "finally",
        "for", "from", "global", "if", "import", "in", "is", "lambda", "nonlocal",
        "not", "or", "pass", "raise", "return", "try", "while", "with", "yield"
    )

    private val jsKeywords = hashSetOf(
        "break", "case", "catch", "class", "const", "continue", "debugger", "default",
        "delete", "do", "else", "export", "extends", "finally", "for", "function",
        "if", "import", "in", "instanceof", "new", "return", "super", "switch",
        "this", "throw", "try", "typeof", "var", "void", "while", "with", "yield",
        "let", "static", "enum", "await", "async", "true", "false", "null", "undefined",
        "of", "from", "as"
    )

    private val sqlKeywords = hashSetOf(
        "SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE", "CREATE", "DROP",
        "ALTER", "TABLE", "INDEX", "VIEW", "JOIN", "LEFT", "RIGHT", "INNER", "OUTER",
        "ON", "AS", "AND", "OR", "NOT", "IN", "BETWEEN", "LIKE", "IS", "NULL",
        "ORDER", "BY", "GROUP", "HAVING", "LIMIT", "OFFSET", "UNION", "ALL", "DISTINCT",
        "PRIMARY", "KEY", "FOREIGN", "REFERENCES", "CONSTRAINT", "DEFAULT", "VALUES",
        "INTO", "SET", "TRUE", "FALSE"
    )

    fun highlight(
        text: String,
        language: Language,
        isDark: Boolean
    ): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")
        if (language == Language.PLAIN) return AnnotatedString(text)

        // For large files skip expensive highlighting to keep UI smooth
        if (text.length > MAX_HIGHLIGHT_CHARS) {
            return AnnotatedString(text)
        }

        val colors = if (isDark) darkColors else lightColors
        if (language == Language.HTML || language == Language.XML) {
            return highlightMarkup(text, colors, language == Language.HTML)
        }
        val keywords = when (language) {
            Language.KOTLIN -> kotlinKeywords
            Language.JAVA, Language.C -> javaKeywords
            Language.PYTHON -> pythonKeywords
            Language.JAVASCRIPT, Language.TYPESCRIPT -> jsKeywords
            Language.SQL -> sqlKeywords
            else -> emptySet()
        }

        val isXmlLike = language == Language.XML || language == Language.MARKDOWN
        val isPython = language == Language.PYTHON
        val isSql = language == Language.SQL

        return buildAnnotatedString {
            var i = 0
            val len = text.length

            while (i < len) {
                val c = text[i]

                // Comments
                when {
                    isXmlLike && text.startsWith("<!--", i) -> {
                        val end = text.indexOf("-->", i).let { if (it < 0) len else it + 3 }
                        withStyle(SpanStyle(color = colors.comment)) { append(text, i, end) }
                        i = end
                        continue
                    }
                    !isPython && !isSql && text.startsWith("//", i) -> {
                        val end = text.indexOf('\n', i).let { if (it < 0) len else it }
                        withStyle(SpanStyle(color = colors.comment)) { append(text, i, end) }
                        i = end
                        continue
                    }
                    !isPython && !isSql && text.startsWith("/*", i) -> {
                        val end = text.indexOf("*/", i).let { if (it < 0) len else it + 2 }
                        withStyle(SpanStyle(color = colors.comment)) { append(text, i, end) }
                        i = end
                        continue
                    }
                    isPython && c == '#' -> {
                        val end = text.indexOf('\n', i).let { if (it < 0) len else it }
                        withStyle(SpanStyle(color = colors.comment)) { append(text, i, end) }
                        i = end
                        continue
                    }
                    isSql && text.startsWith("--", i) -> {
                        val end = text.indexOf('\n', i).let { if (it < 0) len else it }
                        withStyle(SpanStyle(color = colors.comment)) { append(text, i, end) }
                        i = end
                        continue
                    }
                    // XML tags
                    isXmlLike && c == '<' -> {
                        val end = text.indexOf('>', i).let { if (it < 0) len else it + 1 }
                        withStyle(SpanStyle(color = colors.keyword, fontWeight = FontWeight.Medium)) {
                            append(text, i, end)
                        }
                        i = end
                        continue
                    }
                    // Strings
                    c == '"' || c == '\'' -> {
                        val quote = c
                        var j = i + 1
                        while (j < len) {
                            if (text[j] == '\\' && j + 1 < len) {
                                j += 2
                                continue
                            }
                            if (text[j] == quote) {
                                j++
                                break
                            }
                            j++
                        }
                        withStyle(SpanStyle(color = colors.string)) { append(text, i, j) }
                        i = j
                        continue
                    }
                    // Numbers
                    c.isDigit() -> {
                        var j = i + 1
                        while (j < len && (text[j].isDigit() || text[j] == '.' || text[j] == 'x' ||
                                    text[j] in 'a'..'f' || text[j] in 'A'..'F' || text[j] == '_')) {
                            j++
                        }
                        withStyle(SpanStyle(color = colors.number)) { append(text, i, j) }
                        i = j
                        continue
                    }
                    // Identifiers / keywords
                    c.isLetter() || c == '_' -> {
                        var j = i + 1
                        while (j < len && (text[j].isLetterOrDigit() || text[j] == '_')) j++
                        val word = text.substring(i, j)
                        val isKeyword = keywords.contains(word) ||
                                (isSql && keywords.contains(word.uppercase()))
                        var k = j
                        while (k < len && text[k].isWhitespace()) k++
                        val isFunc = k < len && text[k] == '('

                        when {
                            isKeyword -> withStyle(
                                SpanStyle(color = colors.keyword, fontWeight = FontWeight.SemiBold)
                            ) { append(word) }
                            isFunc -> withStyle(SpanStyle(color = colors.function)) { append(word) }
                            word.first().isUpperCase() && language in listOf(
                                Language.KOTLIN, Language.JAVA, Language.TYPESCRIPT
                            ) -> withStyle(SpanStyle(color = colors.type)) { append(word) }
                            else -> withStyle(SpanStyle(color = colors.default)) { append(word) }
                        }
                        i = j
                        continue
                    }
                    // Operators
                    c in "+-*/%=<>!&|^~?:;,.()[]{}" -> {
                        withStyle(SpanStyle(color = colors.operator)) { append(c) }
                        i++
                        continue
                    }
                    else -> {
                        withStyle(SpanStyle(color = colors.default)) { append(c) }
                        i++
                    }
                }
            }
        }
    }
}
