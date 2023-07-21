package io.agora.metagpt.ui.game;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import io.agora.metagpt.databinding.GameAiPartnerBinding;
import io.agora.metagpt.ui.base.BaseActivity;
import io.agora.metagpt.utils.Constants;

public class GameAIPartnerActivity extends BaseActivity {
    private GameAiPartnerBinding binding;

    private final static String TAG = Constants.TAG + "-" + GameAIPartnerActivity.class.getSimpleName();

    public static void startActivity(Context activity) {
        Intent intent = new Intent(activity, GameAIPartnerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initContentView() {
        super.initContentView();
        binding = GameAiPartnerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.getRoot().setPaddingRelative(inset.left, 0, inset.right, 0);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    @Override
    protected void initData() {
        super.initData();

    }

}
