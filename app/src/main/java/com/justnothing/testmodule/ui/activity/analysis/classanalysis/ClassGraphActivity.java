package com.justnothing.testmodule.ui.activity.analysis.classanalysis;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.functions.classcmd.response.ClassHierarchyResult;
import com.justnothing.testmodule.databinding.ActivityClassGraphBinding;
import com.justnothing.testmodule.databinding.DialogSelectOverloadBinding;
import com.justnothing.testmodule.databinding.ItemHierarchyBinding;
import com.justnothing.testmodule.databinding.ItemInterfaceSelectBinding;
import com.justnothing.testmodule.ui.activity.BaseActivity;
import com.justnothing.testmodule.ui.viewmodel.analysis.ClassGraphViewModel;
import com.justnothing.testmodule.utils.format.DescriptorColorizer;

import java.util.ArrayList;
import java.util.List;

public class ClassGraphActivity extends BaseActivity {
    
    public static final String EXTRA_CLASS_NAME = "className";
    
    private String className;
    private ActivityClassGraphBinding binding;
    
    private ClassGraphViewModel viewModel;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityClassGraphBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        className = getIntent().getStringExtra(EXTRA_CLASS_NAME);
        
        initViews();
        initViewModel();
        
        if (className != null && !className.isEmpty()) {
            viewModel.queryClassHierarchy(className);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (binding.horizontalScroll != null) {
            binding.horizontalScroll.post(() -> binding.horizontalScroll.scrollTo(0, 0));
        }
        if (binding.rvHierarchy != null) {
            binding.rvHierarchy.post(() -> binding.rvHierarchy.scrollToPosition(0));
        }
    }
    
