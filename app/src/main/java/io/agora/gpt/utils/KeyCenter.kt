package io.agora.gpt.utils

import android.content.Context
import android.text.TextUtils
import io.agora.aigc.sdk.model.AIRole
import io.agora.gpt.BuildConfig
import io.agora.gpt.MainApplication
import io.agora.gpt.ui.main.SportsTextModel
import io.agora.media.RtcTokenBuilder
import io.agora.rtm.RtmTokenBuilder
import java.io.BufferedReader
import java.io.InputStreamReader
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
    var mUserName: String? = null

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


    private var innerSportList: MutableList<SportsTextModel> = mutableListOf()

    val mSportList: List<SportsTextModel>
        get() {
            if (innerSportList.isEmpty()){
                innerSportList.addAll(getSportsFromAssets(MainApplication.mGlobalApplication))
            }
            return innerSportList
        }

    private fun getSportsFromAssets(context: Context): List<SportsTextModel> {
        val result = mutableListOf<SportsTextModel>()
        try {
            InputStreamReader(context.resources.assets.open("ai_tts.txt")).use { inputReader ->
                BufferedReader(inputReader).use { bufReader ->
                    var line: String?
                    while (bufReader.readLine().also { line = it } != null) {
                        val textList = line?.split('\'') ?: continue
                        val sportsTextModel = SportsTextModel(0, "")
                        sportsTextModel.time = textList[0].toIntOrNull() ?: 0
                        sportsTextModel.content = textList[1]
                        sportsTextModel.content = textList[1].replaceFirst(" - ","")
                        result.add(sportsTextModel)
                    }
                    return result.toList()
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return result.toList()
    }
}