package com.ltb.woordle.models;

import lombok.*;
import java.util.ArrayList;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Word {

    private String word;
    private ArrayList<Character> characters;
    private ArrayList<Character> currentFeedback;

    public void populateCharacters() {

        // Loop through String word to populate the characters ArrayList
        ArrayList<Character> tempCharacters = new ArrayList<>();
        for (int i = 0; i < this.word.length(); i++) {
            tempCharacters.add(this.word.charAt(i));
        }

        this.characters = tempCharacters;
    }

}
