package com.xtc.sync;

import java.util.ArrayList;
import java.util.List;

/* compiled from: SystemProperty.java */
/* loaded from: classes2.dex */
public class elr {
    public static final String A = "persist.sys.officialstat";
    public static final String B = "persist.sys.powersave.switch";
    public static final String C = "persist.sys.quietnessmode";
    public static final String D = "persist.sys.close_ble";
    public static final String E = "persist.sys.bt.shutdown";
    public static final String F = "persist.sys.bt.warn";
    public static final String G = "persist.sys.wifi_visible_status";
    public static final String H = "persist.sys.wifi_user_operate";
    public static final String I = "persist.sys.power_on_off";
    public static final String J = "persist.sys.setting_location";
    public static final String K = "persist.sys.setting_savepower";
    public static final String L = "persist.sys.mobliedata.control";
    public static final String M = "persist.sys.mobliedata.time";
    public static final String N = "persist.sys.unknownsources";
    public static final String O = "persist.sys.wificonnect.enable";
    public static final String P = "persist.sys.cellular.netease";
    public static final String Q = "persist.sys.shakemusic";
    public static final String R = "persist.sys.im.status";
    public static final String S = "persist.sys.cellular.threshold";
    public static final String T = "persist.sys.weichat.more.emoji";
    public static final String U = "persist.sys.sport.pk.tct";
    public static final String V = "persist.sys.serverinner";
    public static final String W = "persist.sys.setting.keyid";
    public static final String X = "persist.sys.forbid.rank";
    public static final String Y = "persist.sys.launcher.theme";
    public static final String Z = "persist.sys.modestatus";

    /* renamed from: a, reason: collision with root package name */
    public static final String f26075a = "persist.sys.watchlossstatus";
    public static final String aA = "log.tag.factoryreset";
    public static final String aB = "log.tag.appdata";
    public static final String aC = "log.tag.btwifi";
    public static final String aD = "log.tag.xtc_bugreport";
    public static final String aE = "ro.product.current.softversion";
    public static final String aF = "ro.product.model";
    public static final String aG = "ro.product.pri.model";
    public static final String aH = "ro.product.innermodel";
    public static final String aI = "ro.product.careme.version";
    public static final String aJ = "ro.product.innermodel.ex";
    public static final String aK = "ro.product.locale.region";
    public static final String aL = "ro.product.locale";
    public static final String aM = "ro.build.date.utc";
    public static final String aN = "ro.build.type";
    public static final String aO = "ro.build.version.auth";
    public static final String aP = "ro.boot.bindnumber";
    public static final String aQ = "ro.telephony.default_network";
    public static final String aR = "ro.hardware";
    public static final String aS = "ro.cta_enabled";
    public static final String aT = "ro.com.android.mobiledata";
    public static final String aU = "ro.xtcwatch.color";
    public static final String aV = "wifi.autoconnect.disable";
    public static final String aW = "wifi.is.sleep";
    public static final String aX = "sys.im.init.enable";
    public static final String aY = "persist.service.xtc_hacker";
    public static final String aZ = "gsm.signal.strength";
    public static final String aa = "persist.sys.nwmode";
    public static final String ab = "persist.sys.hall.opened";
    public static final String ac = "persist.sys.appconfig";
    public static final String ad = "persist.sys.cta.permission";
    public static final String ae = "ro.xtc.ctaversion";
    public static final String af = "persist.sys.xtc.adb_port";
    public static final String ag = "persist.sys.cpu.monitor.status";
    public static final String ah = "persist.sys.path.lcation.status";
    public static final String ai = "persist.sys.power.func.type";
    public static final String aj = "persist.sys.power.click.type";
    public static final String ak = "persist.sys.quickdialstatus";
    public static final String al = "persist.sys.setting.ring";
    public static final String am = "persist.sys.ring.switch";
    public static final String an = "persist.sys.location.high.ac";
    public static final String ao = "persist.sys.allowpoweroff";
    public static final String ap = "persist.sys.allowrepair";
    public static final String aq = "persist.sys.b";
    public static final String ar = "persist.sys.headset";
    public static final String as = "persist.sys.last.device";
    public static final String at = "persist.sys.bt.callonce";
    public static final String au = "persist.sys.vc.calling";
    public static final String av = "persist.sys.takephoto.mode";
    public static final String aw = "persist.sys.dial120status";
    public static final String ax = "persist.sys.dial110status";
    public static final String ay = "persist.sys.watch.id";
    public static final String az = "log.tag.modem";

    /* renamed from: b, reason: collision with root package name */
    public static final String f26076b = "persist.sys.currentclockfile";
    public static final String ba = "gsm.ct.volte";
    public static final String bb = "gsm.version.baseband";
    public static final String c = "persist.sys.charge.usable";
    public static final String d = "persist.sys.unlock.hidden";
    public static final String e = "persist.sys.bt.rejectcall";
    public static final String f = "persist.sys.motion.switch.state";
    public static final String g = "persist.sys.occurdump";
    public static final String h = "persist.sys.appcrash";
    public static final String i = "persist.sys.SwipeDismiss";
    public static final String j = "persist.sys.call.end";
    public static final String k = "persist.sys.dure.limits";
    public static final String l = "persist.sys.standby.display";
    public static final String m = "persist.sys.status_bar";
    public static final String n = "persist.sys.powersave.enable";
    public static final String o = "persist.sys.close_wifi";
    public static final String p = "persist.sys.close_gps";
    public static final String q = "persist.sys.gps_collect_wifi";
    public static final String r = "persist.sys.autoanswer";
    public static final String s = "persist.sys.refusestrangercall";
    public static final String t = "persist.sys.reportcallposition";
    public static final String u = "persist.sys.bindstatus";

