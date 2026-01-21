package com.justnothing.testmodule.utils.tips;

import com.justnothing.testmodule.utils.tips.lang.ChineseTips;
import com.justnothing.testmodule.utils.tips.lang.EnglishTips;
import com.justnothing.testmodule.utils.functions.Logger;

import java.util.*;

public class TipSystem {
    private static final String TAG = "TipSystem";
    private final Map<TipType, List<TipCallback>> tipMap;
    private final Map<Integer, SimpleTipCallback> didYouKnowTipsMap;
    private final Random random;
    private final String language;
    
    private final Logger logger = new Logger() {
        @Override
        public String getTag() {
            return TAG;
        }
    };
    
    public TipSystem() {
        this.tipMap = new HashMap<>();
        this.didYouKnowTipsMap = new LinkedHashMap<>();
        this.random = new Random();
        this.language = Locale.getDefault().getLanguage();
        initializeTips();
    }
    
    public TipSystem(String language) {
        this.tipMap = new HashMap<>();
        this.didYouKnowTipsMap = new LinkedHashMap<>();
        this.random = new Random();
        this.language = language;
        initializeTips();
    }
    

    private void initializeTips() {
        if ("zh".equals(language) || "zh-CN".equals(language) || "zh-TW".equals(language)) {
            initializeChineseTips();
        } else {
            initializeEnglishTips();
        }
        
        logger.info("提示系统初始化完成，特殊提示:" +
                Objects.requireNonNull(tipMap.get(TipType.SPECIAL_TIP)).size()
                + "条，你知道吗提示:" +
                Objects.requireNonNull(tipMap.get(TipType.DID_YOU_KNOW)).size()
                + "条");
    }

    private void initializeChineseTips() {
        initializeChineseSpecialTips();
        initializeChineseDidYouKnowTips();
    }

    private void initializeEnglishTips() {
        initializeEnglishSpecialTips();
        initializeEnglishDidYouKnowTips();
    }

    private void initializeChineseSpecialTips() {
        List<TipCallback> list = new ArrayList<>(ChineseTips.SpecialTips.getSpecialTips());
        tipMap.put(TipType.SPECIAL_TIP, list);
    }
    

    private void initializeEnglishSpecialTips() {
        List<TipCallback> list = new ArrayList<>(EnglishTips.SpecialTips.getSpecialTips());
        tipMap.put(TipType.SPECIAL_TIP, list);
    }

    private void initializeChineseDidYouKnowTips() {
        didYouKnowTipsMap.clear();
        Map<Integer, SimpleTipCallback> map = ChineseTips.DidYouKnowTips.getDidYouKnowTips();
        didYouKnowTipsMap.putAll(map);
        tipMap.put(TipType.DID_YOU_KNOW, new ArrayList<>(map.values()));
    }

    private void initializeEnglishDidYouKnowTips() {
        didYouKnowTipsMap.clear();
        Map<Integer, SimpleTipCallback> map = EnglishTips.DidYouKnowTips.getDidYouKnowTips();
        didYouKnowTipsMap.putAll(map);
        tipMap.put(TipType.DID_YOU_KNOW, new ArrayList<>(map.values()));
    }


    public void addDidYouKnowTip(SimpleTipCallback tip) {
        int nextIndex = didYouKnowTipsMap.size() + 1;
        didYouKnowTipsMap.put(nextIndex, tip);
        logger.info("添加\"你知道吗\"提示, index = " + nextIndex +
                ", 内容如下： " + tip.getContent());
    }


    public TipCallback getDisplayTipForWelcome() {
        logger.info("内存中没有特殊提示");
        List<TipCallback> specialTips = tipMap.get(TipType.SPECIAL_TIP);
        if (specialTips == null) return null;
        logger.info("尝试从" + specialTips.size() + "个特殊提示中获取用于显示的内容");
        if (specialTips.isEmpty()) return null;
        List<TipCallback> displayTips = new ArrayList<>();
        for (TipCallback tip : specialTips) {
            if (tip.shouldShow()) {
                displayTips.add(tip);
            }
        }
        if (displayTips.isEmpty()) return null;
        displayTips.sort((t1, t2) ->
                Integer.compare(t2.getPriority(), t1.getPriority()));
        TipCallback result = displayTips.get(0);
        logger.info("显示提示: " + result.getContent() + ", priority = " + result.getPriority());
        return result;
    }
    

    public TipCallback getRandomDidYouKnowTip() {
        List<TipCallback> tips = tipMap.get(TipType.DID_YOU_KNOW);
        if (tips == null || tips.isEmpty()) {
            return null;
        }
        
        List<TipCallback> availableTips = new ArrayList<>();
        for (TipCallback tip : tips) {
            if (tip.shouldShow()) {
                availableTips.add(tip);
            }
        }
        
        if (availableTips.isEmpty()) {
            return null;
        }
        
        int index = random.nextInt(availableTips.size());
        TipCallback selectedTip = availableTips.get(index);
        
        if (selectedTip.hasSpecialLogic()) {
            selectedTip.executeSpecialLogic();
        }
        
        logger.info("随机选择\"你知道吗\"的提示: " + selectedTip.getContent());
        
        return selectedTip;
    }


    public int getTipCount(TipType type) {
        List<TipCallback> tips = tipMap.get(type);
        return tips != null ? tips.size() : 0;
    }
    

    public int getDidYouKnowTipIndex(TipCallback tip) {
        for (Map.Entry<Integer, SimpleTipCallback> entry : didYouKnowTipsMap.entrySet()) {
            if (entry.getValue().equals(tip)) {
                return entry.getKey();
            }
        }
        return -1;
    }

    public void clearDidYouKnowTip() {
        didYouKnowTipsMap.clear();
    }
}