package io.agora.metagpt.ui.game;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.jakewharton.rxbinding2.view.RxView;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.agora.metagpt.R;
import io.agora.metagpt.adapter.ChatMessageAdapter;
import io.agora.metagpt.adapter.GamerAdapter;
import io.agora.metagpt.context.GameContext;
import io.agora.metagpt.context.MetaContext;
import io.agora.metagpt.databinding.GameUnderConverActivityBinding;
import io.agora.metagpt.models.DisplayUserInfo;
import io.agora.metagpt.models.wiu.GamerInfo;
import io.agora.metagpt.ui.base.BaseActivity;
import io.agora.metagpt.ui.main.CreateRoomActivity;
import io.agora.metagpt.utils.Constants;
import io.agora.metagpt.utils.KeyCenter;
import io.agora.metagpt.utils.Utils;
import io.agora.rtc2.DataStreamConfig;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.reactivex.disposables.Disposable;

public class GameUnderCoverActivity extends BaseActivity{
    private final static String TAG = Constants.TAG + "-" + GameUnderCoverActivity.class.getSimpleName();
    private final static String KEY_ROLE = "key_role";
    private GameUnderConverActivityBinding binding;

    private AIModeratorViewModel moderatorViewModel;
    private GamerViewModel gamerViewModel;

    private int mStreamId = -1;

    private ChatMessageAdapter mHistoryListAdapter;

    private GamerAdapter mGamerAdapter;

    private boolean aiCanSpeak; // 同一个按钮，本轮发言-> 请求 ai 发言
    private boolean aiCanVote; // 同一个按钮，本轮投票-> 请求 ai 投票

    private int role;

    public static void startActivity(Context activity, int role) {
        Intent intent = new Intent(activity, GameUnderCoverActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra(KEY_ROLE, role);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initRtc();
    }

    @Override
    protected void initContentView() {
        super.initContentView();
        binding = GameUnderConverActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.ivLoading, (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.ivLoading.setPaddingRelative(inset.left, 0, inset.right, 0);
            return WindowInsetsCompat.CONSUMED;
        });
        binding.ivLoading.postDelayed(() -> binding.ivLoading.setVisibility(View.GONE), 1000);
    }

    @Override
    protected void initData() {
        super.initData();
        role = getIntent().getIntExtra(KEY_ROLE, Constants.GAME_ROLE_USER);
        if (role == Constants.GAME_ROLE_USER) {
            gamerViewModel = new ViewModelProvider(this).get(GamerViewModel.class);
            gamerViewModel.initData();
        } else if (role == Constants.GAME_ROLE_MODERATOR) {
            moderatorViewModel = new ViewModelProvider(this).get(AIModeratorViewModel.class);
            moderatorViewModel.initData();
        }
    }

    @Override
    protected void initView() {
        binding.tvRoomName.setText(MetaContext.getInstance().getRoomName());
        binding.tvNickname.setText(MetaContext.getInstance().getUserName());


        List<DisplayUserInfo> listData = new ArrayList<>();
        for (int i = 0; i < Constants.MAX_GAMER_NUM; i++) {
            DisplayUserInfo userInfo = new DisplayUserInfo();
            userInfo.setNumber(i + 1);
            listData.add(userInfo);
        }
        mGamerAdapter = new GamerAdapter(this, listData);
        binding.recyclerGamer.setLayoutManager(new GridLayoutManager(this, Constants.MAX_GAMER_NUM / 2));
        binding.recyclerGamer.setAdapter(mGamerAdapter);

        if (role == Constants.GAME_ROLE_MODERATOR) {
            binding.layoutGamerBottom.getRoot().setVisibility(View.GONE);
            binding.layoutModeratorBottom.getRoot().setVisibility(View.VISIBLE);
            mHistoryListAdapter = new ChatMessageAdapter(this, moderatorViewModel.mChatMessageDataList);
            binding.recyclerMessage.setAdapter(mHistoryListAdapter);
            binding.recyclerMessage.setLayoutManager(new LinearLayoutManager(this));
            binding.recyclerMessage.addItemDecoration(new ChatMessageAdapter.SpacesItemDecoration(10));
            moderatorViewModel.iMessageView = mHistoryListAdapter;
            moderatorViewModel.iMessageViewListener = () -> {
                binding.recyclerMessage.scrollToPosition(mHistoryListAdapter.getDataList().size() - 1);
            };
        } else if (role == Constants.GAME_ROLE_USER) {
            binding.layoutGamerBottom.getRoot().setVisibility(View.VISIBLE);
            binding.layoutModeratorBottom.getRoot().setVisibility(View.GONE);
            mHistoryListAdapter = new ChatMessageAdapter(getApplicationContext(), gamerViewModel.mChatMessageDataList);
            binding.recyclerMessage.setAdapter(mHistoryListAdapter);
            binding.recyclerMessage.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
            binding.recyclerMessage.addItemDecoration(new ChatMessageAdapter.SpacesItemDecoration(10));
            gamerViewModel.iMessageView = mHistoryListAdapter;
            gamerViewModel.iMessageViewListener = () -> {
                binding.recyclerMessage.scrollToPosition(mHistoryListAdapter.getDataList().size() - 1);
            };
        }
    }

