package com.justnothing.testmodule.ui.viewmodel.analysis;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.justnothing.testmodule.ui.viewmodel.BaseViewModel;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.functions.exportcontext.ContextFieldInfo;
import com.justnothing.testmodule.command.functions.exportcontext.ExportContextRequest;
import com.justnothing.testmodule.command.functions.exportcontext.ExportContextResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ExportContextQueryViewModel extends BaseViewModel<ExportContextRequest, ExportContextResult> {

    private final MutableLiveData<List<ContextFieldInfo>> fields = new MutableLiveData<>();
    private final MutableLiveData<List<String>> categories = new MutableLiveData<>();

    public LiveData<List<ContextFieldInfo>> getFields() { return fields; }
    public LiveData<List<String>> getCategories() { return categories; }

    public ExportContextQueryViewModel(@NonNull Application application) {
        super(application, ExportContextResult.class);
    }

    public void queryContext() {
        isLoading.setValue(true);
        error.setValue(null);

        getExecutor().execute(() -> {
            ExportContextResult result = execute(new ExportContextRequest());
            if (result != null && result.isSuccess() && result.getFields() != null) {
                List<ContextFieldInfo> data = result.getFields();
                Collections.sort(data, Comparator.comparing(ContextFieldInfo::getCategory)
                        .thenComparing(ContextFieldInfo::getLabel));

                List<String> cats = new ArrayList<>();
                for (ContextFieldInfo field : data) {
                    if (!cats.contains(field.getCategory())) cats.add(field.getCategory());
                }

                logger.info("查询成功, 共 " + data.size() + " 个字段, " + cats.size() + " 个分类");
                fields.postValue(data);
                categories.postValue(cats);
            } else if (result != null) {
                postError(result.getError(), R.string.analysis_export_context_failed);
            }
        });
    }
}
