package com.ltb.woordle.models;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Game {

    private String entry;
    private Word answer;
    private int attempts;
    private int length;

}