    @Override
    protected void initListener() {
        super.initListener();
        if (role == Constants.GAME_ROLE_MODERATOR) {// 主持人
            moderatorViewModel.viewStatus().observe(this, moderatorViewStatus -> {
                if (moderatorViewStatus instanceof ModeratorViewStatus.Initial) { // 初始状态
                    binding.layoutModeratorBottom.btnModeratorStartGame.setVisibility(View.VISIBLE);
                    binding.layoutModeratorBottom.btnModeratorEndGame.setVisibility(View.GONE);
                    binding.layoutModeratorBottom.btnModeratorStartSpeak.setVisibility(View.GONE);
                    binding.layoutModeratorBottom.btnModeratorStartVote.setVisibility(View.GONE);
                } else if (moderatorViewStatus instanceof ModeratorViewStatus.StartGame) { // 开始游戏
                    binding.layoutModeratorBottom.btnModeratorStartGame.setVisibility(View.GONE);
                    binding.layoutModeratorBottom.btnModeratorEndGame.setVisibility(View.VISIBLE);
                    binding.layoutModeratorBottom.btnModeratorStartSpeak.setVisibility(View.VISIBLE);
                    binding.layoutModeratorBottom.btnModeratorStartSpeak.setText(R.string.start_new_speak);
                    binding.layoutModeratorBottom.btnModeratorStartSpeak.setBackgroundResource(R.drawable.bg_button_7a5_left_corners);
                    binding.layoutModeratorBottom.btnModeratorStartSpeak.setEnabled(true);
                    binding.layoutModeratorBottom.btnModeratorStartVote.setVisibility(View.VISIBLE);
                    binding.layoutModeratorBottom.btnModeratorStartVote.setText(R.string.start_vote_tip);
                    binding.layoutModeratorBottom.btnModeratorStartVote.setBackgroundResource(R.drawable.bg_button_7a5_right_corners);
                    binding.layoutModeratorBottom.btnModeratorStartVote.setEnabled(true);
                    mGamerAdapter.restore();
                } else if (moderatorViewStatus instanceof ModeratorViewStatus.RoundsReady) { // 新一轮准备
                    aiCanSpeak = false;
                    aiCanVote = false;
                    binding.layoutModeratorBottom.btnModeratorStartSpeak.setVisibility(View.VISIBLE);
                    binding.layoutModeratorBottom.btnModeratorStartSpeak.setText(R.string.start_new_speak);
                    binding.layoutModeratorBottom.btnModeratorStartSpeak.setBackgroundResource(R.drawable.bg_button_7a5_left_corners);
                    binding.layoutModeratorBottom.btnModeratorStartSpeak.setEnabled(true);
                    binding.layoutModeratorBottom.btnModeratorStartVote.setVisibility(View.VISIBLE);
                    binding.layoutModeratorBottom.btnModeratorStartVote.setText(R.string.start_vote_tip);
                    binding.layoutModeratorBottom.btnModeratorStartVote.setBackgroundResource(R.drawable.bg_button_7a5_right_corners);
                    binding.layoutModeratorBottom.btnModeratorStartVote.setEnabled(true);
                } else if (moderatorViewStatus instanceof ModeratorViewStatus.AISpeakOver) { // AI 发言结束
                    binding.layoutModeratorBottom.btnModeratorStartSpeak.setEnabled(true);
                    binding.layoutModeratorBottom.btnModeratorStartSpeak.setText(R.string.start_new_speak);
                    binding.layoutModeratorBottom.btnModeratorStartSpeak.setBackgroundResource(R.drawable.bg_button_7a5_left_corners);
                } else if (moderatorViewStatus instanceof ModeratorViewStatus.EndGame) { // 结束游戏
                    binding.layoutModeratorBottom.btnModeratorStartGame.setVisibility(View.VISIBLE);
                    binding.layoutModeratorBottom.btnModeratorEndGame.setVisibility(View.GONE);
                    binding.layoutModeratorBottom.btnModeratorStartSpeak.setVisibility(View.GONE);
                    binding.layoutModeratorBottom.btnModeratorStartVote.setVisibility(View.GONE);
                    binding.layoutModeratorBottom.btnModeratorStartVote.setEnabled(true);
                    binding.layoutModeratorBottom.btnModeratorStartVote.setText(R.string.start_vote_tip);
                    binding.layoutModeratorBottom.btnModeratorStartVote.setBackgroundResource(R.drawable.bg_button_7a5_right_corners);
                }
            });
            moderatorViewModel.displayUserInfo().observe(this, displayUserInfo -> {
                mGamerAdapter.update(displayUserInfo);
            });
            moderatorViewModel.removeDisplayUser().observe(this, displayUserInfo -> {
                mGamerAdapter.update(displayUserInfo);
            });
            moderatorViewModel.aiGamerInfo().observe(this, gamerInfo -> {
                // TODO: 2023/5/24 ai gamerInfo
            });
            moderatorViewModel.gameSpeakInfo().observe(this, displayUserInfo -> {
                mGamerAdapter.update(displayUserInfo);
            });
            moderatorViewModel.aiCanSpeak().observe(this, result -> {
                if (result) {
                    binding.layoutModeratorBottom.btnModeratorStartSpeak.setText(R.string.request_ai_speaking);
                    binding.layoutModeratorBottom.btnModeratorStartSpeak.setBackgroundResource(R.drawable.bg_button_693_left_corners);
                } else {
                    binding.layoutModeratorBottom.btnModeratorStartSpeak.setText(R.string.start_new_speak);
                    binding.layoutModeratorBottom.btnModeratorStartSpeak.setBackgroundResource(R.drawable.bg_button_7a5_left_corners);
                }
            });
            moderatorViewModel.aiCanVote().observe(this, result -> {
                if (result) {
                    binding.layoutModeratorBottom.btnModeratorStartSpeak.setText(R.string.request_ai_voting);
                    binding.layoutModeratorBottom.btnModeratorStartSpeak.setBackgroundResource(R.drawable.bg_button_693_left_corners);
                } else {
                    binding.layoutModeratorBottom.btnModeratorStartSpeak.setText(R.string.start_new_speak);
                    binding.layoutModeratorBottom.btnModeratorStartSpeak.setBackgroundResource(R.drawable.bg_button_7a5_left_corners);
                }
            });
        } else if (role == Constants.GAME_ROLE_USER) { // 玩家
            gamerViewModel.viewStatus().observe(this, viewStatus -> {
                binding.layoutGamerBottom.tvWaitingGame.setVisibility(View.GONE);
                binding.layoutGamerBottom.tvMyWords.setVisibility(View.GONE);
                binding.layoutGamerBottom.tvOtherPlayerSpeak.setVisibility(View.GONE);
                binding.layoutGamerBottom.btnUserSpeak.setVisibility(View.GONE);
                binding.layoutGamerBottom.tvYourOutSpeak.setVisibility(View.GONE);
                binding.layoutGamerBottom.layoutUserVote.setVisibility(View.GONE);
                binding.layoutGamerBottom.tvYourOutVote.setVisibility(View.GONE);
                if (viewStatus instanceof GamerViewStatus.Initial) {
                    binding.layoutGamerBottom.tvWaitingGame.setVisibility(View.VISIBLE);
                } else if (viewStatus instanceof GamerViewStatus.StartGame) {
                    mGamerAdapter.restore();
                    binding.layoutGamerBottom.tvMyWords.setVisibility(View.VISIBLE);
                    binding.layoutGamerBottom.tvOtherPlayerSpeak.setVisibility(View.VISIBLE);
                } else if (viewStatus instanceof GamerViewStatus.StartSpeak) {
                    binding.layoutGamerBottom.tvMyWords.setVisibility(View.VISIBLE);
                    if (gamerViewModel.isOut()) {
                        binding.layoutGamerBottom.tvMyWords.setVisibility(View.VISIBLE);
                        binding.layoutGamerBottom.tvYourOutSpeak.setVisibility(View.VISIBLE);
                    } else {
                        binding.layoutGamerBottom.btnUserSpeak.setVisibility(View.VISIBLE);
                        binding.layoutGamerBottom.btnUserSpeak.setText(R.string.start_speak);
                        binding.layoutGamerBottom.btnUserSpeak.setBackgroundResource(R.drawable.bg_button_7a5_corners);
                    }
                } else if (viewStatus instanceof GamerViewStatus.StartVote) { // 投票
                    if (gamerViewModel.isOut()) {
                        binding.layoutGamerBottom.tvYourOutVote.setVisibility(View.VISIBLE);
                    } else {
                        binding.layoutGamerBottom.layoutUserVote.setVisibility(View.VISIBLE);
                    }
                    initVoteView();
                } else if (viewStatus instanceof GamerViewStatus.OtherSpeak) { //其他用户发言
                    binding.layoutGamerBottom.tvMyWords.setVisibility(View.VISIBLE);
                    if (((GamerViewStatus.OtherSpeak) viewStatus).isSpeaking) {
                        //发言中
                        binding.layoutGamerBottom.tvOtherPlayerSpeak.setVisibility(View.VISIBLE);
                    } else {
                        //发言结束
                        binding.layoutGamerBottom.btnUserSpeak.setText(R.string.start_speak);
                        binding.layoutGamerBottom.btnUserSpeak.setVisibility(View.VISIBLE);
                        binding.layoutGamerBottom.btnUserSpeak.setBackgroundResource(R.drawable.bg_button_7a5_corners);
                        binding.layoutGamerBottom.tvOtherPlayerSpeak.setVisibility(View.GONE);
                    }
                } else if (viewStatus instanceof GamerViewStatus.SelfSpeak) { // 自己发言
                    binding.layoutGamerBottom.tvMyWords.setVisibility(View.VISIBLE);
                    if (((GamerViewStatus.SelfSpeak) viewStatus).isSpeaking) {
                        //发言中
                        binding.layoutGamerBottom.btnUserSpeak.setVisibility(View.VISIBLE);
                        binding.layoutGamerBottom.btnUserSpeak.setText(R.string.end_speak);
                        binding.layoutGamerBottom.btnUserSpeak.setBackgroundResource(R.drawable.bg_button_067_corners);
                    } else {
                        //发言结束
                        binding.layoutGamerBottom.tvOtherPlayerSpeak.setVisibility(View.VISIBLE);
                    }
                } else if (viewStatus instanceof GamerViewStatus.EndGame) { // 结束游戏
                    binding.layoutGamerBottom.tvWaitingGame.setVisibility(View.VISIBLE);
                }
            });
            gamerViewModel.gameInfoList().observe(this, gamerInfoList -> {

            });
            gamerViewModel.displayUserInfo().observe(this, userInfoList -> {
                mGamerAdapter.update(userInfoList);
            });
            gamerViewModel.removeDisplayUser().observe(this, displayUserInfo -> {
                mGamerAdapter.update(displayUserInfo);
            });
            gamerViewModel.selfGamerInfo().observe(this, gamerInfo -> {
                binding.layoutGamerBottom.tvMyWords.setText(getString(R.string.my_words, gamerInfo.getWord()));
                if (gamerInfo.isOut()) {
                }
            });
            gamerViewModel.gameSpeakInfo().observe(this, displayUserInfo -> {
                mGamerAdapter.update(displayUserInfo);
            });
        }
    }

