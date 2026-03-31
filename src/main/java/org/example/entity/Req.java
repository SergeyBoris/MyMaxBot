package org.example.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.entity.Contragents.Contragent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Req {
    @JsonProperty("request_number")
    private String requestNumber;
    @JsonProperty("request_text")
    private String requestText;
    @JsonProperty("tid")
    private String TID;
    @JsonProperty("phone")
    private String phone;
    @JsonProperty("priority")
    private String priority;
    @JsonProperty("creation_time")
    private LocalDateTime creationTime;
    @JsonProperty("request_address")
    private String requestAddress;
    @JsonProperty("to_users")
    private List<Long> toUsersId;
    @JsonProperty("contragent")
    private Contragent contragent;
    @JsonProperty("sla")
    private String sla;
    @JsonIgnore
    private boolean actual;

    private static void appendIfNotNull(StringBuilder sb, String fieldName, Object value) {
        if (value != null) {
            sb.append("\n").append(fieldName).append(": ").append(value);
        }
    }
    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        String formattedDate = creationTime != null ? creationTime.format(formatter) : null;
        StringBuilder textBuilder = new StringBuilder();
        textBuilder.append(requestNumber).append("\n");
        textBuilder.append(requestText);

        appendIfNotNull(textBuilder, "TID", TID);
        appendIfNotNull(textBuilder, "Тел", phone);
        appendIfNotNull(textBuilder, "priority", priority);
        appendIfNotNull(textBuilder, "Время создания", formattedDate);
        appendIfNotNull(textBuilder, "Адрес", requestAddress);
        appendIfNotNull(textBuilder, "contragent", contragent);
        appendIfNotNull(textBuilder, "sla", sla);



        return textBuilder.toString();
    }
}
