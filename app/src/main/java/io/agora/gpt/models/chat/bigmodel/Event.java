package io.agora.gpt.models.chat.bigmodel;

import androidx.annotation.NonNull;

public class Event {
    private String event;
    private String id;
    private String data;
    private String meta;

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }

    @NonNull
    @Override
    public String toString() {
        return "Event{" +
                "event='" + event + '\'' +
                ", id='" + id + '\'' +
                ", data='" + data + '\'' +
                ", meta='" + meta + '\'' +
                '}';
    }
}