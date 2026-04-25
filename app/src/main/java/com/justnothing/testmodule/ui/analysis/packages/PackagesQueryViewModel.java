package com.justnothing.testmodule.ui.analysis.packages;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.justnothing.methodsclient.UiClient;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.protocol.json.JsonProtocol;
import com.justnothing.testmodule.protocol.json.request.PackagesRequest;
import com.justnothing.testmodule.protocol.json.response.CommandResult;
import com.justnothing.testmodule.protocol.json.response.PackagesResult;
import com.justnothing.testmodule.utils.logging.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PackagesQueryViewModel extends AndroidViewModel {

    private static final String TAG = "PackagesQueryVM";
    private final Logger logger = new Logger() {
        @Override
        public String getTag() {
            return TAG;
        }
    };

    private final UiClient client = UiClient.getInstance();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<java.util.List<String>> packages = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public LiveData<Boolean> isLoading() {
        return isLoading;
    }

    public LiveData<java.util.List<String>> getPackages() {
        return packages;
    }

    public LiveData<String> getError() {
        return error;
    }

    public PackagesQueryViewModel(@NonNull Application application) {
        super(application);
    }

    public void queryPackages() {
        isLoading.setValue(true);
        error.setValue(null);

        executor.execute(() -> {
            try {
                logger.info("开始查询包列表");
                PackagesRequest request = new PackagesRequest();
                String jsonResponse = client.executeCommandRequest(JsonProtocol.toJson(request));
                CommandResult parsedResult = JsonProtocol.parseResponse(jsonResponse);

                if (parsedResult instanceof PackagesResult result) {
                    if (result.isSuccess() && result.getPackages() != null) {
                        logger.info("查询成功, 共 " + result.getPackages().size() + " 个包");
                        packages.postValue(result.getPackages());
                    } else {
                        String errorMsg = result.getError() != null
                            ? result.getError().getMessage()
                            : getApplication().getString(R.string.packages_query_failed);
                        logger.error("查询失败: " + errorMsg);
                        error.postValue(errorMsg);
                    }
                } else {
                    String errorMsg = getApplication().getString(R.string.packages_response_type_error,
                        "PackagesResult", parsedResult.getClass().getName());
                    logger.error(errorMsg);
                    error.postValue(errorMsg);
                }
            } catch (Exception e) {
                logger.error("查询异常", e);
                error.postValue(getApplication().getString(R.string.packages_query_failed) + ": " + e.getMessage());
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
