package com.xtc.sync;

import android.text.TextUtils;
import java.util.TimeZone;

/* compiled from: SystemTimeUtils.java */
/* loaded from: classes2.dex */
public final class elt {
    private elt() {
        throw new UnsupportedOperationException("You can't instantiate SystemTimeUtils");
    }

    public static String a() {
        String displayName = TimeZone.getDefault().getDisplayName(false, 0);
        return a(displayName) ? displayName : a(TimeZone.getDefault().getRawOffset());
    }

    private static boolean a(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        int length = str.length();
        for (int i = 0; i < length; i++) {
            char charAt = str.charAt(i);
            if ((charAt <= 31 && charAt != '\t') || charAt >= 127) {
                return false;
            }
        }
        return true;
    }

    private static String a(int i) {
        char c;
        int i2 = i / dev.d;
        if (i2 < 0) {
            c = '-';
            i2 = -i2;
        } else {
            c = '+';
        }
        StringBuilder sb = new StringBuilder(9);
        sb.append("GMT");
        sb.append(c);
        a(sb, 2, i2 / 60);
        sb.append(':');
        a(sb, 2, i2 % 60);
        return sb.toString();
    }

    private static void a(StringBuilder sb, int i, int i2) {
        String num = Integer.toString(i2);
        for (int i3 = 0; i3 < i - num.length(); i3++) {
            sb.append('0');
        }
        sb.append(num);
    }
}