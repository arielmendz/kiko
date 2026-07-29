package com.kiko.app;

import java.util.List;
import java.util.Locale;

public final class SpeechLanguageSelector {
    public static final String PREFERRED_SPANISH = "es-US";

    private SpeechLanguageSelector() {
    }

    public static String selectSpanish(List<String> languageTags) {
        if (languageTags == null) {
            return null;
        }

        for (String languageTag : languageTags) {
            if (PREFERRED_SPANISH.equalsIgnoreCase(languageTag)) {
                return languageTag;
            }
        }

        for (String languageTag : languageTags) {
            if (languageTag != null
                    && "es".equals(Locale.forLanguageTag(languageTag).getLanguage())) {
                return languageTag;
            }
        }
        return null;
    }
}

