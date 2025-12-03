package com.ltb.woordle.services;

import com.ltb.woordle.exceptions.DictionaryServiceException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import static com.ltb.woordle.utils.WordValidator.*;

import java.util.*;

@Service
public class WordService {

    @Autowired
    DictionaryService dictionaryService;

    private static final char CORRECT = 'C';
    private static final char ABSENT = 'A';
    private static final char PRESENT = 'P';

    /**
     * Primary method handling a player's guess.
     * Begins by validating the lowercased guess against a regex and the dictionary API.
     * If valid, checks the guess against the answer for correctness and letter presence.
     *
     * @param characters List of Characters representing the player's guess.
     * @param answer     The correct answer word.
     * @return A List of Characters representing feedback: 'C' for Correct, 'P' for Present, 'A' for Absent.
     * @throws IllegalArgumentException if input is invalid.
     */
    @NotNull
    public List<Character> handleGuess(List<Character> characters, String answer) {

        if (characters == null || characters.isEmpty() || answer == null || answer.isEmpty()) {
            throw new IllegalArgumentException("Guessed characters and stored answer " +
                    "cannot be null or empty when handling guess.");
        }

        String normalizedGuess = normalizeWord(concatenateGuess(characters));
        String normalizedAnswer = normalizeWord(answer);
        List<Character> feedback = new ArrayList<>();

        // If the guessed word is valid, check it against the answer
        try {
            if (isValidAlphabeticWord(normalizedGuess) && dictionaryService.isValidDictionaryWord(normalizedGuess)) {
                // If the guess is exactly correct, return all 'C's
                if (isCorrectWord(normalizedGuess, normalizedAnswer)) {
                    for (int i = 0; i < normalizedGuess.length(); i++) {
                        feedback.add('C');
                    }
                // Else, check letters for presence and position
                } else {
                    feedback = checkLetters(normalizedGuess, normalizedAnswer);
                }
            } else {
                throw new IllegalArgumentException("Could not validate guess \"" + normalizedGuess + "\"");
            }
        } catch (IllegalArgumentException e) {
            throw new DictionaryServiceException("Could not validate guess \"" + normalizedGuess + "\"", e);
        }

        return feedback;

    }

    @Contract("null -> fail")
    @NotNull
    private String concatenateGuess(List<Character> characters) {

        if (characters == null || characters.isEmpty()) {
            throw new IllegalArgumentException("Character list cannot be null or empty.");
        }

        StringBuilder guess = new StringBuilder();
        for (Character character : characters) {
            guess.append(character);
        }
        return guess.toString();
    }

    @Contract("null, _ -> fail; !null, null -> fail")
    private boolean isCorrectWord(String guess, String answer) {

        if (guess == null || answer == null || guess.isEmpty() || answer.isEmpty()) {
            throw new IllegalArgumentException("Guess and answer passed to isCorrectWord() cannot be null or empty.");
        }
        return Objects.equals(guess, answer);
    }

    /**
     * Checks each letter in String guess for presence and position accuracy within String answer.
     * Returns feedback: 'C' for Correct position, 'P' for Present but wrong position, and 'A' for Absent.
     * <p>
     * <p> Algorithm:
     * <p> 1. Initialize feedback list with ABSENT for all letters
     * <p> 2. First pass: Mark all exact matches (correct position)
     * <p> 3. Count remaining letters in answer (the ones that weren't exact matches)
     * <p> 4. Second pass: For non-exact matches, check if letter exists in remaining pool
     * <p> 5. If yes, mark as PRESENT and decrement count. If no, make ABSENT explicit.
     *
     * @param guess  the guessed word
     * @param answer the stored answer word
     * @return a List of Characters representing feedback for each letter in the guess
     */
    @Contract("null, _ -> fail; !null, null -> fail")
    @NotNull
    private List<Character> checkLetters(String guess, String answer) {
        if (guess == null || answer == null || guess.length() != answer.length()) {
            throw new IllegalArgumentException("Guess and answer must be non-null and same length");
        }

        int length = guess.length();
        List<Character> feedback = new ArrayList<>(length);

        // Initialize all feedback as ABSENT
        for (int i = 0; i < length; i++) {
            feedback.add(ABSENT);
        }

        // Track how many of each letter are available in the answer
        int[] availableLetters = new int[26];

        // First pass: Mark correct positions
        for (int i = 0; i < length; i++) {
            if (guess.charAt(i) == answer.charAt(i)) {
                feedback.set(i, CORRECT);
            } else {
                // This letter in the answer is available for yellow matches
                availableLetters[answer.charAt(i) - 'a']++;
            }
        }

        // Second pass: Mark present letters from the remaining pool
        for (int i = 0; i < length; i++) {
            // Skip letters we already marked as correct
            if (feedback.get(i) == CORRECT) {
                continue;
            }

            char guessLetter = guess.charAt(i);
            int letterIndex = guessLetter - 'a';

            // If the letter exists in the available pool, mark as PRESENT
            if (availableLetters[letterIndex] > 0) {
                feedback.set(i, PRESENT);
                availableLetters[letterIndex]--;  // Use up one instance of this letter
            } else {
                feedback.set(i, ABSENT);          // Make ABSENT explicit
            }
        }

        return feedback;

    }

}