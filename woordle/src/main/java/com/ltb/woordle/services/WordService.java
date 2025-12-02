package com.ltb.woordle.services;

import com.ltb.woordle.exceptions.WordServiceException;
import com.ltb.woordle.models.Word;
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

@Service
public class WordService {

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

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

    private HttpEntity<Void> createRequestEntity() {
        // RapidAPI requires these headers for authentication
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-rapidapi-host", hostHeader);
        headers.set("x-rapidapi-key", apiKey);
        return new HttpEntity<>(headers);
    }

    public String getRandomWord() {

        /*
         For MVP, gets a random 5-letter word from the dictionary API using getRandomWord(int length).
         */

        return getRandomWord(5);

    }

    /**
     * Fetches a random word of the specified length from the dictionary API.
     *
     * @param length length of the desired word; must be between 1 and 15.
     * @return a random word of the specified length.
     * @throws IllegalArgumentException if length is invalid or no word is available.
     * @throws WordServiceException if there is an error communicating with the dictionary API.
     */
    public String getRandomWord(int length) {

        if (length <= 0 || length > 15) {
            throw new IllegalArgumentException("Length must be a positive integer no greater than 20.");
        }

        try {
            HttpEntity<Void> requestEntity = createRequestEntity();
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/words/?letters=" + length + "&random=true",
                    HttpMethod.GET, requestEntity, String.class);

            if (!response.hasBody() || response.getStatusCode().is4xxClientError()) {
                throw new IllegalArgumentException("No words of length " + length + " available");
            } else {
                // Parse the response body to extract the word
                Word wordObj = objectMapper.readValue(response.getBody(), Word.class);
                if (wordObj != null) {
                    wordObj.populateCharacters();
                    return wordObj.getWord();
                } else {
                    throw new WordServiceException("Received invalid word from dictionary API");
                }
            }
        } catch (RestClientException | IOException e) {
            throw new WordServiceException("Failed to fetch or parse random word from dictionary API", e);
        }

    }

    public List<Character> handleGuess(List<Character> characters, String answer) {

        if (characters == null || characters.isEmpty() || answer == null || answer.isEmpty()) {
            throw new IllegalArgumentException("Characters and answer cannot be null or empty.");
        }

        // First, concatenate the player's guessed characters into a String
        String guess = concatenateGuess(characters);

        // Instantiate an ArrayList<Character> to hold feedback to be returned
        List<Character> feedback = new ArrayList<>();

        // If the guessed word is valid, check it against the answer
        if (isValidWord(guess)) {
            if (isCorrectWord(guess, answer)) {

                // If the guess is exactly correct, return all 'C's
                for (int i = 0; i < guess.length(); i++) {
                    feedback.add('C');
                }

            // Else, check letters for presence and position
            } else {
                feedback = checkLetters(guess, answer);
            }
        }

        return feedback;

    }

    public String concatenateGuess(List<Character> characters) {

        if (characters == null || characters.isEmpty()) {
            throw new IllegalArgumentException("Character list cannot be null or empty.");
        }

        // Concatenate characters into a String
        StringBuilder guess = new StringBuilder();
        for (Character character : characters) {
            guess.append(character);
        }
        return guess.toString();
    }

    public boolean isValidWord(String guess) {

        /*
         Method to validate player guess.
         Searches dictionary API for the guessed word.
         Returns boolean based on API response.
        */

        if (guess == null || guess.isEmpty()) {
            return false;
        }

        try {
            HttpEntity<Void> requestEntity = createRequestEntity();
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl +"/" + guess,
                    HttpMethod.GET, requestEntity, String.class);

            // wordsapi returns a 404 if a word is not present
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            throw new WordServiceException("Failed to validate word \"" + guess + "\"", e);
        }

    }

    public boolean isCorrectWord(String guess, String answer) {

        if (guess == null || answer == null) {
            throw new IllegalArgumentException("Guess and answer cannot be null.");
        }

        return Objects.equals(guess, answer);
    }

    public List<Character> checkLetters(String guess, String answer) {

        /*
         This greater method will check each letter in String guess
         for presence and position accuracy within String answer by looping through each.
        */

        if (guess == null || answer == null || guess.length() != answer.length()) {
            throw new IllegalArgumentException("Guess and answer must be non-null and of the same length.");
        }

        // First, instantiate an ArrayList<Character> of coded feedback characters to be mapped onto.
        // This array will mark letters in String guess as
        // 'C', 'A', or 'P' for Correct, Absent, or Present, respectively
        ArrayList<Character> feedbackArray = new ArrayList<>();
        for (int i = 0; i < guess.length(); i++) {
            feedbackArray.add('A'); // default characters to 'A' for Absent
        }

        // Second, convert String guess and String answer to ArrayList<Character>
        ArrayList<Character> guessArray = new ArrayList<>();
        for (char c : guess.toCharArray()) {
            guessArray.add(c);
        }
        ArrayList<Character> answerArray = new ArrayList<>();
        for (char c : answer.toCharArray()) {
            answerArray.add(c);
        }

        // counts HashMap keeps track of non-matching characters
        Map<Character, Integer> counts = new HashMap<>();

        // First pass: Loop through guessArray and answerArray,
        // marking letters as 'C' for Correct in feedbackArray
        for (int i = 0; i < guessArray.size(); i++) {

            char g = guessArray.get(i);
            char a = answerArray.get(i);

            if (g == a) {
                feedbackArray.set(i, 'C');
            } else {
                // count only answer letters that were NOT correct
                counts.put(a, counts.getOrDefault(a, 0) + 1);
            }

        }

        // Second pass: Loop through guessArray and answerArray,
        // marking letters as 'P' for Present or 'A' for Absent in feedbackArray
        for (int i = 0; i < guessArray.size(); i++) {
            if (feedbackArray.get(i) == 'C') {
                continue; // because this is already handled
            }

            char g = guessArray.get(i);
            Integer count = counts.get(g);
            if (count != null && count > 0) {
                feedbackArray.set(i, 'P');      // Present but wrong position
                counts.put(g, count - 1);       // consume one occurrence
            } else {
                feedbackArray.set(i, 'A');      // Absent (already default, but make explicit)
            }
        }

        return feedbackArray;
    }

}