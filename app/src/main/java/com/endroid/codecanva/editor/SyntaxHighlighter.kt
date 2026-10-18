package com.endroid.codecanva.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

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
        val default: Color,
    )

    val darkColors = ThemeColors(
        keyword = Color(0xFFBB9AF7),
        string = Color(0xFF9ECE6A),
        comment = Color(0xFF565F89),
        number = Color(0xFFFF9E64),
        function = Color(0xFF7AA2F7),
        type = Color(0xFF2AC3DE),
        operator = Color(0xFF89DDFF),
        default = Color(0xFFC0CAF5),
    )

    val lightColors = ThemeColors(
        keyword = Color(0xFF7C3AED),
        string = Color(0xFF059669),
        comment = Color(0xFF6B7280),
        number = Color(0xFFD97706),
        function = Color(0xFF2563EB),
        type = Color(0xFF0891B2),
        operator = Color(0xFF4B5563),
        default = Color(0xFF1F2937),
    )

    private val kotlinKeywords = setOf(
        "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in",
        "interface", "is", "null", "object", "package", "return", "super", "this", "throw",
        "true", "try", "typealias", "typeof", "val", "var", "when", "while", "by", "catch",
        "constructor", "delegate", "dynamic", "field", "file", "finally", "get", "import",
        "init", "param", "property", "receiver", "set", "setparam", "where", "actual",
        "abstract", "annotation", "companion", "const", "crossinline", "data", "enum",
        "expect", "external", "final", "infix", "inline", "inner", "internal", "lateinit",
        "noinline", "open", "operator", "out", "override", "private", "protected", "public",
        "reified", "sealed", "suspend", "tailrec", "value", "vararg",
    )
    private val javaKeywords = setOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
        "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
        "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
        "interface", "long", "native", "new", "package", "private", "protected", "public",
        "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
        "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false", "null",
    )
    private val pythonKeywords = setOf(
        "False", "None", "True", "and", "as", "assert", "async", "await", "break", "class",
        "continue", "def", "del", "elif", "else", "except", "finally", "for", "from", "global",
        "if", "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass", "raise",
        "return", "try", "while", "with", "yield",
    )
    private val jsKeywords = setOf(
        "await", "break", "case", "catch", "class", "const", "continue", "debugger", "default",
        "delete", "do", "else", "export", "extends", "false", "finally", "for", "function",
        "if", "import", "in", "instanceof", "let", "new", "null", "return", "static", "super",
        "switch", "this", "throw", "true", "try", "typeof", "var", "void", "while", "with",
        "yield", "async", "of", "from",
    )
    private val sqlKeywords = setOf(
        "SELECT", "FROM", "WHERE", "AND", "OR", "INSERT", "INTO", "VALUES", "UPDATE", "SET",
        "DELETE", "CREATE", "TABLE", "DROP", "ALTER", "JOIN", "LEFT", "RIGHT", "INNER", "ON",
        "AS", "ORDER", "BY", "GROUP", "HAVING", "LIMIT", "NULL", "NOT", "IN", "IS", "LIKE",
    )
    private val cssKeywords = setOf(
        "color", "background", "margin", "padding", "border", "display", "flex", "grid",
        "width", "height", "font", "font-size", "font-family", "font-weight", "position",
        "top", "left", "right", "bottom", "opacity", "transform", "transition", "animation",
        "justify-content", "align-items", "gap", "overflow", "z-index", "box-shadow",
        "border-radius", "text-align", "line-height", "max-width", "min-height",
    )

    fun highlight(text: String, language: Language, isDark: Boolean): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")
        if (language == Language.PLAIN) return AnnotatedString(text)
        if (text.length > MAX_HIGHLIGHT_CHARS) return AnnotatedString(text)

        val colors = if (isDark) darkColors else lightColors
        if (language == Language.HTML || language == Language.XML) {
            return highlightMarkup(text, colors, withEmbedded = language == Language.HTML)
        }

        val keywords = when (language) {
            Language.KOTLIN -> kotlinKeywords
            Language.JAVA, Language.C -> javaKeywords
            Language.PYTHON -> pythonKeywords
            Language.JAVASCRIPT, Language.TYPESCRIPT -> jsKeywords
            Language.SQL -> sqlKeywords
            else -> emptySet()
        }
        val isPython = language == Language.PYTHON

        return buildAnnotatedString {
            var i = 0
            val len = text.length
            while (i < len) {
                val c = text[i]
                when {
                    text.startsWith("/*", i) -> {
                        val end = text.indexOf("*/", i + 2).let { if (it < 0) len else it + 2 }
                        withStyle(SpanStyle(color = colors.comment)) { append(text, i, end) }
                        i = end
                    }
                    (text.startsWith("//", i) || (isPython && text[i] == '#')) -> {
                        val end = text.indexOf('\n', i).let { if (it < 0) len else it }
                        withStyle(SpanStyle(color = colors.comment)) { append(text, i, end) }
                        i = end
                    }
                    c == '"' || c == '\'' || c == '`' -> {
                        var j = i + 1
                        while (j < len && text[j] != c) {
                            if (text[j] == '\\' && j + 1 < len) j += 2 else j++
                        }
                        if (j < len) j++
                        withStyle(SpanStyle(color = colors.string)) { append(text, i, j) }
                        i = j
                    }
                    c.isDigit() -> {
                        var j = i
                        while (j < len && (text[j].isDigit() || text[j] == '.' || text[j] == 'x')) j++
                        withStyle(SpanStyle(color = colors.number)) { append(text, i, j) }
                        i = j
                    }
                    c.isLetter() || c == '_' -> {
                        var j = i + 1
                        while (j < len && (text[j].isLetterOrDigit() || text[j] == '_')) j++
                        val word = text.substring(i, j)
                        val isKeyword = word in keywords || word.uppercase() in keywords
                        val isFunc = j < len && text[j] == '('
                        when {
                            isKeyword -> withStyle(SpanStyle(color = colors.keyword, fontWeight = FontWeight.SemiBold)) { append(word) }
                            isFunc -> withStyle(SpanStyle(color = colors.function)) { append(word) }
                            word.first().isUpperCase() -> withStyle(SpanStyle(color = colors.type)) { append(word) }
                            else -> withStyle(SpanStyle(color = colors.default)) { append(word) }
                        }
                        i = j
                    }
                    c in "+-*/%=<>!&|^~?:;,.()[]{}" -> {
                        withStyle(SpanStyle(color = colors.operator)) { append(c) }
                        i++
                    }
                    else -> {
                        withStyle(SpanStyle(color = colors.default)) { append(c) }
                        i++
                    }
                }
            }
        }
    }

    private fun highlightMarkup(text: String, colors: ThemeColors, withEmbedded: Boolean): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            while (i < text.length) {
                if (withEmbedded && text.startsWith("<script", i, ignoreCase = true)) {
                    val tagEnd = text.indexOf('>', i).let { if (it < 0) text.length else it + 1 }
                    append(highlightTag(text.substring(i, tagEnd), colors))
                    i = tagEnd
                    val close = text.indexOf("</script>", i, ignoreCase = true).let { if (it < 0) text.length else it }
                    append(highlightJsCssRegion(text.substring(i, close), colors, js = true))
                    i = close
                    continue
                }
                if (withEmbedded && text.startsWith("<style", i, ignoreCase = true)) {
                    val tagEnd = text.indexOf('>', i).let { if (it < 0) text.length else it + 1 }
                    append(highlightTag(text.substring(i, tagEnd), colors))
                    i = tagEnd
                    val close = text.indexOf("</style>", i, ignoreCase = true).let { if (it < 0) text.length else it }
                    append(highlightJsCssRegion(text.substring(i, close), colors, js = false))
                    i = close
                    continue
                }
                if (text.startsWith("<!--", i)) {
                    val end = text.indexOf("-->", i).let { if (it < 0) text.length else it + 3 }
                    withStyle(SpanStyle(color = colors.comment)) { append(text.substring(i, end)) }
                    i = end
                    continue
                }
                if (text[i] == '<') {
                    val end = text.indexOf('>', i).let { if (it < 0) text.length else it + 1 }
                    append(highlightTag(text.substring(i, end), colors))
                    i = end
                    continue
                }
                val next = text.indexOf('<', i).let { if (it < 0) text.length else it }
                withStyle(SpanStyle(color = colors.default)) { append(text.substring(i, next)) }
                i = next
            }
        }
    }

    private fun highlightTag(tag: String, colors: ThemeColors): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            while (i < tag.length) {
                when {
                    tag[i] == '<' || tag[i] == '>' || tag[i] == '/' -> {
                        withStyle(SpanStyle(color = colors.operator)) { append(tag[i]) }
                        i++
                    }
                    tag[i].isLetter() || tag[i] == '!' -> {
                        var j = i + 1
                        while (j < tag.length && (tag[j].isLetterOrDigit() || tag[j] in "_:-")) j++
                        val word = tag.substring(i, j)
                        val isAttr = i > 0 && tag[i - 1].isWhitespace()
                        withStyle(
                            SpanStyle(
                                color = if (isAttr) colors.type else colors.keyword,
                                fontWeight = FontWeight.Medium,
                            ),
                        ) { append(word) }
                        i = j
                    }
                    tag[i] == '"' || tag[i] == '\'' -> {
                        val q = tag[i]
                        var j = i + 1
                        while (j < tag.length && tag[j] != q) j++
                        if (j < tag.length) j++
                        withStyle(SpanStyle(color = colors.string)) { append(tag.substring(i, j)) }
                        i = j
                    }
                    tag[i].isDigit() -> {
                        var j = i
                        while (j < tag.length && (tag[j].isDigit() || tag[j] == '.')) j++
                        withStyle(SpanStyle(color = colors.number)) { append(tag.substring(i, j)) }
                        i = j
                    }
                    else -> {
                        withStyle(SpanStyle(color = colors.default)) { append(tag[i]) }
                        i++
                    }
                }
            }
        }
    }

    private fun highlightJsCssRegion(text: String, colors: ThemeColors, js: Boolean): AnnotatedString {
        return buildAnnotatedString {
            val kw = if (js) jsKeywords else cssKeywords
            var i = 0
            while (i < text.length) {
                when {
                    text.startsWith("/*", i) -> {
                        val end = text.indexOf("*/", i + 2).let { if (it < 0) text.length else it + 2 }
                        withStyle(SpanStyle(color = colors.comment)) { append(text.substring(i, end)) }
                        i = end
                    }
                    js && text.startsWith("//", i) -> {
                        val end = text.indexOf('\n', i).let { if (it < 0) text.length else it }
                        withStyle(SpanStyle(color = colors.comment)) { append(text.substring(i, end)) }
                        i = end
                    }
                    text[i] == '"' || text[i] == '\'' || (js && text[i] == '`') -> {
                        val q = text[i]
                        var j = i + 1
                        while (j < text.length && text[j] != q) {
                            if (text[j] == '\\' && j + 1 < text.length) j += 2 else j++
                        }
                        if (j < text.length) j++
                        withStyle(SpanStyle(color = colors.string)) { append(text.substring(i, j)) }
                        i = j
                    }
                    text[i].isLetter() || text[i] == '_' || (!js && text[i] == '-') -> {
                        var j = i + 1
                        while (j < text.length && (text[j].isLetterOrDigit() || text[j] == '_' || text[j] == '-')) j++
                        val w = text.substring(i, j)
                        if (w in kw) withStyle(SpanStyle(color = colors.keyword, fontWeight = FontWeight.Medium)) { append(w) }
                        else withStyle(SpanStyle(color = colors.default)) { append(w) }
                        i = j
                    }
                    text[i].isDigit() -> {
                        var j = i
                        while (j < text.length && (text[j].isDigit() || text[j] == '.' || text[j] == '%')) j++
                        withStyle(SpanStyle(color = colors.number)) { append(text.substring(i, j)) }
                        i = j
                    }
                    else -> {
                        withStyle(SpanStyle(color = colors.operator)) { append(text[i]) }
                        i++
                    }
                }
            }
        }
    }
}
