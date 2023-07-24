package io.agora.metagpt.ui.main;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jakewharton.rxbinding2.view.RxView;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.agora.metagpt.R;
import io.agora.metagpt.chat.gpt.GptRetrofitManager;
import io.agora.metagpt.chat.minimax.MiniMaxRetrofitManager;
import io.agora.metagpt.context.GameContext;
import io.agora.metagpt.context.MetaContext;
import io.agora.metagpt.databinding.CreateRoomFragmentBinding;
import io.agora.metagpt.models.GameInfo;
import io.agora.metagpt.tts.minimax.MinimaxTtsRetrofitManager;
import io.agora.metagpt.tts.ms.MsTtsRetrofitManager;
import io.agora.metagpt.ui.game.GameAIPartnerActivity;
import io.agora.metagpt.ui.game.GameUnderCoverActivity;
import io.agora.metagpt.ui.base.BaseFragment;
import io.agora.metagpt.utils.Constants;
import io.agora.metagpt.utils.KeyCenter;
import io.reactivex.disposables.Disposable;

public class CreateRoomFragment extends BaseFragment {
    private static final String TAG = CreateRoomFragment.class.getSimpleName();

    private CreateRoomFragmentBinding binding;

    private Random random = new Random();
    private String[] nicknamePrefix;
    private String[] nicknameSuffix;

    private int gameId = Constants.GAME_AI_VOICE_ASSISTANT;
    private int role = Constants.GAME_ROLE_MODERATOR;

    public static CreateRoomFragment newInstance() {
        return new CreateRoomFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return binding.getRoot();
    }

