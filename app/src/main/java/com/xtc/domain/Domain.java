package com.xtc.domain;

/* loaded from: classes2.dex */
public enum Domain {
    WATCH("watchDomain", "watch.okii.com", "watch-sa.imoo.com", "watch-us.imoo.com", "watch-oz.okii.com", "watch-vn.imoo.com"),
    CHAT("chatDomain", "watch.okii.com", "watch-sa.imoo.com", "watch-us.imoo.com", "watch-oz.okii.com", "watch-vn.imoo.com"),
    LOCATION("locationDomain", "location.watch.okii.com", "location-sa.imoo.com", "location-us.imoo.com", "location-oz.okii.com", "location-vn.imoo.com"),
    SENSOR_SERVICE("sensorServiceDomain", "report-data-watch.okii.com", "api-sa.imoo.com", "api-us.imoo.com", "api-oz.okii.com", "api-vn.imoo.com"),
    POINTS("pointsDomain", "points.okii.com", "points-sa.imoo.com", "points-us.imoo.com", "points-oz.okii.com", "points-vn.imoo.com"),
    TCX_POINTS("pointsDomainTcx", "points.tiancaixing.com", "points-sa.imoo.com", "points-us.imoo.com", "points-oz.okii.com", "points-vn.imoo.com"),
    ACTIVITY("activityDomain", "act.okii.com", "act.okii.com", "act.okii.com", "act.okii.com", "act.okii.com"),
    TCX_ACTIVITY("activityDomainTcx", "act.tiancaixing.com", "acttest.okii.com", "acttest.okii.com", "acttest.okii.com", "acttest.okii.com"),
    SSO("ssoDomain", "account.okii.com", "account-sa.imoo.com", "account-us.imoo.com", "account-oz.okii.com", "account-vn.imoo.com"),
    IM_JOIN("imJoinDomain", "gw.im.okii.com:8000", "gw-sa.im.imoo.com:8000", "gw-us.im.imoo.com:8000", "gw-oz.im.okii.com:8000", "gw-vn.im.imoo.com:8000"),
    SPORT("sportDomain", "sport.watch.okii.com", "sport-sa.imoo.com", "sport-us.imoo.com", "sport-oz.okii.com", "sport-vn.imoo.com"),
    WEATHER("weatherDomain", "sport.watch.okii.com", "sport-sa.imoo.com", "sport-us.imoo.com", "sport-oz.okii.com", "sport-vn.imoo.com"),
    H5("h5Domain", "static.watch.okii.com", "static-watch.imoo.com", "static-watch.imoo.com", "static-watch.imoo.com", "static-watch.imoo.com"),
    THIRD("thirdDomain", "thirdparty.watch.okii.com", "thirdparty.watch.okii.com", "thirdparty.watch.okii.com", "thirdparty.watch.okii.com", "thirdparty.watch.okii.com"),
    STORE("storeDomain", "store.watch.okii.com", "store-sa.imoo.com", "store-us.imoo.com", "store-oz.okii.com", "store-vn.imoo.com"),
    TCX_STORE("storeDomainTcx", "store-watch.tiancaixing.com", "store-sa.imoo.com", "store-us.imoo.com", "store-oz.okii.com", "store-vn.imoo.com"),
    IM_BACKUP("imBackupDomainList", "106.75.31.151:1883,106.75.31.151:8000", "", "", "106.75.86.52:1883,106.75.86.52:8000", ""),
    IM_JOIN_GREY("imJoinGreyDomain", "gw.beta.im.okii.com:8000", "gw-sa-beta.im.imoo.com:8000", "gw-us-beta.im.imoo.com:8000", "gw-oz.beta.im.okii.com:8000", "gw-vn.im.imoo.com:8000"),
    GAME("gameDomain", "game.watch.okii.com", "game-sa.imoo.com", "game-us.imoo.com", "game-oz.okii.com", "game-vn.imoo.com"),
    TCX_GAME("gameDomainTcx", "game-watch.tiancaixing.com", "game-sa.imoo.com", "game-us.imoo.com", "game-oz.okii.com", "game-vn.imoo.com"),
    THEME("themeDomain", "theme.watch.okii.com", "theme-sa.imoo.com", "theme-us.imoo.com", "theme-oz.okii.com", "theme-vn.imoo.com"),
    EMOJI("emojiDomain", "smartwatch.qiniucdn.com", "smartwatch.qiniucdn.com", "smartwatch.qiniucdn.com", "smartwatch.qiniucdn.com", "smartwatch.qiniucdn.com"),
    MOMENT("momentDomain", "moment.watch.okii.com", "moment-sa.imoo.com", "moment-us.imoo.com", "moment.watch.okii.com", "moment-vn.imoo.com"),
    GATEWAY("gatewayDomain", "api.watch.okii.com", "api-sa.imoo.com", "api-us.imoo.com", "api-oz.okii.com", "api-vn.imoo.com"),
    TCX_GATEWAY("gatewayDomainTcx", "api-watch.tiancaixing.com", "api-sa.imoo.com", "api-us.imoo.com", "api-oz.okii.com", "api-vn.imoo.com"),
    SHOP("shopDomain", "oper.tiancaixing.com", "oper.tiancaixing.com", "oper.tiancaixing.com", "oper.tiancaixing.com", "oper.tiancaixing.com"),
    ROUTE("routeDomain", "route.okii.com", "route-sa.imoo.com", "route-us.imoo.com", "route-oz.okii.com", "route-vn.imoo.com"),
    BIGDATA("bigdataDomain", "watchda.eebbk.net", "watchda.eebbk.net", "watchda.eebbk.net", "watchda-eu.imoo.com", "watchda.eebbk.net"),
    QINIU_PRE_QUERY("qiniuPreQueryDomain", "uc.qbox.me", "kodo-config.qiniuapi.com", "kodo-config.qiniuapi.com", "", "kodo-config.qiniuapi.com"),
    WEB_SOCKET("webSocketDomain", "ws-gw.okii.com", "", "", "", ""),
    IME_WS("imeWSDomain", "ime-ws-gw.okii.com", "", "", "", ""),
    AIAST_WS("aiAstWSDomain", "ai-ast-ws-gw.okii.com", "", "", "", ""),
    QIUNIU_UPLOAD("qiniuUploadDomain", "", "", "upload-as0.qiniup.com", "", "upload-as0.qiniup.com");

    private final String cnDomain;
    private final String deDomain;
    private final String name;
    private final String saDomain;
    private final String usDomain;
    private final String vnDomain;

    Domain(String str, String str2, String str3, String str4, String str5, String str6) {
        this.name = str;
        this.cnDomain = str2;
        this.saDomain = str3;
        this.usDomain = str4;
        this.deDomain = str5;
        this.vnDomain = str6;
    }

    public final String getName() {
        return this.name;
    }

    public final String getCnDomain() {
        return this.cnDomain;
    }

    public final String getSaDomain() {
        return this.saDomain;
    }

    public final String getUsDomain() {
        return this.usDomain;
    }

    public String getDeDomain() {
        return this.deDomain;
    }

    public String getVnDomain() {
        return this.vnDomain;
    }
}