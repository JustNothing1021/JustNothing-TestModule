package com.justnothing.testmodule.ui.viewmodel.analysis;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.justnothing.testmodule.ui.viewmodel.BaseViewModel;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.functions.classcmd.model.ClassInfo;
import com.justnothing.testmodule.command.functions.classcmd.request.ClassInfoRequest;
import com.justnothing.testmodule.command.functions.classcmd.response.ClassInfoResult;

public class ClassBrowserViewModel extends BaseViewModel<ClassInfoRequest, ClassInfoResult> {

    private final MutableLiveData<ClassInfo> classInfo = new MutableLiveData<>();
    private final MutableLiveData<Boolean> serverStatus = new MutableLiveData<>();

    public LiveData<ClassInfo> getClassInfo() { return classInfo; }
    public LiveData<Boolean> getServerStatus() { return serverStatus; }

    public ClassBrowserViewModel(@NonNull Application application) {
        super(application, ClassInfoResult.class);
    }

    public void checkServerStatus() {
        getExecutor().execute(() -> serverStatus.postValue(client.isServerAvailable()));
    }

    public void queryClassInfo(String className) {
        if (className == null || className.trim().isEmpty()) {
            error.setValue(getApplication().getString(R.string.analysis_enter_class_name_hint));
            return;
        }
        isLoading.setValue(true);
        error.setValue(null);

        getExecutor().execute(() -> {
            ClassInfoRequest request = new ClassInfoRequest(className);
            request.setShowInterfaces(true);
            request.setShowConstructors(true);
            request.setShowSuper(true);
            request.setShowModifiers(true);
            request.setShowMethods(true);
            request.setShowFields(true);

            ClassInfoResult result = execute(request);
            if (result != null && result.isSuccess() && result.getClassInfo() != null) {
                classInfo.postValue(result.getClassInfo());
            } else if (result != null) {
                postError(result.getError(), R.string.analysis_class_query_failed_format);
            }
        });
    }
}
