package com.justnothing.testmodule.ui.analysis.classanalysis;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.justnothing.methodsclient.UiClient;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.protocol.json.JsonProtocol;
import com.justnothing.testmodule.protocol.json.request.InvokeMethodRequest;
import com.justnothing.testmodule.protocol.json.response.CommandResult;
import com.justnothing.testmodule.protocol.json.response.InvokeMethodResult;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MethodDetailViewModel extends AndroidViewModel {
    
    private final UiClient client = UiClient.getInstance();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<InvokeMethodResult> result = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    
    public LiveData<Boolean> isLoading() { return isLoading; }
    public LiveData<InvokeMethodResult> getResult() { return result; }
    public LiveData<String> getError() { return error; }
    
    public MethodDetailViewModel(@NonNull Application application) {
        super(application);
    }
    
    public void invokeMethod(String className, String methodName, String signature,
                             String targetInstance, List<String> params, 
                             List<String> paramTypes, boolean freeMode, boolean isStatic) {
        isLoading.setValue(true);
        error.setValue(null);
        
        executor.execute(() -> {
            try {
                InvokeMethodRequest request = new InvokeMethodRequest();
                request.setClassName(className);
                request.setMethodName(methodName);
                request.setSignature(signature);
                request.setTargetInstance(targetInstance);
                request.setParams(params);
                request.setParamTypes(paramTypes);
                request.setFreeMode(freeMode);
                request.setStatic(isStatic);
                
                String jsonResponse = client.executeCommandRequest(JsonProtocol.toJson(request));
                
                CommandResult parsedResult = JsonProtocol.parseResponse(jsonResponse);
                
                if (parsedResult instanceof InvokeMethodResult invokeResult) {
                    if (invokeResult.isSuccess()) {
                        result.postValue(invokeResult);
                    } else {
                        CommandResult.ErrorInfo err = invokeResult.getError();
                        error.postValue(err != null ? err.getMessage() : getApplication().getString(R.string.analysis_invoke_unknown_error));
                    }
                } else {
                    error.postValue(getApplication().getString(R.string.analysis_response_type_error,
                        "InvokeMethodResult", parsedResult.getClass().getSimpleName()));
                }
            } catch (Exception e) {
                error.postValue(getApplication().getString(R.string.analysis_invoke_exception, e.getMessage()));
            } finally {
                isLoading.postValue(false);
            }
        });
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
