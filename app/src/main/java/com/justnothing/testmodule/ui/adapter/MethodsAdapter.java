package com.justnothing.testmodule.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.protocol.json.model.MethodInfo;

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
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_method, parent, false);
        return new ViewHolder(view);
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
        private final TextView tvMethodName;
        private final TextView tvReturnType;
        private final TextView tvParameters;
        private final TextView tvModifiers;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMethodName = itemView.findViewById(R.id.tv_method_name);
            tvReturnType = itemView.findViewById(R.id.tv_return_type);
            tvParameters = itemView.findViewById(R.id.tv_parameters);
            tvModifiers = itemView.findViewById(R.id.tv_modifiers);
        }
        
        public void bind(MethodInfo method) {
            tvMethodName.setText(method.getName());
            tvReturnType.setText(method.getReturnType());
            tvModifiers.setText(method.getModifiersString());
            
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
            tvParameters.setText(params.toString());
        }
    }
}
