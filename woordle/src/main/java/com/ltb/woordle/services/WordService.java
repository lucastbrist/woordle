package com.ltb.woordle.services;

import com.ltb.woordle.exceptions.WordServiceException;
import com.ltb.woordle.models.Word;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class WordService {

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

    /**
     * Regex to allow only a-z and A-Z letters in fetched and guessed words.
     */
    private static final Pattern ALPHABETIC_PATTERN = Pattern.compile("^[a-zA-Z]+$");

    public WordService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Value("${dictionary.api.key}")
    private String apiKey;

    @Value("${dictionary.base-url}")
    private String baseUrl;

    @Value("${dictionary.host}")
    private String hostHeader;

    @Contract(" -> new")
    private @NotNull HttpEntity<Void> createRequestEntity() {
        // RapidAPI requires these headers for authentication
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-rapidapi-host", hostHeader);
        headers.set("x-rapidapi-key", apiKey);
        return new HttpEntity<>(headers);
    }

    /**
     * For MVP, fetches a random 5-letter word by wrapping a more flexible API-calling method.
     *
     * @return a String of length 5 from the dictionary API.
     */
    @NotNull
    public String getRandomWord() {
        return getRandomWord(5);
    }

    /**
     * Fetches a random word of the specified length from the dictionary API.
     *
     * @param length length of the desired word; must be between 1 and 15.
     * @return a random word of the specified length.
     * @throws IllegalArgumentException if length is invalid or no word is available.
     * @throws WordServiceException     if there is an error communicating with the dictionary API.
     */
    @NotNull
    public String getRandomWord(int length) {
        if (length <= 0 || length > 15) {
            throw new IllegalArgumentException("Length must be a positive integer no greater than 15.");
        }

        try {

            HttpEntity<Void> requestEntity = createRequestEntity();
            String word;

            do {
                ResponseEntity<String> response = restTemplate.exchange(
                        baseUrl + "/words/?letters=" + length + "&random=true",
                        HttpMethod.GET, requestEntity, String.class);

                if (!response.hasBody() || response.getStatusCode().is4xxClientError()) {
                    throw new IllegalArgumentException("No words of length " + length + " available");
                }

                Word wordObj = objectMapper.readValue(response.getBody(), Word.class);

                if (wordObj == null) {
                    throw new WordServiceException("Received invalid word from dictionary API");
                }

                wordObj.populateCharacters();
                word = normalizeWord(wordObj.getWord());

            } while (!isValidAlphabeticWord(word));

            return word;

        } catch (RestClientException | IOException e) {
            throw new WordServiceException("Failed to fetch or parse random word from dictionary API", e);
        }
    }

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
        if (isValidAlphabeticWord(normalizedGuess) && isValidDictionaryWord(normalizedGuess)) {
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

        return feedback;

    }

    @Contract("null -> fail")
    private @NotNull String normalizeWord(String word) {
        if (word == null || word.isEmpty()) {
            throw new IllegalArgumentException("Word passed to normalize cannot be null or empty.");
        }
        return word.toLowerCase(Locale.ROOT);
    }

    @Contract("null -> fail")
    private @NotNull String concatenateGuess(List<Character> characters) {

        if (characters == null || characters.isEmpty()) {
            throw new IllegalArgumentException("Character list cannot be null or empty.");
        }

        StringBuilder guess = new StringBuilder();
        for (Character character : characters) {
            guess.append(character);
        }
        return guess.toString();
    }

    @Contract("null -> fail")
    private boolean isValidAlphabeticWord(String word) {
        if (word == null || word.isEmpty()) {
            throw new IllegalArgumentException("Word passed to alphabetic validation cannot be null.");
        }
        return ALPHABETIC_PATTERN.matcher(word).matches();
    }

    @Contract("null -> fail")
    private boolean isValidDictionaryWord(String guess) {

        /*
         Method to validate player guess.
         Searches dictionary API for the guessed word.
         Returns boolean based on API response.
        */

        if (guess == null || guess.isEmpty()) {
            throw new IllegalArgumentException("Word passed to dictionary validation cannot be null or empty.");
        }

        try {
            HttpEntity<Void> requestEntity = createRequestEntity();
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/" + guess,
                    HttpMethod.GET, requestEntity, String.class);

            // WordsAPI returns a 404 if a word is not present
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            throw new WordServiceException("Failed to validate word \"" + guess + "\"", e);
        }

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