package com.ltb.woordle.services;

import com.ltb.woordle.exceptions.DictionaryServiceException;
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
import static com.ltb.woordle.utils.WordValidator.*;

import java.io.IOException;

@Service
public class DictionaryService {

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

    public DictionaryService(RestTemplate restTemplate, ObjectMapper objectMapper) {
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
     * @throws DictionaryServiceException     if there is an error communicating with the dictionary API.
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
                    throw new DictionaryServiceException("Received invalid word from dictionary API");
                }

                wordObj.populateCharacters();
                word = normalizeWord(wordObj.getWord());

            } while (!isValidAlphabeticWord(word));

            return word;

        } catch (RestClientException | IOException e) {
            throw new DictionaryServiceException("Failed to fetch or parse random word from dictionary API", e);
        }
    }

    @Contract("null -> fail")
    boolean isValidDictionaryWord(String guess) {

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

            // WordsAPI returns a 2xx status if a word is present
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            throw new DictionaryServiceException("Failed to validate word \"" + guess + "\"", e);
        }

    }

}
