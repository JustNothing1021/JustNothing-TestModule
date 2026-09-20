package com.justnothing.testmodule.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.databinding.ActivityDidYouKnowBinding;
import com.justnothing.testmodule.utils.tips.TipCallback;
import com.justnothing.testmodule.utils.tips.TipSystem;
import com.justnothing.testmodule.utils.tips.TipType;


public class DidYouKnowActivity extends BaseActivity {
    private ActivityDidYouKnowBinding binding;

    private TipSystem tipSystem;

    private int totalTips = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDidYouKnowBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        tipSystem = new TipSystem();
        initViews();
        logger.info("你知道吗界面初始化完成");
    }

    private void initViews() {
        binding.tipContainer.setOnClickListener(v -> showRandomTip());

        totalTips = tipSystem.getTipCount(TipType.DID_YOU_KNOW);
        updateCounter(0);
    }

    private void showRandomTip() {
        TipCallback tip = tipSystem.getRandomDidYouKnowTip();
        if (tip != null) {
            int index = tipSystem.getDidYouKnowTipIndex(tip);
            updateCounter(index);
            animateTipChange(tip);
            logger.info("显示你知道吗提示 " + index + "/" + totalTips + ": " + tip.getContent());
        } else {
            binding.tipContent.setText(getString(R.string.did_you_know_no_tip_available));
            binding.tipAuthor.setVisibility(View.GONE);
            logger.warn("没有找到可用的你知道吗提示");
        }
    }

    private void updateCounter(int index) {
        binding.tipCounter.setText(getString(R.string.did_you_known_index_format, index, totalTips));
    }

    private void animateTipChange(final TipCallback tip) {
        AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(150);
        fadeOut.setFillAfter(true);

        binding.tipContent.startAnimation(fadeOut);
        if (binding.tipAuthor.getVisibility() == View.VISIBLE) {
            binding.tipAuthor.startAnimation(fadeOut);
        }

        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                binding.tipContent.setText(tip.getContent());

                String author = tip.getAuthor();
                if (author != null && !author.trim().isEmpty()) {
                    binding.tipAuthor.setText(getString(R.string.did_you_know_author_format, author));
                    binding.tipAuthor.setVisibility(View.VISIBLE);
                } else {
                    binding.tipAuthor.setVisibility(View.GONE);
                }

                AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
                fadeIn.setDuration(150);
                fadeIn.setFillAfter(true);

                binding.tipContent.startAnimation(fadeIn);
                if (binding.tipAuthor.getVisibility() == View.VISIBLE) {
                    binding.tipAuthor.startAnimation(fadeIn);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
    }
}
