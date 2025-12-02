package com.ltb.woordle.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.regex.Pattern;

public class WordValidator {

    /**
     * Regex to allow only a-z and A-Z letters in fetched and guessed words.
     */
    private static final Pattern ALPHABETIC_PATTERN = Pattern.compile("^[a-zA-Z]+$");

    @Contract("null -> fail")
    public static boolean isValidAlphabeticWord(String word) {
        if (word == null || word.isEmpty()) {
            throw new IllegalArgumentException("Word passed to alphabetic validation cannot be null or empty.");
        }
        return ALPHABETIC_PATTERN.matcher(word).matches();
    }

    @Contract("null -> fail")
    @NotNull
    public static String normalizeWord(String word) {
        if (word == null || word.isEmpty()) {
            throw new IllegalArgumentException("Word passed to normalize cannot be null or empty.");
        }
        return word.toLowerCase(Locale.ROOT);
    }
}
