package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.entity.mapper.ParamsJsonConverter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Entity
@Table(name = "requests", schema = "dbo",
        uniqueConstraints = @UniqueConstraint(name = "uq_requests_number", columnNames = "request_number"))
@Getter
@Setter
@NoArgsConstructor
public class Req {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "request_number", nullable = false, length = 50)
    private String requestNumber;

    @Column(name = "request_text", columnDefinition = "NVARCHAR(MAX)")
    private String requestText;

    @Column(length = 50)
    private String tid;

    @Column(length = 50)
    private String phone;

    @Column(length = 20)
    private String priority;

    @Column(name = "creation_time", nullable = false)
    private LocalDateTime creationTime = LocalDateTime.now();

    @Column(name = "request_address", length = 500)
    private String requestAddress;

    @Column(length = 50)
    private String sla;

    @Column(length = 50)
    private String contragent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReqStatus status = ReqStatus.NEW;

    @OneToMany(mappedBy = "req",
            cascade = CascadeType.ALL,
            orphanRemoval = false,
            fetch = FetchType.LAZY)
    private List<Equipment> equipment = new ArrayList<>();

    public void addEquipment(Equipment eq) {
        equipment.add(eq);
        eq.setReq(this);
    }
    @Convert(converter = ParamsJsonConverter.class)
    @Column(name = "params_json", columnDefinition = "NVARCHAR(MAX)")
    private Map<String, String> params = new HashMap<>();

    public void setParams(String key, String val) {
        this.params.put(key, val);
    }
    public String getParam(String key) {
        return this.params.get(key);
    }
    private static void appendIfNotNull(StringBuilder sb, String fieldName, Object value) {
        if (value != null) {
            sb.append("\n").append(fieldName).append(": ").append(value);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Req req)) return false;
        return Objects.equals(requestNumber, req.requestNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(requestNumber);
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        String formattedDate = creationTime != null ? creationTime.format(formatter) : null;
        StringBuilder textBuilder = new StringBuilder();
        textBuilder.append(requestNumber).append("\n");
        textBuilder.append(contragent).append("\n");
        textBuilder.append(requestText);

        appendIfNotNull(textBuilder, "SN ADM", params.get("params[591]"));
        appendIfNotNull(textBuilder, "БАНК", params.get("владелец"));
        appendIfNotNull(textBuilder, "TID", tid);
        appendIfNotNull(textBuilder, "Клиент", params.get("клиент"));
        appendIfNotNull(textBuilder, "Тел", phone);
        appendIfNotNull(textBuilder, "priority", priority);
        appendIfNotNull(textBuilder, "Время создания", formattedDate);
        appendIfNotNull(textBuilder, "Адрес", requestAddress);
        appendIfNotNull(textBuilder, "sla", sla);
        appendIfNotNull(textBuilder, "Время работы", params.get("Режим работы"));



        return textBuilder.toString();
    }
}