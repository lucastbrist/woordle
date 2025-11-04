package com.ltb.woordle.models;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @JsonProperty("userId")
    private Long id;
    private String username;
    private String password;

    private int points;
    private int gamesWon;
    private int gamesLost;

}
