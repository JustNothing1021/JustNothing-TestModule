package com.justnothing.testmodule.ui.analysis.systeminfo;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.justnothing.methodsclient.UiClient;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.protocol.json.JsonProtocol;
import com.justnothing.testmodule.protocol.json.model.SystemFieldInfo;
import com.justnothing.testmodule.protocol.json.request.SystemInfoRequest;
import com.justnothing.testmodule.protocol.json.response.CommandResult;
import com.justnothing.testmodule.protocol.json.response.SystemInfoResult;
import com.justnothing.testmodule.utils.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SystemInfoQueryViewModel extends AndroidViewModel {

    private static final String TAG = "SystemInfoQueryVM";
    private final Logger logger = new Logger() {
        @Override
        public String getTag() {
            return TAG;
        }
    };

    private final UiClient client = UiClient.getInstance();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<SystemFieldInfo>> fields = new MutableLiveData<>();
    private final MutableLiveData<List<String>> categories = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public LiveData<Boolean> isLoading() { return isLoading; }
    public LiveData<List<SystemFieldInfo>> getFields() { return fields; }
    public LiveData<List<String>> getCategories() { return categories; }
    public LiveData<String> getError() { return error; }

    public SystemInfoQueryViewModel(@NonNull Application application) {
        super(application);
    }

    public void querySystemInfo() {
        isLoading.setValue(true);
        error.setValue(null);

        executor.execute(() -> {
            try {
                logger.info("开始查询系统信息");
                SystemInfoRequest request = new SystemInfoRequest();
                String jsonResponse = client.executeCommandRequest(JsonProtocol.toJson(request));
                CommandResult parsedResult = JsonProtocol.parseResponse(jsonResponse);

                if (parsedResult instanceof SystemInfoResult result) {
                    if (result.isSuccess() && result.getFields() != null) {
                        List<SystemFieldInfo> data = result.getFields();
                        Collections.sort(data, Comparator.comparing(SystemFieldInfo::getCategory)
                                .thenComparing(SystemFieldInfo::getLabel));

                        List<String> cats = new ArrayList<>();
                        for (SystemFieldInfo field : data) {
                            if (!cats.contains(field.getCategory())) {
                                cats.add(field.getCategory());
                            }
                        }

                        logger.info("查询成功, 共 " + data.size() + " 个字段, " + cats.size() + " 个分类");
                        fields.postValue(data);
                        categories.postValue(cats);
                    } else {
                        String errorMsg = result.getError() != null
                                ? result.getError().getMessage()
                                : getApplication().getString(R.string.analysis_system_info_failed);
                        logger.error("查询失败: " + errorMsg);
                        error.postValue(errorMsg);
                    }
                } else {
                    String errorMsg = getApplication().getString(R.string.analysis_response_type_error,
                            "SystemInfoResult", parsedResult.getClass().getName());
                    logger.error(errorMsg);
                    error.postValue(errorMsg);
                }
            } catch (Exception e) {
                logger.error("查询异常", e);
                error.postValue(getApplication().getString(R.string.analysis_system_info_failed) + ": " + e.getMessage());
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
