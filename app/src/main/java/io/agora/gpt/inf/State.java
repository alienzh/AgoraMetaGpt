package io.agora.gpt.inf;

import android.os.Message;

public interface State {
    default void enter() {
    }

    default void exit() {
    }

    default boolean processMessage(Message msg) {
        return true;
    }

    default void transitionTo(State state) {
        if (null != state) {
            this.exit();
            state.enter();
        }
    }
}
