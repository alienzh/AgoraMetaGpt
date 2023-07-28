package io.agora.gpt.chat;

import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.agora.gpt.chat.gpt.GptChatRobot;
import io.agora.gpt.chat.minimax.MinimaxChatRobot;
import io.agora.gpt.chat.xf.XfChatRobot;
import io.agora.gpt.inf.ChatCallback;
import io.agora.gpt.inf.IChatRobot;
import io.agora.gpt.models.chat.ChatBotRole;
import io.agora.gpt.models.chat.ChatRobotMessage;
import io.agora.gpt.models.chat.minimax.MinimaxMessage;
import io.agora.gpt.models.chat.minimax.MinimaxProMessage;
import io.agora.gpt.utils.Config;
import io.agora.gpt.utils.Constants;
import io.agora.gpt.utils.Utils;

public class ChatRobotManager {
    private final Context mContext;
    private IChatRobot mChatRobot;
    private ChatCallback mCallback;
    private final ExecutorService mExecutorService;

    private final List<ChatRobotMessage> mGptChatMessageList;

    private static long mLastRequestTime;

    private static int mChatPlatformIndex;
    private ChatBotRole mChatBotRole;
    private final String mGptKeyInfoPrompt;

    public ChatRobotManager(Context context) {
        mContext = context;
        mExecutorService = new ThreadPoolExecutor(1, 1,
                0, TimeUnit.MILLISECONDS,
                new LinkedBlockingDeque<Runnable>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        mGptChatMessageList = new ArrayList<>();
        mLastRequestTime = 0;
        mGptKeyInfoPrompt = Utils.getFromAssets(context, Constants.ASSETS_GPT_KEY_INFO_PROMPT);
    }

    public void setChatRobotPlatformIndex(int platformIndex) {
        mChatPlatformIndex = platformIndex;
        switch (platformIndex) {
            case Constants.AI_PLATFORM_CHAT_GPT_35:
            case Constants.AI_PLATFORM_CHAT_GPT_40:
                mChatRobot = new GptChatRobot();
                break;
            case Constants.AI_PLATFORM_MINIMAX_CHAT_COMPLETION_5:
            case Constants.AI_PLATFORM_MINIMAX_CHAT_COMPLETION_55:
            case Constants.AI_PLATFORM_MINIMAX_CHAT_COMPLETION_PRO_55:
                mChatRobot = new MinimaxChatRobot();
                break;
            case Constants.AI_PLATFORM_CHAT_XUNFEI:
                mChatRobot = new XfChatRobot();
                break;
            default:
                break;
        }
        if (mChatRobot != null) {
            mChatRobot.init();
            mChatRobot.setChatBotRole(mChatBotRole);
            mChatRobot.setModelIndex(platformIndex);
            mChatRobot.setChatCallback(mCallback);
        }
    }

    public void setChatBotRole(ChatBotRole chatBotRole) {
        this.mChatBotRole = chatBotRole;
        if (null != mChatRobot) {
            mChatRobot.setChatBotRole(mChatBotRole);
        }
    }

    public void appendChatMessage(String role, String content) {
        if (Constants.GPT_ROLE_USER.equals(role)) {
            mGptChatMessageList.add(new ChatRobotMessage(role, content));
        } else if (Constants.GPT_ROLE_ASSISTANT.equals(role)) {
            if (mGptChatMessageList.size() > 0) {
                ChatRobotMessage chatRobotMessage = mGptChatMessageList.get(mGptChatMessageList.size() - 1);
                if (Constants.GPT_ROLE_ASSISTANT.equals(chatRobotMessage.getRole())) {
                    chatRobotMessage.setContent(chatRobotMessage.getContent() + content);
                } else {
                    mGptChatMessageList.add(new ChatRobotMessage(role, content));
                }
            }

        } else {
            mGptChatMessageList.add(new ChatRobotMessage(role, content));
        }
    }


    public void deleteLastChatMessage() {
        mGptChatMessageList.remove(mGptChatMessageList.size() - 1);
    }


    public void clearChatMessage() {
        mGptChatMessageList.clear();
    }

    public void requestChat(String role, String content) {
        if (!Config.ENABLE_SAVE_GPT_CHAT_HISTORY) {
            clearChatMessage();
        }
        mGptChatMessageList.add(new ChatRobotMessage(role, content));
        requestChat();
    }

    public void requestChat() {
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                if (mGptChatMessageList.size() == 0) {
                    return;
                }
                try {
                    long curTime = System.currentTimeMillis();
                    if (curTime - mLastRequestTime < Constants.INTERVAL_MIN_REQUEST_CHAT) {
                        Thread.sleep(Constants.INTERVAL_MIN_REQUEST_CHAT - curTime + mLastRequestTime);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                mLastRequestTime = System.currentTimeMillis();

                if (null != mChatRobot) {
                    JSONArray requestGptJsonArray = getRequestJsonArray();
                    if (requestGptJsonArray.toJSONString().length() >= Constants.GPT_MAX_TOKENS / 2) {
                        mGptChatMessageList.remove(0);
                        mGptChatMessageList.remove(1);
                    }

                    mChatRobot.requestChat(requestGptJsonArray);
                }
            }
        });
    }

