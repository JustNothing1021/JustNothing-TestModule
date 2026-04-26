package com.justnothing.testmodule.ui.analysis.alias;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.justnothing.methodsclient.UiClient;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.protocol.json.JsonProtocol;
import com.justnothing.testmodule.protocol.json.model.AliasInfo;
import com.justnothing.testmodule.protocol.json.request.AliasRequest;
import com.justnothing.testmodule.protocol.json.response.AliasResult;
import com.justnothing.testmodule.protocol.json.response.CommandResult;
import com.justnothing.testmodule.utils.logging.Logger;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AliasQueryViewModel extends AndroidViewModel {

    private static final String TAG = "AliasQueryVM";
    private final Logger logger = new Logger() {
        @Override
        public String getTag() {
            return TAG;
        }
    };

    private final UiClient client = UiClient.getInstance();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<AliasInfo>> aliases = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> message = new MutableLiveData<>();

    public LiveData<Boolean> isLoading() { return isLoading; }
    public LiveData<List<AliasInfo>> getAliases() { return aliases; }
    public LiveData<String> getError() { return error; }
    public LiveData<String> getMessage() { return message; }

    public AliasQueryViewModel(@NonNull Application application) {
        super(application);
    }

    public void loadAliases() {
        isLoading.setValue(true);
        error.setValue(null);
        message.setValue(null);

        executor.execute(() -> {
            try {
                logger.info("开始加载别名列表");
                AliasRequest request = new AliasRequest(AliasRequest.ACTION_LIST);
                String jsonResponse = client.executeCommandRequest(JsonProtocol.toJson(request));
                CommandResult parsedResult = JsonProtocol.parseResponse(jsonResponse);

                if (parsedResult instanceof AliasResult result) {
                    if (result.isSuccess() && result.getAliases() != null) {
                        logger.info("加载成功, 共 " + result.getAliases().size() + " 个别名");
                        aliases.postValue(result.getAliases());
                    } else {
                        String errorMsg = result.getError() != null
                                ? result.getError().getMessage()
                                : getApplication().getString(R.string.analysis_alias_load_failed);
                        logger.error("加载失败: " + errorMsg);
                        error.postValue(errorMsg);
                    }
                } else {
                    String errorMsg = getApplication().getString(R.string.analysis_response_type_error,
                            "AliasResult", parsedResult.getClass().getName());
                    logger.error(errorMsg);
                    error.postValue(errorMsg);
                }
            } catch (Exception e) {
                logger.error("加载异常", e);
                error.postValue(getApplication().getString(R.string.analysis_alias_load_failed) + ": " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    public void addAlias(String name, String command) {
        isLoading.setValue(true);
        error.setValue(null);
        message.setValue(null);

        executor.execute(() -> {
            try {
                logger.info("开始添加别名: " + name + " -> " + command);
                AliasRequest request = new AliasRequest(AliasRequest.ACTION_ADD, name, command);
                String jsonResponse = client.executeCommandRequest(JsonProtocol.toJson(request));
                CommandResult parsedResult = JsonProtocol.parseResponse(jsonResponse);

                if (parsedResult instanceof AliasResult result) {
                    if (result.isSuccess()) {
                        logger.info("添加成功");
                        message.postValue(getApplication().getString(R.string.analysis_alias_add_success));
                        // 重新加载列表
                        loadAliases();
                    } else {
                        String errorMsg = result.getError() != null
                                ? result.getError().getMessage()
                                : getApplication().getString(R.string.analysis_alias_add_failed);
                        logger.error("添加失败: " + errorMsg);
                        error.postValue(errorMsg);
                    }
                } else {
                    String errorMsg = getApplication().getString(R.string.analysis_response_type_error,
                            "AliasResult", parsedResult.getClass().getName());
                    logger.error(errorMsg);
                    error.postValue(errorMsg);
                }
            } catch (Exception e) {
                logger.error("添加异常", e);
                error.postValue(getApplication().getString(R.string.analysis_alias_add_failed) + ": " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    public void removeAlias(String name) {
        isLoading.setValue(true);
        error.setValue(null);
        message.setValue(null);

        executor.execute(() -> {
            try {
                logger.info("开始删除别名: " + name);
                AliasRequest request = new AliasRequest(AliasRequest.ACTION_REMOVE, name, null);
                String jsonResponse = client.executeCommandRequest(JsonProtocol.toJson(request));
                CommandResult parsedResult = JsonProtocol.parseResponse(jsonResponse);

                if (parsedResult instanceof AliasResult result) {
                    if (result.isSuccess()) {
                        logger.info("删除成功");
                        message.postValue(getApplication().getString(R.string.analysis_alias_remove_success));
                        // 重新加载列表
                        loadAliases();
                    } else {
                        String errorMsg = result.getError() != null
                                ? result.getError().getMessage()
                                : getApplication().getString(R.string.analysis_alias_remove_failed);
                        logger.error("删除失败: " + errorMsg);
                        error.postValue(errorMsg);
                    }
                } else {
                    String errorMsg = getApplication().getString(R.string.analysis_response_type_error,
                            "AliasResult", parsedResult.getClass().getName());
                    logger.error(errorMsg);
                    error.postValue(errorMsg);
                }
            } catch (Exception e) {
                logger.error("删除异常", e);
                error.postValue(getApplication().getString(R.string.analysis_alias_remove_failed) + ": " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    public void clearAliases() {
        isLoading.setValue(true);
        error.setValue(null);
        message.setValue(null);

        executor.execute(() -> {
            try {
                logger.info("开始清空别名");
                AliasRequest request = new AliasRequest(AliasRequest.ACTION_CLEAR);
                String jsonResponse = client.executeCommandRequest(JsonProtocol.toJson(request));
                CommandResult parsedResult = JsonProtocol.parseResponse(jsonResponse);

                if (parsedResult instanceof AliasResult result) {
                    if (result.isSuccess()) {
                        logger.info("清空成功");
                        message.postValue(getApplication().getString(R.string.analysis_alias_clear_success));
                        // 重新加载列表
                        loadAliases();
                    } else {
                        String errorMsg = result.getError() != null
                                ? result.getError().getMessage()
                                : getApplication().getString(R.string.analysis_alias_clear_failed);
                        logger.error("清空失败: " + errorMsg);
                        error.postValue(errorMsg);
                    }
                } else {
                    String errorMsg = getApplication().getString(R.string.analysis_response_type_error,
                            "AliasResult", parsedResult.getClass().getName());
                    logger.error(errorMsg);
                    error.postValue(errorMsg);
                }
            } catch (Exception e) {
                logger.error("清空异常", e);
                error.postValue(getApplication().getString(R.string.analysis_alias_clear_failed) + ": " + e.getMessage());
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