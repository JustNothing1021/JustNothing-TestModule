package com.justnothing.testmodule.ui.viewmodel.analysis;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.justnothing.testmodule.ui.viewmodel.BaseViewModel;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.functions.classcmd.request.ClassHierarchyRequest;
import com.justnothing.testmodule.command.functions.classcmd.response.ClassHierarchyResult;

public class ClassGraphViewModel extends BaseViewModel<ClassHierarchyRequest, ClassHierarchyResult> {

    private final MutableLiveData<ClassHierarchyResult> hierarchyResult = new MutableLiveData<>();

    public LiveData<ClassHierarchyResult> getHierarchyResult() { return hierarchyResult; }

    public ClassGraphViewModel(@NonNull Application application) {
        super(application, ClassHierarchyResult.class);
    }

    public void queryClassHierarchy(String className) {
        if (className == null || className.trim().isEmpty()) {
            error.setValue(getApplication().getString(R.string.analysis_enter_class_name_hint));
            return;
        }

        logger.info("开始查询类继承图: " + className);
        isLoading.setValue(true);
        error.setValue(null);

        getExecutor().execute(() -> {
            ClassHierarchyResult result = execute(new ClassHierarchyRequest(className));
            if (result != null && result.isSuccess() && result.getClassChain() != null) {
                logger.info("查询成功: " + className);
                hierarchyResult.postValue(result);
            } else if (result != null) {
                postError(result.getError(), R.string.analysis_class_query_failed_format);
            }
        });
    }
}
