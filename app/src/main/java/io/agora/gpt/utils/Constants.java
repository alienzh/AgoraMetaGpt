package io.agora.gpt.utils;

public class Constants {
    public static final String TAG = "MetaGpt";

    public static final String MMKV_ID = TAG;

    public static final int SCENE_ID_GPT = 25;
    public static final int SCENE_INDEX_LIVE = 0;

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


    public static final int AI_PLATFORM_CHAT_GPT_35 = 0;
    public static final int AI_PLATFORM_CHAT_GPT_40 = 1;
    public static final int AI_PLATFORM_MINIMAX_CHAT_COMPLETION_5 = 2;
    public static final int AI_PLATFORM_MINIMAX_CHAT_COMPLETION_55 = 3;
    public static final int AI_PLATFORM_MINIMAX_CHAT_COMPLETION_PRO_55 = 4;
    public static final int AI_PLATFORM_CHAT_XUNFEI = 5;


    public static final int TTS_PLATFORM_XF = 0;
    public static final int TTS_PLATFORM_MS = 1;
    public static final int TTS_PLATFORM_MINIMAX = 2;

    public static final String PLATFORM_NAME_XF = "xf";
    public static final String PLATFORM_NAME_MS = "ms";
    public static final String PLATFORM_NAME_MINIMAX = "minimax";

    public static final String GPT_ROLE_USER = "user";
    public static final String GPT_ROLE_ASSISTANT = "assistant";
    public static final String GPT_ROLE_SYSTEM = "system";

    public static final String MINIMAX_SENDER_TYPE_USER = "USER";
    public static final String MINIMAX_SENDER_TYPE_BOT = "BOT";

    public static final int MAX_GAMER_NUM = 6;

    public static final String CHAT_GPT_MODEL_35 = "gpt-3.5-turbo";
    public static final String CHAT_GPT_MODEL_40 = "gpt-4";

    public static final String MINIMAX_MODEL_5 = "abab5-chat";
    public static final String MINIMAX_MODEL_55 = "abab5.5-chat";

    public static final String ASSETS_AI_ROLE = "ai_role.json";
    public static final String ASSETS_GAMES = "games.json";
    public static final String ASSETS_LOCAL_GAME_WORDS = "local_game_words.json";
    public static final String ASSETS_QUESTION_WORDS = "question_words.json";
    public static final String ASSETS_TEST_TTS = "test_tts.json";

    public static final String ASSETS_CHAT_BOT_ROLE = "chat_bot_role.json";
    public static final String ASSETS_GPT_RESPONSE_HELLO = "gpt_response_hello.json";
    public static final String ASSETS_GPT_KEY_INFO_PROMPT = "gpt_key_info_prompt.txt";

    public static final Integer RTC_AUDIO_SAMPLE_RATE = 16000;
    public static final Integer RTC_AUDIO_SAMPLE_NUM_OF_CHANNEL = 1;
    public static final Integer RTC_AUDIO_SAMPLES = 640;

    public static final int STT_SAMPLE_RATE = 16000;
    public static final int STT_SAMPLE_NUM_OF_CHANNEL = 1;
    public static final int STT_BITS_PER_SAMPLE = 16;

    public static final float TTS_BYTE_PER_SAMPLE = 1.0f * Constants.STT_BITS_PER_SAMPLE / 8 * Constants.STT_SAMPLE_NUM_OF_CHANNEL;
    public static final float TTS_DURATION_PER_SAMPLE = 1000.0f / Constants.STT_SAMPLE_RATE;
    public static final float TTS_SAMPLE_COUNT_PER_MS = Constants.STT_SAMPLE_RATE * 1.0f / 1000;
    public static final int TTS_BUFFER_SAMPLE_COUNT = (int) (TTS_SAMPLE_COUNT_PER_MS * 20);
    public static final int TTS_BUFFER_BYTE_SIZE = (int) (TTS_BUFFER_SAMPLE_COUNT * TTS_BYTE_PER_SAMPLE);
    public static final long TTS_BUFFER_DURATION = (long) (TTS_BUFFER_SAMPLE_COUNT * TTS_DURATION_PER_SAMPLE);

    //ms
    public static final int INTERVAL_XF_REQUEST = 40;
    public static final long INTERVAL_MIN_REQUEST_CHAT = 3000;
    public static final long INTERVAL_CHAT_IDLE_TIME = 10 * 1000;

    public static final String ASSETS_REPLACE_GPT_RESPONSE_HELLO_USERNAME_LABEL = "username";

    public static final int MAX_COUNT_GPT_RESPONSE_HELLO = 3;

    public static final int GPT_MAX_TOKENS = 16384;
    public static final float GPT_TEMPERATURE = 0.5f;
    public static final float GPT_TOP_P = 0.95f;

    public static final String HMAC_SHA1 = "HmacSHA1";
    public static final String SYMBOLS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public final static int WAIT_AUDIO_FRAME_COUNT = 3;
    public final static int STT_FILTER_NUMBER = 3;
}
