package com.justnothing.testmodule.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.protocol.json.model.FieldInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 字段列表适配器。
 */
public class FieldsAdapter extends RecyclerView.Adapter<FieldsAdapter.ViewHolder> {
    
    private final List<FieldInfo> fields = new ArrayList<>();
    
    public FieldsAdapter(List<FieldInfo> fields) {
        if (fields != null) {
            this.fields.addAll(fields);
        }
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_field, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FieldInfo field = fields.get(position);
        holder.bind(field);
    }
    
    @Override
    public int getItemCount() {
        return fields.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvFieldName;
        private final TextView tvFieldType;
        private final TextView tvModifiers;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFieldName = itemView.findViewById(R.id.tv_field_name);
            tvFieldType = itemView.findViewById(R.id.tv_field_type);
            tvModifiers = itemView.findViewById(R.id.tv_modifiers);
        }
        
        public void bind(FieldInfo field) {
            tvFieldName.setText(field.getName());
            tvFieldType.setText(field.getType());
            tvModifiers.setText(field.getModifiersString());
        }
    }
}
