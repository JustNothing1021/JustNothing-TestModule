package com.justnothing.testmodule.ui.analysis.classanalysis;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.justnothing.methodsclient.UiClient;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.protocol.json.JsonProtocol;
import com.justnothing.testmodule.protocol.json.request.GetFieldValueRequest;
import com.justnothing.testmodule.protocol.json.request.SetFieldValueRequest;
import com.justnothing.testmodule.protocol.json.response.CommandResult;
import com.justnothing.testmodule.protocol.json.response.GetFieldValueResult;
import com.justnothing.testmodule.protocol.json.response.SetFieldValueResult;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FieldDetailViewModel extends AndroidViewModel {
    
    private final UiClient client = UiClient.getInstance();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<GetFieldValueResult> result = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> setSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> setError = new MutableLiveData<>();
    
    public LiveData<Boolean> isLoading() { return isLoading; }
    public LiveData<GetFieldValueResult> getResult() { return result; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getSetSuccess() { return setSuccess; }
    public LiveData<String> getSetError() { return setError; }
    
    public FieldDetailViewModel(@NonNull Application application) {
        super(application);
    }
    
    public void getFieldValue(String className, String fieldName, 
                              String targetInstance, boolean isStatic) {
        isLoading.setValue(true);
        error.setValue(null);
        
        executor.execute(() -> {
            try {
                GetFieldValueRequest request = new GetFieldValueRequest();
                request.setClassName(className);
                request.setFieldName(fieldName);
                request.setTargetInstance(targetInstance);
                request.setStatic(isStatic);
                
                String jsonResponse = client.executeCommandRequest(JsonProtocol.toJson(request));
                
                CommandResult parsedResult = JsonProtocol.parseResponse(jsonResponse);
                
                if (parsedResult instanceof GetFieldValueResult fieldResult) {
                    if (fieldResult.isSuccess()) {
                        result.postValue(fieldResult);
                    } else {
                        CommandResult.ErrorInfo err = fieldResult.getError();
                        error.postValue(err != null ? err.getMessage() : getApplication().getString(R.string.get_failed));
                    }
                } else {
                    error.postValue(getApplication().getString(R.string.response_type_error, 
                        "GetFieldValueResult", parsedResult.getClass().getSimpleName()));
                }
            } catch (Exception e) {
                error.postValue(getApplication().getString(R.string.get_exception, e.getMessage()));
            } finally {
                isLoading.postValue(false);
            }
        });
    }
    
    public void setFieldValue(String className, String fieldName, 
                              String targetInstance, String valueExpression, 
                              String valueTypeHint, boolean isStatic) {
        isLoading.setValue(true);
        setError.setValue(null);
        
        executor.execute(() -> {
            try {
                SetFieldValueRequest request = new SetFieldValueRequest();
                request.setClassName(className);
                request.setFieldName(fieldName);
                request.setTargetInstance(targetInstance);
                request.setValueExpression(valueExpression);
                request.setValueTypeHint(valueTypeHint);
                request.setStatic(isStatic);
                
                String jsonResponse = client.executeCommandRequest(JsonProtocol.toJson(request));
                
                CommandResult parsedResult = JsonProtocol.parseResponse(jsonResponse);
                
                if (parsedResult instanceof SetFieldValueResult setResult) {
                    if (setResult.isSuccess()) {
                        setSuccess.postValue(true);
                    } else {
                        CommandResult.ErrorInfo err = setResult.getError();
                        setError.postValue(err != null ? err.getMessage() : getApplication().getString(R.string.set_failed_msg));
                    }
                } else {
                    setError.postValue(getApplication().getString(R.string.response_type_error,
                        "SetFieldValueResult", parsedResult.getClass().getSimpleName()));
                }
            } catch (Exception e) {
                Throwable cause = e.getCause();
                String errorMsg = cause != null ? cause.getClass().getName() + ": " + cause.getMessage() : e.getMessage();
                setError.postValue(getApplication().getString(R.string.set_exception, errorMsg));
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
