package org.example.util;

import org.example.entity.Req;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import java.io.IOException;

public class RequestSerializer implements Serializer<Req> {



    @Override
    public void serialize(@NotNull DataOutput2 dataOutput2, @NotNull Req req) throws IOException {
        dataOutput2.writeUTF(req.getRequestNumber());
        dataOutput2.writeUTF(req.getRequestText());
        dataOutput2.writeUTF(req.getRequestAddress());
    }

    @Override
    public Req deserialize(@NotNull DataInput2 dataInput2, int i) throws IOException {
        String requestNumber = dataInput2.readUTF();
        String requestText = dataInput2.readUTF();
        String requestAddress = dataInput2.readUTF();
        Req req = new Req();
        req.setRequestNumber(requestNumber);
        req.setRequestText(requestText);
        req.setRequestAddress(requestAddress);
        return req;
    }

    @Override
    public int fixedSize() {
        return -1; // переменный размер
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }
}