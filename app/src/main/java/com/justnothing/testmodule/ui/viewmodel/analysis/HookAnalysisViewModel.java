package com.justnothing.testmodule.ui.viewmodel.analysis;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.justnothing.testmodule.ui.viewmodel.BaseViewModel;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.functions.hook.request.HookAddRequest;
import com.justnothing.testmodule.command.functions.hook.request.HookListRequest;
import com.justnothing.testmodule.command.functions.hook.request.HookRemoveRequest;
import com.justnothing.testmodule.command.functions.hook.request.HookInfoRequest;
import com.justnothing.testmodule.command.functions.hook.request.HookOutputRequest;
import com.justnothing.testmodule.command.functions.hook.request.HookEnableRequest;
import com.justnothing.testmodule.command.functions.hook.request.HookDisableRequest;
import com.justnothing.testmodule.command.functions.hook.request.HookClearRequest;
import com.justnothing.testmodule.command.functions.hook.HookAddResult;
import com.justnothing.testmodule.command.functions.hook.HookListResult;
import com.justnothing.testmodule.ui.activity.analysis.hook.HookSnapshot;

import java.util.Locale;

public class HookAnalysisViewModel extends BaseViewModel<HookListRequest, HookListResult> {

    private final MutableLiveData<HookSnapshot> hookData = new MutableLiveData<>();
    private final MutableLiveData<String> lastUpdateTime = new MutableLiveData<>("");
    private final MutableLiveData<HookAddResult> actionResult = new MutableLiveData<>();

    public LiveData<HookSnapshot> getHookData() { return hookData; }
    public LiveData<String> getLastUpdateTime() { return lastUpdateTime; }
    public LiveData<HookAddResult> getActionResult() { return actionResult; }

    public HookAnalysisViewModel(@NonNull Application application) {
        super(application, HookListResult.class);
    }

    public void queryHookList() {
        isLoading.postValue(true);
        error.postValue(null);

        getExecutor().execute(() -> {
            HookListResult result = execute(new HookListRequest());
            if (result != null && result.isSuccess()) {
                HookSnapshot snapshot = HookSnapshot.fromResult(result);
                hookData.postValue(snapshot);
                updateLastUpdateTime(result.getTimestamp());
            } else if (result != null) {
                postError(result.getError(), R.string.analysis_hook_error_query_failed);
            }
        });
    }

    public void addHook(String className, String methodName, String signature,
                         String beforeCode, String afterCode, String replaceCode,
                         String beforeCodebase, String afterCodebase, String replaceCodebase) {
        isLoading.postValue(true);
        error.postValue(null);

        getExecutor().execute(() -> {
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

            HookAddResult addResult = executeAny(request, HookAddResult.class);
            if (addResult != null) {
                actionResult.postValue(addResult);
                if (addResult.isSuccessAction()) queryHookList();
            }
        });
    }

    public void performAction(String action, String hookId) {
        performAction(action, hookId, 50);
    }

    public void performAction(String action, String hookId, int outputCount) {
        isLoading.postValue(true);
        error.postValue(null);

        getExecutor().execute(() -> {
            CommandRequest request = switch (action) {
                case "remove" -> {
                    HookRemoveRequest req = new HookRemoveRequest();
                    req.setHookId(hookId);
                    yield req;
                }
                case "info" -> {
                    HookInfoRequest req = new HookInfoRequest();
                    req.setHookId(hookId);
                    yield req;
                }
                case "output" -> {
                    HookOutputRequest req = new HookOutputRequest();
                    req.setHookId(hookId);
                    req.setOutputCount(outputCount);
                    yield req;
                }
                case "enable" -> {
                    HookEnableRequest req = new HookEnableRequest();
                    req.setHookId(hookId);
                    yield req;
                }
                case "disable" -> {
                    HookDisableRequest req = new HookDisableRequest();
                    req.setHookId(hookId);
                    yield req;
                }
                case "clear" -> new HookClearRequest();
                default -> throw new IllegalArgumentException("Unknown action: " + action);
            };

            HookAddResult actionResult = executeAny(request, HookAddResult.class);
            if (actionResult != null) {
                this.actionResult.postValue(actionResult);
                if (!"info".equals(action) && !"output".equals(action)) {
                    queryHookList();
                }
            }
        });
    }

    private void updateLastUpdateTime(long timestamp) {
        if (timestamp <= 0) timestamp = System.currentTimeMillis();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        lastUpdateTime.postValue(sdf.format(new java.util.Date(timestamp)));
    }
}