    @Override
    protected void initContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, boolean attachToParent) {
        super.initContentView(inflater, container, attachToParent);
        binding = CreateRoomFragmentBinding.inflate(inflater, container, attachToParent);
    }

    @Override
    protected void initData() {
        super.initData();
        nicknamePrefix = getResources().getStringArray(R.array.user_nickname_prefix);
        nicknameSuffix = getResources().getStringArray(R.array.user_nickname_suffix);
    }

    @Override
    protected void initView() {
        super.initView();
        int prefixIndex = random.nextInt(nicknamePrefix.length);
        int suffixIndex = random.nextInt(nicknameSuffix.length);
        binding.etNickname.setText(nicknamePrefix[prefixIndex] + nicknameSuffix[suffixIndex]);
        binding.etNickname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                GameContext.getInstance().setUserName(s.toString());
            }
        });

        binding.etRoomName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().isEmpty()) {
                    binding.tvRoomNameEmpty.setVisibility(View.VISIBLE);
                } else {
                    binding.tvRoomNameEmpty.setVisibility(View.INVISIBLE);
                }
                GameContext.getInstance().setRoomName(s.toString());
            }
        });

        InputFilter typeFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                Pattern p = Pattern.compile("[0-9a-zA-Z]+");
                Matcher m = p.matcher(source.toString());
                if (!m.matches()) {
                    return "";
                }
                ;
                return null;
            }
        };
        binding.etRoomName.setFilters(new InputFilter[]{typeFilter});
        binding.btnModerator.setActivated(true);
        binding.btnUser.setActivated(false);
        binding.groupNickname.setVisibility(View.INVISIBLE);

        GameContext.getInstance().setGameRole(Constants.GAME_ROLE_MODERATOR);
        GameContext.getInstance().setUserName(getResources().getString(R.string.moderator));
    }

    @Override
    protected void initClickEvent() {
        super.initClickEvent();
        //防止多次频繁点击异常处理
        Disposable disposable;
        disposable = RxView.clicks(binding.btnEnterRoom).throttleFirst(1, TimeUnit.SECONDS).subscribe(o -> {
            if (TextUtils.isEmpty(GameContext.getInstance().getRoomName())) {
                Toast.makeText(requireActivity(), R.string.enter_room_name, Toast.LENGTH_LONG).show();
                binding.tvRoomNameEmpty.setVisibility(View.VISIBLE);
            } else {
                switch (gameId) {
                    case Constants.GAME_WHO_IS_UNDERCOVER:
                        if (Constants.GAME_ROLE_USER == GameContext.getInstance().getGameRole()) {
                            if (TextUtils.isEmpty(GameContext.getInstance().getUserName())) {
                                Toast.makeText(requireActivity(), R.string.enter_nickname, Toast.LENGTH_LONG).show();
                            } else {
                                MetaContext.getInstance().initRoomNameAndUid(GameContext.getInstance().getRoomName(), KeyCenter.getUserUid(), GameContext.getInstance().getUserName());
                                initServices();
                                GameUnderCoverActivity.startActivity(getContext(), Constants.GAME_ROLE_USER);
                            }
                        } else {
                            MetaContext.getInstance().initRoomNameAndUid(GameContext.getInstance().getRoomName(), KeyCenter.getModeratorUid(), requireContext().getResources().getString(R.string.moderator));
                            initServices();
                            GameUnderCoverActivity.startActivity(getContext(), Constants.GAME_ROLE_MODERATOR);
                        }
                        break;
                    case Constants.GAME_AI_VOICE_ASSISTANT:
                        if (TextUtils.isEmpty(GameContext.getInstance().getUserName())) {
                            Toast.makeText(requireActivity(), R.string.enter_nickname, Toast.LENGTH_LONG).show();
                        } else {
                            MetaContext.getInstance().initRoomNameAndUid(GameContext.getInstance().getRoomName(), KeyCenter.getUserUid(), GameContext.getInstance().getUserName());
                            initServices();
                            GameAIPartnerActivity.startActivity(getContext());
                        }
                        break;
                    default:
                        break;
                }

            }
        });
        compositeDisposable.add(disposable);

        disposable = RxView.clicks(binding.btnModerator).throttleFirst(1, TimeUnit.SECONDS).subscribe(o -> {
            role = Constants.GAME_ROLE_MODERATOR;
            GameContext.getInstance().setGameRole(Constants.GAME_ROLE_MODERATOR);
            GameContext.getInstance().setUserName(getResources().getString(R.string.moderator));
            updateView();
        });
        compositeDisposable.add(disposable);

        disposable = RxView.clicks(binding.btnUser).throttleFirst(1, TimeUnit.SECONDS).subscribe(o -> {
            role = Constants.GAME_ROLE_USER;
            GameContext.getInstance().setGameRole(Constants.GAME_ROLE_USER);
            GameContext.getInstance().setUserName(binding.etNickname.getText().toString());
            updateView();
        });
        compositeDisposable.add(disposable);

        disposable = RxView.clicks(binding.tvNicknameRandom).throttleFirst(1, TimeUnit.SECONDS).subscribe(o -> {
            int prefixIndex = random.nextInt(nicknamePrefix.length);
            int suffixIndex = random.nextInt(nicknameSuffix.length);
            binding.etNickname.setText(nicknamePrefix[prefixIndex] + nicknameSuffix[suffixIndex]);
        });
        compositeDisposable.add(disposable);

        disposable = RxView.clicks(binding.btnAiPartner).throttleFirst(1, TimeUnit.SECONDS).subscribe(o -> {
            gameId = Constants.GAME_AI_VOICE_ASSISTANT;
            GameContext.getInstance().setUserName(binding.etNickname.getText().toString());
            updateView();
        });
        compositeDisposable.add(disposable);

        disposable = RxView.clicks(binding.btnUnderCover).throttleFirst(1, TimeUnit.SECONDS).subscribe(o -> {
            gameId = Constants.GAME_WHO_IS_UNDERCOVER;
            GameContext.getInstance().setUserName(binding.etNickname.getText().toString());
            updateView();
        });
        compositeDisposable.add(disposable);
    }

    private void initServices() {
        GptRetrofitManager.getInstance().init();
        MiniMaxRetrofitManager.getInstance().init();
        MsTtsRetrofitManager.getInstance().init();
        MinimaxTtsRetrofitManager.getInstance().init();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onStart() {
        super.onStart();
    }


    @Override
    public void onResume() {
        super.onResume();
        if (!GameContext.getInstance().isInitRes()) {
            GameContext.getInstance().initData(getActivity());
            binding.btnUnderCover.setText(GameContext.getInstance().findGameById(Constants.GAME_WHO_IS_UNDERCOVER).getGameName());
            binding.btnAiPartner.setText(GameContext.getInstance().findGameById(Constants.GAME_AI_VOICE_ASSISTANT).getGameName());
            updateView();
        }
    }

    private void updateView() {
        GameInfo gameInfo = GameContext.getInstance().findGameById(gameId);
        GameContext.getInstance().setCurrentGame(gameInfo);
        switch (gameId){
            case Constants.GAME_WHO_IS_UNDERCOVER:
                binding.btnUnderCover.setActivated(true);
                binding.btnAiPartner.setActivated(false);
                binding.groupRole.setVisibility(View.VISIBLE);
                if (role==Constants.GAME_ROLE_USER){
                    binding.btnModerator.setActivated(false);
                    binding.btnUser.setActivated(true);
                    binding.groupNickname.setVisibility(View.VISIBLE);
                }else if (role==Constants.GAME_ROLE_MODERATOR){
                    binding.btnModerator.setActivated(true);
                    binding.btnUser.setActivated(false);
                    binding.groupNickname.setVisibility(View.GONE);
                }
                break;
            case Constants.GAME_AI_VOICE_ASSISTANT:
                binding.btnUnderCover.setActivated(false);
                binding.btnAiPartner.setActivated(true);
                binding.groupRole.setVisibility(View.GONE);
                binding.groupNickname.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}