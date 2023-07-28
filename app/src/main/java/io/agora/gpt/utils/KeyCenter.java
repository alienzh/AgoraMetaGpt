package io.agora.gpt.utils;

import java.util.Random;

import io.agora.media.RtcTokenBuilder;
import io.agora.gpt.BuildConfig;
import io.agora.rtm.RtmTokenBuilder;

public class KeyCenter {
    public static final int USER_MAX_UID = 10000;

    public static final int AI_MAX_UID = 20000;

    public static final String APP_ID = BuildConfig.APP_ID;

    private static int USER_RTC_UID = -1;

    private static int AI_RTC_UID = -1;

    private static int MODERATOR_RTC_UID = -1;

    public static int getUserUid() {
        if (-1 == USER_RTC_UID) {
            USER_RTC_UID = new Random().nextInt(USER_MAX_UID);
        }
        return USER_RTC_UID;
    }

    public static int getAiUid() {
        if (-1 == AI_RTC_UID) {
            AI_RTC_UID = new Random().nextInt(USER_MAX_UID) + USER_MAX_UID;
        }
        return AI_RTC_UID;
    }

    public static int getModeratorUid() {
        if (-1 == MODERATOR_RTC_UID) {
            MODERATOR_RTC_UID = new Random().nextInt(USER_MAX_UID) + AI_MAX_UID;
        }
        return MODERATOR_RTC_UID;
    }

    public static String getRtcToken(String channelId, int uid) {
        return new RtcTokenBuilder().buildTokenWithUid(
                APP_ID,
                BuildConfig.APP_CERTIFICATE,
                channelId,
                uid,
                RtcTokenBuilder.Role.Role_Publisher,
                0
        );
    }

    public static String getRtmToken(int uid) {
        try {
            return new RtmTokenBuilder().buildToken(
                    APP_ID,
                    BuildConfig.APP_CERTIFICATE,
                    String.valueOf(uid),
                    RtmTokenBuilder.Role.Rtm_User,
                    0
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

}
