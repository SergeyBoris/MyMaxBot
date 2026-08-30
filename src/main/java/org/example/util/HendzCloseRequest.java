package org.example.util;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.example.entity.Req;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HendzCloseRequest {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 YaBrowser/26.4.0.0 Safari/537.36";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    public static boolean close(Req req, UserUploadSession userUploadSession,String cookie) {
        String ticketId = req.params.get("ticketId");;
        String outgoingId = req.params.get("outgoing");
        LocalDateTime timeNow = LocalDateTime.now();
        DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter formatterTime = DateTimeFormatter.ofPattern("HH:mm");
        String workDate = timeNow.format(formatterDate);
        LocalDateTime beginTime = timeNow.minusMinutes(10);
        String arrivalTime = beginTime.format(formatterTime);
        String endTime = timeNow.format(formatterTime);
        String statusUid;
        String closeMode;
        String testWorkDescription = userUploadSession.getText();
        System.out.println("testWorkDescription = " + testWorkDescription);
        String workDescription = userUploadSession.getText();;
        List<Path> photos = userUploadSession.getSavedJpgPaths();
        Map<String, Object> params = userUploadSession.params;
//        if (userUploadSession.getStatus().equals("Закрыто")){
//            params.put("params[731]",workDescription);
//        }else {
//          params.put("params[792]", workDescription);
//        }

        String url = "https://work.hendz.ru:10294/pfi/close/" + outgoingId;


        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                // Основные поля
                .addFormDataPart("ticket_id", ticketId)
                .addFormDataPart("outgoing", outgoingId)
                .addFormDataPart("matrix_root_device_uid", "661")
                .addFormDataPart("executor", "3824")
                .addFormDataPart("work_date", workDate)
                .addFormDataPart("arrival_time", arrivalTime)
                .addFormDataPart("begin_time", arrivalTime)
                .addFormDataPart("end_time", endTime);
          //      .addFormDataPart("task_uid", "83");           // ← ID задачи (83-SLM, 88-осмотр перед абонементом, 121- подготовка к демонтажу )

        params.forEach( (k,v) -> {
            log.info("closed PFI param: {} - {} \n", k, v.toString());

            if ("params[593]".equals(k) && v == null){
                v= "000000";
            }

            if ("params[591]".equals(k)) {
                if (v==null || !v.toString().matches("2600000\\d{6}")){
                    v = "2600000000000";
                }
            }

            if (v == null){
                v = "000000";
            }
            builder.addFormDataPart(k, v.toString());

        });
        // matrix_ctx_device поля (все возможные узлы)
        addMatrixContextFields(builder);

        // ✅ Добавляем НЕСКОЛЬКО фото (одинаковое имя поля "visit_files[]")
        if (photos != null && !photos.isEmpty()) {
            for (Path photo : photos) {
                if (photo.toFile().exists()) {
                    RequestBody fileBody = RequestBody.create(
                            photo.toFile(),
                            MediaType.parse("image/jpeg")
                    );
                    builder.addFormDataPart("visit_files[]", photo.toFile().getName(), fileBody);
                }
            }
        }

        Request request = new Request.Builder()
                .url(url)
                .post(builder.build())
                .addHeader("Cookie", cookie)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("X-October-Request-Handler", "onSendToModeration")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("Origin", "https://work.hendz.ru:10294")
                .addHeader("Referer", "https://work.hendz.ru:10294/pfi/close/" + outgoingId)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            System.out.println("Status: " + response.code());
            System.out.println("Response: " + responseBody);
            return response.isSuccessful();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    private static void addMatrixContextFields(MultipartBody.Builder builder) {
        // matrix_ctx_device поля (как в браузере)
        for (int i = 0; i < 3; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\]", "661");
        }
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1761]", "625");
        builder.addFormDataPart("analogue_pick[\\1768\\1761][3]", "");
        builder.addFormDataPart("analogue_get[\\1768\\1761][3]", "");
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1761]", "625");
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1761\\2004]", "572");
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1761\\2004]", "572");

        for (int i = 0; i < 5; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1768\\1760]", "658");
        }
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1760\\1974]", "766");
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1760\\1974]", "766");
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1760\\1975]", "766");
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1760\\1975]", "766");
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1760\\2038]", "610");
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1760\\2038]", "610");
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1760\\2005]", "707");

        for (int i = 0; i < 3; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1768\\1771]", "664");
        }
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1771\\1926]", "751");
        for (int i = 0; i < 3; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1768\\1771\\1927]", "718");
        }
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1771\\1927\\1966]", "742");
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1771\\1927\\1966]", "742");
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1771\\1927\\1925]", "610");
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1771\\1927\\1925]", "610");
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1771\\1927\\1999]", "121");
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1771\\1927\\2000\\1854]", "124");
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1771\\1927\\2000\\1857]", "121");
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1771\\1927\\2000\\1855]", "201");
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1771\\1927\\2000\\1856]", "201");

        for (int i = 0; i < 3; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1768\\2041]", "768");
        }
        for (int i = 0; i < 3; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1768\\1784]", "90");
        }

        builder.addFormDataPart("matrix_ctx_device[\\1768\\1785]", "673");
        builder.addFormDataPart("analogue_pick[\\1768\\1785][3]", "");
        builder.addFormDataPart("analogue_get[\\1768\\1785][3]", "");
        for (int i = 0; i < 3; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1768\\1785]", "673");
        }

        for (int i = 0; i < 3; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1768\\1783]", "672");
        }
        for (int i = 0; i < 2; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1768\\2025]", "262");
        }
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1782]", "671");
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1781]", "670");
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1808]", "679");

        for (int i = 0; i < 2; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1768\\1780]", "669");
        }
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1763]", "381");
        builder.addFormDataPart("analogue_pick[\\1768\\1763][3]", "");
        builder.addFormDataPart("analogue_get[\\1768\\1763][3]", "");
        for (int i = 0; i < 2; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1768\\1763]", "381");
        }

        for (int i = 0; i < 3; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1768\\1762]", "428");
        }

        // Сейф
        builder.addFormDataPart("matrix_ctx_device[\\1769\\1764]", "665");
        builder.addFormDataPart("matrix_ctx_device[\\1769\\1764\\1773]", "666");
        builder.addFormDataPart("matrix_ctx_device[\\1769\\1764\\1774]", "667");

        for (int i = 0; i < 2; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1769\\2007]", "756");
        }
        builder.addFormDataPart("matrix_ctx_device[\\1769\\1775]", "493");

        for (int i = 0; i < 2; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1769\\1779]", "572");
        }

        for (int i = 0; i < 3; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1769\\1766]", "90");
        }

        for (int i = 0; i < 2; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1769\\1776]", "668");
        }

        for (int i = 0; i < 2; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1769\\1777]", "41");
        }

        for (int i = 0; i < 2; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1769\\1778]", "41");
        }

        for (int i = 0; i < 3; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1769\\1765]", "90");
        }

        for (int i = 0; i < 3; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1769\\2026]", "90");
        }

        for (int i = 0; i < 4; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1769\\2001]", "752");
        }
        builder.addFormDataPart("matrix_ctx_device[\\1769\\2006]", "755");

        // Проводка
        builder.addFormDataPart("matrix_ctx_device[\\1807\\1805\\1945]", "677");
        builder.addFormDataPart("matrix_ctx_device[\\1807\\1805\\1806]", "677");
        builder.addFormDataPart("matrix_ctx_device[\\1807\\1805\\1803]", "677");
        builder.addFormDataPart("matrix_ctx_device[\\1807\\1805\\1804]", "677");

        builder.addFormDataPart("matrix_ctx_device[\\1807\\2012]", "758");
        builder.addFormDataPart("analogue_pick[\\1807\\2012][3]", "");
        builder.addFormDataPart("analogue_get[\\1807\\2012][3]", "");

        builder.addFormDataPart("matrix_ctx_device[\\1807\\2012\\2009]", "757");
        builder.addFormDataPart("matrix_ctx_device[\\1807\\2012\\2014]", "757");
        builder.addFormDataPart("matrix_ctx_device[\\1807\\2012\\2013]", "757");
        builder.addFormDataPart("matrix_ctx_device[\\1807\\2012\\2019]", "757");
        builder.addFormDataPart("matrix_ctx_device[\\1807\\2012\\2015]", "757");
        builder.addFormDataPart("matrix_ctx_device[\\1807\\2012\\2016]", "757");

        for (int i = 0; i < 2; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1807\\2012\\2018]", "41");
        }

        builder.addFormDataPart("matrix_ctx_device[\\1807\\2012\\2021]", "759");
        builder.addFormDataPart("matrix_ctx_device[\\1807\\2012\\2021\\2020]", "757");
        builder.addFormDataPart("matrix_ctx_device[\\1807\\2012\\2021\\2023]", "760");

        builder.addFormDataPart("matrix_ctx_device[\\1807\\1800]", "676");
        builder.addFormDataPart("analogue_pick[\\1807\\1800][3]", "");
        builder.addFormDataPart("analogue_get[\\1807\\1800][3]", "");

        builder.addFormDataPart("matrix_ctx_device[\\1807\\1798]", "505");
        builder.addFormDataPart("matrix_ctx_device[\\1807\\1797]", "505");
        builder.addFormDataPart("matrix_ctx_device[\\1807\\1799]", "505");

        builder.addFormDataPart("matrix_ctx_device[\\1807\\2024]", "759");
        builder.addFormDataPart("matrix_ctx_device[\\1807\\2024\\2020]", "757");
        builder.addFormDataPart("matrix_ctx_device[\\1807\\2024\\2023]", "760");

        builder.addFormDataPart("matrix_ctx_device[\\1788\\1786]", "674");
        builder.addFormDataPart("matrix_ctx_device[\\1788\\1787]", "674");

        builder.addFormDataPart("matrix_ctx_device[\\2040]", "767");
        builder.addFormDataPart("matrix_ctx_device[\\2039]", "676");
    }



}
