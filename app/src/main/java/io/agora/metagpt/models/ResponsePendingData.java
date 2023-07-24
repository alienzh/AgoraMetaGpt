package io.agora.metagpt.models;

import androidx.annotation.NonNull;

import java.util.Arrays;

public class ResponsePendingData {
    private byte[] data;
    private boolean isFinished;

    public ResponsePendingData() {
        data = null;
        isFinished = false;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    public byte[] getData() {
        return data;
    }

    public void putData(byte[] newData, int length) {
        if (null == data) {
            data = new byte[0];
        }
        byte[] temp = new byte[data.length + length];
        if (data.length != 0) {
            System.arraycopy(data, 0, temp, 0, data.length);
        }
        System.arraycopy(newData, 0, temp, data.length, length);
        data = temp;
    }

    public void clearData() {
        data = null;
    }

    @NonNull
    @Override
    public String toString() {
        return "ResponsePendingData{" +
                "data=" + Arrays.toString(data) +
                ", isFinished=" + isFinished +
                '}';
    }
}
