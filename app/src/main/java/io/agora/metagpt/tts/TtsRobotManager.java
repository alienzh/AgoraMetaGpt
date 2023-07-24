package io.agora.metagpt.tts;

import android.content.Context;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import io.agora.metagpt.R;
import io.agora.metagpt.inf.ITtsRobot;
import io.agora.metagpt.inf.TtsCallback;
import io.agora.metagpt.models.chat.ChatBotRole;
import io.agora.metagpt.tts.minimax.MinimaxTtsRobot;
import io.agora.metagpt.tts.ms.MsTtsRobot;
import io.agora.metagpt.tts.xf.XfTtsRobot;
import io.agora.metagpt.utils.Constants;
import io.agora.metagpt.utils.Utils;

public class TtsRobotManager {
    private Context mContext;
    private ITtsRobot mTtsRobot;
    private String mTempTtsPcmFilePath;
    private TtsCallback mTtsCallback;

    private boolean mIsSpeaking;

    private boolean mRingBufferReady;

    private String[] mChatTipMessages;
    private List<String> mRequestChatTipPaths;
    private String mTtsVoiceName;

    private String mPendingTtsMessage;
    private final Pattern mSentencePattern;

    private String[] mSttAiSentence;
    private String mSttAiSentenceReplace;

    private String mLastTtsSentence;
    private boolean mWelcomeTip;
    private ChatBotRole mChatBotRole;

    private final static int WAIT_AUDIO_FRAME_COUNT = 3;


    public TtsRobotManager() {
        mIsSpeaking = false;
        mRingBufferReady = false;
        mSentencePattern = Pattern.compile("[,.?!:;，。？！：；]");
        mWelcomeTip = false;
    }

    public void init(Context context, String tempPcmPath) {
        mContext = context;
        mTempTtsPcmFilePath = tempPcmPath;
        clearData();
        mIsSpeaking = false;
        mSttAiSentence = context.getResources().getStringArray(R.array.stt_ai_self_sentence);
        if (null != mChatBotRole) {
            mSttAiSentenceReplace = mChatBotRole.getIntroduceReplace();
        }
    }

    public void setTtsCallback(TtsCallback ttsCallback) {
        mTtsCallback = ttsCallback;
    }

    public void setAiTtsPlatformIndex(int aiTtsPlatformIndex) {
        switch (aiTtsPlatformIndex) {
            case Constants.TTS_PLATFORM_XF:
                mTtsRobot = new XfTtsRobot();
                break;
            case Constants.TTS_PLATFORM_MS:
                mTtsRobot = new MsTtsRobot();
                break;
            case Constants.TTS_PLATFORM_MINIMAX:
                mTtsRobot = new MinimaxTtsRobot();
                break;
            default:
                break;
        }
        if (null != mTtsRobot) {
            mTtsRobot.init(mTempTtsPcmFilePath);
            mTtsRobot.setTtsCallback(mTtsCallback);
            mTtsRobot.setVoiceName(mTtsVoiceName);
            mTtsRobot.setIsSpeaking(mIsSpeaking);
            checkTipTtsRequest();
            if (!mWelcomeTip && null != mChatBotRole) {
                mWelcomeTip = true;
                mTtsRobot.tts(mChatBotRole.getWelcomeMessage(), false);
            }
        }
    }

    public void setTtsPlatformVoiceName(String ttsVoiceName, boolean fromUser) {
        if (fromUser) {
            if (null != mTtsRobot) {
                mTtsRobot.setVoiceName(ttsVoiceName);
                checkTipTtsRequest();
            }
        } else {
            mTtsVoiceName = ttsVoiceName;
        }
    }

    public void setChatTipMessages(String[] chatTipMessages) {
        this.mChatTipMessages = chatTipMessages;
    }

    public void requestTts(String message) {
        if (TextUtils.isEmpty(message)) {
            return;
        }
        if (!TextUtils.isEmpty(mPendingTtsMessage)) {
            message = mPendingTtsMessage + message;
        }
        mPendingTtsMessage = "";

        String[] splitSentence = mSentencePattern.split(message);
        if (splitSentence.length > 0) {
            String lastSentence = message.substring(message.indexOf(splitSentence[splitSentence.length - 1]));
            if (!mSentencePattern.matcher(lastSentence).find()) {
                mPendingTtsMessage = lastSentence;
            }

            if (!TextUtils.isEmpty(mPendingTtsMessage)) {
                message = message.substring(0, message.indexOf(mPendingTtsMessage));
            }
        }
        if (null != mTtsRobot && !TextUtils.isEmpty(message)) {
            requestTtsMessage(message);
        }
    }

