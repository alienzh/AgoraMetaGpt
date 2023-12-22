package io.agora.gpt.utils

import io.agora.aigc.sdk.model.AIRole
import io.agora.gpt.BuildConfig
import io.agora.gpt.R
import io.agora.media.RtcTokenBuilder
import io.agora.rtm.RtmTokenBuilder
import java.util.Random

object KeyCenter {

    fun getAvatarName(aiRole: AIRole): String {
        val avatarName = when (aiRole.roleId) {
            Constant.EN_Role_ID_WENDY -> Constant.Avatar_Mina
            Constant.EN_Role_ID_Cindy -> Constant.Avatar_Mina
            Constant.EN_Role_ID_yunibobo -> Constant.Avatar_Mina
            Constant.CN_Role_ID_yunibobo -> Constant.Avatar_Mina
            Constant.CN_Role_ID_jingxiang -> Constant.Avatar_Kda
            else -> Constant.Avatar_Mina
        }
        return avatarName
    }

    fun getAvatarDrawableStr(aiRole: AIRole): String {
        val drawableStr: String = if (aiRole.roleId.startsWith(Constant.Role_ID_yunibobo, true)) {
            Constant.AI_Avatar_1
        } else if (aiRole.roleId.startsWith(Constant.Role_ID_jingxiang, true)) {
            Constant.AI_Avatar_2
        } else if (aiRole.roleId.startsWith(Constant.Role_ID_Foodie, true)) {
            Constant.AI_Avatar_1
        } else if (aiRole.roleId.startsWith(Constant.Role_ID_WENDY, true)) {
            Constant.AI_Avatar_2
        } else if (aiRole.roleId.startsWith(Constant.Role_ID_Cindy, true)) {
            Constant.AI_Avatar_3
        } else {
            Constant.AI_Avatar_1
        }
        return drawableStr
    }

    private const val USER_MAX_UID = 10000
    private const val DIGITAL_HUMAN_MAX_UID = 20000
    const val APP_ID = BuildConfig.APP_ID
    private var USER_RTC_UID = -1
    private var DIGITAL_HUMAN_RTC_UID = -1
    private var innerRoomName: String? = null
    var mUserName: String? = null

//    val mRoomName: String?
//        get() {
//            if (innerRoomName == null || innerRoomName == "") {
//                innerRoomName = getRandomString(12)
//            }
//            return innerRoomName
//        }
    val mUserUid: Int
        get() {
            if (-1 == USER_RTC_UID) {
                USER_RTC_UID = Random().nextInt(USER_MAX_UID)
            }
            return USER_RTC_UID
        }
    val mVirtualHumanUid: Int
        get() {
            if (-1 == DIGITAL_HUMAN_RTC_UID) {
                DIGITAL_HUMAN_RTC_UID = Random().nextInt(USER_MAX_UID) + DIGITAL_HUMAN_MAX_UID
            }
            return DIGITAL_HUMAN_RTC_UID
        }

    fun getRtcToken(channelId: String?, uid: Int): String {
        return RtcTokenBuilder().buildTokenWithUid(
            APP_ID,
            BuildConfig.APP_CERTIFICATE,
            channelId,
            uid,
            RtcTokenBuilder.Role.Role_Publisher,
            0
        )
    }

    fun getRtmToken(uid: Int): String? {
        return try {
            RtmTokenBuilder().buildToken(
                APP_ID,
                BuildConfig.APP_CERTIFICATE, uid.toString(),
                RtmTokenBuilder.Role.Rtm_User,
                0
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getRandomString(length: Int): String {
        val str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = Random()
        val sb = StringBuffer()
        for (i in 0 until length) {
            val number = random.nextInt(62)
            sb.append(str[number])
        }
        return sb.toString()
    }
}