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
import androidx.recyclerview.widget.RecyclerView;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.protocol.json.model.FieldInfo;
import com.justnothing.testmodule.utils.reflect.DescriptorColorizer;

import java.util.ArrayList;
import java.util.List;

public class FieldAdapter extends RecyclerView.Adapter<FieldAdapter.ViewHolder> {

    private final List<FieldInfo> fields = new ArrayList<>();
    private final String currentClassName;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(FieldInfo field);
    }

    public FieldAdapter(List<FieldInfo> fields, String currentClassName) {
        this.currentClassName = currentClassName;
        if (fields != null) {
            this.fields.addAll(fields);
        }
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_field_info, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(fields.get(position));
    }

    @Override
    public int getItemCount() {
        return fields.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvFieldName;
        private final TextView tvFieldType;
        private final TextView tvModifiers;
        private final TextView tvInheritedFrom;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFieldName = itemView.findViewById(R.id.tv_field_name);
            tvFieldType = itemView.findViewById(R.id.tv_field_type);
            tvModifiers = itemView.findViewById(R.id.tv_modifiers);
            tvInheritedFrom = itemView.findViewById(R.id.tv_inherited_from);

            itemView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        onItemClickListener.onItemClick(fields.get(pos));
                    }
                }
            });
        }

        public void bind(FieldInfo field) {
            tvFieldName.setText(field.getName());

            int colorGreen = ContextCompat.getColor(itemView.getContext(), R.color.green);

            String genericType = field.getGenericType();
            String typeName;
            if (genericType != null && !genericType.isEmpty()) {
                typeName = DescriptorColorizer.formatTypeName(genericType);
            } else {
                typeName = DescriptorColorizer.formatTypeName(field.getType());
            }

            String typeLabel = itemView.getContext().getString(R.string.field_type_label);
            SpannableStringBuilder typeBuilder = new SpannableStringBuilder();
            typeBuilder.append(typeLabel).append(": ");
            int typeStart = typeBuilder.length();
            typeBuilder.append(typeName);
            typeBuilder.setSpan(new ForegroundColorSpan(colorGreen), typeStart, typeBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            tvFieldType.setText(typeBuilder);

            String modifier = field.getModifiersString();
            String modifierLabel = itemView.getContext().getString(R.string.field_modifiers_label);
            StringBuilder modifierBuilder = new StringBuilder();
            modifierBuilder.append(modifierLabel).append(": ");
            if (modifier != null && !modifier.isEmpty()) {
                modifierBuilder.append(modifier);
            } else {
                modifierBuilder.append(itemView.getContext().getString(R.string.field_no_modifiers));
            }
            tvModifiers.setText(modifierBuilder.toString());
            
            String declaringClass = field.getDeclaringClass();
            if (declaringClass != null && !declaringClass.equals(currentClassName)) {
                tvInheritedFrom.setVisibility(View.VISIBLE);
                if (field.isDeclaringClassIsInterface()) {
                    tvInheritedFrom.setText(String.format(itemView.getContext().getString(R.string.implements_interface_bracket), DescriptorColorizer.formatTypeName(declaringClass)));
                } else {
                    tvInheritedFrom.setText(String.format(itemView.getContext().getString(R.string.inherited_from_bracket), DescriptorColorizer.formatTypeName(declaringClass)));
                }
            } else {
                tvInheritedFrom.setVisibility(View.GONE);
            }
        }
    }
}
