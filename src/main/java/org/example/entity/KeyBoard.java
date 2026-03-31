package org.example.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class KeyBoard {
    private final List<List<Button>> buttons;


    public KeyBoard(List<Button> buttons) {

        List<Button> row = new ArrayList<>();
        row.addAll(buttons);
        this.buttons = new ArrayList<>();
        this.buttons.add(row);


    }
}
