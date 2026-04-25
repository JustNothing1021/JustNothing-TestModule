package com.justnothing.testmodule.ui.analysis.packages;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.justnothing.testmodule.R;

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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_package, parent, false);
        return new ViewHolder(view, clickListener);
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

        private final TextView tvPackageName;
        private final ImageView ivCopy;
        private final OnPackageClickListener clickListener;

        public ViewHolder(@NonNull View itemView, OnPackageClickListener clickListener) {
            super(itemView);
            this.clickListener = clickListener;
            tvPackageName = itemView.findViewById(R.id.tv_package_name);
            ivCopy = itemView.findViewById(R.id.iv_copy);
        }

        public void bind(String pkgName) {
            tvPackageName.setText(pkgName);

            ivCopy.setOnClickListener(v -> {
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
