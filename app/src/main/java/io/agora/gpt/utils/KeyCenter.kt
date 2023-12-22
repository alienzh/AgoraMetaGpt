package io.agora.gpt.utils

import io.agora.gpt.BuildConfig
import io.agora.gpt.MainApplication
import io.agora.gpt.R
import io.agora.gpt.ui.main.AIRoleAvatarModel
import io.agora.media.RtcTokenBuilder
import io.agora.rtm.RtmTokenBuilder
import java.util.Random

object KeyCenter {

//    val mEnRoleAvatars: List<AIRoleAvatarModel> by lazy {
//        mutableListOf(
//            AIRoleAvatarModel("Wendy-en-US", "mina"),
//            AIRoleAvatarModel("Cindy-en-US", "mina"),
//            AIRoleAvatarModel("yunibobo-en-US", "kda")
//        )
//    }
//    val mCnRoleAvatars: List<AIRoleAvatarModel> by lazy {
//        mutableListOf(
//            AIRoleAvatarModel("yunibobo-zh-CN", "mina"),
//            AIRoleAvatarModel("jingxiang-zh-CN", "kda")
//        )
//    }


    private const val USER_MAX_UID = 10000
    private const val DIGITAL_HUMAN_MAX_UID = 20000
    const val APP_ID = BuildConfig.APP_ID
    private var USER_RTC_UID = -1
    private var DIGITAL_HUMAN_RTC_UID = -1
    private var mRoomName: String? = null
    private var internalAvatars: Array<String>? = null
    var userName: String? = null
//    val roomName: String?
//        get() {
//            if (mRoomName == null || mRoomName == "") {
//                mRoomName = getRandomString(12)
//            }
//            return mRoomName
//        }
    val userUid: Int
        get() {
            if (-1 == USER_RTC_UID) {
                USER_RTC_UID = Random().nextInt(USER_MAX_UID)
            }
            return USER_RTC_UID
        }
    val virtualHumanUid: Int
        get() {
            if (-1 == DIGITAL_HUMAN_RTC_UID) {
                DIGITAL_HUMAN_RTC_UID = Random().nextInt(USER_MAX_UID) + DIGITAL_HUMAN_MAX_UID
            }
            return DIGITAL_HUMAN_RTC_UID
        }

    val avatars: Array<String>
        get() {
            if (internalAvatars == null) {
                internalAvatars = MainApplication.mGlobalApplication.resources.getStringArray(R.array.avatars)
            }
            return internalAvatars!!
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