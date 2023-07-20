package io.agora.metagpt.ui.main;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.afollestad.materialdialogs.MaterialDialog;
import com.jakewharton.rxbinding2.view.RxView;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.agora.metagpt.adapter.SelectAdapter;
import io.agora.metagpt.ai.gpt.GptRetrofitManager;
import io.agora.metagpt.ai.minimax.MiniMaxRetrofitManager;
import io.agora.metagpt.context.GameContext;
import io.agora.metagpt.tts.ms.MsTtsRetrofitManager;
import io.agora.metagpt.ui.activity.AIRobotActivity;
import io.agora.metagpt.ui.activity.ModeratorActivity;
import io.agora.metagpt.ui.base.BaseFragment;
import io.agora.metagpt.context.MetaContext;
import io.agora.metagpt.R;
import io.agora.metagpt.databinding.MainFragmentBinding;
import io.agora.metagpt.ui.activity.UserActivity;
import io.agora.metagpt.ui.view.CustomDialog;
import io.agora.metagpt.utils.KeyCenter;
import io.agora.metagpt.utils.Constants;
import io.reactivex.disposables.Disposable;

public class MainFragment extends BaseFragment {
    private static final String TAG = MainFragment.class.getSimpleName();

    private MainViewModel mViewModel;
    private MainFragmentBinding binding;
    private int downloadProgress;

    private MaterialDialog progressDialog;
    private MaterialDialog downloadingChooserDialog;

    public static MainFragment newInstance() {
        return new MainFragment();
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
        binding = MainFragmentBinding.inflate(inflater, container, attachToParent);
    }

