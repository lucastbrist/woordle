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

//    public boolean isValidWord(String word) throws Exception {
//    }

//    public String generateFeedback(String guess, String target) {
//        guess = guess.toUpperCase();
//        target = target.toUpperCase();
//
//        StringBuilder feedback = new StringBuilder();
//        char[] targetChars = target.toCharArray();
//        char[] guessChars = guess.toCharArray();
//        boolean[] targetUsed = new boolean[target.length()];
//
//        // First pass: mark correct positions (green)
//        for (int i = 0; i < guess.length(); i++) {
//            if (guessChars[i] == targetChars[i]) {
//                feedback.append('C'); // Correct
//                targetUsed[i] = true;
//            } else {
//                feedback.append('?');
//            }
//        }
//
//        // Second pass: mark present letters (yellow)
//        for (int i = 0; i < guess.length(); i++) {
//            if (feedback.charAt(i) == '?') {
//                boolean found = false;
//                for (int j = 0; j < target.length(); j++) {
//                    if (!targetUsed[j] && guessChars[i] == targetChars[j]) {
//                        feedback.setCharAt(i, 'P'); // Present
//                        targetUsed[j] = true;
//                        found = true;
//                        break;
//                    }
//                }
//                if (!found) {
//                    feedback.setCharAt(i, 'A'); // Absent
//                }
//            }
//        }
//
//        return feedback.toString();
//    }
}