package com.classroomassistant.android.ui

import java.util.Locale

enum class AppLanguage(val code: String) {
    AUTO("AUTO"),
    ZH_CN("ZH_CN"),
    EN_US("EN_US");

    companion object {
        fun fromCode(raw: String?): AppLanguage {
            if (raw.isNullOrBlank()) {
                return AUTO
            }
            return values().firstOrNull { it.code.equals(raw.trim(), ignoreCase = true) } ?: AUTO
        }
    }
}

fun resolveEffectiveLanguage(selection: AppLanguage, locale: Locale = Locale.getDefault()): AppLanguage {
    if (selection != AppLanguage.AUTO) {
        return selection
    }
    return if (locale.language.lowercase(Locale.ROOT).startsWith("zh")) {
        AppLanguage.ZH_CN
    } else {
        AppLanguage.EN_US
    }
}

fun tr(language: AppLanguage, zhText: String, enText: String): String {
    return if (language == AppLanguage.ZH_CN) zhText else enText
}
