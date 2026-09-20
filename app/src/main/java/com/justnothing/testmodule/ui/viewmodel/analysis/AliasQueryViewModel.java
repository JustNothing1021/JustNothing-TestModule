package com.justnothing.testmodule.ui.viewmodel.analysis;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.justnothing.testmodule.ui.viewmodel.BaseViewModel;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.alias.model.AliasInfo;
import com.justnothing.testmodule.command.functions.alias.request.AliasAddRequest;
import com.justnothing.testmodule.command.functions.alias.request.AliasClearRequest;
import com.justnothing.testmodule.command.functions.alias.request.AliasListRequest;
import com.justnothing.testmodule.command.functions.alias.request.AliasRemoveRequest;
import com.justnothing.testmodule.command.functions.alias.response.AliasResult;
import com.justnothing.testmodule.utils.logging.Logger;

import java.util.List;

/**
 * 别名管理页。
 *
 * <p>注意：服务端已把 alias 拆成 alias:list / alias:add / alias:remove / alias:clear
 * 四条路由与四个 Request，这里必须用对应 Request 发起——旧的聚合 AliasRequest
 * （key=Alias，靠 action 字段区分）已不再注册，发出去只会得到"未注册的命令类型"。</p>
 */
public class AliasQueryViewModel extends BaseViewModel<AliasListRequest, AliasResult> {

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
            AliasResult result = execute(new AliasListRequest());
            if (result != null && result.isSuccess() && result.getAliases() != null) {
                logger.info("加载成功, 共 " + result.getAliases().size() + " 个别名");
                aliases.postValue(result.getAliases());
            } else if (result != null && !result.isSuccess()) {
                postError(result.getError(), R.string.analysis_alias_load_failed);
            }
        });
    }

    public void addAlias(String name, String command) {
        AliasAddRequest request = new AliasAddRequest();
        request.setName(name);
        request.setCommand(command);
        runAction(
            request,
            R.string.analysis_alias_add_success,
            R.string.analysis_alias_add_failed,
            this::loadAliases
        );
    }

    public void removeAlias(String name) {
        AliasRemoveRequest request = new AliasRemoveRequest();
        request.setName(name);
        runAction(
            request,
            R.string.analysis_alias_remove_success,
            R.string.analysis_alias_remove_failed,
            this::loadAliases
        );
    }

    public void clearAliases() {
        runAction(
            new AliasClearRequest(),
            R.string.analysis_alias_clear_success,
            R.string.analysis_alias_clear_failed,
            this::loadAliases
        );
    }

    private void runAction(CommandRequest<?> request, int successMsgRes, int errorMsgRes, Runnable onSuccess) {
        isLoading.setValue(true);
        error.setValue(null);

        getExecutor().execute(() -> {
            AliasResult result = executeAny(request, AliasResult.class);
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
