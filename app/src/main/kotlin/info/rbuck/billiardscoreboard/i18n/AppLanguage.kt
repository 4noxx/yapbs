package info.rbuck.billiardscoreboard.i18n

/**
 * The 4 languages the app language switcher in Settings offers. Drives both [Strings] (the growing
 * general UI catalog) and [Translations] (the larger templated tournament / training-rule /
 * rebuild-rule copy). German is fully translated; ES/FR are complete for [Translations] but still
 * fall back to English for anything only in [Strings].
 */
enum class AppLanguage(val code: String, val label: String) {
    EN("en", "English"),
    DE("de", "Deutsch"),
    ES("es", "Español"),
    FR("fr", "Français"),
    ;

    companion object {
        fun fromCode(code: String?): AppLanguage = entries.firstOrNull { it.code == code } ?: EN
    }
}
