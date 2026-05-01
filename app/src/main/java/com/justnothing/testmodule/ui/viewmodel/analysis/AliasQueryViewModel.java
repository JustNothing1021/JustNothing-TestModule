package com.justnothing.testmodule.ui.viewmodel.analysis;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.justnothing.testmodule.ui.viewmodel.BaseViewModel;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.functions.alias.model.AliasInfo;
import com.justnothing.testmodule.command.functions.alias.request.AliasRequest;
import com.justnothing.testmodule.command.functions.alias.response.AliasResult;
import com.justnothing.testmodule.utils.logging.Logger;

import java.util.List;

public class AliasQueryViewModel extends BaseViewModel<AliasRequest, AliasResult> {

    private static final String TAG = "AliasQueryVM";
    private final Logger logger = Logger.getLoggerForName(TAG);

    private final MutableLiveData<List<AliasInfo>> aliases = new MutableLiveData<>();

    public LiveData<List<AliasInfo>> getAliases() { return aliases; }

    public AliasQueryViewModel(@NonNull Application application) {
        super(application, AliasResult.class);
    }

    public void loadAliases() {
        isLoading.setValue(true);
        error.setValue(null);

        getExecutor().execute(() -> {
            AliasResult result = execute(new AliasRequest(AliasRequest.ACTION_LIST));
            if (result != null && result.isSuccess() && result.getAliases() != null) {
                logger.info("加载成功, 共 " + result.getAliases().size() + " 个别名");
                aliases.postValue(result.getAliases());
            } else if (result != null && !result.isSuccess()) {
                postError(result.getError(), R.string.analysis_alias_load_failed);
            }
        });
    }

    public void addAlias(String name, String command) {
        runAction(
            new AliasRequest(AliasRequest.ACTION_ADD, name, command),
            R.string.analysis_alias_add_success,
            R.string.analysis_alias_add_failed,
            this::loadAliases
        );
    }

    public void removeAlias(String name) {
        runAction(
            new AliasRequest(AliasRequest.ACTION_REMOVE, name, null),
            R.string.analysis_alias_remove_success,
            R.string.analysis_alias_remove_failed,
            this::loadAliases
        );
    }

    public void clearAliases() {
        runAction(
            new AliasRequest(AliasRequest.ACTION_CLEAR),
            R.string.analysis_alias_clear_success,
            R.string.analysis_alias_clear_failed,
            this::loadAliases
        );
    }

    private void runAction(AliasRequest request, int successMsgRes, int errorMsgRes, Runnable onSuccess) {
        isLoading.setValue(true);
        error.setValue(null);

        getExecutor().execute(() -> {
            AliasResult result = execute(request);
            if (result != null && result.isSuccess()) {
                logger.info("操作成功");
                getMessage().setValue(getApplication().getString(successMsgRes));
                onSuccess.run();
            } else if (result != null) {
                postError(result.getError(), errorMsgRes);
            }
        });
    }


}
