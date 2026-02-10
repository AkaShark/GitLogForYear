package com.git.log.common.git.model

import kotlinx.serialization.Serializable

// 编程语言枚举
@Serializable
enum class SourceLanguage(val displayName: String) {
    C("C"),
    CSHARP("C#"),
    CPP("C++"),
    CSS("CSS"),
    DART("Dart"),
    ELIXIR("Elixir"),
    GO("Go"),
    GROOVY("Groovy"),
    HTML("HTML"),
    JAVA("Java"),
    JAVASCRIPT("JavaScript"),
    KOTLIN("Kotlin"),
    OBJC("Objective-C"),
    PERL("Perl"),
    PHP("PHP"),
    POWERSHELL("PowerShell"),
    PYTHON("Python"),
    RUBY("Ruby"),
    RUST("Rust"),
    SCALA("Scala"),
    SHELL("Shell"),
    SWIFT("Swift"),
    TYPESCRIPT("TypeScript"),
    VUE("Vue");

    companion object {
        fun fromExtension(ex: String): SourceLanguage? {
            return when (ex.lowercase()) {
                "c" -> C
                "cs", "csharp" -> CSHARP
                "cpp", "cc", "cxx", "c++" -> CPP
                "css" -> CSS
                "dart" -> DART
                "ex" -> ELIXIR
                "go" -> GO
                "groovy", "gvy", "gy", "gsh" -> GROOVY
                "html", "htm" -> HTML
                "java", "jav", "j" -> JAVA
                "js", "jsx" -> JAVASCRIPT
                "kt", "ktm" -> KOTLIN
                "m", "mm" -> OBJC
                "pl" -> PERL
                "php" -> PHP
                "ps1" -> POWERSHELL
                "py" -> PYTHON
                "rb" -> RUBY
                "rs" -> RUST
                "scala", "sc" -> SCALA
                "sh", "command" -> SHELL
                "swift" -> SWIFT
                "ts", "tsx" -> TYPESCRIPT
                "vue" -> VUE
                else -> null
            }
        }

        fun fromFilePath(path: String): SourceLanguage? {
            val ext = path.substringAfterLast('.', "")
            return if (ext.isNotEmpty()) fromExtension(ext) else null
        }
    }
}