    private void initVoteView() {
        binding.layoutGamerBottom.layoutVoteContainer.setVisibility(View.VISIBLE);
        binding.layoutGamerBottom.layoutVoteContainer.removeAllViews();
        List<GamerInfo> gamerInfoList = gamerViewModel.mGamerInfoList;
        Button button;
        for (GamerInfo gamerInfo : gamerInfoList) {
            if (gamerInfo.getUid() == KeyCenter.getUserUid()) {
                continue;
            }
            button = new Button(this);
            button.setId(gamerInfo.getUid());
            button.setText(getString(R.string.number, gamerInfo.getGamerNumber()));
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            button.setLayoutParams(layoutParams);
            ((ViewGroup.MarginLayoutParams) (button.getLayoutParams())).rightMargin = Utils.dip2px(this, 6f);
            button.setBackgroundResource(R.drawable.bg_button_7a5_corners);
            button.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white, null));
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
            button.setMinWidth(Utils.dip2px(this, 56f)); // 有个默认宽度
            button.setOnClickListener(v -> {
                gamerViewModel.sendUserVoteInfo(gamerInfo.getGamerNumber());
            });
            if (gamerInfo.isOut()) {
                button.setEnabled(false);
                button.setAlpha(0.5f);
            } else {
                button.setEnabled(true);
                button.setAlpha(1f);
            }
            binding.layoutGamerBottom.layoutVoteContainer.addView(button);
        }
    }

    @Override
    protected void initClickEvent() {
        super.initClickEvent();
        Disposable disposable;
        disposable = RxView.clicks(binding.btnClose).throttleFirst(1, TimeUnit.SECONDS).subscribe(o -> {
            exit();
        });
        compositeDisposable.add(disposable);

        // =============== moderator start ===================
        if (role == Constants.GAME_ROLE_MODERATOR) {
            // 点击开始游戏
            disposable = RxView.clicks(binding.layoutModeratorBottom.btnModeratorStartGame).throttleFirst(1, TimeUnit.SECONDS).subscribe(o -> {
                moderatorViewModel.startGame();
            });
            compositeDisposable.add(disposable);

            // 点击结束游戏
            disposable = RxView.clicks(binding.layoutModeratorBottom.btnModeratorEndGame).throttleFirst(1, TimeUnit.SECONDS).subscribe(o -> {
                moderatorViewModel.endGame(getString(R.string.end_game_tips));
            });
            compositeDisposable.add(disposable);

            // 点击新一轮发言 or 请求ai 发言
            // 开始新一轮发言→点击后→请求AI发言
            disposable = RxView.clicks(binding.layoutModeratorBottom.btnModeratorStartSpeak).throttleFirst(1, TimeUnit.SECONDS).subscribe(o -> {
                if (aiCanSpeak) {
                    aiCanSpeak = false;
                    moderatorViewModel.roundsSpeak(false);
                    binding.layoutModeratorBottom.btnModeratorStartSpeak.setText(R.string.request_ai_speak_ing);
                    binding.layoutModeratorBottom.btnModeratorStartSpeak.setEnabled(false);
                    binding.layoutModeratorBottom.btnModeratorStartSpeak.setBackgroundResource(R.drawable.bg_button_693_left_corners);
                } else {
                    aiCanSpeak = true;
                    moderatorViewModel.roundsSpeak(true);
                    binding.layoutModeratorBottom.btnModeratorStartSpeak.setText(R.string.request_ai_speaking);
                    binding.layoutModeratorBottom.btnModeratorStartSpeak.setBackgroundResource(R.drawable.bg_button_7a5_left_corners);
                }
            });
            compositeDisposable.add(disposable);

            // 点击本轮投票 or 请求 ai 投票
            // 开始本轮投票→点击后→请求AI投票
            disposable = RxView.clicks(binding.layoutModeratorBottom.btnModeratorStartVote).throttleFirst(1, TimeUnit.SECONDS).subscribe(o -> {
                if (aiCanVote) {
                    aiCanVote = false;
                    moderatorViewModel.roundsVote(false);
                    binding.layoutModeratorBottom.btnModeratorStartVote.setText(R.string.request_ai_vote_ing);
                    binding.layoutModeratorBottom.btnModeratorStartVote.setEnabled(false);
                    binding.layoutModeratorBottom.btnModeratorStartVote.setBackgroundResource(R.drawable.bg_button_693_right_corners);
                } else {
                    aiCanVote = true;
                    moderatorViewModel.roundsVote(true);
                    binding.layoutModeratorBottom.btnModeratorStartVote.setText(R.string.request_ai_voting);
                    binding.layoutModeratorBottom.btnModeratorStartVote.setBackgroundResource(R.drawable.bg_button_7a5_right_corners);
                }
            });
            compositeDisposable.add(disposable);
        } else if (role == Constants.GAME_ROLE_USER) {
            // 点击开始发言
            disposable = RxView.clicks(binding.layoutGamerBottom.btnUserSpeak).throttleFirst(1, TimeUnit.SECONDS).subscribe(o -> {
                gamerViewModel.startUserSpeak();
            });
            compositeDisposable.add(disposable);
        }
    }

    private void initRtc() {
        if (MetaContext.getInstance().initializeRtc(this.getApplicationContext())) {
            registerRtc();
            if (role == Constants.GAME_ROLE_MODERATOR) {
                moderatorViewModel.updatePublishCustomAudioTrackChannelOptions();
            }
            MetaContext.getInstance().joinChannel(io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER);
        } else {
            Log.i(TAG, "initRtc fail");
        }
    }

    @Override
    public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
        if (-1 == mStreamId) {
            DataStreamConfig cfg = new DataStreamConfig();
            cfg.syncWithAudio = false;
            cfg.ordered = true;
            mStreamId = MetaContext.getInstance().createDataStream(cfg);
        }
        runOnUiThread(() -> {
            if (role == Constants.GAME_ROLE_MODERATOR) {
                // 默认添加AI
                DisplayUserInfo displayUserInfo = new DisplayUserInfo(KeyCenter.getAiUid(), getString(R.string.ai), true);
                moderatorViewModel.addUserInfo(displayUserInfo);
                moderatorViewModel.onJoinSuccess(mStreamId);
            } else if (role == Constants.GAME_ROLE_USER) {
                DisplayUserInfo displayUserInfo = new DisplayUserInfo(KeyCenter.getUserUid(), GameContext.getInstance().getUserName(), false);
                gamerViewModel.addUserInfo(displayUserInfo);
                gamerViewModel.onJoinSuccess(mStreamId);
                gamerViewModel.registerAudioFrameObserver(this);
            }
        });
    }

    @Override
    public void onLeaveChannel(IRtcEngineEventHandler.RtcStats stats) {
        runOnUiThread(() -> {
            MetaContext.getInstance().destroy();
            goBackHome();
        });
    }

    @Override
    public void onUserJoined(int uid, int elapsed) {
        super.onUserJoined(uid, elapsed);
    }

    @Override
    public void onUserOffline(int uid, int reason) {
        super.onUserOffline(uid, reason);
        runOnUiThread(() -> {
            if (role == Constants.GAME_ROLE_MODERATOR) {
                moderatorViewModel.onUserOffline(uid);
            } else if (role == Constants.GAME_ROLE_USER) {
                gamerViewModel.onUserOffline(uid);
            }
        });
    }

    @Override
    public void onStreamMessage(int uid, int streamId, byte[] data) {
        super.onStreamMessage(uid, streamId, data);
        if (role == Constants.GAME_ROLE_MODERATOR) {
            moderatorViewModel.onStreamMessage(uid, streamId, data);
        } else if (role == Constants.GAME_ROLE_USER) {
            gamerViewModel.onStreamMessage(uid, streamId, data);
        }
    }

    private void goBackHome() {
        Intent intent = new Intent(GameUnderCoverActivity.this, CreateRoomActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        finish();
    }

    private void exit() {
        if (role == Constants.GAME_ROLE_MODERATOR) {
            moderatorViewModel.exit();
        } else {
            gamerViewModel.exit();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initRtc();
    }

    @Override
    public boolean onRecordAudioFrame(String channelId, int type, int samplesPerChannel, int bytesPerSample, int channels, int samplesPerSec, ByteBuffer buffer, long renderTimeMs, int avsync_type) {
        int length = buffer.remaining();
        byte[] origin = new byte[length];
        buffer.get(origin);
        buffer.flip();
        if (role == Constants.GAME_ROLE_MODERATOR) {

        } else {
            gamerViewModel.requestStt(origin);
        }
        return false;
    }
}
