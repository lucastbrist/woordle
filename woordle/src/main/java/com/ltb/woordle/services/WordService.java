package com.ltb.woordle.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class WordService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${dictionary.api.key}")
    private String apiKey;

    public String getRandomWord() {

        /*
         Gets a random word from the dictionary API.
         It can take any int length in its constructor, but for MVP,
         it is defaulted to a length of 5.
         */

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-rapidapi-host", "wordsapiv1.p.rapidapi.com");
            headers.set("x-rapidapi-key", apiKey);

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://wordsapiv1.p.rapidapi.com/words/?letters=5&random=true",
                    HttpMethod.GET, requestEntity, String.class);

            if (!response.hasBody() || response.getStatusCode().is4xxClientError()) {
                throw new IllegalArgumentException("No words available");
            } else {
                return response.getBody();
            }
        } catch (RestClientException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }

    }

    public String getRandomWord(int length) {

        /*
         Gets a random word from the dictionary API.
         It can take any int length in its constructor, but for MVP,
         it is defaulted to a length of 5.
         */

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-rapidapi-host", "wordsapiv1.p.rapidapi.com");
        headers.set("x-rapidapi-key", apiKey);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "https://wordsapiv1.p.rapidapi.com/words/?letters=" + length + "&random=true",
                HttpMethod.GET, requestEntity, String.class);

        if (!response.hasBody() || response.getStatusCode().is4xxClientError()) {
            throw new IllegalArgumentException("No words of length " + length + " available");
        } else {
            return response.getBody();
        }

    }

//    public void handleGuess(ArrayList<Character> characters, String answer) {
//        String guess = concatenateGuess(characters);
//        if (isValidWord(guess)) {
//            if (isCorrectWord(guess, answer)) {
//                // success!!!
//            } else {
//                // check if letters are correct
//            }
//        }
//    }

    public String concatenateGuess(ArrayList<Character> characters) {

        /*
         Takes in the guessed characters and returns them as a String.
         */

        if (characters == null || characters.isEmpty()) {
            return "";
        }

        StringBuilder guess = new StringBuilder();
        for (Character character : characters) {
            guess.append(character);
        }
        return String.valueOf(guess);
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

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-rapidapi-host", "wordsapiv1.p.rapidapi.com");
        headers.set("x-rapidapi-key", apiKey);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "https://wordsapiv1.p.rapidapi.com/words/" + guess,
                HttpMethod.GET, requestEntity, String.class);

        // wordsapi returns a 404 if a word is not present
        return response.hasBody() && !response.getStatusCode().is4xxClientError();

    }

    public boolean isCorrectWord(String guess, String answer) {

        /*
         Simply checks String guess for exactly matching String answer.
         */

        if (guess == null || answer == null) {
            return false;
        }

        return Objects.equals(guess, answer);
    }

    public ArrayList<Character> checkLetters(String guess, String answer) {

        /*
         This greater method will check each letter in String guess
         for presence and position accuracy within String answer by looping through each.
        */

        // First, instantiate an ArrayList<Character> of coded feedback characters to be mapped onto.
        // This array will mark letters in String guess as
        // 'C', 'A', or 'P' for Correct, Absent, or Present, respectively

        if (guess == null || answer == null || guess.length() != answer.length()) {
            throw new IllegalArgumentException("Guess and answer must be non-null and of the same length.");
        }

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