package org.example.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Update {
    @JsonProperty("callback")
    private CallBack callBack;
    private Message message;
    private long timestamp; // в мс
    private String userLocale;
    private String updateType;



}