    @Deprecated
    public static final String v = "persist.sys.preschoolstatus";
    public static final String w = "persist.sys.ostype";
    public static final String x = "persist.sys.longbatterylife";
    public static final String y = "persist.sys.classmodestatus";
    public static final String z = "persist.sys.sleepmodestatus";

    /* compiled from: SystemProperty.java */
    /* loaded from: classes2.dex */
    public interface a {

        /* renamed from: a, reason: collision with root package name */
        public static final int f26077a = 1;

        /* renamed from: b, reason: collision with root package name */
        public static final int f26078b = 0;
    }

    /* compiled from: SystemProperty.java */
    /* loaded from: classes2.dex */
    public interface b {

        /* compiled from: SystemProperty.java */
        /* loaded from: classes2.dex */
        public interface a {
            public static final String A = "IDI13";
            public static final String B = "IDI6C";
            public static final String C = "Y01";
            public static final String D = "Y02";
            public static final String E = "Y1A";
            public static final String F = "I25";
            public static final String G = "I26";
            public static final String H = "I26A";
            public static final String I = "I28";
            public static final String J = "I25C";
            public static final String K = "I32";
            public static final String L = "I25D";
            public static final String M = "ND01";
            public static final String N = "ND03";
            public static final String O = "ND07";
            public static final String P = "";

            /* renamed from: a, reason: collision with root package name */
            public static final String f26079a = "DI01";

            /* renamed from: b, reason: collision with root package name */
            public static final String f26080b = "DI02";
            public static final String c = "F1";
            public static final String d = "F2";
            public static final String e = "F3";
            public static final String f = "F4";
            public static final String g = "GLI17";
            public static final String h = "HKI17";
            public static final String i = "PHI17";
            public static final String j = "THI17";
            public static final String k = "IA";
            public static final String l = "IB";
            public static final String m = "I12";
            public static final String n = "I13";
            public static final String o = "I13C";
            public static final String p = "I16";
            public static final String q = "I17";
            public static final String r = "I17D";
            public static final String s = "I17E";
            public static final String t = "I18";
            public static final String u = "I19";
            public static final String v = "I20";
            public static final String w = "I2C";
            public static final String x = "I3";
            public static final String y = "I6";
            public static final String z = "I8";
        }

        /* compiled from: SystemProperty.java */
        /* renamed from: com.xtc.i3launcher.elr$b$b, reason: collision with other inner class name */
        /* loaded from: classes2.dex */
        public interface InterfaceC0091b {

            /* renamed from: a, reason: collision with root package name */
            public static final String f26081a = "QCH";

            /* renamed from: b, reason: collision with root package name */
            public static final String f26082b = "SN";
        }

        /* compiled from: SystemProperty.java */
        /* loaded from: classes2.dex */
        public interface c {

            /* renamed from: a, reason: collision with root package name */
            public static final String f26083a = "XTC_Z3";

            /* renamed from: b, reason: collision with root package name */
            public static final String f26084b = "XTC_Z2y";
            public static final String c = "XTC_Z5";
            public static final String d = "XTC V1";
        }
    }

    /* compiled from: SystemProperty.java */
    /* loaded from: classes2.dex */
    public interface c {

        /* renamed from: a, reason: collision with root package name */
        public static final String f26085a = "preschool";

        /* renamed from: b, reason: collision with root package name */
        public static final String f26086b = "primary";
        public static final String c = "junior";
        public static final String d = "senior";
        public static final String e = "primary";
    }

    /* compiled from: SystemProperty.java */
    /* loaded from: classes2.dex */
    public interface d {

        /* renamed from: a, reason: collision with root package name */
        public static final String f26087a = "normal";

        /* renamed from: b, reason: collision with root package name */
        public static final String f26088b = "official_disable";
        public static final String c = "sleep";
        public static final String d = "power_save";
        public static final String e = "long_battery";
        public static final String f = "class";
        public static final String g = "high_temp";
        public static final String h = "watch_loss";
        public static final String i = "watch_long_endurance";
    }

    public static List<String> a() {
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add(o);
        arrayList.add(D);
        arrayList.add(ak);
        arrayList.add(ah);
        arrayList.add(al);
        arrayList.add(an);
        arrayList.add(ar);
        arrayList.add(as);
        arrayList.add(at);
        arrayList.add(E);
        arrayList.add(F);
        arrayList.add(Z);
        arrayList.add(x);
        arrayList.add(y);
        arrayList.add("persist.sys.sleepmodestatus");
        arrayList.add(A);
        arrayList.add(K);
        arrayList.add(f26075a);
        arrayList.add(e);
        arrayList.add(d);
        arrayList.add(v);
        arrayList.add("persist.sys.vc.calling");
        arrayList.add(ak);
        arrayList.add(ax);
        arrayList.add(aw);
        arrayList.add(am);
        arrayList.add(al);
        arrayList.add(G);
        arrayList.add(H);
        arrayList.add(av);
        arrayList.add("persist.sys.cpu.monitor.status");
        return arrayList;
    }
}