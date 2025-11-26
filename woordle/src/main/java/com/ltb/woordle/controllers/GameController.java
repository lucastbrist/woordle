package com.ltb.woordle.controllers;

import org.springframework.web.bind.annotation.GetMapping;

public class GameController {

    @GetMapping("/play")
    public String playGame() {
        return "game";
    }

}
