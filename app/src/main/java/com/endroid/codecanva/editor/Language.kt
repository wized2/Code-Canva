package com.endroid.codecanva.editor

enum class Language(val displayName: String, val extensions: List<String>) {
    PLAIN("Plain Text", listOf("txt", "text", "log")),
    KOTLIN("Kotlin", listOf("kt", "kts")),
    JAVA("Java", listOf("java")),
    XML("XML", listOf("xml", "html", "htm", "svg")),
    JSON("JSON", listOf("json")),
    JAVASCRIPT("JavaScript", listOf("js", "mjs", "cjs")),
    TYPESCRIPT("TypeScript", listOf("ts", "tsx")),
    PYTHON("Python", listOf("py", "pyw")),
    MARKDOWN("Markdown", listOf("md", "markdown")),
    SQL("SQL", listOf("sql")),
    CSS("CSS", listOf("css", "scss")),
    SHELL("Shell", listOf("sh", "bash", "zsh")),
    C("C/C++", listOf("c", "cpp", "h", "hpp", "cc")),
    RUST("Rust", listOf("rs")),
    GO("Go", listOf("go")),
    YAML("YAML", listOf("yml", "yaml")),
    TOML("TOML", listOf("toml"));

    companion object {
        fun fromFileName(name: String): Language {
            val ext = name.substringAfterLast('.', "").lowercase()
            return entries.find { ext in it.extensions } ?: PLAIN
        }
    }
}
