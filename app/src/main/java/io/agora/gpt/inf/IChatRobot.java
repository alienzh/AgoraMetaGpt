package io.agora.gpt.inf;

import com.alibaba.fastjson.JSONArray;

import io.agora.gpt.models.chat.ChatBotRole;

public interface IChatRobot {
    void init();

    void setModelIndex(int index);

    void setChatBotRole(ChatBotRole chatBotRole);


    void setChatCallback(ChatCallback callback);

    void requestChat(JSONArray messageJsonArray);

    void requestChat(String message);

    void requestChatKeyInfo(JSONArray messageJsonArray);

    void cancelRequestChat(boolean cancel);

    void close();
}