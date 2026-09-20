package com.justnothing.testmodule.ui.viewmodel.analysis;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.justnothing.testmodule.ui.viewmodel.BaseViewModel;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.functions.classcmd.request.InvokeConstructorRequest;
import com.justnothing.testmodule.command.functions.classcmd.response.InvokeConstructorResult;

import java.util.List;

public class ConstructorDetailViewModel extends BaseViewModel<InvokeConstructorRequest, InvokeConstructorResult> {

    private final MutableLiveData<InvokeConstructorResult> result = new MutableLiveData<>();

    public LiveData<InvokeConstructorResult> getResult() { return result; }

    public ConstructorDetailViewModel(@NonNull Application application) {
        super(application, InvokeConstructorResult.class);
    }

    public void invokeConstructor(String className, String signature,
                                   List<String> params, List<String> paramTypes, boolean freeMode) {
        isLoading.setValue(true);
        error.setValue(null);

        getExecutor().execute(() -> {
            InvokeConstructorRequest request = new InvokeConstructorRequest();
            request.setClassName(className);
            if (signature != null && !signature.isEmpty()) request.setSignature(signature);
            request.setParams(params);
            request.setParamTypes(paramTypes);
            request.setFreeMode(freeMode);

            InvokeConstructorResult invokeResult = execute(request);
            if (invokeResult != null && invokeResult.isSuccess()) {
                result.postValue(invokeResult);
            } else if (invokeResult != null) {
                postError(invokeResult.getError(), R.string.analysis_invoke_unknown_error);
            }
        });
    }
}
