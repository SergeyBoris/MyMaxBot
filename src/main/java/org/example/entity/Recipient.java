package org.example.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class Recipient {
    @JsonProperty("chat_id")
    private long chatId;
    @JsonProperty("chat_type")
    private String chatType;
    @JsonProperty("user_id")
    private long userId = 111674804;
}
