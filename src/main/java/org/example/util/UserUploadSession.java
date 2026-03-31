package org.example.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserUploadSession {
    String requestNumber;
    List<String> photoUrls = new ArrayList<>();
    String text;
    String status;
    boolean isChanged = false;

    public UserUploadSession(String requestNumber) {
        this.requestNumber = requestNumber;
    }
    public void setPhotoUrls(String photoUrls) {
        this.photoUrls.add(photoUrls);
    }
}
