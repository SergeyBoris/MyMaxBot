package org.example.entity.Contragents;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.db.MapDB;
import org.example.services.MessageService;
import org.mapdb.DB;

import java.net.http.HttpClient;

public class test {
    public static void main(String[] args) {

        Pfi pfi = new Pfi(new MessageService(HttpClient.newBuilder().build()),new MapDB(new ObjectMapper()));
        pfi.getAllRequests();
    }
}
