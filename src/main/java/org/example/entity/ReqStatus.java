package org.example.entity;

public enum ReqStatus {

    /**
     * Заявка создана, но ещё не взята в работу.
     */
    NEW("Новая", "new"),

    /**
     * Закрыта — работа выполнена.
     */
    CLOSED("Закрыта", "closed"),

    /**
     * Закрыта — работа выполнена.
     */
    CLOSED_BY_ENGINEER("Закрыта инженером", "closedByEngineer"),
    /**
     * Отменена — не требует выполнения.
     */
    CANCELLED("Отменена", "cancelled");

    private final String displayName;
    private final String code;

    ReqStatus(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }
}