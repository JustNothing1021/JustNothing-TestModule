package com.xtc.sync;

import com.xtc.domain.Domain;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/* compiled from: BuiltInDomainClient.java */
/* loaded from: classes5.dex */
public class byw implements byy {

    /* renamed from: a, reason: collision with other field name */
    private static final String f4877a = "CN_BJ";

    /* renamed from: a, reason: collision with other field name */
    private static final Map<String, Domain> f4878a;

    /* renamed from: b, reason: collision with root package name */
    private static final String f22596b = "SG_SG";
    private static final String c = "US_CA";
    private static final String d = "DE_FRA";
    private static final String e = "VN_VN";
    private static final String f = "正式环境";
    private static final String g = "东南亚正式环境";
    private static final String h = "美国正式环境";
    private static final String i = "欧洲正式环境";
    private static final String j = "越南正式环境";
    private static final String k = elw.d();

    /* renamed from: a, reason: collision with other field name */
    private static final boolean f4879a = a();

    /* renamed from: b, reason: collision with other field name */
    private static final boolean f4880b = b();

    /* renamed from: c, reason: collision with other field name */
    private static final boolean f4881c = c();

    /* renamed from: d, reason: collision with other field name */
    private static final boolean f4882d = d();


    static {
        HashMap<String, Domain> hashMap = new HashMap<>(Domain.values().length);
        for (Domain domain : Domain.values()) {
            hashMap.put(domain.getName(), domain);
        }
        f4878a = Collections.unmodifiableMap(hashMap);
    }

    private static boolean a() {
        return "TW".equals(k) || "ID".equals(k) || "TH".equals(k) || "MY".equals(k) || "SG".equals(k) || "IN".equals(k);
    }

    private static boolean b() {
        return "US".equals(k);
    }

    private static boolean c() {
        return "DE".equals(k) || "AU".equals(k) || "GB".equals(k);
    }

    private static boolean d() {
        return "VN".equals(k);
    }

    @Override // com.xtc.sync.byy
    /* renamed from: a, reason: collision with other method in class */
    public String mo2771a() {
        return f4879a ? f22596b : f4880b ? c : f4881c ? d : f4882d ? e : f4877a;
    }

    @Override // com.xtc.sync.byy
    public String a(String str) {
        Domain domain = f4878a.get(str);
        if (domain == null) {
            return null;
        }
        if (f4879a) {
            return domain.getSaDomain();
        }
        if (f4880b) {
            return domain.getUsDomain();
        }
        if (f4881c) {
            return domain.getDeDomain();
        }
        if (f4882d) {
            return domain.getVnDomain();
        }
        return domain.getCnDomain();
    }
}