package com.justnothing.testmodule.ui.adapter.analysis;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.justnothing.testmodule.databinding.ItemPackageBinding;

import java.util.ArrayList;
import java.util.List;

public class PackagesAdapter extends RecyclerView.Adapter<PackagesAdapter.ViewHolder> {

    private final List<String> packages = new ArrayList<>();
    private final OnPackageClickListener clickListener;

    public interface OnPackageClickListener {
        void onPackageClick(String packageName);
    }

    public PackagesAdapter(List<String> packages, OnPackageClickListener clickListener) {
        if (packages != null) {
            this.packages.addAll(packages);
        }
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPackageBinding binding = ItemPackageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding, clickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String pkgName = packages.get(position);
        holder.bind(pkgName);
    }

    @Override
    public int getItemCount() {
        return packages.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemPackageBinding binding;
        private final OnPackageClickListener clickListener;

        public ViewHolder(@NonNull ItemPackageBinding binding, OnPackageClickListener clickListener) {
            super(binding.getRoot());
            this.binding = binding;
            this.clickListener = clickListener;
        }

        public void bind(String pkgName) {
            binding.tvPackageName.setText(pkgName);

            binding.ivCopy.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onPackageClick(pkgName);
                }
            });

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onPackageClick(pkgName);
                }
            });
        }
    }
}
