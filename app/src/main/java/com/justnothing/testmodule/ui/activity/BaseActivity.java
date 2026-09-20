package com.justnothing.testmodule.ui.activity;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.justnothing.testmodule.utils.logging.Logger;

/**
 * 所有 Activity 的公共父类。
 *
 * <p>只收三样东西，且都是「每个 Activity 都会自己写一遍、且写法完全一致」的：
 * 日志器、主线程 Handler、Toast。其他一切照旧 —— 这个类不做模板方法、不接管生命周期、
 * 不强制 {@code initView()} 之类的钩子，因为那才是真正会把 34 个界面绑死的做法。</p>
 *
 * <h3>logger</h3>
 * {@code Logger} 在项目里是抽象类，每个类都要么继承它重写 {@code getTag()}，
 * 要么写 {@code Logger.getLoggerForName("类名")} —— 后者在每个 Activity 里都写成了一行
 * {@code private static final Logger logger = Logger.getLoggerForName("XXXActivity")}，
 * 里面的字符串是手抄的类名，改类名时没人会记得同步它。
 * 这里用 {@code getClass().getSimpleName()} 取，不可能写错。
 *
 * <p>注意：它必须是实例字段而不是 static。tag 要跟着实际类型走，
 * 而 static 字段在继承体系里只会拿到声明它的那个类。</p>
 *
 * <h3>mainHandler</h3>
 * 原来有 3 个 Activity 各写了一遍 {@code handler = new Handler(Looper.getMainLooper())}，
 * 另有 2 处直接在方法里 new 一个用完就扔。主线程 Handler 本来就是「谁都能用、没有状态」的东西，
 * 没必要每个界面各持一份。</p>
 *
 * <h3>showToast 为什么有两个参数版本</h3>
 * 原来的 Toast 里 {@code LENGTH_SHORT} 和 {@code LENGTH_LONG} 都在用，
 * 而且长短的选择是有意的（比如「导出成功，文件在 xxx」用 LONG 让人看清路径，
 * 而「已复制」用 SHORT）。所以这里保留时长参数，不做「统一成 SHORT」这种会改变观感的精简。
 * 一参版本默认 SHORT，覆盖占多数的调用点。</p>
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected final Logger logger = Logger.getLoggerForName(getClass().getSimpleName());

    protected final Handler mainHandler = new Handler(Looper.getMainLooper());

    protected void showToast(CharSequence message) {
        showToast(message, Toast.LENGTH_SHORT);
    }

    protected void showToast(@StringRes int resId) {
        showToast(resId, Toast.LENGTH_SHORT);
    }

    protected void showToast(CharSequence message, int duration) {
        Toast.makeText(this, message, duration).show();
    }

    protected void showToast(@StringRes int resId, int duration) {
        Toast.makeText(this, resId, duration).show();
    }
}
