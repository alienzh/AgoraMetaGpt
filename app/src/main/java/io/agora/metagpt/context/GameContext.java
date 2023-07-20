package io.agora.metagpt.context;


import android.content.Context;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;


import io.agora.metagpt.models.GameInfo;
import io.agora.metagpt.utils.Constants;
import io.agora.metagpt.utils.Utils;

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
            String jsonStr = Utils.getFromAssets(context, "ai_role.json");
            JSONObject jsonObject = JSON.parseObject(jsonStr);
            mAiRoles = jsonObject.getJSONArray("ai_role").toArray(new String[]{});
            mAiStatement = jsonObject.getJSONArray("statement").toArray(new String[]{});

            jsonStr = Utils.getFromAssets(context, "games.json");
            jsonObject = JSON.parseObject(jsonStr);
            mGameInfoArray = JSON.parseObject(jsonObject.getJSONArray("games").toJSONString(), GameInfo[].class);

            jsonStr = Utils.getFromAssets(context, "local_game_words1.json");
            jsonObject = JSON.parseObject(jsonStr);
            mLocalGameWords = JSON.parseObject(jsonObject.getJSONArray("local_game_words").toJSONString(), JSONArray[].class);

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
}
