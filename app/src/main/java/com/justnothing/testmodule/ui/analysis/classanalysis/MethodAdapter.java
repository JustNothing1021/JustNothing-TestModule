package com.justnothing.testmodule.ui.analysis.classanalysis;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.protocol.json.model.MethodInfo;
import com.justnothing.testmodule.utils.reflect.DescriptorColorizer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class MethodAdapter extends RecyclerView.Adapter<MethodAdapter.ViewHolder> {

    private final List<MethodGroup> groups = new ArrayList<>();
    private final String currentClassName;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(MethodInfo method);
    }

    static class MethodGroup {
        final String name;
        final List<MethodInfo> overloads;

        MethodGroup(String name) {
            this.name = name;
            this.overloads = new ArrayList<>();
        }
    }

    public MethodAdapter(List<MethodInfo> methods, String currentClassName) {
        this.currentClassName = currentClassName;
        if (methods != null) {
            LinkedHashMap<String, MethodGroup> groupMap = new LinkedHashMap<>();
            for (MethodInfo method : methods) {
                String name = method.getName();
                MethodGroup group = groupMap.get(name);
                if (group == null) {
                    group = new MethodGroup(name);
                    groupMap.put(name, group);
                }
                group.overloads.add(method);
            }
            groups.addAll(groupMap.values());
        }
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_method_info, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(groups.get(position));
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMethodName;
        private final TextView tvOverloads;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMethodName = itemView.findViewById(R.id.tv_method_name);
            tvOverloads = itemView.findViewById(R.id.tv_overloads);
        }

        public void bind(MethodGroup group) {
            int count = group.overloads.size();
            tvMethodName.setText(String.format(itemView.getContext().getString(R.string.method_name_with_overloads), group.name, count));

            int colorGreen = ContextCompat.getColor(itemView.getContext(), R.color.green);
            int colorBlue = ContextCompat.getColor(itemView.getContext(), R.color.blue);
            int colorGray = ContextCompat.getColor(itemView.getContext(), android.R.color.darker_gray);

            SpannableStringBuilder sb = new SpannableStringBuilder();

            sb.append(itemView.getContext().getString(R.string.return_type_label)).append("：");
            int returnTypeStart = sb.length();
            String returnTypeName = DescriptorColorizer.formatTypeName(
                group.overloads.get(0).getGenericReturnType());
            sb.append(returnTypeName);
            sb.setSpan(new ForegroundColorSpan(colorGreen), returnTypeStart, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            for (int i = 0; i < group.overloads.size(); i++) {
                MethodInfo method = group.overloads.get(i);
                sb.append("\n");

                String modifier = method.getModifiersString();
                if (modifier != null && !modifier.isEmpty()) {
                    sb.append("(");
                    int modifierStart = sb.length();
                    sb.append(modifier);
                    sb.setSpan(new ForegroundColorSpan(colorBlue), modifierStart, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.append(") ");
                }

                List<String> genericParamTypes = method.getGenericParameterTypes();
                if (genericParamTypes.isEmpty()) {
                    sb.append(itemView.getContext().getString(R.string.no_params));
                } else {
                    for (int j = 0; j < genericParamTypes.size(); j++) {
                        if (j > 0) sb.append(", ");
                        sb.append(DescriptorColorizer.formatTypeName(genericParamTypes.get(j)));
                    }
                }
                
                String declaringClass = method.getDeclaringClass();
                if (declaringClass != null && !declaringClass.equals(currentClassName)) {
                    sb.append("  ");
                    int inheritStart = sb.length();
                    if (method.isDeclaringClassIsInterface()) {
                        sb.append("[").append(itemView.getContext().getString(R.string.implements_interface, DescriptorColorizer.formatTypeName(declaringClass))).append("]");
                    } else {
                        sb.append("[").append(itemView.getContext().getString(R.string.inherited_from, DescriptorColorizer.formatTypeName(declaringClass))).append("]");
                    }
                    sb.setSpan(new ForegroundColorSpan(colorGray), inheritStart, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            tvOverloads.setText(sb);

            itemView.setOnClickListener(v -> {
                if (onItemClickListener != null && group.overloads.size() == 1) {
                    onItemClickListener.onItemClick(group.overloads.get(0));
                } else if (onItemClickListener != null && group.overloads.size() > 1) {
                    showOverloadSelector(group);
                }
            });
        }

        private void showOverloadSelector(MethodGroup group) {
            View dialogView = LayoutInflater.from(itemView.getContext())
                .inflate(R.layout.dialog_select_overload, null);
            
            androidx.core.widget.NestedScrollView scrollView = dialogView.findViewById(R.id.nested_scroll);
            
            RecyclerView rvOverloads = dialogView.findViewById(R.id.rv_overloads);
            rvOverloads.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            
            OverloadAdapter adapter = new OverloadAdapter(group.overloads, (position, method) -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(method);
                }
            });
            rvOverloads.setAdapter(adapter);
            
            androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(itemView.getContext())
                .setView(dialogView)
                .create();
            
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
    }

    static class OverloadAdapter extends RecyclerView.Adapter<OverloadAdapter.OverloadViewHolder> {

        private final List<MethodInfo> overloads;
        private final OnOverloadClickListener listener;

        interface OnOverloadClickListener {
            void onOverloadClick(int position, MethodInfo method);
        }

        OverloadAdapter(List<MethodInfo> overloads, OnOverloadClickListener listener) {
            this.overloads = overloads;
            this.listener = listener;
        }

        @NonNull
        @Override
        public OverloadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_overload, parent, false);
            return new OverloadViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull OverloadViewHolder holder, int position) {
            holder.bind(overloads.get(position), position);
        }

        @Override
        public int getItemCount() {
            return overloads.size();
        }

        class OverloadViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvIndex;
            private final TextView tvParams;

            OverloadViewHolder(@NonNull View itemView) {
                super(itemView);
                tvIndex = itemView.findViewById(R.id.tv_index);
                tvParams = itemView.findViewById(R.id.tv_params);
            }

            void bind(MethodInfo method, int position) {
                tvIndex.setText(String.format(itemView.getContext().getString(R.string.index_format), position + 1));
                
                StringBuilder paramsBuilder = new StringBuilder();
                List<String> paramTypes = method.getGenericParameterTypes();
                if (paramTypes.isEmpty()) {
                    paramsBuilder.append(itemView.getContext().getString(R.string.no_params));
                } else {
                    for (int j = 0; j < paramTypes.size(); j++) {
                        if (j > 0) paramsBuilder.append(", ");
                        paramsBuilder.append(DescriptorColorizer.formatTypeName(paramTypes.get(j)));
                    }
                }
                tvParams.setText(paramsBuilder.toString());

                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onOverloadClick(getAdapterPosition(), method);
                    }
                });
            }
        }
    }
}
