package com.justnothing.testmodule.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.justnothing.methodsclient.UiClient;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.protocol.json.JsonProtocol;
import com.justnothing.testmodule.protocol.json.request.ClassInfoRequest;
import com.justnothing.testmodule.protocol.json.response.ClassInfoResult;
import com.justnothing.testmodule.protocol.json.response.CommandResult;
import com.justnothing.testmodule.protocol.json.model.ClassInfo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 类浏览器ViewModel。
 * 
 * <p>展示如何使用UiClient进行类信息查询。</p>
 */
public class ClassBrowserViewModel extends AndroidViewModel {
    
    private final UiClient client = UiClient.getInstance();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<ClassInfo> classInfo = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> serverStatus = new MutableLiveData<>();
    
    public LiveData<Boolean> isLoading() { return isLoading; }
    public LiveData<ClassInfo> getClassInfo() { return classInfo; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getServerStatus() { return serverStatus; }
    
    public ClassBrowserViewModel(@NonNull Application application) {
        super(application);
    }
    
    /**
     * 检查服务端状态。
     */
    public void checkServerStatus() {
        executor.execute(() -> {
            boolean available = client.isServerAvailable();
            serverStatus.postValue(available);
        });
    }
    
    /**
     * 查询类信息。
     * 
     * @param className 类名
     */
    public void queryClassInfo(String className) {
        if (className == null || className.trim().isEmpty()) {
            error.setValue(getApplication().getString(R.string.enter_class_name_hint));
            return;
        }
        
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
                
                String jsonResponse = client.executeCommandRequest(JsonProtocol.toJson(request));
                
                CommandResult parsedResult = JsonProtocol.parseResponse(jsonResponse);
                
                if (parsedResult instanceof ClassInfoResult result) {
                    if (result.isSuccess() && result.getClassInfo() != null) {
                        classInfo.postValue(result.getClassInfo());
                    } else {
                        String errorMsg = result.getError() != null 
                            ? result.getError().getMessage() 
                            : getApplication().getString(R.string.query_failed);
                        error.postValue(errorMsg);
                    }
                } else {
                    error.postValue(getApplication().getString(R.string.response_type_error,
                        "ClassInfoResult", parsedResult.getClass().getSimpleName()));
                }
            } catch (Exception e) {
                error.postValue(getApplication().getString(R.string.query_failed) + ": " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }
    
    /**
     * 执行方法调用。
     * 
     * @param className 类名
     * @param methodName 方法名
     * @param args 参数
     */
    public void invokeMethod(String className, String methodName, String... args) {
        isLoading.setValue(true);
        
        error.postValue(getApplication().getString(R.string.method_invoke_not_implemented));
        isLoading.postValue(false);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
