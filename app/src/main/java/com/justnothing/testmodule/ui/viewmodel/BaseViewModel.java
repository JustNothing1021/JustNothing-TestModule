package com.justnothing.testmodule.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.justnothing.methodsclient.UiClient;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.protocol.JsonProtocol;
import com.justnothing.testmodule.command.base.CommandRequest;
import com.justnothing.testmodule.command.base.CommandResult;
import com.justnothing.testmodule.utils.logging.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class BaseViewModel<RequestType extends CommandRequest, ResultType extends CommandResult> extends AndroidViewModel {
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

    @Nullable
    protected <Result extends CommandResult> Result executeAny(CommandRequest request, Class<Result> resultClass) {
        try {
            logger.debug("开始执行命令: " + request.getClass().getSimpleName());
            String jsonResponse = client.executeCommandRequest(JsonProtocol.toJson(request));
            CommandResult parsedResult = JsonProtocol.parseResponse(jsonResponse);
            logger.debug("执行命令响应: " + parsedResult.getClass().getSimpleName());
            if (resultClass.isInstance(parsedResult)) {
                return resultClass.cast(parsedResult);
            } else {
                String errorMsg = getApplication().getString(R.string.analysis_response_type_error,
                        resultClass.getSimpleName(), parsedResult.getClass().getName());
                logger.error(errorMsg);
                error.postValue(errorMsg);
            }
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
        String msg = err != null ? err.getMessage() : getApplication().getString(fallbackRes);
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
