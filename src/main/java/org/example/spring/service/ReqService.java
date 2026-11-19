package org.example.spring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.Req;
import org.example.entity.ReqStatus;
import org.example.services.MessageService;
import org.example.spring.repo.RequestRepo;
import org.example.util.AllReqCash;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReqService {
    private final RequestRepo requestRepo;
    private final AllReqCash allReqCash;
    private final MessageService messageService;

    public List<Req> getAllReqs() {
        return requestRepo.findAll();
    }
    public Req findReqByReqNumber(String reqNumber) {
        return requestRepo.findByRequestNumber(reqNumber).orElse(null);
    }

    public List<Req> getNewWithPending(String contragent,
                                       Comparator<Req> sorter) {
        List<Req> fromDb = requestRepo.findByContragentAndStatuses(contragent,List.of(ReqStatus.NEW,ReqStatus.CLOSED_BY_ENGINEER));
        fromDb.sort(sorter);
        return fromDb;
    }


    public Req getReqByNumber(String requestNumber) {
       return requestRepo.findByRequestNumber(requestNumber).orElse(null);
    }

    public boolean addReq(Req req) {
        String number = req.getRequestNumber();

        try {
            requestRepo.save(req);
        } catch (DataIntegrityViolationException e) {
            log.warn("Заявка {} уже есть в БД", number);
            allReqCash.addReqNumber(number);   // ← в кэш (не повторяем)
            return true;                        // ← TRUE (в БД ЕСТЬ)
        } catch (Exception e) {
            log.error("Ошибка сохранения заявки {}: {}", number, e.getMessage(), e);
            return false;
        }

        allReqCash.addReqNumber(number);   // всегда — успех или UNIQUE
        return true;
    }

    @Transactional
    public void setReqStatus(String requestNumber, ReqStatus status) {
        Req req = requestRepo.findByRequestNumber(requestNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Заявка не найдена: " + requestNumber));
        req.setStatus(status);

    }

    public Set<String> findAllRequestNumbers(ReqStatus reqStatus) {
        return requestRepo.findAllRequestNumbers(reqStatus);
    }

    public Map<String, Req> findByStatusesAndContragent(List<ReqStatus> statuses, String contragent) {
        Map<String, Req> reqMap = new HashMap<>();
        for (Req req : requestRepo.findByStatusesAndContragent(statuses, contragent)) {
            reqMap.put(req.getRequestNumber(), req);
        }
        return reqMap;
    }


    private List<Req> mergeWithPending(List<Req> fromDb, Collection<Req> pending) {
        if (pending == null || pending.isEmpty()) {
            sortBySla(fromDb);
            return fromDb;
        }

        Set<String> dbNumbers = fromDb.stream()
                .map(Req::getRequestNumber)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Req> result = new ArrayList<>(fromDb);
        for (Req p : pending) {
            if (p.getRequestNumber() != null
                    && !dbNumbers.contains(p.getRequestNumber())) {
                result.add(p);
            }
        }

        sortBySla(result);
        return result;
    }

    private void sortBySla(List<Req> list) {
        list.sort(Comparator.comparing(Req::getSla,
                Comparator.nullsLast(Comparator.reverseOrder())));
    }


}
