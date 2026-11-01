package org.example.entity.mapper;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.example.entity.Contragents.Contragent;
import org.example.entity.Dto.ReqMaxDto;
import org.example.entity.Req;
import org.example.services.ContragentFactory;

import java.time.LocalDateTime;
import java.util.HashMap;

@NoArgsConstructor
@AllArgsConstructor
public class ReqMapper {

    private ContragentFactory contragentFactory;

    // ============================================
    // ReqMaxDto → Req (entity)
    // ============================================
    public Req toEntity(ReqMaxDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("ReqMaxDto не может быть null");
        }

        // КРИТИЧНО: contragent обязателен
        if (dto.getContragent() == null) {
            throw new IllegalStateException(
                    "ReqMaxDto.contragent = null для заявки " + dto.getRequestNumber()
                            + " — контрагент обязателен"
            );
        }
        String contragentName = dto.getContragent().getContragentName();
        if (contragentName == null || contragentName.isBlank()) {
            throw new IllegalStateException(
                    "Contragent.getContragentName() вернул пустое значение для заявки "
                            + dto.getRequestNumber()
            );
        }

        Req entity = new Req();
        entity.setRequestNumber(dto.getRequestNumber());
        entity.setRequestText(dto.getRequestText());
        entity.setTid(dto.getTID());
        entity.setPhone(dto.getPhone());
        entity.setPriority(dto.getPriority());
        entity.setCreationTime(dto.getCreationTime() != null
                ? dto.getCreationTime()
                : LocalDateTime.now());
        entity.setRequestAddress(dto.getRequestAddress());
        entity.setSla(dto.getSla());
        entity.setContragent(contragentName);

        return entity;
    }

    // ============================================
    // Req (entity) → ReqMaxDto
    // ============================================
    public ReqMaxDto toDto(Req entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Req (entity) не может быть null");
        }

        // КРИТИЧНО: в БД contragent обязателен
        if (entity.getContragent() == null || entity.getContragent().isBlank()) {
            throw new IllegalStateException(
                    "Req.contragent пуст в БД для заявки " + entity.getRequestNumber()
                            + " — данные повреждены"
            );
        }

        // КРИТИЧНО: фабрика нужна для восстановления Contragent
        if (contragentFactory == null) {
            throw new IllegalStateException(
                    "ContragentFactory не установлен в ReqMapper — "
                            + "невозможно восстановить Contragent для " + entity.getContragent()
            );
        }

        Contragent contragent = contragentFactory.getContragent(entity.getContragent());
        if (contragent == null) {
            throw new IllegalStateException(
                    "Contragent '" + entity.getContragent() + "' не зарегистрирован "
                            + "в ContragentFactory (заявка " + entity.getRequestNumber() + ")"
            );
        }

        ReqMaxDto dto = new ReqMaxDto();
        dto.setRequestNumber(entity.getRequestNumber());
        dto.setRequestText(entity.getRequestText());
        dto.setTID(entity.getTid());
        dto.setPhone(entity.getPhone());
        dto.setPriority(entity.getPriority());
        dto.setCreationTime(entity.getCreationTime());
        dto.setRequestAddress(entity.getRequestAddress());
        dto.setSla(entity.getSla());
        dto.setContragent(contragent);
        dto.setParams(new HashMap<>());

        return dto;
    }
}