    @Override
    protected void initView() {
        super.initView();


        binding.etUsername.addTextChangedListener(new TextWatcher() {
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


    }

    @Override
    protected void initClickEvent() {
        super.initClickEvent();
        //防止多次频繁点击异常处理
        Disposable disposable = RxView.clicks(binding.enter).throttleFirst(1, TimeUnit.SECONDS).subscribe(o -> {
            if (TextUtils.isEmpty(GameContext.getInstance().getRoomName())) {
                Toast.makeText(requireActivity(), "请输入房间名", Toast.LENGTH_LONG).show();
            } else {
                if (Constants.GAME_ROLE_USER == GameContext.getInstance().getGameRole()) {
                    if (TextUtils.isEmpty(GameContext.getInstance().getUserName())) {
                        Toast.makeText(requireActivity(), "请输入用户名", Toast.LENGTH_LONG).show();
                    } else {
                        MetaContext.getInstance().initRoomNameAndUid(GameContext.getInstance().getRoomName(), KeyCenter.getUserUid(), GameContext.getInstance().getUserName());
                        mViewModel.getScenes();
                    }
                } else {
                    Intent intent = null;
                    if (Constants.GAME_ROLE_AI == GameContext.getInstance().getGameRole()) {
                        intent = new Intent(getContext(), AIRobotActivity.class);
                        MetaContext.getInstance().initRoomNameAndUid(GameContext.getInstance().getRoomName(), KeyCenter.getAiUid(), requireContext().getResources().getString(R.string.ai));
                        initServices();
                    } else if (Constants.GAME_ROLE_MODERATOR == GameContext.getInstance().getGameRole()) {
                        intent = new Intent(getContext(), ModeratorActivity.class);
                        MetaContext.getInstance().initRoomNameAndUid(GameContext.getInstance().getRoomName(), KeyCenter.getModeratorUid(), requireContext().getResources().getString(R.string.moderator));
                    }
                    if (null != intent) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivity(intent);
                    }
                }
            }
        });
        compositeDisposable.add(disposable);

        disposable = RxView.clicks(binding.avatar).throttleFirst(1, TimeUnit.SECONDS).subscribe(o -> {

        });

        compositeDisposable.add(disposable);
    }

    private void initServices() {
        GptRetrofitManager.getInstance().init();
        MiniMaxRetrofitManager.getInstance().init();
        MsTtsRetrofitManager.getInstance().init();
    }

    @Override
    protected void initData() {
        super.initData();
        downloadProgress = -1;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViewModel();
    }


    private void initViewModel() {
        mViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        LifecycleOwner owner = getViewLifecycleOwner();
        Context context = requireContext();

        mViewModel.getSceneList().observe(owner, metachatSceneInfos -> {
            // TODO choose one
            if (metachatSceneInfos.size() > 0) {
                for (int a = 0; a < metachatSceneInfos.size(); a++) {
                    if (metachatSceneInfos.get(a).getSceneId() == MetaContext.getInstance().getSceneId()) {
                        mViewModel.prepareScene(metachatSceneInfos.get(a));
                        break;
                    }
                }
            }
        });
        mViewModel.getSelectScene().observe(owner, sceneInfo -> {
            if (!MetaContext.getInstance().isInitMetachat()) {
                return;
            }

            if (-1 != downloadProgress) {
                downloadProgress = -1;
            }
            if (progressDialog != null) {
                progressDialog.dismiss();
                progressDialog = null;
            }
            Intent intent = new Intent(context, UserActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });
        mViewModel.getRequestDownloading().observe(owner, aBoolean -> {
            if (!MetaContext.getInstance().isInitMetachat()) {
                return;
            }
            if (aBoolean) {
                downloadingChooserDialog = CustomDialog.showDownloadingChooser(context, materialDialog -> {
                    mViewModel.downloadScene(MetaContext.getInstance().getSceneInfo());

                    return null;
                }, null);

            }
        });
        mViewModel.getDownloadingProgress().observe(owner, integer -> {
            if (!MetaContext.getInstance().isInitMetachat()) {
                return;
            }
            if (integer >= 0) {
                downloadProgress = integer;
            }
            if (progressDialog == null) {
                progressDialog = CustomDialog.showDownloadingProgress(context, materialDialog -> {
                    downloadProgress = -1;
                    mViewModel.cancelDownloadScene(MetaContext.getInstance().getSceneInfo());
                    return null;
                });
            } else if (integer < 0) {
                if (mIsFront) {
                    progressDialog.dismiss();
                    progressDialog = null;
                }
                return;
            }

            if (!progressDialog.isShowing()) {
                progressDialog.show();
            }

            ConstraintLayout constraintLayout = CustomDialog.getCustomView(progressDialog);
            ProgressBar progressBar = constraintLayout.findViewById(R.id.progressBar);
            TextView textView = constraintLayout.findViewById(R.id.textView);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                progressBar.setProgress(integer, true);
            } else {
                progressBar.setProgress(integer);
            }
            textView.setText(String.format(Locale.getDefault(), "%d%%", integer));

        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!MetaContext.getInstance().isInitMetachat() && progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        if (!MetaContext.getInstance().isInitMetachat() && downloadingChooserDialog != null && downloadingChooserDialog.isShowing()) {
            downloadingChooserDialog.dismiss();
            downloadingChooserDialog = null;
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (downloadProgress >= 0) {
            if (!MetaContext.getInstance().downloadScene(MetaContext.getInstance().getSceneInfo())) {
                Log.e(TAG, "onResume continue download fail");
            }
        }

        if (!GameContext.getInstance().isInitRes()) {
            GameContext.getInstance().initData(getActivity());
            updateView();
        }
    }

    private void updateView() {
        SelectAdapter gameAdapter = new SelectAdapter(requireContext(), GameContext.getInstance().getGameNames());
        binding.gameSpinner.setAdapter(gameAdapter);
        binding.gameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                binding.gameEt.setText(GameContext.getInstance().getGameNames()[i]);
                gameAdapter.check(i);
                GameContext.getInstance().setCurrentGame(GameContext.getInstance().getGameInfoArray()[i]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        binding.gameSpinner.setSelection(0);

        SelectAdapter gameRoleAdapter = new SelectAdapter(requireContext(), requireContext().getResources().getStringArray(R.array.game_role));
        binding.gameRoleSpinner.setAdapter(gameRoleAdapter);
        binding.gameRoleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                binding.gameRoleEt.setText(requireContext().getResources().getStringArray(R.array.game_role)[i]);
                gameRoleAdapter.check(i);
                GameContext.getInstance().setGameRole(i);
                binding.groupNickname.setVisibility(Constants.GAME_ROLE_USER == i ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        binding.gameRoleSpinner.setSelection(0);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (downloadProgress >= 0) {
            if (!MetaContext.getInstance().cancelDownloadScene(MetaContext.getInstance().getSceneInfo())) {
                Log.e(TAG, "onPause cancel download fail");
            }
        }
    }
}