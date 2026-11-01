package org.example.util;

import org.example.entity.Dto.ReqMaxDto;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import java.io.IOException;

public class RequestSerializer implements Serializer<ReqMaxDto> {



    @Override
    public void serialize(@NotNull DataOutput2 dataOutput2, @NotNull ReqMaxDto reqMaxDTO) throws IOException {
        dataOutput2.writeUTF(reqMaxDTO.getRequestNumber());
        dataOutput2.writeUTF(reqMaxDTO.getRequestText());
        dataOutput2.writeUTF(reqMaxDTO.getRequestAddress());
    }

    @Override
    public ReqMaxDto deserialize(@NotNull DataInput2 dataInput2, int i) throws IOException {
        String requestNumber = dataInput2.readUTF();
        String requestText = dataInput2.readUTF();
        String requestAddress = dataInput2.readUTF();
        ReqMaxDto reqMaxDTO = new ReqMaxDto();
        reqMaxDTO.setRequestNumber(requestNumber);
        reqMaxDTO.setRequestText(requestText);
        reqMaxDTO.setRequestAddress(requestAddress);
        return reqMaxDTO;
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