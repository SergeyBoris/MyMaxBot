package org.example.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.Contragents.Contragent;
import org.example.entity.Dto.ReqMaxDto;
import org.example.entity.InlineKeyboard;
import org.example.entity.Req;
import org.example.entity.mapper.ReqMapper;
import org.example.util.UserUploadSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Optional;

public class BotRequestService {

    private static final Logger log = LoggerFactory.getLogger(BotRequestService.class);
    private final ContragentFactory contragentFactory;
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final ReqMapper reqMapper;

    public BotRequestService(ContragentFactory contragentFactory, HttpClient client, ObjectMapper mapper, ReqMapper reqMapper) {
        this.contragentFactory = contragentFactory;
        this.client = client;
        this.mapper = mapper;
        this.reqMapper = reqMapper;
    }

    public List<ReqMaxDto> getAllRequests(String contragentType) {
        Contragent contragent = contragentFactory.getContragent(contragentType);
        return contragent.getAllRequests().stream().map(reqMapper::toDto).toList();
    }

    public Optional<ReqMaxDto> getRequestByNumber(String contragentType, String requestNumber) {
        Contragent contragent = contragentFactory.getContragent(contragentType);
        return contragent.getRequestByNumber(requestNumber).map(reqMapper::toDto);
    }

    public boolean closeRequest(UserUploadSession userUploadSession) {
        Contragent contragent = contragentFactory.getContragent(userUploadSession.getContragent());
        ReqMaxDto reqMaxDto = getRequestByNumber(userUploadSession.getContragent(),userUploadSession.getRequestNumber()).orElse(null);
        if (reqMaxDto == null) {
            log.info("request not found {}",userUploadSession.getRequestNumber());
            return false;
        }

        return contragent.closeReq(reqMapper.toEntity(reqMaxDto), userUploadSession);  // извлекаем ReqMaxDto из Optional
    }

    public void specialContragentAction(String contragentType, String action,UserUploadSession userUploadSession){
        Contragent contragent = contragentFactory.getContragent(contragentType);
        contragent.specialContragentAction(action,userUploadSession);
    }

    public InlineKeyboard getContragentKeyboard(String contragentType){
        Contragent contragent = contragentFactory.getContragent(contragentType);
        return contragent.getContragentReqKeyboard();
    }
}
