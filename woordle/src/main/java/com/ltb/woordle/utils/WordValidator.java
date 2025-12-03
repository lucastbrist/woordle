package com.ltb.woordle.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.regex.Pattern;

public final class WordValidator {

    private WordValidator() {
        throw new UnsupportedOperationException("WordValidator is a utility class and is not meant to be instantiated.");
    }

    /**
     * Regex to allow only a-z and A-Z letters in fetched and guessed words.
     */
    private static final Pattern ALPHABETIC_PATTERN = Pattern.compile("^[a-zA-Z]+$");

    /**
     * Validates that a word contains only alphabetic characters (a-z, A-Z).
     *
     * @param word the word to validate
     * @return true if word contains only letters, false otherwise
     * @throws IllegalArgumentException if word is null or empty
     */
    @Contract("null -> fail")
    public static boolean isValidAlphabeticWord(String word) {
        if (word == null || word.isEmpty()) {
            throw new IllegalArgumentException("Word passed to alphabetic validation cannot be null or empty.");
        }
        return ALPHABETIC_PATTERN.matcher(word).matches();
    }

    /**
     * Normalizes a word by trimming whitespace and converting to lowercase.
     *
     * @param word the word to normalize
     * @return the normalized word
     * @throws IllegalArgumentException if word is null, empty, or only whitespace
     */
    @Contract("null -> fail")
    @NotNull
    public static String normalizeWord(String word) {
        if (word == null) {
            throw new IllegalArgumentException("Word cannot be null.");
        }

        String trimmed = word.trim();

        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Word cannot be empty or whitespace.");
        }

        return trimmed.toLowerCase(Locale.ROOT);
    }
}
