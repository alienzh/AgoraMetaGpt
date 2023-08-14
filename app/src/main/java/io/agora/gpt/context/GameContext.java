package io.agora.gpt.context;


import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.agora.gpt.models.GameInfo;
import io.agora.gpt.models.chat.ChatBotRole;
import io.agora.gpt.utils.Constants;
import io.agora.gpt.utils.Utils;

public class GameContext {

    private final static String TAG = Constants.TAG + "-" + GameContext.class.getSimpleName();
    private volatile static GameContext instance = null;
    private GameInfo mCurrentGame;
    private int mGameRole;
    private String mRoomName;
    private String mUserName;
    private String mAvatar;

    private boolean mInitRes;
    private String[] mAiRoles;
    private String[] mAiStatement;

    private GameInfo[] mGameInfoArray;

    private JSONArray[] mLocalGameWords;

    private String[] mQuestionWords;

    private ChatBotRole[] mChatBotRoleArray;

    private String[] mGptResponseHello;

    private int currentChatBotRoleIndex;

    private String mLanguage;

    private boolean mEnableSpeakerDiarization;

    private String[] mChatIdleTipMessagesArray;
    private String[] mAiSelfIntroduceReplaceArray;

    private GameContext() {
        mInitRes = false;
        initData();
    }

    public void initData() {
        mCurrentGame = null;
        mGameRole = Constants.GAME_ROLE_USER;
        mRoomName = "";
        mUserName = "";
        mAvatar = "";

        mLanguage = Constants.LANG_ZH_CN;

        mEnableSpeakerDiarization = false;
    }

    public static GameContext getInstance() {
        if (instance == null) {
            synchronized (GameContext.class) {
                if (instance == null) {
                    instance = new GameContext();
                }
            }
        }
        return instance;
    }

