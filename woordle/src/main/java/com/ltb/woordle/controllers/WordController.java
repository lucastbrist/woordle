package com.ltb.woordle.controllers;

import com.ltb.woordle.services.WordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WordController {

    @Autowired
    WordService wordService;

    @GetMapping("/random")
    public String getRandomWord() {
        return wordService.getRandomWord();
    }

    @GetMapping("/random")
    public String getRandomWord(@RequestParam int length) {
        return wordService.getRandomWord(length);
    }

}
