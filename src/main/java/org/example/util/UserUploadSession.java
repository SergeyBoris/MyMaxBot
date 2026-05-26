package org.example.util;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.file.Path;
import java.util.*;


@Setter
@Getter
@NoArgsConstructor
public class UserUploadSession {
    String requestNumber;
    String contragent;
    List<String> photoUrls = new ArrayList<>();
    List<Path> savedJpgPaths = new ArrayList<>();
    Map<String, Object> params = new HashMap<>();
    String text;
    String status;
    boolean isChanged = false;

    public UserUploadSession(String requestNumber, String contragent) {
        this.requestNumber = requestNumber;
        this.contragent = contragent;
    }

    public void setChanged(boolean isChanged) {
        this.isChanged = isChanged;
    }
    public void setPhotoUrls(String photoUrls) {
        this.photoUrls.add(photoUrls);
    }
}
