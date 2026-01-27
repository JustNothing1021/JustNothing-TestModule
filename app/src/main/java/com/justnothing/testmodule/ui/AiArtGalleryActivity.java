package com.justnothing.testmodule.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.utils.functions.Logger;
import com.justnothing.testmodule.utils.ui.UISettings;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class AiArtGalleryActivity extends AppCompatActivity {
    private static final String TAG = "AiArtGalleryActivity";

    private static final Logger logger = Logger.getLoggerForName(TAG);

    private static class ArtWork {
        String fileName;
        int titleResId;
        int descriptionResId;
        int backgroundColor;

        ArtWork(String fileName, int titleResId, int descriptionResId, int backgroundColor) {
            this.fileName = fileName;
            this.titleResId = titleResId;
            this.descriptionResId = descriptionResId;
            this.backgroundColor = backgroundColor;
        }
    }

    private List<ArtWork> artWorks;
    private int currentIndex = 0;

    private FrameLayout svgContainer;
    private TextView tvTitle;
    private TextView tvDescription;
    private WebView webView;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_art_gallery);

        UISettings uiSettings = UISettings.getInstance(this);
        uiSettings.applyUIScale(this);

        svgContainer = findViewById(R.id.svg_container);
        tvTitle = findViewById(R.id.tv_ai_art_title);
        tvDescription = findViewById(R.id.tv_ai_art_description);
        webView = findViewById(R.id.webview);

        initArtWorks();
        loadArtWork(currentIndex);

        webView.setOnTouchListener(new View.OnTouchListener() {
            private float startX;
            private float startY;


            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        startY = event.getY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        float endX = event.getX();
                        float endY = event.getY();
                        float deltaX = Math.abs(endX - startX);
                        float deltaY = Math.abs(endY - startY);
                        if (deltaX < 50 && deltaY < 50) {
                            switchToNextArtWork();
                        }
                        return true;
                }
                return false;
            }
        });


    }

    private void initArtWorks() {
        artWorks = new ArrayList<>();
        artWorks.add(new ArtWork("landscape.svg", R.string.ai_art_work_title, R.string.ai_art_work_description, 0xFF87CEEB));
        artWorks.add(new ArtWork("night_sky.svg", R.string.ai_art_work_night_title, R.string.ai_art_work_night_description, 0xFF0a0a2e));
        artWorks.add(new ArtWork("sunrise.svg", R.string.ai_art_work_sunrise_title, R.string.ai_art_work_sunrise_description, 0xFF1B263B));
        artWorks.add(new ArtWork("sunset.svg", R.string.ai_art_work_sunset_title, R.string.ai_art_work_sunset_description, 0xFF2D1B4E));
    }

    private void loadArtWork(int index) {
        ArtWork artWork = artWorks.get(index);
        
        tvTitle.setText(artWork.titleResId);
        tvDescription.setText(artWork.descriptionResId);
        svgContainer.setBackgroundColor(artWork.backgroundColor);

        try {
            InputStream inputStream = getAssets().open(artWork.fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder svgContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                svgContent.append(line).append("\n");
            }
            reader.close();
            inputStream.close();

            String htmlContent = "<!DOCTYPE html><html><head><style>body,html{margin:0;padding:0;width:100%;height:100%;overflow:hidden;background-color:transparent;}svg{width:100%;height:100%;overflow:hidden;display:block;}</style></head><body>" + svgContent.toString() + "</body></html>";

            webView.setBackgroundColor(0x00000000);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setLoadWithOverviewMode(true);
            webView.getSettings().setUseWideViewPort(true);
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    webView.setAlpha(1.0f);
                }
            });

            webView.setAlpha(0.0f);
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
        } catch (Exception e) {
            logger.error("加载Webview时遇到错误", e);
        }
    }

    private void switchToNextArtWork() {
        AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(300);
        fadeOut.setFillAfter(true);

        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(300);
        fadeIn.setFillAfter(true);

        svgContainer.startAnimation(fadeOut);

        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                currentIndex = (currentIndex + 1) % artWorks.size();
                loadArtWork(currentIndex);
                svgContainer.startAnimation(fadeIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
    }
}
