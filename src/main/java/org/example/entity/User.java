package org.example.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("id")
    private Integer id;
    @Column(name = "user_bot_id", nullable = false, unique = true)
    @JsonProperty("user_id")
    private long userBotId;
    @Column(name = "chat_id")
    @JsonProperty("chat_id")
    private long chatId;
    @Column(name = "first_name")
    @JsonProperty("first_name")
    private String firstName;
    @Column(name = "last_name")
    @JsonProperty("last_name")
    private String lastName;
    @JsonProperty("is_bot")
    private boolean isBot;
    @JsonProperty("last_activity_time")
    private Long lastActivityTime; // в мс
    private String name;


}