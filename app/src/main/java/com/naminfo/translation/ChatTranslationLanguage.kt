package com.naminfo.translation

enum class ChatTranslationLanguage(
    val code: String?,
    val label: String,
    val apiValue: String
) {
    NONE(null, "None", "none"),
    TAMIL("ta", "Tamil", "tamil"),
    ENGLISH("en", "English", "english"),
    HINDI("hi", "Hindi", "hindi"),
    CHINESE("zh", "Chinese", "chinese"),
    ARABIC("ar", "Arabic", "arabic"),
    SPANISH("es", "Spanish", "spanish");

    companion object {
        fun fromCode(code: String?): ChatTranslationLanguage {
            if (code.isNullOrEmpty()) return NONE
            val trimmed = code.trim()
            return entries.find {
                it.name.equals(trimmed, ignoreCase = true) ||
                        it.label.equals(trimmed, ignoreCase = true) ||
                        it.apiValue.equals(trimmed, ignoreCase = true) ||
                        (it.code != null && it.code.equals(trimmed, ignoreCase = true))
            } ?: NONE
        }
    }
}
