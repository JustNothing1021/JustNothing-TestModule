package com.justnothing.testmodule.ui.viewmodel.analysis;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.justnothing.testmodule.ui.viewmodel.BaseViewModel;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.functions.classcmd.request.FieldRequest;
import com.justnothing.testmodule.command.functions.classcmd.response.GetFieldValueResult;

/**
 * 字段详情页。
 *
 * <p>服务端 class:field 路由绑定的是 {@link FieldRequest}（用 --get/--set 操作符表达操作），
 * 旧的 GetFieldValueRequest / SetFieldValueRequest 已不再注册。这里直接设置 FieldRequest 的
 * useGet/useSet 字段来表达操作——JSON 路径不经过命令行解析，receivedOperators 是空的，
 * 所以 FieldRequest.getOperationMode() 也认这两个字段。</p>
 */
public class FieldDetailViewModel extends BaseViewModel<FieldRequest, GetFieldValueResult> {

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
            FieldRequest request = new FieldRequest();
            request.setClassName(className);
            request.setTargetInstance(targetInstance);
            request.setStaticOnly(isStatic);
            request.setUseGet(true);
            request.setGetTargetFieldName(fieldName);
            request.setShowValue(true);

            GetFieldValueResult fieldResult = execute(request);
            if (fieldResult != null && fieldResult.isSuccess()) {
                result.postValue(fieldResult);
            } else if (fieldResult != null) {
                postError(fieldResult.getError(), R.string.analysis_field_get_failed);
            }
        });
    }

    /**
     * 设置字段值。
     *
     * <p>class:field 的 FieldRequest 没有类型提示字段，服务端直接按表达式求值
     * （类型由表达式自身推断），因此这里不需要 valueTypeHint。</p>
     */
    public void setFieldValue(String className, String fieldName,
                              String targetInstance, String valueExpression,
                              boolean isStatic) {
        isLoading.setValue(true);
        setError.setValue(null);

        getExecutor().execute(() -> {
            FieldRequest request = new FieldRequest();
            request.setClassName(className);
            request.setTargetInstance(targetInstance);
            request.setStaticOnly(isStatic);
            request.setUseSet(true);
            request.setSetTargetFieldName(fieldName);
            request.setSetValueToSet(valueExpression);

            GetFieldValueResult setResult = execute(request);
            if (setResult != null && setResult.isSuccess()) {
                setSuccess.postValue(true);
            } else if (setResult != null) {
                postError(setResult.getError(), R.string.analysis_set_field_unknown_error);
            }
        });
    }
}
