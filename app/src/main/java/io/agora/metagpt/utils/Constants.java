package io.agora.metagpt.utils;

public class Constants {
    public static final String TAG = "MetaGpt";

    public static final String MMKV_ID = TAG;

    public static final int SCENE_ID_WORD = 20;
    public static final int SCENE_INDEX_WORD = 2;

    public static final int GAME_ROLE_USER = 0;
    public static final int GAME_ROLE_AI = 1;
    public static final int GAME_ROLE_MODERATOR = 2;

    public static final int GAME_WHO_IS_UNDERCOVER = 0;
    public static final int GAME_AI_VOICE_ASSISTANT = 1;

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

    public static final String CHAT_GPT_MODEL_35 = "gpt-3.5-turbo";
    public static final String CHAT_GPT_MODEL_40 = "gpt-4";

    public static final String ASSETS_AI_ROLE = "ai_role.json";
    public static final String ASSETS_GAMES = "games.json";
    public static final String ASSETS_LOCAL_GAME_WORDS = "local_game_words.json";
    public static final String ASSETS_QUESTION_WORDS = "question_words.json";
    public static final String ASSETS_TEST_TTS = "test_tts.json";

    public static final String ASSETS_CHAT_BOT_ROLE = "chat_bot_role.json";
    public static final String ASSETS_GPT_RESPONSE_HELLO = "gpt_response_hello.json";
    public static final String ASSETS_GPT_KEY_INFO_PROMPT = "gpt_key_info_prompt.txt";

}
