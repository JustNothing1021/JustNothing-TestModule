package com.justnothing.testmodule.ui.viewmodel.analysis;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.justnothing.testmodule.ui.viewmodel.BaseViewModel;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.functions.classcmd.request.GetFieldValueRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.SetFieldValueRequest;
import com.justnothing.testmodule.command.functions.classcmd.response.GetFieldValueResult;
import com.justnothing.testmodule.command.functions.classcmd.response.SetFieldValueResult;

public class FieldDetailViewModel extends BaseViewModel<GetFieldValueRequest, GetFieldValueResult> {

    private final MutableLiveData<GetFieldValueResult> result = new MutableLiveData<>();
    private final MutableLiveData<Boolean> setSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> setError = new MutableLiveData<>();

    public LiveData<GetFieldValueResult> getResult() { return result; }
    public LiveData<Boolean> getSetSuccess() { return setSuccess; }
    public LiveData<String> getSetError() { return setError; }

    public FieldDetailViewModel(@NonNull Application application) {
        super(application, GetFieldValueResult.class);
    }

    public void getFieldValue(String className, String fieldName,
                              String targetInstance, boolean isStatic) {
        isLoading.setValue(true);
        error.setValue(null);

        getExecutor().execute(() -> {
            GetFieldValueRequest request = new GetFieldValueRequest();
            request.setClassName(className);
            request.setFieldName(fieldName);
            request.setTargetInstance(targetInstance);
            request.setStatic(isStatic);

            GetFieldValueResult fieldResult = execute(request);
            if (fieldResult != null && fieldResult.isSuccess()) {
                result.postValue(fieldResult);
            } else if (fieldResult != null) {
                postError(fieldResult.getError(), R.string.analysis_field_get_failed);
            }
        });
    }

    public void setFieldValue(String className, String fieldName,
                              String targetInstance, String valueExpression,
                              String valueTypeHint, boolean isStatic) {
        isLoading.setValue(true);
        setError.setValue(null);

        getExecutor().execute(() -> {
            SetFieldValueRequest request = new SetFieldValueRequest();
            request.setClassName(className);
            request.setFieldName(fieldName);
            request.setTargetInstance(targetInstance);
            request.setValueExpression(valueExpression);
            request.setValueTypeHint(valueTypeHint);
            request.setStatic(isStatic);

            SetFieldValueResult setResult = executeAny(request, SetFieldValueResult.class);
            if (setResult != null && setResult.isSuccess()) {
                setSuccess.postValue(true);
            } else if (setResult != null) {
                postError(setResult.getError(), R.string.analysis_set_field_unknown_error);
            }
        });
    }
}
