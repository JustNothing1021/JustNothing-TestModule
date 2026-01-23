package com.xtc.sync;

import android.text.TextUtils;

import com.justnothing.testmodule.utils.functions.Logger;

import java.util.Locale;

/* compiled from: WatchModelUtil.java */
/* loaded from: classes.dex */
public class elw {


    /* renamed from: a, reason: collision with other field name */
    private static final Logger logger = new Logger() {
        @Override
        public String getTag() {
            return "com.xtc.sync.elw";
        }
    };

    /* renamed from: b, reason: collision with root package name */
    private static String f26092b = "";
    private static String c = "";
    private static String f;

    public static void a(String str) {
        els.m4920a(elr.V, str);
    }

    public static String a() {
        String h;
        if (!TextUtils.isEmpty(f)) {
            return f;
        }
        String j = j();
        if (j.isEmpty() || m4937k()) {
            h = h();
            if (h.isEmpty()) {
                h = b() + k();
            }
        } else {
            if (b().equals("I13") && j.equals("ID")) {
                return "IDI13";
            }
            h = b() + "-" + j + k();
        }
        f = h;
        return f;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String b() {
        return els.a(elr.aH, "IB");
    }

    private static String h() {
        return els.a(elr.V, "");
    }

    private static String i() {
        return els.a(elr.aL, "");
    }

    private static String j() {
        return els.a(elr.aK, "");
    }

    public static String c() {
        return Locale.getDefault().getLanguage();
    }

    public static String d() {
        if (m4938l()) {
            return l();
        }
        return m();
    }

    private static String k() {
        String a2 = els.a(elr.aJ, "");
        if (TextUtils.isEmpty(a2)) {
            return "";
        }
        return "-" + a2;
    }

    private static String l() {
        String b2 = b();
        String i = i();
        if (e(b2)) {
            if (i.equals(ema.a("zh", "CN"))) {
                return "CN";
            }
            String j = j();
            if (!j.isEmpty()) {
                return j;
            }
            String h = h();
            if (h.isEmpty()) {
                return "";
            }
            int length = h.length();
            return h.substring(length - 2, length);
        }
        if (i.isEmpty()) {
            return "";
        }
        int length2 = i.length();
        return i.substring(length2 - 2, length2);
    }

    private static String m() {
        return j();
    }

    @Deprecated
    /* renamed from: a, reason: collision with other method in class */
    public static boolean m4926a() {
        return b("TH");
    }

    @Deprecated
    /* renamed from: b, reason: collision with other method in class */
    public static boolean m4928b() {
        return b("ID");
    }

    @Deprecated
    /* renamed from: c, reason: collision with other method in class */
    public static boolean m4929c() {
        return !b("CN") && !b("");
    }

    public static String e() {
        if (TextUtils.isEmpty(f26092b)) {
            f26092b = els.a(elr.aF, elr.b.c.f26083a);
        }
        return f26092b;
    }

    public static String f() {
        if (TextUtils.isEmpty(c)) {
            c = els.a(elr.aG, "");
        }
        if (TextUtils.isEmpty(c)) {
            if (m4927a("I12")) {
                c = n();
            } else if (m4927a("IB")) {
                c = o();
            } else {
                c = e();
            }
        }
        return c;
    }

    private static String n() {
        return els.a("ro.product.showmodel", cst.a.f23561a);
    }

    private static String o() {
        return els.a("ro.product.showmodel", cst.a.f23562b);
    }

    @Deprecated
    /* renamed from: d, reason: collision with other method in class */
    public static boolean m4930d() {
        return m4927a("IB");
    }

    @Deprecated
    /* renamed from: e, reason: collision with other method in class */
    public static boolean m4931e() {
        return m4927a("I12");
    }

    @Deprecated
    /* renamed from: f, reason: collision with other method in class */
    public static boolean m4932f() {
        return m4927a("I13");
    }

    @Deprecated
    /* renamed from: g, reason: collision with other method in class */
    public static boolean m4933g() {
        return m4927a(elr.b.a.o);
    }

    @Deprecated
    /* renamed from: h, reason: collision with other method in class */
    public static boolean m4934h() {
        return m4927a(elr.b.a.p);
    }

    @Deprecated
    /* renamed from: i, reason: collision with other method in class */
    public static boolean m4935i() {
        return m4927a(elr.b.a.q);
    }

    @Deprecated
    /* renamed from: j, reason: collision with other method in class */
    public static boolean m4936j() {
        return m4927a(elr.b.a.t);
    }

    public static boolean a(String str, String str2) {
        if (str == null || str2 == null) {
            logger.error("isModel error: innerModel == null || isModel == null");
            return false;
        }
        return str2.equals(str.split("-")[0]);
    }

    /* renamed from: a, reason: collision with other method in class */
    public static boolean m4927a(String str) {
        if (str == null) {
            logger.error("isModel error: isModel == null");
            return false;
        }
        return a(b(), str);
    }

    public static boolean b(String str) {
        if (str == null) {
            logger.error("isRegion error: isRegion == null");
            return false;
        }
        return d().equals(str);
    }

    public static boolean c(String str) {
        if (str == null) {
            logger.error("checkRegionOfInnerModel error: innerModel == null");
            return false;
        }
        return b(a(), str);
    }

    public static boolean b(String str, String str2) {
        if (str == null || str2 == null) {
            logger.error("checkRegionOfInnerModel error: innerModel == null || isRegion == null");
            return false;
        }
        if (str2.isEmpty()) {
            return !str.contains("-");
        }
        return str.endsWith(str2);
    }

    public static boolean a(String str, String str2, String str3) {
        if (str == null || str2 == null || str3 == null) {
            logger.error("checkModelAndRegionOfInnerModel error: innerModel == null || isModel == null || isRegion == null");
            return false;
        }
        return str.equals(str2 + "-" + str3);
    }

    /* renamed from: k, reason: collision with other method in class */
    public static boolean m4937k() {
        return !m4938l();
    }

    /* renamed from: l, reason: collision with other method in class */
    public static boolean m4938l() {
        return emc.f26106b.equals(els.a(elr.aR, ""));
    }

    public static boolean d(String str) {
        if (str != null) {
            return str.equals(elr.b.a.d) || str.equals(elr.b.a.k) || str.equals(elr.b.a.w) || str.equals(elr.b.a.x) || str.equals(elr.b.a.y) || str.equals(elr.b.a.z) || str.equals(elr.b.a.B) || str.equals(elr.b.a.C) || str.equals(elr.b.a.D) || str.equals(elr.b.a.E);
        }
        logger.error("isWatchModelY error: innerModel == null");
        return false;
    }


    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean e(String str) {
        if (str == null) {
            logger.error("isModelRegionChangable error: model == null");
            return false;
        }
        String str2 = str.split("-")[0];
        return str2.equals("I13") || str2.equals(elr.b.a.t) || str2.equals(elr.b.a.M);
    }



}