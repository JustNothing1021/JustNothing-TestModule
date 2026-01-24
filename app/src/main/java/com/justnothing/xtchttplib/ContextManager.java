package com.justnothing.xtchttplib;

import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.Paths;


public class ContextManager {
    private String grey; 
    private Integer ts;
    private String ae;
    private String rsaPublicKey;
    private String selfRsaPublicKey;
    private String httpHeadParam;
    private String encSwitch;
    private String watchId; // return context.getContentResolver().getType(Uri.parse("content://com.xtc.provider/BaseDataProvider/watchId/1"));
    private String macAddr; // 形如 02:00:00:00:00:00
    private String bindNumber; // ro.boot.bindnumber
    private Integer packageVersionCode;
    private String packageVersionName;
    private String packageName;
    private Integer androidSdk;
    private String innerModel = "IB"; // ro.product.innermodel
    private String serverInner; // persist.sys.serverinner
    private String locale; // ro.product.locale
    private String region; // ro.product.locale.region
    private String language; // Locale.getDefault().getLanguage()
    private String innerModelEx; // ro.product.innermodel.ex
    private String watchModel = "Z3"; // ro.product.model
    private String watchPriModel; // ro.product.pri.model
    private String buildType = "user"; // ro.build.type
    private String hardware = "qcom"; // ro.hardware
    private String caremeOsVersion; // ro.product.careme.version
    private String showModel; // ro.product.showmodel
    private String timeZone = "GMT+08:00"; // TODO: 原逻辑为elt.a();
    private String dataCenterCode = "CN-BJ"; // TODO: 原逻辑为byk.a(this.f5323a).m2760a();
    private String chipId; // ro.boot.xtc.chipid
    private String buildRelease; // ro.build.version.release
    private String softVersion; // ro.product.current.softversion

    private static ContextManager instance;

    public static ContextManager getInstance() {
        return instance;
    }

    public static void setInstance(ContextManager instance) {
        ContextManager.instance = instance;
    }

    public String toJson() throws Exception {
        JSONObject json = new JSONObject();
        json.put("grey", grey);
        json.put("ts", ts);
        json.put("ae", ae);
        json.put("rsaPublicKey", rsaPublicKey);
        json.put("selfRsaPublicKey", selfRsaPublicKey);
        json.put("httpHeadParam", httpHeadParam);
        json.put("encSwitch", encSwitch);
        json.put("watchId", watchId);
        json.put("macAddr", macAddr);
        json.put("bindNumber", bindNumber);
        json.put("packageVersionCode", packageVersionCode);
        json.put("packageVersionName", packageVersionName);
        json.put("packageName", packageName);
        json.put("androidSdk", androidSdk);
        json.put("innerModel", innerModel);
        json.put("serverInner", serverInner);
        json.put("locale", locale);
        json.put("region", region);
        json.put("language", language);
        json.put("innerModelEx", innerModelEx);
        json.put("watchModel", watchModel);
        json.put("watchPriModel", watchPriModel);
        json.put("buildType", buildType);
        json.put("hardware", hardware);
        json.put("caremeOsVersion", caremeOsVersion);
        json.put("showModel", showModel);
        json.put("timeZone", timeZone);
        json.put("dataCenterCode", dataCenterCode);
        json.put("chipId", chipId);
        json.put("buildRelease", buildRelease);
        json.put("softVersion", softVersion);
        return json.toString();
    }

    public static ContextManager fromJson(String jsonStr) throws Exception {
        JSONObject json = new JSONObject(jsonStr);
        ContextManager ctx = new ContextManager();
        
        if (json.has("grey")) ctx.setGrey(json.getString("grey"));
        if (json.has("ts")) ctx.setTs(json.getInt("ts"));
        if (json.has("ae")) ctx.setAe(json.getString("ae"));
        if (json.has("rsaPublicKey")) ctx.setRsaPublicKey(json.getString("rsaPublicKey"));
        if (json.has("selfRsaPublicKey")) ctx.setSelfRsaPublicKey(json.optString("selfRsaPublicKey", null));
        if (json.has("httpHeadParam")) ctx.setHttpHeadParam(json.optString("httpHeadParam", null));
        if (json.has("encSwitch")) ctx.setEncSwitch(json.getString("encSwitch"));
        if (json.has("watchId")) ctx.setWatchId(json.getString("watchId"));
        if (json.has("macAddr")) ctx.setMacAddr(json.getString("macAddr"));
        if (json.has("bindNumber")) ctx.setBindNumber(json.getString("bindNumber"));
        if (json.has("packageVersionCode")) ctx.setPackageVersionCode(json.getInt("packageVersionCode"));
        if (json.has("packageVersionName")) ctx.setPackageVersionName(json.getString("packageVersionName"));
        if (json.has("packageName")) ctx.setPackageName(json.getString("packageName"));
        if (json.has("androidSdk")) ctx.setAndroidSdk(json.getInt("androidSdk"));
        if (json.has("innerModel")) ctx.setInnerModel(json.getString("innerModel"));
        if (json.has("serverInner")) ctx.setServerInner(json.optString("serverInner", ""));
        if (json.has("locale")) ctx.setLocale(json.getString("locale"));
        if (json.has("region")) ctx.setRegion(json.optString("region", ""));
        if (json.has("language")) ctx.setLanguage(json.getString("language"));
        if (json.has("innerModelEx")) ctx.setInnerModelEx(json.optString("innerModelEx", ""));
        if (json.has("watchModel")) ctx.setWatchModel(json.getString("watchModel"));
        if (json.has("watchPriModel")) ctx.setWatchPriModel(json.optString("watchPriModel", ""));
        if (json.has("buildType")) ctx.setBuildType(json.getString("buildType"));
        if (json.has("hardware")) ctx.setHardware(json.getString("hardware"));
        if (json.has("caremeOsVersion")) ctx.setCaremeOsVersion(json.optString("caremeOsVersion", ""));
        if (json.has("showModel")) ctx.setShowModel(json.optString("showModel", ""));
        if (json.has("timeZone")) ctx.setTimeZone(json.getString("timeZone"));
        if (json.has("dataCenterCode")) ctx.setDataCenterCode(json.getString("dataCenterCode"));
        if (json.has("chipId")) ctx.setChipId(json.getString("chipId"));
        if (json.has("buildRelease")) ctx.setBuildRelease(json.getString("buildRelease"));
        if (json.has("softVersion")) ctx.setSoftVersion(json.getString("softVersion"));
        
        return ctx;
    }

    

