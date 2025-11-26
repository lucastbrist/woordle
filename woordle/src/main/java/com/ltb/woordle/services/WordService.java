package com.ltb.woordle.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WordService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${dictionary.api.key}")
    private String apiKey;

    public String getRandomWord(int length) {

        // Gets a random word from the dictionary API.

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

    public void handleGuess(Character[] characters) {
        String guess = concatenateGuess(characters);
        if (isValidWord(guess)) {
            // check if word is then also the correct word
        }
    }

    public String concatenateGuess(Character[] characters) {

        // Takes in the guessed characters and returns them as a String.

        StringBuilder guess = new StringBuilder();
        for (Character character : characters) {
            guess.append(character);
        }
        return String.valueOf(guess);
    }

    public boolean isValidWord(String guess) {

        // Method to validate player guess.
        // Searches dictionary API for the guessed word.
        // Returns boolean based on API response.

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-rapidapi-host", "wordsapiv1.p.rapidapi.com");
        headers.set("x-rapidapi-key", apiKey);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "https://wordsapiv1.p.rapidapi.com/words/" + guess,
                HttpMethod.GET, requestEntity, String.class);

        return response.hasBody() && !response.getStatusCode().is4xxClientError();

    }
}