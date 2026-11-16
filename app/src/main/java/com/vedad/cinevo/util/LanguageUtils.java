package com.vedad.cinevo.util;

import android.content.Context;

import java.util.Locale;

public class LanguageUtils {

    public static final String[] FILTER_LANGUAGES = {
            "en", "ko", "ja", "fr", "es", "de", "it", "hi", "zh", "cn", "tr", "ru", "pt",
            "bs", "hr", "sr", "sl", "mk",
            "pl", "nl", "sv", "da", "no", "fi", "el", "cs", "hu", "ro", "bg", "uk",
            "ar", "fa", "he", "th", "id", "vi"
    };

    public static boolean isKnown(String code) {
        return code != null && !code.isEmpty() && !"xx".equals(code);
    }

    public static String displayName(Context context, String code) {
        if (!isKnown(code)) return "";

        Locale appLocale = context.getResources().getConfiguration().getLocales().get(0);
        String tag = "cn".equals(code) ? "yue" : code;
        String name = Locale.forLanguageTag(tag).getDisplayLanguage(appLocale);

        if (name.isEmpty() || name.equalsIgnoreCase(tag)) {
            return code.toUpperCase(Locale.ROOT);
        }
        return name.substring(0, 1).toUpperCase(appLocale) + name.substring(1);
    }
}