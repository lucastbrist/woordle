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

    /**
     * Greater method handling a player's guess.
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
     * 'C' for Correct position, 'P' for Present but wrong position, and 'A' for Absent.
     *
     * @param guess  the guessed word
     * @param answer the stored answer word
     * @return a List of Characters representing feedback for each letter in the guess
     */
    @Contract("null, _ -> fail; !null, null -> fail")
    private @NotNull List<Character> checkLetters(String guess, String answer) {

        if (guess == null || answer == null
                || guess.isEmpty() || answer.isEmpty()
                || guess.length() != answer.length()) {
            throw new IllegalArgumentException("Guess and answer passed to checkLetters() must be non-null, " +
                    "non-empty, and of the same length.");
        }

        ArrayList<Character> feedbackArray = new ArrayList<>();
        for (int i = 0; i < guess.length(); i++) {
            feedbackArray.add('A'); // default characters to 'A' for Absent
        }

        // counts HashMap keeps track of non-matching characters
        Map<Character, Integer> counts = new HashMap<>();

        // First pass: mark correct positions
        for (int i = 0; i < guess.length(); i++) {
            char g = guess.charAt(i);
            char a = answer.charAt(i);

            if (g == a) {
                feedbackArray.set(i, 'C');
            } else {
                counts.put(a, counts.getOrDefault(a, 0) + 1);
            }
        }

        // Second pass: mark present/absent
        for (int i = 0; i < guess.length(); i++) {
            if (feedbackArray.get(i) == 'C') continue;      // because this is already handled

            char g = guess.charAt(i);
            Integer count = counts.get(g);
            if (count != null && count > 0) {
                feedbackArray.set(i, 'P');      // Present but wrong position
                counts.put(g, count - 1);       // Consume one occurrence
            } else {
                feedbackArray.set(i, 'A');      // Default but make explicit
            }
        }

        return feedbackArray;

    }

}