    public void initData(Context context) {
        try {
            String jsonStr = Utils.getFromAssets(context, Constants.ASSETS_AI_ROLE);
            JSONObject jsonObject = JSON.parseObject(jsonStr);
            mAiRoles = jsonObject.getJSONArray("ai_role").toArray(new String[]{});
            mAiStatement = jsonObject.getJSONArray("statement").toArray(new String[]{});

            jsonStr = Utils.getFromAssets(context, Constants.ASSETS_GAMES);
            jsonObject = JSON.parseObject(jsonStr);
            mGameInfoArray = JSON.parseObject(jsonObject.getJSONArray("games").toJSONString(), GameInfo[].class);

            jsonStr = Utils.getFromAssets(context, Constants.ASSETS_LOCAL_GAME_WORDS);
            jsonObject = JSON.parseObject(jsonStr);
            mLocalGameWords = JSON.parseObject(jsonObject.getJSONArray("local_game_words").toJSONString(), JSONArray[].class);

            jsonStr = Utils.getFromAssets(context, Constants.ASSETS_QUESTION_WORDS);
            jsonObject = JSON.parseObject(jsonStr);
            mQuestionWords = JSON.parseObject(jsonObject.getJSONArray("question_words").toJSONString(), String[].class);

            jsonStr = Utils.getFromAssets(context, Constants.ASSETS_CHAT_BOT_ROLE);
            mChatBotRoleArray = JSON.parseObject(jsonStr, ChatBotRole[].class);
            currentChatBotRoleIndex = 0;

            jsonStr = Utils.getFromAssets(context, Constants.ASSETS_GPT_RESPONSE_HELLO);
            mGptResponseHello = JSON.parseObject(jsonStr, String[].class);

            jsonStr = Utils.getFromAssets(context,  Constants.ASSETS_CHAT_IDLE_TIP_MESSAGES);
            mChatIdleTipMessagesArray = JSON.parseObject(jsonStr, String[].class);

            jsonStr = Utils.getFromAssets(context,  Constants.ASSETS_AI_SELF_INTRODUCE_REPLACE);
            mAiSelfIntroduceReplaceArray = JSON.parseObject(jsonStr, String[].class);

            mInitRes = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String[] getAiRoles() {
        return mAiRoles;
    }

    public String[] getStatement() {
        return mAiStatement;
    }

    public GameInfo[] getGameInfoArray() {
        return mGameInfoArray;
    }

    public String[] getGameNames() {
        String[] games = null;
        if (null != mGameInfoArray) {
            games = new String[mGameInfoArray.length];
            for (int i = 0; i < mGameInfoArray.length; i++) {
                games[i] = mGameInfoArray[i].getGameName();
            }
        }
        return games;
    }

    public boolean isInitRes() {
        return mInitRes;
    }

    public GameInfo getCurrentGame() {
        return mCurrentGame;
    }

    public void setCurrentGame(GameInfo currentGame) {
        this.mCurrentGame = currentGame;
    }

    public void setGameRole(int gameRole) {
        this.mGameRole = gameRole;
    }

    public int getGameRole() {
        return mGameRole;
    }

    public String getRoomName() {
        return mRoomName;
    }

    public void setRoomName(String roomName) {
        this.mRoomName = roomName;
    }

    public String getUserName() {
        return mUserName;
    }

    public void setUserName(String userName) {
        this.mUserName = userName;
    }

    public String getAvatar() {
        return mAvatar;
    }

    public void setAvatar(String avatar) {
        this.mAvatar = avatar;
    }

    public JSONArray[] getLocalGameWords() {
        return mLocalGameWords;
    }

    public ChatBotRole[] getChatBotRoleArray() {
        return mChatBotRoleArray;
    }

    public List<ChatBotRole> getAvailableChatBotRoles(){
        List<ChatBotRole> list = new ArrayList<>();
        if (mChatBotRoleArray!=null){
            for (int i = 0; i < mChatBotRoleArray.length; i++) {
                ChatBotRole chatBotRole = mChatBotRoleArray[i];
                if (chatBotRole.isEnable()){
                    list.add(chatBotRole);
                }
            }
        }
        return list;
    }

    public boolean isQuestionSentence(String sentence) {
        if (null != mQuestionWords) {
            for (String word : mQuestionWords) {
                if (sentence.contains(word)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String[] getChatBotRoles() {
        if (null != mChatBotRoleArray) {
            String[] chatRoles = new String[mChatBotRoleArray.length];
            for (int i = 0; i < mChatBotRoleArray.length; i++) {
                chatRoles[i] = mChatBotRoleArray[i].getChatBotRole();
            }
            return chatRoles;
        }
        return null;
    }

    public ChatBotRole getChatBotRoleByIndex(int index) {
        if (null != mChatBotRoleArray && index < mChatBotRoleArray.length && index >= 0) {
            return mChatBotRoleArray[index];
        }
        return null;
    }

    public void setCurrentChatBotRoleIndex(int index){
        this.currentChatBotRoleIndex = index;
    }

    public int getCurrentChatBotRoleIndex(){
        return this.currentChatBotRoleIndex;
    }

    public ChatBotRole getCurrentChatBotRole(){
        if (null != mChatBotRoleArray && currentChatBotRoleIndex < mChatBotRoleArray.length && currentChatBotRoleIndex >= 0) {
            return mChatBotRoleArray[currentChatBotRoleIndex];
        }
        return null;
    }

    public String[] getGptResponseHello() {
        return mGptResponseHello;
    }

    public GameInfo findGameById(int id) {
        if (null != mGameInfoArray) {
            for (int i = 0; i < mGameInfoArray.length; i++) {
                if (id == mGameInfoArray[i].getGameId()) {
                    return mGameInfoArray[i];
                }
            }
        }
        return null;
    }

    public String getLanguage() {
        return mLanguage;
    }

    public void setLanguage(String language) {
        this.mLanguage = language;
    }

    public boolean isEnableSpeakerDiarization() {
        return mEnableSpeakerDiarization;
    }

    public void setEnableSpeakerDiarization(boolean enableSpeakerDiarization) {
        this.mEnableSpeakerDiarization = enableSpeakerDiarization;
    }

    public String[] getChatIdleTipMessagesArray() {
        return mChatIdleTipMessagesArray;
    }

    public String[] getAiSelfIntroduceReplaceArray() {
        return mAiSelfIntroduceReplaceArray;
    }

}
