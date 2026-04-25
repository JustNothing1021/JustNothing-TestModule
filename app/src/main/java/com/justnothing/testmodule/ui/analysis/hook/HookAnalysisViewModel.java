package com.justnothing.testmodule.ui.analysis.hook;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.justnothing.methodsclient.UiClient;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.protocol.json.JsonProtocol;
import com.justnothing.testmodule.protocol.json.request.HookActionRequest;
import com.justnothing.testmodule.protocol.json.request.HookAddRequest;
import com.justnothing.testmodule.protocol.json.request.HookListRequest;
import com.justnothing.testmodule.protocol.json.response.CommandResult;
import com.justnothing.testmodule.protocol.json.response.HookListResult;
import com.justnothing.testmodule.protocol.json.response.HookAddResult;
import com.justnothing.testmodule.utils.logging.Logger;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HookAnalysisViewModel extends AndroidViewModel {

    private static final String TAG = "HookAnalysisVM";
    private final Logger logger = Logger.getLoggerForName(TAG);

    private final UiClient client = UiClient.getInstance();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<HookSnapshot> hookData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> lastUpdateTime = new MutableLiveData<>("");
    private final MutableLiveData<HookAddResult> actionResult = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public LiveData<HookSnapshot> getHookData() { return hookData; }
    public LiveData<Boolean> isLoading() { return isLoading; }
    public LiveData<String> getLastUpdateTime() { return lastUpdateTime; }
    public LiveData<HookAddResult> getActionResult() { return actionResult; }
    public LiveData<String> getError() { return error; }

    public HookAnalysisViewModel(@NonNull Application application) {
        super(application);
    }

    public void queryHookList() {
        isLoading.postValue(true);
        error.postValue(null);

        executor.execute(() -> {
            try {
                HookListRequest request = new HookListRequest();
                String jsonResponse = client.executeCommandRequest(JsonProtocol.toJson(request));
                CommandResult parsedResult = JsonProtocol.parseResponse(jsonResponse);

                if (parsedResult instanceof HookListResult result) {
                    if (result.isSuccess()) {
                        HookSnapshot snapshot = HookSnapshot.fromResult(result);
                        hookData.postValue(snapshot);
                        updateLastUpdateTime(result.getTimestamp());
                    } else {
                        String msg = result.getError() != null ? result.getError().getMessage()
                                : getApplication().getString(R.string.analysis_hook_error_query_failed);
                        error.postValue(msg);
                    }
                } else {
                    error.postValue(getApplication().getString(
                            R.string.analysis_hook_error_response_type, parsedResult.getClass().getSimpleName()));
                }
            } catch (Exception e) {
                logger.error("Hook列表查询异常", e);
                error.postValue(getApplication().getString(
                        R.string.analysis_hook_error_query_format, e.getMessage()));
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    public void addHook(String className, String methodName, String signature,
                         String beforeCode, String afterCode, String replaceCode,
                         String beforeCodebase, String afterCodebase, String replaceCodebase) {
        isLoading.postValue(true);
        error.postValue(null);

        executor.execute(() -> {
            try {
                HookAddRequest request = new HookAddRequest();
                request.setClassName(className);
                request.setMethodName(methodName);
                if (signature != null && !signature.isEmpty()) request.setSignature(signature);
                if (beforeCode != null) request.setBeforeCode(beforeCode);
                if (afterCode != null) request.setAfterCode(afterCode);
                if (replaceCode != null) request.setReplaceCode(replaceCode);
                if (beforeCodebase != null) request.setBeforeCodebase(beforeCodebase);
                if (afterCodebase != null) request.setAfterCodebase(afterCodebase);
                if (replaceCodebase != null) request.setReplaceCodebase(replaceCodebase);

                String jsonResponse = client.executeCommandRequest(JsonProtocol.toJson(request));
                CommandResult parsedResult = JsonProtocol.parseResponse(jsonResponse);

                if (parsedResult instanceof HookAddResult result) {
                    actionResult.postValue(result);
                    if (result.isSuccessAction()) {
                        queryHookList();
                    }
                } else {
                    error.postValue(getApplication().getString(
                            R.string.analysis_hook_error_response_type, parsedResult.getClass().getSimpleName()));
                }
            } catch (Exception e) {
                logger.error("添加Hook异常", e);
                error.postValue(getApplication().getString(
                        R.string.analysis_hook_error_add_format, e.getMessage()));
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    public void performAction(String action, String hookId) {
        performAction(action, hookId, 50);
    }

    public void performAction(String action, String hookId, int outputCount) {
        isLoading.postValue(true);
        error.postValue(null);

        executor.execute(() -> {
            try {
                HookActionRequest request = new HookActionRequest();
                request.setAction(action);
                if (hookId != null) request.setHookId(hookId);
                request.setOutputCount(outputCount);

                String jsonResponse = client.executeCommandRequest(JsonProtocol.toJson(request));
                CommandResult parsedResult = JsonProtocol.parseResponse(jsonResponse);

                if (parsedResult instanceof HookAddResult result) {
                    actionResult.postValue(result);
                    if (!HookActionRequest.ACTION_INFO.equals(action)
                            && !HookActionRequest.ACTION_OUTPUT.equals(action)) {
                        queryHookList();
                    }
                } else {
                    error.postValue(getApplication().getString(
                            R.string.analysis_hook_error_response_type, parsedResult.getClass().getSimpleName()));
                }
            } catch (Exception e) {
                logger.error("Hook操作异常: " + action, e);
                error.postValue(getApplication().getString(
                        R.string.analysis_hook_error_action_format, action, e.getMessage()));
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    private void updateLastUpdateTime(long timestamp) {
        if (timestamp <= 0) timestamp = System.currentTimeMillis();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        lastUpdateTime.postValue(sdf.format(new java.util.Date(timestamp)));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
