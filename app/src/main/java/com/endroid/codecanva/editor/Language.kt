package com.endroid.codecanva.editor

enum class Language(
    val displayName: String,
    val extensions: List<String>,
) {
    HTML("HTML", listOf("html", "htm")),
    KOTLIN("Kotlin", listOf("kt", "kts")),
    JAVA("Java", listOf("java")),
    PYTHON("Python", listOf("py")),
    JAVASCRIPT("JavaScript", listOf("js", "mjs")),
    TYPESCRIPT("TypeScript", listOf("ts", "tsx")),
    JSON("JSON", listOf("json")),
    XML("XML", listOf("xml")),
    C("C/C++", listOf("c", "cpp", "h", "hpp")),
    SQL("SQL", listOf("sql")),
    PLAIN("Plain", listOf("txt", "md")),
    ;

    companion object {
        fun fromFileName(name: String): Language {
            val ext = name.substringAfterLast('.', "").lowercase()
            return entries.firstOrNull { ext in it.extensions } ?: PLAIN
        }
    }
}
