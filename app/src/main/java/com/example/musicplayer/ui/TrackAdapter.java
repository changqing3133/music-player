package com.example.musicplayer.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.R;
import com.example.musicplayer.data.TrackEntity;
import com.example.musicplayer.databinding.ItemTrackBinding;
import com.example.musicplayer.util.TimeUtils;
import com.google.android.material.color.MaterialColors;

import java.util.Objects;

public class TrackAdapter extends ListAdapter<TrackEntity, TrackAdapter.TrackViewHolder> {

    public interface OnTrackClickListener {
        void onTrackClick(TrackEntity track);
    }

    private static final DiffUtil.ItemCallback<TrackEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<TrackEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull TrackEntity oldItem, @NonNull TrackEntity newItem) {
                    return oldItem.getUri().equals(newItem.getUri());
                }

                @Override
                public boolean areContentsTheSame(@NonNull TrackEntity oldItem, @NonNull TrackEntity newItem) {
                    return oldItem.getMediaId() == newItem.getMediaId()
                            && oldItem.getTitle().equals(newItem.getTitle())
                            && oldItem.getArtist().equals(newItem.getArtist())
                            && oldItem.getAlbum().equals(newItem.getAlbum())
                            && oldItem.getDurationMs() == newItem.getDurationMs();
                }
            };

    private final OnTrackClickListener listener;
    private String currentMediaId;

    public TrackAdapter(OnTrackClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setCurrentMediaId(String mediaId) {
        if (Objects.equals(currentMediaId, mediaId)) {
            return;
        }
        currentMediaId = mediaId;
        notifyItemRangeChanged(0, getItemCount());
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTrackBinding binding = ItemTrackBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TrackViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    final class TrackViewHolder extends RecyclerView.ViewHolder {

        private final ItemTrackBinding binding;
        private final int defaultTitleColor;
        private final int accentColor;

        TrackViewHolder(ItemTrackBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            defaultTitleColor = binding.title.getCurrentTextColor();
            accentColor = MaterialColors.getColor(
                    binding.getRoot(), androidx.appcompat.R.attr.colorPrimary);
            binding.getRoot().setOnClickListener(view -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onTrackClick(getItem(position));
                }
            });
        }

        void bind(TrackEntity track) {
            binding.title.setText(track.getTitle());
            binding.subtitle.setText(binding.getRoot().getContext().getString(
                    R.string.track_subtitle, track.getArtist(), track.getAlbum()));
            binding.duration.setText(TimeUtils.format(track.getDurationMs()));

            boolean isCurrent = track.getUri().equals(currentMediaId);
            binding.playingIndicator.setVisibility(isCurrent ? View.VISIBLE : View.INVISIBLE);
            binding.title.setTextColor(isCurrent ? accentColor : defaultTitleColor);
        }
    }
}
