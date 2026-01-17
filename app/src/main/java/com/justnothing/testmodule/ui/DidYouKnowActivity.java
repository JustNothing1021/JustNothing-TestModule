package com.justnothing.testmodule.ui;

import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.utils.functions.Logger;
import com.justnothing.testmodule.utils.tips.TipCallback;
import com.justnothing.testmodule.utils.tips.TipSystem;
import com.justnothing.testmodule.utils.tips.TipType;


public class DidYouKnowActivity extends AppCompatActivity {
    private static final String TAG = "DidYouKnowActivity";
    
    // Logger实例
    private final Logger logger = new Logger() {
        @Override
        public String getTag() {
            return TAG;
        }
    };
    
    private TipSystem tipSystem;
    private TextView tipContent;
    private TextView tipAuthor;
    private TextView tipCounter;
    
    private int totalTips = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_did_you_know);
        tipSystem = new TipSystem();
        initViews();
        logger.info("你知道吗界面初始化完成");
    }
    
    private void initViews() {
        tipContent = findViewById(R.id.tip_content);
        tipAuthor = findViewById(R.id.tip_author);
        tipCounter = findViewById(R.id.tip_counter);
        View tipContainer = findViewById(R.id.tip_container);
        
        tipContainer.setOnClickListener(v -> showRandomTip());
        
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
            tipContent.setText(getString(R.string.did_you_know_no_tip_available));
            tipAuthor.setVisibility(View.GONE);
            logger.warn("没有找到可用的你知道吗提示");
        }
    }
    
    private void updateCounter(int index) {
        tipCounter.setText(getString(R.string.did_you_known_index_format, index, totalTips));
    }
    
    private void animateTipChange(final TipCallback tip) {
        AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(150);
        fadeOut.setFillAfter(true);
        
        tipContent.startAnimation(fadeOut);
        if (tipAuthor.getVisibility() == View.VISIBLE) {
            tipAuthor.startAnimation(fadeOut);
        }
        
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}
            
            @Override
            public void onAnimationEnd(Animation animation) {
                tipContent.setText(tip.getContent());
                
                String author = tip.getAuthor();
                if (author != null && !author.trim().isEmpty()) {
                    tipAuthor.setText(getString(R.string.did_you_know_author_format, author));
                    tipAuthor.setVisibility(View.VISIBLE);
                } else {
                    tipAuthor.setVisibility(View.GONE);
                }
                
                AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
                fadeIn.setDuration(150);
                fadeIn.setFillAfter(true);
                
                tipContent.startAnimation(fadeIn);
                if (tipAuthor.getVisibility() == View.VISIBLE) {
                    tipAuthor.startAnimation(fadeIn);
                }
            }
            
            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
    }
}