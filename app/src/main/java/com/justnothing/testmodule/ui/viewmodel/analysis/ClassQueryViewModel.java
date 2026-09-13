package com.justnothing.testmodule.ui.viewmodel.analysis;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.justnothing.methodsclient.UiClient;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.framework.utils.GsonFactory;
import com.justnothing.testmodule.command.functions.classcmd.request.ClassInfoRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.functions.classcmd.response.ClassInfoResult;
import com.justnothing.testmodule.command.functions.classcmd.model.ClassInfo;
import com.justnothing.testmodule.utils.logging.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 类查询ViewModel。
 */
public class ClassQueryViewModel extends AndroidViewModel {
    
    private static final String TAG = "ClassQueryViewModel";
    private final Logger logger = new Logger() {
        @Override
        public String getTag() {
            return TAG;
        }
    };
    
    private final UiClient client = UiClient.getInstance();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<ClassInfo> classInfo = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    
    public LiveData<Boolean> isLoading() { return isLoading; }
    public LiveData<ClassInfo> getClassInfo() { return classInfo; }
    public LiveData<String> getError() { return error; }
    
    public ClassQueryViewModel(@NonNull Application application) {
        super(application);
    }
    
    public void queryClassInfo(String className) {
        if (className == null || className.trim().isEmpty()) {
            error.setValue(getApplication().getString(R.string.analysis_enter_class_name_hint));
            return;
        }
        
        logger.info("开始查询类信息: " + className);
        isLoading.setValue(true);
        error.setValue(null);
        
        executor.execute(() -> {
            try {
                ClassInfoRequest request = new ClassInfoRequest(className);
                request.setShowInterfaces(true);
                request.setShowConstructors(true);
                request.setShowSuper(true);
                request.setShowModifiers(true);
                request.setShowMethods(true);
                request.setShowFields(true);
                
                logger.debug("创建请求: " + GsonFactory.getInstance().toJson(request));

                String jsonResponse = client.executeCommandRequest(GsonFactory.getInstance().toJson(request));
                logger.debug("收到响应: " + jsonResponse);

                // 先解析为 CommandResult 检查成功状态
                CommandResult parsedResult = GsonFactory.getInstance().fromJson(jsonResponse, CommandResult.class);
                logger.debug("解析结果, 成功: " + parsedResult.isSuccess());
                
                if (parsedResult.isSuccess()) {
                    // 成功时用 ClassInfoResult 类型重新解析，获取完整数据
                    ClassInfoResult result = GsonFactory.getInstance().fromJson(jsonResponse, ClassInfoResult.class);
                    if (result.getClassInfo() != null) {
                        logger.info("查询成功: " + result.getClassInfo().getName());
                        classInfo.postValue(result.getClassInfo());
                    } else {
                        String errorMsg = getApplication().getString(R.string.analysis_class_query_failed_format);
                        logger.error("查询失败: 响应中没有类信息");
                        error.postValue(errorMsg);
                    }
                } else {
                    String errorMsg = parsedResult.getMessage() != null
                        ? parsedResult.getMessage()
                        : getApplication().getString(R.string.analysis_class_query_failed_format);
                    logger.error("查询失败: " + errorMsg);
                    error.postValue(errorMsg);
                }
            } catch (Exception e) {
                logger.error("查询异常", e);
                error.postValue(getApplication().getString(R.string.analysis_class_query_failed_format) + ": " + e.getMessage());
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
