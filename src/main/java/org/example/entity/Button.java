package org.example.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Button {
    private String type;
    private String text;
    private String payload;

    public Button(String type, String text, String payload) {
       this.type = type;
       this.text = text;
       this.payload = payload;
    }
}
