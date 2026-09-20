package com.justnothing.testmodule.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.justnothing.methodsclient.UiClient;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.framework.utils.GsonFactory;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.model.CommandRouter;
import com.justnothing.testmodule.utils.logging.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class BaseViewModel<RequestType extends CommandRequest<?>, ResultType extends CommandResult>
        extends AndroidViewModel {
    protected final Logger logger;
    protected final UiClient client = UiClient.getInstance();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    protected final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    protected final MutableLiveData<String> error = new MutableLiveData<>();
    protected final MutableLiveData<String> message = new MutableLiveData<>();
    private final Class<ResultType> resultTypeClass;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledTask;

    public BaseViewModel(@NonNull Application application, @NonNull Class<ResultType> resultTypeClass) {
        super(application);
        this.resultTypeClass = resultTypeClass;
        logger = Logger.getLoggerForName(getClass().getSimpleName());
    }

    public MutableLiveData<Boolean> isLoading() { return isLoading; }
    public MutableLiveData<String> getError() { return error; }
    public MutableLiveData<String> getMessage() { return message; }
    protected ExecutorService getExecutor() { return executor; }


    protected @Nullable ResultType execute(RequestType request) {
        return executeAny(request, resultTypeClass);
    }

    /**
     * 强类型入口：结果类型直接从请求的泛型签名推出（{@code CommandRequest<Res>}），
     * 调用处不必再传 {@code Class}，也不可能把结果赋给错误的类型。
     *
     * <p>运行时用路由表把 {@code Res} 还原成具体 Class——结果类型本就由 handler 泛型推导，
     * 与 Request 声明的一致性由构建期测试钉死，所以这是可靠反查而非猜测。</p>
     */
    protected @Nullable <Res extends CommandResult> Res executeAny(CommandRequest<Res> request) {
        @SuppressWarnings("unchecked")
        Class<? extends CommandRequest<?>> requestType =
                (Class<? extends CommandRequest<?>>) (Class<?>) request.getClass();
        Class<? extends CommandResult> resultType =
                CommandRouter.getInstance().getResultTypeFor(requestType);
        if (resultType == null) {
            String msg = getApplication().getString(R.string.analysis_unregistered_route_error_format,
                    request.getClass().getSimpleName());
            logger.error(msg);
            error.postValue(msg);
            return null;
        }
        @SuppressWarnings("unchecked")
        Class<Res> typed = (Class<Res>) resultType;
        return executeAny(request, typed);
    }

    @Nullable
    protected <Result extends CommandResult> Result executeAny(CommandRequest<?> request, Class<Result> resultClass) {
        try {
            logger.debug("开始执行命令: " + request.getClass().getSimpleName());
            String jsonResponse = client.executeCommandRequest(GsonFactory.getInstance().toJson(request));
            Result parsedResult = GsonFactory.getInstance().fromJson(jsonResponse, resultClass);
            if (parsedResult == null) {
                String errorMsg = getApplication().getString(R.string.analysis_execution_failed_format,
                        getApplication().getString(R.string.analysis_response_parse_failed));
                logger.error(errorMsg);
                error.postValue(errorMsg);
                return null;
            }
            logger.debug("执行命令响应: " + parsedResult.getClass().getSimpleName());
            return parsedResult;
        } catch (Exception e) {
            logger.error("执行命令时出现异常", e);
            error.postValue(getApplication().getString(R.string.analysis_execution_failed_format, e.getMessage()));
        } finally {
            isLoading.postValue(false);
        }
        return null;
    }

    // ==================== 自动刷新调度器 ====================

    protected void startAutoRefresh(int intervalSeconds, Runnable refreshTask) {
        stopAutoRefresh();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduledTask = scheduler.scheduleWithFixedDelay(() -> {
            try {
                refreshTask.run();
            } catch (Exception e) {
                logger.warn("自动刷新任务异常", e);
            }
        }, 0, intervalSeconds, TimeUnit.SECONDS);
        logger.info("自动刷新已启动, 间隔: " + intervalSeconds + "秒");
    }

    protected void stopAutoRefresh() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(false);
        }
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        scheduledTask = null;
        scheduler = null;
    }

    // ==================== 工具方法 ====================

    protected void postError(@Nullable CommandResult.ErrorInfo err, int fallbackRes) {
        String serverMsg = err != null ? err.getMessage() : null;
        String msg;
        if (serverMsg != null && !serverMsg.trim().isEmpty()) {
            msg = serverMsg;
        } else {
            // 服务端没有给出错误信息（例如只回了一个空 result）：补一个占位符，
            // 避免把带 %s 的模板资源原样显示成"查询失败: %s"这种没信息量的文案。
            String placeholder = getApplication().getString(R.string.error_server_returned_void);
            String template = getApplication().getString(fallbackRes);
            msg = template.contains("%")
                    ? getApplication().getString(fallbackRes, placeholder)
                    : template + ": " + placeholder;
        }
        logger.error("操作失败: " + msg);
        error.postValue(msg);
    }

    protected void postError(String msg) {
        error.postValue(msg);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopAutoRefresh();
        executor.shutdown();
    }
}