    public static ContextManager fromJsonFile(String filePath) throws Exception {
        String jsonStr = new String(Files.readAllBytes(Paths.get(filePath)));
        return fromJson(jsonStr);
    }

    public String getGrey() {
        return grey;
    }

    public Integer getTs() {
        return ts;
    }

    public String getAe() {
        return ae;
    }

    public String getRsaPublicKey() {
        return rsaPublicKey;
    }

    public String getSelfRsaPublicKey() {
        return selfRsaPublicKey;
    }

    public String getHttpHeadParam() {
        return httpHeadParam;
    }

    public String getEncSwitch() {
        return encSwitch;
    }

    public String getWatchId() {
        return watchId;
    }

    public String getMacAddr() {
        return macAddr;
    }

    public String getBindNumber() {
        return bindNumber;
    }

    public Integer getPackageVersionCode() {
        return packageVersionCode;
    }

    public String getPackageVersionName() {
        return packageVersionName;
    }

    public String getPackageName() {
        return packageName;
    }

    public Integer getAndroidSdk() {
        return androidSdk;
    }

    public String getInnerModel() {
        return innerModel;
    }

    public String getServerInner() {
        return serverInner;
    }

    public String getLocale() {
        return locale;
    }

    public String getRegion() {
        return region;
    }

    public String getLanguage() {
        return language;
    }

    public String getInnerModelEx() {
        return innerModelEx;
    }

    public String getWatchModel() {
        return watchModel;
    }

    public String getWatchPriModel() {
        return watchPriModel;
    }

    public String getBuildType() {
        return buildType;
    }

    public String getHardware() {
        return hardware;
    }

    public String getCaremeOsVersion() {
        return caremeOsVersion;
    }

    public String getShowModel() {
        return showModel;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public String getDataCenterCode() {
        return dataCenterCode;
    }

    public String getChipId() {
        return chipId;
    }

    public String getBuildRelease() {
        return buildRelease;
    }

    public String getSoftVersion() {
        return softVersion;
    }


    public void setGrey(String grey) {
        this.grey = grey;
    }

    public void setTs(Integer ts) {
        this.ts = ts;
    }

    public void setAe(String ae) {
        this.ae = ae;
    }

    public void setRsaPublicKey(String rsaPublicKey) {
        this.rsaPublicKey = rsaPublicKey;
    }

    public void setSelfRsaPublicKey(String selfRsaPublicKey) {
        this.selfRsaPublicKey = selfRsaPublicKey;
    }

    public void setHttpHeadParam(String httpHeadParam) {
        this.httpHeadParam = httpHeadParam;
    }

    public void setEncSwitch(String encSwitch) {
        this.encSwitch = encSwitch;
    }

    public void setWatchId(String watchId) {
        this.watchId = watchId;
    }

    public void setMacAddr(String macAddr) {
        this.macAddr = macAddr;
    }

    public void setBindNumber(String bindNumber) {
        this.bindNumber = bindNumber;
    }

    public void setPackageVersionCode(Integer packageVersionCode) {
        this.packageVersionCode = packageVersionCode;
    }

    public void setPackageVersionName(String packageVersionName) {
        this.packageVersionName = packageVersionName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setAndroidSdk(Integer androidSdk) {
        this.androidSdk = androidSdk;
    }

    public void setInnerModel(String innerModel) {
        this.innerModel = innerModel;
    }

    public void setServerInner(String serverInner) {
        this.serverInner = serverInner;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setInnerModelEx(String innerModelEx) {
        this.innerModelEx = innerModelEx;
    }

    public void setWatchModel(String watchModel) {
        this.watchModel = watchModel;
    }

    public void setWatchPriModel(String watchPriModel) {
        this.watchPriModel = watchPriModel;
    }

    public void setBuildType(String buildType) {
        this.buildType = buildType;
    }

    public void setHardware(String hardware) {
        this.hardware = hardware;
    }

    public void setCaremeOsVersion(String caremeOsVersion) {
        this.caremeOsVersion = caremeOsVersion;
    }

    public void setShowModel(String showModel) {
        this.showModel = showModel;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public void setDataCenterCode(String dataCenterCode) {
        this.dataCenterCode = dataCenterCode;
    }

    public void setChipId(String chipId) {
        this.chipId = chipId;
    }

    public void setBuildRelease(String buildRelease) {
        this.buildRelease = buildRelease;
    }

    public void setSoftVersion(String softVersion) {
        this.softVersion = softVersion;
    }
}
