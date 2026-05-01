package com.justnothing.testmodule.ui.viewmodel.analysis;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.justnothing.testmodule.ui.viewmodel.BaseViewModel;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.functions.packages.PackagesRequest;
import com.justnothing.testmodule.command.functions.packages.PackagesResult;

import java.util.List;

public class PackagesQueryViewModel extends BaseViewModel<PackagesRequest, PackagesResult> {

    private final MutableLiveData<List<String>> packages = new MutableLiveData<>();

    public LiveData<List<String>> getPackages() { return packages; }

    public PackagesQueryViewModel(@NonNull Application application) {
        super(application, PackagesResult.class);
    }

    public void queryPackages() {
        isLoading.setValue(true);
        error.setValue(null);

        getExecutor().execute(() -> {
            PackagesResult result = execute(new PackagesRequest());
            if (result != null && result.isSuccess() && result.getPackages() != null) {
                logger.info("查询成功, 共 " + result.getPackages().size() + " 个包");
                packages.postValue(result.getPackages());
            } else if (result != null) {
                postError(result.getError(), R.string.packages_query_failed);
            }
        });
    }
}
