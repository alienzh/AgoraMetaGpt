package io.agora.metagpt.utils;

public class Constants {
    public static final String TAG = "MetaGpt";

    public static final String MMKV_ID = TAG;

    public static final int SCENE_ID_WORD = 20;
    public static final int SCENE_INDEX_WORD = 2;

    public static final int GAME_WHO_IS_UNDERCOVER = 0;

    public static final int GAME_ROLE_USER = 0;
    public static final int GAME_ROLE_AI = 1;
    public static final int GAME_ROLE_MODERATOR = 2;

    //讯飞开放平台
    public static final int STT_PLATFORM_XF_RTASR = 0;
    //讯飞直连平台
    public static final int STT_PLATFORM_XF_IST = 1;

    public static final String DATA_STREAM_CMD_USER_JOIN = "user_join";
    public static final String DATA_STREAM_CMD_REQUEST_SYNC_USER = "request_sync_user";
    public static final String DATA_STREAM_CMD_SYNC_GAMER_INFO = "sync_gamer_info";
    public static final String DATA_STREAM_CMD_START_GAME = "start_game";
    public static final String DATA_STREAM_CMD_END_GAME = "end_game";
    public static final String DATA_STREAM_CMD_USER_SPEAK_INFO = "user_speak_info";
    public static final String DATA_STREAM_CMD_REQUEST_AI_SPEAK = "request_ai_speak";
    public static final String DATA_STREAM_CMD_AI_ANSWER = "ai_answer";
    public static final String DATA_STREAM_CMD_AI_ANSWER_OVER = "ai_answer_over";
    public static final String DATA_STREAM_CMD_VOTE_INFO = "vote_info";
    public static final String DATA_STREAM_CMD_START_SPEAK = "start_speak";
    public static final String DATA_STREAM_CMD_START_VOTE = "start_vote";

    // 同步显示的用户
    public static final String DATA_STREAM_CMD_SYNC_DISPLAY_USER = "sync_display_user";

    // 用户发言
    public static final String DATA_STREAM_CMD_USER_SPEAK = "user_speak";
    public static final String DATA_STREAM_CMD_VOTE_DRAW = "vote_draw"; // 平局

    //seconds
    public static final int HTTP_TIMEOUT = 30;


    public static final int AI_PLATFORM_CHAT_GPT = 0;
    public static final int AI_PLATFORM_MINIMAX_COMPLETION5 = 1;
    public static final int AI_PLATFORM_MINIMAX_CHAT_COMPLETION4 = 2;
    public static final int AI_PLATFORM_MINIMAX_CHAT_COMPLETION5 = 3;
    public static final int AI_PLATFORM_CHAT_XUNFEI = 4;


    public static final int TTS_PLATFORM_XF = 0;
    public static final int TTS_PLATFORM_MS = 1;

    public static final String GPT_ROLE_USER = "user";
    public static final String GPT_ROLE_ASSISTANT = "assistant";
    public static final String GPT_ROLE_SYSTEM = "system";

    public static final String MINIMAX_SENDER_TYPE_USER = "USER";
    public static final String MINIMAX_SENDER_TYPE_BOT = "BOT";

    public static final int MAX_GAMER_NUM = 6;

    public static final int XF_REQUEST_INTERVAL = 40;

}