    private void initViews() {
        binding.rvHierarchy.setLayoutManager(new LinearLayoutManager(this));
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.analysis_class_hierarchy);
        }
    }
    
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(ClassGraphViewModel.class);
        
        viewModel.isLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.rvHierarchy.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        });
        
        viewModel.getHierarchyResult().observe(this, result -> {
            if (result != null && result.getClassChain() != null) {
                binding.tvError.setVisibility(View.GONE);
                binding.rvHierarchy.setVisibility(View.VISIBLE);
                List<HierarchyItem> items = buildHierarchyItems(result);
                binding.rvHierarchy.setAdapter(new HierarchyAdapter(items));
            }
        });
        
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                binding.tvError.setVisibility(View.VISIBLE);
                binding.tvError.setText(error);
                binding.rvHierarchy.setVisibility(View.GONE);
            }
        });
    }
    
    private List<HierarchyItem> buildHierarchyItems(ClassHierarchyResult result) {
        List<HierarchyItem> items = new ArrayList<>();
        
        List<ClassHierarchyResult.HierarchyClassInfo> classChain = result.getClassChain();
        List<List<String>> interfacesPerLevel = result.getInterfacesPerLevel();
        
        if (classChain == null || classChain.isEmpty()) {
            return items;
        }
        
        int totalLevels = classChain.size();
        
        for (int i = 0; i < totalLevels; i++) {
            ClassHierarchyResult.HierarchyClassInfo currentClass = classChain.get(i);
            List<String> interfaces = (interfacesPerLevel != null && i < interfacesPerLevel.size()) 
                ? interfacesPerLevel.get(i) : new ArrayList<>();
            
            ItemType type;
            String relation;
            
            if (i == totalLevels - 1) {
                type = ItemType.CURRENT_CLASS;
                relation = getString(R.string.analysis_current_class);
            } else if (currentClass.isInterface()) {
                type = ItemType.INTERFACE;
                relation = getString(R.string.analysis_implemented_interface);
            } else {
                type = ItemType.PARENT_CLASS;
                relation = getString(R.string.analysis_super_class);
            }
            
            String classType = getClassTypeString(currentClass);
            
            boolean hasNextClass = (i < totalLevels - 1);
            
            List<InterfaceInfo> interfaceInfos = new ArrayList<>();
            for (String _interface : interfaces) {
                interfaceInfos.add(new InterfaceInfo(
                    DescriptorColorizer.formatTypeName(_interface),
                    _interface
                ));
            }
            
            items.add(new HierarchyItem(
                DescriptorColorizer.formatTypeName(currentClass.getName()),
                currentClass.getName(),
                relation,
                classType,
                type, 
                hasNextClass,
                interfaceInfos
            ));
        }
        
        return items;
    }
    
    private String getClassTypeString(ClassHierarchyResult.HierarchyClassInfo info) {
        if (info.isAnnotation()) {
            return getString(R.string.analysis_class_type_annotation);
        } else if (info.isInterface()) {
            return getString(R.string.analysis_class_type_interface);
        } else if (info.isEnum()) {
            return getString(R.string.analysis_class_type_enum);
        } else {
            if (info.isAbstract()) {
                return getString(R.string.analysis_class_type_abstract);
            } else if (info.isFinal()) {
                return getString(R.string.analysis_class_type_final);
            } else {
                return getString(R.string.analysis_class_type_class);
            }
        }
    }
    
    private void openClassDetail(String fullClassName) {
        Intent intent = new Intent(this, ClassDetailActivity.class);
        intent.putExtra(ClassDetailActivity.EXTRA_CLASS_NAME, fullClassName);
        startActivity(intent);
    }
    
    static class InterfaceInfo {
        String displayName;
        String fullName;
        
        InterfaceInfo(String displayName, String fullName) {
            this.displayName = displayName;
            this.fullName = fullName;
        }
    }
    
    enum ItemType {
        CURRENT_CLASS,
        PARENT_CLASS,
        INTERFACE
    }
    
    static class HierarchyItem {
        String displayName;
        String fullClassName;
        String relation;
        String classType;
        ItemType type;
        boolean showConnectorBelow;
        List<InterfaceInfo> interfaces;
        
        HierarchyItem(String displayName, String fullClassName, String relation, String classType, ItemType type, boolean showConnectorBelow, List<InterfaceInfo> interfaces) {
            this.displayName = displayName;
            this.fullClassName = fullClassName;
            this.relation = relation;
            this.classType = classType;
            this.type = type;
            this.showConnectorBelow = showConnectorBelow;
            this.interfaces = interfaces;
        }
    }
    
    class HierarchyAdapter extends RecyclerView.Adapter<HierarchyAdapter.ViewHolder> {
        
        private final List<HierarchyItem> items;
        
        HierarchyAdapter(List<HierarchyItem> items) {
            this.items = items;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ItemHierarchyBinding itemBinding = ItemHierarchyBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(itemBinding);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            HierarchyItem item = items.get(position);
            holder.bind(item, position == items.size() - 1);
        }
        
        @Override
        public int getItemCount() {
            return items.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            private final ItemHierarchyBinding binding;
            
            ViewHolder(ItemHierarchyBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
            
            void bind(HierarchyItem item, boolean isLast) {
                binding.tvClassName.setText(item.displayName);
                binding.tvClassType.setText(item.classType);
                binding.tvRelation.setText(item.relation);
                
                int cardColor;
                
                switch (item.type) {
                    case CURRENT_CLASS:
                        cardColor = getColor(R.color.blue);
                        binding.cardClass.setStrokeColor(getColor(R.color.blue));
                        binding.cardClass.setStrokeWidth(2);
                        binding.cardClass.setCardBackgroundColor(MaterialColors.getColor(binding.cardClass, com.google.android.material.R.attr.colorSurface));
                        binding.tvClassType.setTextColor(getColor(R.color.blue));
                        binding.tvRelation.setTextColor(MaterialColors.getColor(binding.cardClass, com.google.android.material.R.attr.colorOnSurfaceVariant));
                        break;
                    case PARENT_CLASS:
                        cardColor = getColor(R.color.green);
                        binding.cardClass.setStrokeWidth(0);
                        binding.cardClass.setCardBackgroundColor(MaterialColors.getColor(binding.cardClass, com.google.android.material.R.attr.colorSurface));
                        binding.tvClassType.setTextColor(getColor(R.color.green));
                        binding.tvRelation.setTextColor(MaterialColors.getColor(binding.cardClass, com.google.android.material.R.attr.colorOnSurfaceVariant));
                        break;
                    case INTERFACE:
                    default:
                        cardColor = getColor(R.color.orange);
                        binding.cardClass.setStrokeWidth(0);
                        binding.cardClass.setCardBackgroundColor(MaterialColors.getColor(binding.cardClass, com.google.android.material.R.attr.colorSurface));
                        binding.tvClassType.setTextColor(getColor(R.color.orange));
                        binding.tvRelation.setTextColor(MaterialColors.getColor(binding.cardClass, com.google.android.material.R.attr.colorOnSurfaceVariant));
                        break;
                }
                
                binding.tvClassName.setTextColor(cardColor);
                
                binding.viewConnectorBottom.setVisibility(item.showConnectorBelow ? View.VISIBLE : View.GONE);
                binding.viewConnectorBottom.setBackgroundColor(cardColor);
                
                binding.cardClass.setOnClickListener(v -> openClassDetail(item.fullClassName));
                
                if (item.interfaces != null && !item.interfaces.isEmpty()) {
                    binding.layoutInterfaces.setVisibility(View.VISIBLE);
                    binding.viewInterfaceLine.setBackgroundColor(getColor(R.color.orange));
                    
                    if (item.interfaces.size() == 1) {
                        binding.tvInterfaceName.setText(item.interfaces.get(0).displayName);
                        binding.cardInterface.setOnClickListener(v -> 
                            openClassDetail(item.interfaces.get(0).fullName));
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < item.interfaces.size(); i++) {
                            if (i > 0) sb.append("\n");
                            sb.append(item.interfaces.get(i).displayName);
                        }
                        binding.tvInterfaceName.setText(sb.toString());
                        
                        final List<InterfaceInfo> interfaceList = item.interfaces;
                        binding.cardInterface.setOnClickListener(v -> showInterfaceSelector(interfaceList));
                    }
                } else {
                    binding.layoutInterfaces.setVisibility(View.GONE);
                }
            }
        }
    }
    
    private void showInterfaceSelector(List<InterfaceInfo> interfaces) {
        DialogSelectOverloadBinding dialogBinding = DialogSelectOverloadBinding.inflate(
            LayoutInflater.from(this));
        
        dialogBinding.tvTitle.setText(R.string.analysis_implemented_interface);
        
        NestedScrollView scrollView = dialogBinding.nestedScroll;
        
        dialogBinding.rvOverloads.setLayoutManager(new LinearLayoutManager(this));
        
        InterfaceSelectAdapter adapter = new InterfaceSelectAdapter(interfaces);
        dialogBinding.rvOverloads.setAdapter(adapter);
        
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogBinding.getRoot())
            .create();
        
        adapter.setOnItemClickListener((position, info) -> {
            dialog.dismiss();
            openClassDetail(info.fullName);
        });
        
        dialog.show();
        
        if (scrollView != null) {
            scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    scrollView.scrollTo(0, 0);
                }
            });
        }
    }
    
    class InterfaceSelectAdapter extends RecyclerView.Adapter<InterfaceSelectAdapter.ViewHolder> {
        
        private final List<InterfaceInfo> interfaces;
        private OnItemClickListener listener;
        
        interface OnItemClickListener {
            void onItemClick(int position, InterfaceInfo info);
        }
        
        void setOnItemClickListener(OnItemClickListener listener) {
            this.listener = listener;
        }
        
        InterfaceSelectAdapter(List<InterfaceInfo> interfaces) {
            this.interfaces = interfaces;
        }
        
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ItemInterfaceSelectBinding itemBinding = ItemInterfaceSelectBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(itemBinding);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            InterfaceInfo info = interfaces.get(position);
            holder.binding.tvIndex.setText(String.format(getString(R.string.index_format), position + 1));
            holder.binding.tvName.setText(info.displayName);
            
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(position, info);
                }
            });
        }
        
        @Override
        public int getItemCount() {
            return interfaces.size();
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            private final ItemInterfaceSelectBinding binding;
            
            ViewHolder(ItemInterfaceSelectBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