    public void requestChatKeyInfo() {
        if (!Config.ENABLE_SAVE_GPT_CHAT_HISTORY) {
            return;
        }
        if (mGptChatMessageList.size() >= 5) {
            JSONArray requestMessage = getRequestJsonArray();
            switch (mChatPlatformIndex) {
                case Constants.AI_PLATFORM_MINIMAX_CHAT_COMPLETION_PRO_55:
                    MinimaxProMessage minimaxProMessage = new MinimaxProMessage();
                    minimaxProMessage.setSender_type(Constants.MINIMAX_SENDER_TYPE_USER);
                    minimaxProMessage.setSender_name(mChatBotRole.getChatBotUserName());
                    minimaxProMessage.setText(mGptKeyInfoPrompt.replace("botName", null != mChatBotRole ? mChatBotRole.getChatBotName() : "Bot"));
                    requestMessage.add(JSON.toJSON(minimaxProMessage));
                    break;
                default:
                    break;
            }
            mChatRobot.requestChatKeyInfo(requestMessage);
        }
    }

    public void requestChat(String message) {
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                if (null != mChatRobot) {
                    mChatRobot.requestChat(message);
                }
            }
        });
    }

    public void setChatCallback(ChatCallback callback) {
        mCallback = callback;
    }

    private JSONArray getRequestJsonArray() {
        JSONArray requestJsonArray = new JSONArray();
        ChatRobotMessage chatGptMsg;
        switch (mChatPlatformIndex) {
            case Constants.AI_PLATFORM_MINIMAX_CHAT_COMPLETION_5:
            case Constants.AI_PLATFORM_MINIMAX_CHAT_COMPLETION_55:
                MinimaxMessage minmaxMsg = new MinimaxMessage();
                for (int i = 0; i < mGptChatMessageList.size(); i++) {
                    chatGptMsg = mGptChatMessageList.get(i);
                    if (chatGptMsg.getRole().equals(Constants.GPT_ROLE_USER)) {
                        minmaxMsg.setSender_type(Constants.MINIMAX_SENDER_TYPE_USER);
                    } else if (chatGptMsg.getRole().equals(Constants.GPT_ROLE_ASSISTANT)) {
                        minmaxMsg.setSender_type(Constants.MINIMAX_SENDER_TYPE_BOT);
                    } else {
                        continue;
                    }
                    minmaxMsg.setText(chatGptMsg.getContent());
                    requestJsonArray.add(JSON.toJSON(minmaxMsg));
                }
                break;
            case Constants.AI_PLATFORM_MINIMAX_CHAT_COMPLETION_PRO_55:
                MinimaxProMessage minimaxProMessage = new MinimaxProMessage();
                for (int i = 0; i < mGptChatMessageList.size(); i++) {
                    chatGptMsg = mGptChatMessageList.get(i);
                    if (chatGptMsg.getRole().equals(Constants.GPT_ROLE_USER)) {
                        minimaxProMessage.setSender_type(Constants.MINIMAX_SENDER_TYPE_USER);
                        minimaxProMessage.setSender_name("我");
                    } else if (chatGptMsg.getRole().equals(Constants.GPT_ROLE_ASSISTANT)) {
                        minimaxProMessage.setSender_type(Constants.MINIMAX_SENDER_TYPE_BOT);
                        minimaxProMessage.setSender_name(mChatBotRole.getChatBotName());
                    } else {
                        continue;
                    }
                    minimaxProMessage.setText(chatGptMsg.getContent());
                    requestJsonArray.add(JSON.toJSON(minimaxProMessage));
                }
                break;
            case Constants.AI_PLATFORM_CHAT_XUNFEI:
                for (int i = 0; i < mGptChatMessageList.size(); i++) {
                    if (mGptChatMessageList.get(i).getRole().equals(Constants.GPT_ROLE_ASSISTANT)) {
                        continue;
                    }
                    requestJsonArray.add(JSON.toJSON(mGptChatMessageList.get(i)));
                }
                break;
            default:
                for (ChatRobotMessage chatMessage : mGptChatMessageList) {
                    requestJsonArray.add(JSON.toJSON(chatMessage));
                }
                break;
        }

        return requestJsonArray;
    }

    public void cancelChatRequest(boolean cancel) {
        if (null != mChatRobot) {
            mChatRobot.cancelRequestChat(cancel);
        }
    }

    public void close() {
        clearChatMessage();
        if (null != mChatRobot) {
            mChatRobot.close();
        }
    }

}
