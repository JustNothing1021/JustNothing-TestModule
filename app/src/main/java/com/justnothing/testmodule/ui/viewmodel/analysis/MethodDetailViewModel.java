package com.justnothing.testmodule.ui.viewmodel.analysis;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.justnothing.testmodule.ui.viewmodel.BaseViewModel;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.functions.classcmd.request.InvokeMethodRequest;
import com.justnothing.testmodule.command.functions.classcmd.response.InvokeMethodResult;

import java.util.List;

public class MethodDetailViewModel extends BaseViewModel<InvokeMethodRequest, InvokeMethodResult> {

    private final MutableLiveData<InvokeMethodResult> result = new MutableLiveData<>();

    public LiveData<InvokeMethodResult> getResult() { return result; }

    public MethodDetailViewModel(@NonNull Application application) {
        super(application, InvokeMethodResult.class);
    }

    public void invokeMethod(String className, String methodName, String signature,
                             String targetInstance, List<String> params,
                             List<String> paramTypes, boolean freeMode, boolean isStatic) {
        isLoading.setValue(true);
        error.setValue(null);

        getExecutor().execute(() -> {
            InvokeMethodRequest request = new InvokeMethodRequest();
            request.setClassName(className);
            request.setMethodName(methodName);
            if (signature != null && !signature.isEmpty()) request.setSignature(signature);
            request.setTargetInstance(targetInstance);
            request.setParams(params);
            request.setParamTypes(paramTypes);
            request.setFreeMode(freeMode);
            request.setStatic(isStatic);

            InvokeMethodResult invokeResult = execute(request);
            if (invokeResult != null && invokeResult.isSuccess()) {
                result.postValue(invokeResult);
            } else if (invokeResult != null) {
                postError(invokeResult.getError(), R.string.analysis_invoke_unknown_error);
            }
        });
    }
}
