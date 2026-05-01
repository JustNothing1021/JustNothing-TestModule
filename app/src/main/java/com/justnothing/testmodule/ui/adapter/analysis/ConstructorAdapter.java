package com.justnothing.testmodule.ui.adapter.analysis;

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
import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;
import com.justnothing.testmodule.utils.reflect.DescriptorColorizer;

import java.util.ArrayList;
import java.util.List;

public class ConstructorAdapter extends RecyclerView.Adapter<ConstructorAdapter.ViewHolder> {

    private final List<MethodInfo> constructors = new ArrayList<>();
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(int position, MethodInfo constructor);
    }

    public ConstructorAdapter(List<MethodInfo> constructors) {
        if (constructors != null) {
            this.constructors.addAll(constructors);
        }
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_constructor, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(constructors.get(position));
    }

    @Override
    public int getItemCount() {
        return constructors.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSignature;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSignature = itemView.findViewById(R.id.tv_signature);
            
            itemView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        onItemClickListener.onItemClick(pos, constructors.get(pos));
                    }
                }
            });
        }

        public void bind(MethodInfo constructor) {
            int index = getAdapterPosition() + 1;
            int colorBlue = ContextCompat.getColor(itemView.getContext(), R.color.blue);

            SpannableStringBuilder sb = new SpannableStringBuilder();
            sb.append("[").append(String.valueOf(index)).append("] ");

            String modifier = constructor.getModifiersString();
            if (modifier != null && !modifier.isEmpty()) {
                sb.append("(");
                int modifierStart = sb.length();
                sb.append(modifier);
                sb.setSpan(new ForegroundColorSpan(colorBlue), modifierStart, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.append(") ");
            }

            List<String> genericParamTypes = constructor.getGenericParameterTypes();
            if (genericParamTypes.isEmpty()) {
                sb.append(itemView.getContext().getString(R.string.analysis_no_params));
            } else {
                for (int i = 0; i < genericParamTypes.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(DescriptorColorizer.formatTypeName(genericParamTypes.get(i)));
                }
            }
            tvSignature.setText(sb);
        }
    }
}
