package com.example.musicplayer.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.Player;

import com.example.musicplayer.R;
import com.example.musicplayer.databinding.FragmentPlayerBinding;
import com.example.musicplayer.model.LyricLine;
import com.example.musicplayer.model.PlayerUiState;
import com.example.musicplayer.util.TimeUtils;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayerFragment extends Fragment {

    private static final long SEEK_STEP_MS = 10_000L;

    private FragmentPlayerBinding binding;
    private PlayerViewModel viewModel;

    private boolean userSeeking;
    private List<LyricLine> currentLyrics = Collections.emptyList();
    private final List<TextView> lyricViews = new ArrayList<>();
    private int shownLyricIndex = Integer.MIN_VALUE;
    private PlayerUiState lastState;

    private int accentColor;
    private int secondaryColor;

    private final ActivityResultLauncher<String[]> lyricPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null && viewModel != null) {
                    viewModel.importLyrics(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPlayerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(PlayerViewModel.class);

        accentColor = MaterialColors.getColor(
                binding.getRoot(), androidx.appcompat.R.attr.colorPrimary);
        secondaryColor = MaterialColors.getColor(
                binding.getRoot(), com.google.android.material.R.attr.colorOnSurfaceVariant);

        binding.btnPlayPause.setOnClickListener(v -> viewModel.togglePlayPause());
        binding.btnPrevious.setOnClickListener(v -> viewModel.previous());
        binding.btnNext.setOnClickListener(v -> viewModel.next());
        binding.btnRewind.setOnClickListener(v -> viewModel.seekBy(-SEEK_STEP_MS));
        binding.btnForward.setOnClickListener(v -> viewModel.seekBy(SEEK_STEP_MS));
        binding.btnRepeat.setOnClickListener(v -> viewModel.cycleRepeat());
        binding.btnShuffle.setOnClickListener(v -> viewModel.toggleShuffle());
        binding.btnImportLyrics.setOnClickListener(v -> lyricPicker.launch(new String[]{"*/*"}));

        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    binding.timeCurrent.setText(TimeUtils.format(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                userSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                userSeeking = false;
                viewModel.seekTo(seekBar.getProgress());
            }
        });

        viewModel.getState().observe(getViewLifecycleOwner(), this::renderState);

        viewModel.getArtwork().observe(getViewLifecycleOwner(), bitmap -> {
            if (bitmap != null) {
                binding.artwork.setImageBitmap(bitmap);
            } else {
                binding.artwork.setImageResource(R.drawable.ic_album_placeholder);
            }
        });

        viewModel.getLyrics().observe(getViewLifecycleOwner(), lines -> {
            currentLyrics = lines == null ? Collections.emptyList() : lines;
            rebuildLyricViews();
            shownLyricIndex = Integer.MIN_VALUE;
            if (lastState != null) {
                renderLyrics(lastState.lyricLineIndex);
            }
        });
    }

    private void renderState(PlayerUiState state) {
        if (state == null || binding == null) {
            return;
        }
        lastState = state;

        if (state.mediaId == null) {
            binding.title.setText(R.string.no_track_selected);
            binding.artist.setText("");
            binding.album.setText("");
        } else {
            binding.title.setText(state.title.isEmpty()
                    ? getString(R.string.unknown_title) : state.title);
            binding.artist.setText(state.artist.isEmpty()
                    ? getString(R.string.unknown_artist) : state.artist);
            binding.album.setText(state.album);
        }

        boolean hasMedia = state.mediaId != null;
        binding.btnPlayPause.setEnabled(hasMedia);
        binding.btnPrevious.setEnabled(hasMedia);
        binding.btnNext.setEnabled(hasMedia);
        binding.btnRewind.setEnabled(hasMedia);
        binding.btnForward.setEnabled(hasMedia);
        binding.btnRepeat.setEnabled(hasMedia);
        binding.btnShuffle.setEnabled(hasMedia);

        boolean showPause = state.isPlaying
                || (state.playWhenReady && state.playbackState == Player.STATE_BUFFERING);
        binding.btnPlayPause.setImageResource(showPause ? R.drawable.ic_pause : R.drawable.ic_play);

        int duration = (int) Math.min(Integer.MAX_VALUE, state.durationMs);
        if (binding.seekBar.getMax() != duration) {
            binding.seekBar.setMax(duration);
        }
        binding.seekBar.setEnabled(duration > 0);
        if (!userSeeking) {
            binding.seekBar.setProgress((int) Math.min(state.positionMs, duration));
        }
        binding.timeCurrent.setText(TimeUtils.format(
                userSeeking ? binding.seekBar.getProgress() : state.positionMs));
        binding.timeTotal.setText(TimeUtils.format(state.durationMs));

        binding.btnRepeat.setImageResource(state.repeatMode == Player.REPEAT_MODE_ONE
                ? R.drawable.ic_repeat_one : R.drawable.ic_repeat);
        binding.btnRepeat.setAlpha(state.repeatMode == Player.REPEAT_MODE_OFF ? 0.4f : 1.0f);
        binding.btnShuffle.setAlpha(state.shuffleEnabled ? 1.0f : 0.4f);

        renderLyrics(state.lyricLineIndex);
    }

    private void rebuildLyricViews() {
        if (binding == null) {
            return;
        }
        binding.lyricContainer.removeAllViews();
        lyricViews.clear();

        if (currentLyrics.isEmpty()) {
            binding.lyricScroll.setVisibility(View.GONE);
            binding.lyricEmpty.setVisibility(View.VISIBLE);
            return;
        }

        binding.lyricScroll.setVisibility(View.VISIBLE);
        binding.lyricEmpty.setVisibility(View.GONE);
        int horizontalPadding = dp(24);
        int verticalPadding = dp(6);
        for (LyricLine line : currentLyrics) {
            TextView textView = new TextView(requireContext());
            textView.setText(line.getText());
            textView.setGravity(Gravity.CENTER);
            textView.setPadding(horizontalPadding, verticalPadding,
                    horizontalPadding, verticalPadding);
            textView.setTextSize(15f);
            textView.setTextColor(secondaryColor);
            binding.lyricContainer.addView(textView);
            lyricViews.add(textView);
        }
    }

    private void renderLyrics(int index) {
        if (binding == null || currentLyrics.isEmpty() || lyricViews.isEmpty()) {
            return;
        }
        int safeIndex = Math.max(-1, Math.min(index, lyricViews.size() - 1));
        if (safeIndex == shownLyricIndex) {
            return;
        }
        shownLyricIndex = safeIndex;

        for (int i = 0; i < lyricViews.size(); i++) {
            TextView textView = lyricViews.get(i);
            boolean isCurrent = i == safeIndex;
            textView.setTextSize(isCurrent ? 18f : 15f);
            textView.setTextColor(isCurrent ? accentColor : secondaryColor);
            textView.setTypeface(null, isCurrent ? Typeface.BOLD : Typeface.NORMAL);
        }

        if (safeIndex >= 0) {
            final View target = lyricViews.get(safeIndex);
            binding.lyricScroll.post(() -> {
                if (binding == null) {
                    return;
                }
                int y = target.getTop()
                        - (binding.lyricScroll.getHeight() - target.getHeight()) / 2;
                binding.lyricScroll.smoothScrollTo(0, Math.max(0, y));
            });
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
