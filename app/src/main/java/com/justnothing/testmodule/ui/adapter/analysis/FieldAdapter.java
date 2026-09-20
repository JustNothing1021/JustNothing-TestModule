package com.justnothing.testmodule.ui.adapter.analysis;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.functions.classcmd.model.FieldInfo;
import com.justnothing.testmodule.databinding.ItemFieldInfoBinding;
import com.justnothing.testmodule.utils.format.DescriptorColorizer;

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
        ItemFieldInfoBinding binding = ItemFieldInfoBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(fields.get(position));
    }

    @Override
    public int getItemCount() {
        return fields.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemFieldInfoBinding binding;

        public ViewHolder(@NonNull ItemFieldInfoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

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
            binding.tvFieldName.setText(field.getName());

            int colorGreen = ContextCompat.getColor(itemView.getContext(), R.color.green);

            String genericType = field.getGenericType();
            String typeName;
            if (genericType != null && !genericType.isEmpty()) {
                typeName = DescriptorColorizer.formatTypeName(genericType);
            } else {
                typeName = DescriptorColorizer.formatTypeName(field.getType());
            }

            String typeLabel = itemView.getContext().getString(R.string.analysis_field_type_label, "");
            SpannableStringBuilder typeBuilder = new SpannableStringBuilder();
            typeBuilder.append(typeLabel);
            int typeStart = typeBuilder.length();
            typeBuilder.append(typeName);
            typeBuilder.setSpan(new ForegroundColorSpan(colorGreen), typeStart, typeBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            binding.tvFieldType.setText(typeBuilder);

            String modifier = field.getModifiersString();
            String modifierLabel = itemView.getContext().getString(R.string.analysis_field_modifiers_label);
            StringBuilder modifierBuilder = new StringBuilder();
            modifierBuilder.append(modifierLabel).append(": ");
            if (modifier != null && !modifier.isEmpty()) {
                modifierBuilder.append(modifier);
            } else {
                modifierBuilder.append(itemView.getContext().getString(R.string.analysis_field_no_modifiers));
            }
            binding.tvModifiers.setText(modifierBuilder.toString());
            
            String declaringClass = field.getDeclaringClass();
            if (declaringClass != null && !declaringClass.equals(currentClassName)) {
                binding.tvInheritedFrom.setVisibility(View.VISIBLE);
                if (field.isDeclaringClassIsInterface()) {
                    binding.tvInheritedFrom.setText(String.format(itemView.getContext().getString(R.string.implements_implements_bracket), DescriptorColorizer.formatTypeName(declaringClass)));
                } else {
                    binding.tvInheritedFrom.setText(String.format(itemView.getContext().getString(R.string.analysis_invoke_extends_bracket), DescriptorColorizer.formatTypeName(declaringClass)));
                }
            } else {
                binding.tvInheritedFrom.setVisibility(View.GONE);
            }
        }
    }
}
