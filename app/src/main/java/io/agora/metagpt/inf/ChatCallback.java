package io.agora.metagpt.inf;

public interface ChatCallback {

    default void onChatRequestStart(String text) {

    }

    default void onChatAnswer(int code, String answer) {

    }

    default void onChatRequestFinish() {

    }

    default void updateChatHistoryInfo(String message) {

    }

    default void onChatKeyInfoUpdate(String text) {

    }
}
