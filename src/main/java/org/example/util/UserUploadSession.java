package org.example.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class UserUploadSession {
    String requestNumber;
    String contragent;
    List<String> photoUrls = new ArrayList<>();
    String text;
    String status;
    boolean isChanged = false;

    public UserUploadSession(String requestNumber, String contragent) {
        this.requestNumber = requestNumber;
        this.contragent = contragent;
    }
    public void setPhotoUrls(String photoUrls) {
        this.photoUrls.add(photoUrls);
    }
}
