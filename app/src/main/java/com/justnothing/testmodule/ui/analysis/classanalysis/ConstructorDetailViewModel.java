package com.justnothing.testmodule.ui.analysis.classanalysis;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.justnothing.methodsclient.UiClient;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.protocol.json.JsonProtocol;
import com.justnothing.testmodule.protocol.json.request.InvokeConstructorRequest;
import com.justnothing.testmodule.protocol.json.response.CommandResult;
import com.justnothing.testmodule.protocol.json.response.InvokeConstructorResult;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConstructorDetailViewModel extends AndroidViewModel {
    
    private final UiClient client = UiClient.getInstance();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<InvokeConstructorResult> result = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    
    public LiveData<Boolean> isLoading() { return isLoading; }
    public LiveData<InvokeConstructorResult> getResult() { return result; }
    public LiveData<String> getError() { return error; }
    
    public ConstructorDetailViewModel(@NonNull Application application) {
        super(application);
    }
    
    public void invokeConstructor(String className, String signature, 
                                   List<String> params, List<String> paramTypes, boolean freeMode) {
        isLoading.setValue(true);
        error.setValue(null);
        
        executor.execute(() -> {
            try {
                InvokeConstructorRequest request = new InvokeConstructorRequest();
                request.setClassName(className);
                request.setSignature(signature);
                request.setParams(params);
                request.setParamTypes(paramTypes);
                request.setFreeMode(freeMode);
                
                String jsonResponse = client.executeCommandRequest(JsonProtocol.toJson(request));
                
                CommandResult parsedResult = JsonProtocol.parseResponse(jsonResponse);
                
                if (parsedResult instanceof InvokeConstructorResult invokeResult) {
                    if (invokeResult.isSuccess()) {
                        result.postValue(invokeResult);
                    } else {
                        CommandResult.ErrorInfo err = invokeResult.getError();
                        error.postValue(err != null ? err.getMessage() : getApplication().getString(R.string.invoke_failed));
                    }
                } else {
                    error.postValue(getApplication().getString(R.string.response_type_error,
                        "InvokeConstructorResult", parsedResult.getClass().getSimpleName()));
                }
            } catch (Exception e) {
                error.postValue(getApplication().getString(R.string.invoke_exception, e.getMessage()));
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