    public void requestTtsFinish() {
        if (!TextUtils.isEmpty(mPendingTtsMessage)) {
            if (null != mTtsRobot) {
                requestTtsMessage(mPendingTtsMessage);
                mPendingTtsMessage = "";
            }
        }
    }

    public byte[] getTtsBuffer(int length) {
        if (null != mTtsRobot) {
            if (mRingBufferReady) {
                return mTtsRobot.getTtsBuffer(length);
            } else {
                //等待length*WAIT_AUDIO_FRAME_COUNT大小的数据
                if (mTtsRobot.getTtsBufferLength() >= length * WAIT_AUDIO_FRAME_COUNT) {
                    mRingBufferReady = true;
                    return mTtsRobot.getTtsBuffer(length);
                } else {
                    return null;
                }
            }
        }
        return null;
    }


    public void clearData() {
        mPendingTtsMessage = "";
        if (null != mTtsRobot) {
            mRingBufferReady = false;
            mTtsRobot.clearRingBuffer();
        }
    }

    public void setIsSpeaking(boolean isSpeaking) {
        mIsSpeaking = isSpeaking;
        if (null != mTtsRobot) {
            mTtsRobot.setIsSpeaking(isSpeaking);
        }
    }

    public void close() {
        if (null != mTtsRobot) {
            mTtsRobot.close();
        }
        mWelcomeTip = false;
    }

    public void requestChatTip() {
        if (mRingBufferReady) {
            clearData();
        }
        if (null != mTtsRobot) {
            String pcmFilePath = null;
            int index = 0;
            do {
                int fileIndex = new Random().nextInt(mRequestChatTipPaths.size());
                if (new File(mRequestChatTipPaths.get(fileIndex)).exists()) {
                    pcmFilePath = mRequestChatTipPaths.get(fileIndex);
                    break;
                }
                index++;
                if (index >= 50) {
                    break;
                }
            } while (true);
            if (!TextUtils.isEmpty(pcmFilePath)) {
                mTtsRobot.addTtsBuffer(Utils.readFileToByteArray(pcmFilePath));
            }
        }
    }

    private void checkTipTtsRequest() {
        if (null == mTtsRobot) {
            return;
        }

        Map<String, String> requestChatTipMessages = new HashMap<>(mChatTipMessages.length);
        if (null == mRequestChatTipPaths) {
            mRequestChatTipPaths = new ArrayList<>(mChatTipMessages.length);
        } else {
            mRequestChatTipPaths.clear();
        }

        mRequestChatTipPaths = new ArrayList<>(mChatTipMessages.length);
        String pcmPath;
        for (int i = 0; i < mChatTipMessages.length; i++) {
            pcmPath = mTempTtsPcmFilePath + "_tip_" + mTtsRobot.getTtsPlatformName() + "_" + i + ".pcm";
            mRequestChatTipPaths.add(pcmPath);
            if (!new File(pcmPath).exists()) {
                requestChatTipMessages.put(mChatTipMessages[i], pcmPath);
            }
        }
        if (requestChatTipMessages.size() > 0) {
            mTtsRobot.tipTts(requestChatTipMessages);
        }
    }

    private void requestTtsMessage(String message) {
        if (!TextUtils.isEmpty(mSttAiSentenceReplace)) {
            int replaceCount = 0;
            for (String selfSentence : mSttAiSentence) {
                if (message.contains(selfSentence)) {
                    String[] splitSentence = mSentencePattern.split(message);
                    for (String s : splitSentence) {
                        if (s.contains(selfSentence)) {
                            message = message.replace(s, mSttAiSentenceReplace);
                            replaceCount++;
                            break;
                        }
                    }
                }
            }
            if (replaceCount > 1) {
                try {
                    message = Utils.removeDuplicates(message, mSttAiSentenceReplace);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        mTtsRobot.tts(message);
        mLastTtsSentence = message;
    }

    public void setChatBotRole(ChatBotRole chatBotRole) {
        this.mChatBotRole = chatBotRole;
    }

    public void cancelTtsRequest(boolean cancel) {
        if (cancel) {
            clearData();
        }
        if (null != mTtsRobot) {
            mTtsRobot.cancelTtsRequest(cancel);
        }
    }
}
