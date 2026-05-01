package com.justnothing.testmodule.ui.activity.analysis.classanalysis;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.functions.classcmd.response.ClassHierarchyResult;
import com.justnothing.testmodule.ui.viewmodel.analysis.ClassGraphViewModel;
import com.justnothing.testmodule.utils.reflect.DescriptorColorizer;

import java.util.ArrayList;
import java.util.List;

public class ClassGraphActivity extends AppCompatActivity {
    
    public static final String EXTRA_CLASS_NAME = "className";
    
    private String className;
    private RecyclerView rvHierarchy;
    private ProgressBar progressBar;
    private TextView tvError;
    private android.widget.HorizontalScrollView horizontalScrollView;
    
    private ClassGraphViewModel viewModel;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_graph);
        
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
        if (horizontalScrollView != null) {
            horizontalScrollView.post(() -> horizontalScrollView.scrollTo(0, 0));
        }
        if (rvHierarchy != null) {
            rvHierarchy.post(() -> rvHierarchy.scrollToPosition(0));
        }
    }
    
    private void initViews() {
        rvHierarchy = findViewById(R.id.rv_hierarchy);
        progressBar = findViewById(R.id.progress_bar);
        tvError = findViewById(R.id.tv_error);
        horizontalScrollView = findViewById(android.R.id.content).findViewById(R.id.horizontal_scroll);
        
        rvHierarchy.setLayoutManager(new LinearLayoutManager(this));
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.analysis_class_hierarchy);
        }
    }
    
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(ClassGraphViewModel.class);
        
        viewModel.isLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            rvHierarchy.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        });
        
        viewModel.getHierarchyResult().observe(this, result -> {
            if (result != null && result.getClassChain() != null) {
                tvError.setVisibility(View.GONE);
                rvHierarchy.setVisibility(View.VISIBLE);
                List<HierarchyItem> items = buildHierarchyItems(result);
                rvHierarchy.setAdapter(new HierarchyAdapter(items));
            }
        });
        
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                tvError.setVisibility(View.VISIBLE);
                tvError.setText(error);
                rvHierarchy.setVisibility(View.GONE);
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
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hierarchy, parent, false);
            return new ViewHolder(view);
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
            private final MaterialCardView cardClass;
            private final TextView tvClassName;
            private final TextView tvClassType;
            private final TextView tvRelation;
            private final View viewConnectorBottom;
            private final View layoutInterfaces;
            private final View viewInterfaceLine;
            private final MaterialCardView cardInterface;
            private final TextView tvInterfaceName;
            
            ViewHolder(View itemView) {
                super(itemView);
                cardClass = itemView.findViewById(R.id.card_class);
                tvClassName = itemView.findViewById(R.id.tv_class_name);
                tvClassType = itemView.findViewById(R.id.tv_class_type);
                tvRelation = itemView.findViewById(R.id.tv_relation);
                viewConnectorBottom = itemView.findViewById(R.id.view_connector_bottom);
                layoutInterfaces = itemView.findViewById(R.id.layout_interfaces);
                viewInterfaceLine = itemView.findViewById(R.id.view_interface_line);
                cardInterface = itemView.findViewById(R.id.card_interface);
                tvInterfaceName = itemView.findViewById(R.id.tv_interface_name);
            }
            
            void bind(HierarchyItem item, boolean isLast) {
                tvClassName.setText(item.displayName);
                tvClassType.setText(item.classType);
                tvRelation.setText(item.relation);
                
                int cardColor;
                
                switch (item.type) {
                    case CURRENT_CLASS:
                        cardColor = getColor(R.color.blue);
                        cardClass.setStrokeColor(getColor(R.color.blue));
                        cardClass.setStrokeWidth(2);
                        cardClass.setCardBackgroundColor(getColor(android.R.color.white));
                        tvClassType.setTextColor(getColor(R.color.blue));
                        tvRelation.setTextColor(getColor(android.R.color.darker_gray));
                        break;
                    case PARENT_CLASS:
                        cardColor = getColor(R.color.green);
                        cardClass.setStrokeWidth(0);
                        cardClass.setCardBackgroundColor(getColor(android.R.color.white));
                        tvClassType.setTextColor(getColor(R.color.green));
                        tvRelation.setTextColor(getColor(android.R.color.darker_gray));
                        break;
                    case INTERFACE:
                    default:
                        cardColor = getColor(R.color.orange);
                        cardClass.setStrokeWidth(0);
                        cardClass.setCardBackgroundColor(getColor(android.R.color.white));
                        tvClassType.setTextColor(getColor(R.color.orange));
                        tvRelation.setTextColor(getColor(android.R.color.darker_gray));
                        break;
                }
                
                tvClassName.setTextColor(cardColor);
                
                viewConnectorBottom.setVisibility(item.showConnectorBelow ? View.VISIBLE : View.GONE);
                viewConnectorBottom.setBackgroundColor(cardColor);
                
                cardClass.setOnClickListener(v -> openClassDetail(item.fullClassName));
                
                if (item.interfaces != null && !item.interfaces.isEmpty()) {
                    layoutInterfaces.setVisibility(View.VISIBLE);
                    viewInterfaceLine.setBackgroundColor(getColor(R.color.orange));
                    
                    if (item.interfaces.size() == 1) {
                        tvInterfaceName.setText(item.interfaces.get(0).displayName);
                        cardInterface.setOnClickListener(v -> 
                            openClassDetail(item.interfaces.get(0).fullName));
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < item.interfaces.size(); i++) {
                            if (i > 0) sb.append("\n");
                            sb.append(item.interfaces.get(i).displayName);
                        }
                        tvInterfaceName.setText(sb.toString());
                        
                        final List<InterfaceInfo> interfaceList = item.interfaces;
                        cardInterface.setOnClickListener(v -> showInterfaceSelector(interfaceList));
                    }
                } else {
                    layoutInterfaces.setVisibility(View.GONE);
                }
            }
        }
    }
    
    private void showInterfaceSelector(List<InterfaceInfo> interfaces) {
        android.view.View dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_select_overload, null);
        
        TextView tvTitle = dialogView.findViewById(R.id.tv_title);
        tvTitle.setText(R.string.analysis_implemented_interface);
        
        androidx.core.widget.NestedScrollView scrollView = dialogView.findViewById(R.id.nested_scroll);
        
        RecyclerView rvInterfaces = dialogView.findViewById(R.id.rv_overloads);
        rvInterfaces.setLayoutManager(new LinearLayoutManager(this));
        
        InterfaceSelectAdapter adapter = new InterfaceSelectAdapter(interfaces);
        rvInterfaces.setAdapter(adapter);
        
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create();
        
        adapter.setOnItemClickListener((position, info) -> {
            dialog.dismiss();
            openClassDetail(info.fullName);
        });
        
        dialog.show();
        
        if (scrollView != null) {
            scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
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
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_interface_select, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            InterfaceInfo info = interfaces.get(position);
            holder.tvIndex.setText(String.format(getString(R.string.index_format), position + 1));
            holder.tvName.setText(info.displayName);
            
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
            TextView tvIndex;
            TextView tvName;
            
            ViewHolder(View itemView) {
                super(itemView);
                tvIndex = itemView.findViewById(R.id.tv_index);
                tvName = itemView.findViewById(R.id.tv_name);
            }
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
