package org.example.util;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class HendzCloseRequest {

    private static final String COOKIE = "user_auth=eyJpdiI6IklJYTF6VVRTV25neGVRU3Jja09sb1E9PSIsInZhbHVlIjoiQTVCalZBaFNoXC9zZDJQTnZyOG1qdENwNm1TUlRPKzVuTklQZVlXaVZFZGFGcHFFb2ZVcFkwV3JCS0Jzb3JOc3RSNmhxMnFUcTk2UGZpK3B5aVVQbnFGZDdWZXNUVEloZUF0UUJ0WlBXYmloTkpXemYwQkg5cHRMZnRHUWdYSjlLRUFwK1ljdTFCSXJPS0ZoNU9UTkRvZz09IiwibWFjIjoiOGQ0NDZiZWQyNzM0ZmMxMjZhYTlhMDBiMjZmZTczMDY4NDFkZDMxMDViNjQ3NjdkZTZhZDNjODFkNDA5MzlkNiJ9; october_session=eyJpdiI6IjgxMmJadmQ0RzhZK0w1c0V4bGFxdUE9PSIsInZhbHVlIjoiY3R1bWpRbHp2NTBvVFwvb2krclJlcWM2dXNLZUprZmpGMmVIOSsyelwvS1pFXC83K0VmZmg1QVwvRXFLekJFWVFEbWJkdnpwK3FoTXN0VjJYR2Y5VFZOZDdRZU9WY0FIOUowRk9BSWlvbmNZa0I2bVJ0Y3E5dFFZYlVyMThMRHorbDA1IiwibWFjIjoiZmVjYTYyMTBkMWFkOTM1OGMxYmMyOGE4OTljZWJkMzI5MWQxOGFiN2E3MTM4ZWFlOGMxY2I3OGM3ZTNlMzU4NiJ9";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 YaBrowser/26.4.0.0 Safari/537.36";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();
    public static void main(String[] args) {
        HendzCloseRequest hendz = new HendzCloseRequest();
        File photo = new File("C:/photo.jpg");

        try {
            boolean success = hendz.closeTicket("1000849", photo);
            System.out.println("Успешно: " + success);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public boolean closeTicket(String outgoingId, List<File> photos) throws IOException {
        String url = "https://work.hendz.ru:10294/pfi/close/" + outgoingId;

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                // Основные поля
                .addFormDataPart("ticket_id", "21851")
                .addFormDataPart("outgoing", "1000849")
                .addFormDataPart("matrix_root_device_uid", "661")
                .addFormDataPart("executor", "3824")
                .addFormDataPart("work_date", "2026-05-25")
                .addFormDataPart("arrival_time", "14:59")
                .addFormDataPart("begin_time", "14:59")
                .addFormDataPart("end_time", "14:59")
                .addFormDataPart("task_uid", "")
                .addFormDataPart("close_mode", "no_onsite");

        // matrix_ctx_device поля (все возможные узлы)
        addMatrixContextFields(builder);

        // ✅ Добавляем НЕСКОЛЬКО фото (одинаковое имя поля "visit_files[]")
        if (photos != null && !photos.isEmpty()) {
            for (File photo : photos) {
                if (photo.exists()) {
                    RequestBody fileBody = RequestBody.create(
                            photo,
                            MediaType.parse("image/jpeg")
                    );
                    builder.addFormDataPart("visit_files[]", photo.getName(), fileBody);
                }
            }
        }

        Request request = new Request.Builder()
                .url(url)
                .post(builder.build())
                .addHeader("Cookie", COOKIE)
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
        }
    }

    private void addMatrixContextFields(MultipartBody.Builder builder) {
        // Повторяющиеся поля (как в браузере)
        for (int i = 0; i < 3; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\]", "661");
        }

        // Контроллер и его узлы
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1761]", "625");
        builder.addFormDataPart("analogue_pick[\\1768\\1761][3]", "");
        builder.addFormDataPart("analogue_get[\\1768\\1761][3]", "");
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1761]", "625");
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1761\\2004]", "572");
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1761\\2004]", "572");

        // Валидатор и его узлы
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

        // Платформа валидатора
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

        // Дополнительные узлы сервисной зоны
        for (int i = 0; i < 3; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1768\\2041]", "768");
        }
        for (int i = 0; i < 3; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1768\\1784]", "90");
        }

        // Блок дисплея
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1785]", "673");
        builder.addFormDataPart("analogue_pick[\\1768\\1785][3]", "");
        builder.addFormDataPart("analogue_get[\\1768\\1785][3]", "");
        for (int i = 0; i < 3; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1768\\1785]", "673");
        }

        // Замки
        for (int i = 0; i < 3; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1768\\1783]", "672");
        }
        for (int i = 0; i < 2; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1768\\2025]", "262");
        }
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1782]", "671");
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1781]", "670");
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1808]", "679");

        // УЗО и ИБП
        for (int i = 0; i < 2; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1768\\1780]", "669");
        }
        builder.addFormDataPart("matrix_ctx_device[\\1768\\1763]", "381");
        builder.addFormDataPart("analogue_pick[\\1768\\1763][3]", "");
        builder.addFormDataPart("analogue_get[\\1768\\1763][3]", "");
        for (int i = 0; i < 2; i++) {
            builder.addFormDataPart("matrix_ctx_device[\\1768\\1763]", "381");
        }

        // Блок питания
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

        // Проводка (кабели)
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

        // Корпус
        builder.addFormDataPart("matrix_ctx_device[\\1788\\1786]", "674");
        builder.addFormDataPart("matrix_ctx_device[\\1788\\1787]", "674");

        // Кабели питания
        builder.addFormDataPart("matrix_ctx_device[\\2040]", "767");
        builder.addFormDataPart("matrix_ctx_device[\\2039]", "676");
    }



}
