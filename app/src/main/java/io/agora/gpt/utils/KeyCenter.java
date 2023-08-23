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

    private static String mRoomName;

    private static String mUserName;

    private static String mAiRoleName;

    public static String getRoomName() {
        if (mRoomName==null|| mRoomName.equals("")){
            mRoomName = getRandomString(12);
        }
        return mRoomName;
    }

    public static String getUserName() {
        return mUserName;
    }

    public static void setUserName(String userName) {
        mUserName = userName;
    }

    public static String getAiRoleName(){
        return mAiRoleName;
    }

    public static void setAiRoleName(String aiRoleName){
        mAiRoleName = aiRoleName;
    }

    public static int getUserUid() {
        if (-1 == USER_RTC_UID) {
            USER_RTC_UID = new Random().nextInt(USER_MAX_UID);
        }
        return USER_RTC_UID;
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

    public static String getRandomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(62);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

}
