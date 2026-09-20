package com.justnothing.testmodule.ui.adapter.analysis;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;
import com.justnothing.testmodule.databinding.ItemMethodBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * 方法列表适配器。
 */
public class MethodsAdapter extends RecyclerView.Adapter<MethodsAdapter.ViewHolder> {
    
    private final List<MethodInfo> methods = new ArrayList<>();
    
    public MethodsAdapter(List<MethodInfo> methods) {
        if (methods != null) {
            this.methods.addAll(methods);
        }
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMethodBinding binding = ItemMethodBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MethodInfo method = methods.get(position);
        holder.bind(method);
    }
    
    @Override
    public int getItemCount() {
        return methods.size();
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemMethodBinding binding;
        
        public ViewHolder(@NonNull ItemMethodBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        
        public void bind(MethodInfo method) {
            binding.tvMethodName.setText(method.getName());
            binding.tvReturnType.setText(method.getReturnType());
            binding.tvModifiers.setText(method.getModifiersString());
            
            StringBuilder params = new StringBuilder();
            List<String> paramTypes = method.getParameterTypes();
            List<String> paramNames = method.getParameters();
            for (int i = 0; i < paramTypes.size(); i++) {
                if (i > 0) params.append(", ");
                params.append(paramTypes.get(i));
                if (i < paramNames.size() && !paramNames.get(i).isEmpty()) {
                    params.append(" ");
                    params.append(paramNames.get(i));
                }
            }
            binding.tvParameters.setText(params.toString());
        }
    }
}
