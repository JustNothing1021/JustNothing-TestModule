package com.justnothing.testmodule.ui.viewmodel.analysis;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.justnothing.testmodule.ui.viewmodel.BaseViewModel;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.functions.system.SystemFieldInfo;
import com.justnothing.testmodule.command.functions.script.SystemInfoRequest;
import com.justnothing.testmodule.command.functions.system.SystemInfoResult;

import java.util.List;

public class SystemInfoQueryViewModel extends BaseViewModel<SystemInfoRequest, SystemInfoResult> {

    private final MutableLiveData<List<SystemFieldInfo>> fields = new MutableLiveData<>();

    public LiveData<List<SystemFieldInfo>> getFields() { return fields; }

    public SystemInfoQueryViewModel(@NonNull Application application) {
        super(application, SystemInfoResult.class);
    }

    public void querySystemInfo() {
        isLoading.setValue(true);
        error.setValue(null);

        getExecutor().execute(() -> {
            SystemInfoResult result = execute(new SystemInfoRequest());
            if (result != null && result.isSuccess() && result.getFields() != null) {
                logger.info("系统信息查询成功, 共 " + result.getFields().size() + " 个字段");
                fields.postValue(result.getFields());
            } else if (result != null) {
                postError(result.getError(), R.string.analysis_system_info_failed);
            }
        });
    }
}
