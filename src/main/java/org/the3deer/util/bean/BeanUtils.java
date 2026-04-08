package org.the3deer.util.bean;

/**
 * Copyright 2026 The Android Open Source Project
 *
 * @author andresoviedo
 * @author Gemini AI
 */
public class BeanUtils {

    /**
     * Convert a class name to snake case.
     * @param clazz The class to convert
     * @return The snake case string
     */
    public static String getSnakeCase(Class<?> clazz) {
        String name = clazz.getSimpleName();
        if (name.isEmpty()) return "";

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);

            if (i > 0 && Character.isUpperCase(c)) {
                char prev = name.charAt(i - 1);
                char next = (i + 1 < name.length()) ? name.charAt(i + 1) : '\0';

                // Add underscore if:
                // 1. Transition from lowercase to uppercase (e.g., 'i' to 'D' in guiDefault)
                // 2. Transition from acronym to start of new word (e.g., 'I' to 'D' in GUIDefault)
                if (Character.isLowerCase(prev) || (Character.isUpperCase(prev) && Character.isLowerCase(next))) {
                    result.append('_');
                }
            }
            result.append(Character.toLowerCase(c));
        }
        return result.toString();
    }
}
