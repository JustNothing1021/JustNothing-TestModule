package com.justnothing.testmodule.ui.adapter.analysis;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.justnothing.testmodule.command.functions.alias.model.AliasInfo;
import com.justnothing.testmodule.databinding.ItemAliasBinding;

import java.util.ArrayList;
import java.util.List;

public class AliasAdapter extends RecyclerView.Adapter<AliasAdapter.AliasViewHolder> {

    private final OnAliasClickListener listener;
    private List<AliasInfo> aliases;

    public interface OnAliasClickListener {
        void onAliasClick(AliasInfo alias);
    }

    public AliasAdapter(OnAliasClickListener listener) {
        this.listener = listener;
        this.aliases = new ArrayList<>();
    }

    public void setAliases(List<AliasInfo> aliases) {
        this.aliases = aliases != null ? aliases : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AliasViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAliasBinding binding = ItemAliasBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new AliasViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AliasViewHolder holder, int position) {
        AliasInfo alias = aliases.get(position);
        holder.bind(alias);
    }

    @Override
    public int getItemCount() {
        return aliases.size();
    }

    public class AliasViewHolder extends RecyclerView.ViewHolder {

        private final ItemAliasBinding binding;

        public AliasViewHolder(@NonNull ItemAliasBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onAliasClick(aliases.get(position));
                }
            });
        }

        public void bind(AliasInfo alias) {
            binding.tvAliasName.setText(alias.getName());
            binding.tvAliasCommand.setText(alias.getCommand());
        }
    }
}
