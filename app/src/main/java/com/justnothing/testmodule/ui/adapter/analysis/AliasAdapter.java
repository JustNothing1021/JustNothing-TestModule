package com.justnothing.testmodule.ui.adapter.analysis;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.functions.alias.model.AliasInfo;

import java.util.ArrayList;
import java.util.List;

public class AliasAdapter extends RecyclerView.Adapter<AliasAdapter.AliasViewHolder> {

    private final Context context;
    private final OnAliasClickListener listener;
    private List<AliasInfo> aliases;

    public interface OnAliasClickListener {
        void onAliasClick(AliasInfo alias);
    }

    public AliasAdapter(Context context, OnAliasClickListener listener) {
        this.context = context;
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
        View view = LayoutInflater.from(context).inflate(R.layout.item_alias, parent, false);
        return new AliasViewHolder(view);
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

        private final TextView tvName;
        private final TextView tvCommand;

        public AliasViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_alias_name);
            tvCommand = itemView.findViewById(R.id.tv_alias_command);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onAliasClick(aliases.get(position));
                }
            });
        }

        public void bind(AliasInfo alias) {
            tvName.setText(alias.getName());
            tvCommand.setText(alias.getCommand());
        }
    }
}
