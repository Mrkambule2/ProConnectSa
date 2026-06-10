package com.example.proconnectsa.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.proconnectsa.databinding.ItemTradespersonBinding;
import com.example.proconnectsa.models.Tradesperson;

import java.util.List;
import java.util.Locale;

public class TradespersonAdapter extends RecyclerView.Adapter<TradespersonAdapter.ViewHolder> {

    public interface OnTradespersonClickListener {
        void onTradespersonClick(Tradesperson profile);
    }

    private final List<Tradesperson> profiles;
    private final OnTradespersonClickListener listener;

    public TradespersonAdapter(List<Tradesperson> profiles, OnTradespersonClickListener listener) {
        this.profiles = profiles;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTradespersonBinding binding = ItemTradespersonBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Tradesperson profile = profiles.get(position);
        holder.binding.tradespersonName.setText(profile.getName());
        holder.binding.tradeAndArea.setText(String.format("%s • %s", profile.getTrade(), profile.getArea()));
        holder.binding.ratingBar.setRating(profile.getRating());
        holder.binding.ratingText.setText(String.format(Locale.getDefault(), "%.1f★", profile.getRating()));
        
        Glide.with(holder.itemView.getContext())
                .load(profile.getProfileImageUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.binding.profileImage);

        holder.itemView.setOnClickListener(v -> listener.onTradespersonClick(profile));
    }

    @Override
    public int getItemCount() {
        return profiles.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTradespersonBinding binding;

        public ViewHolder(ItemTradespersonBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}