package com.example.musicplayer.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.musicplayer.MusicApp;
import com.example.musicplayer.R;
import com.example.musicplayer.data.Prefs;
import com.example.musicplayer.data.TrackEntity;
import com.example.musicplayer.databinding.FragmentLibraryBinding;

import java.util.List;

public class LibraryFragment extends Fragment {

    private FragmentLibraryBinding binding;
    private PlayerViewModel viewModel;
    private TrackAdapter adapter;
    private boolean autoScanned;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (binding == null || !isAdded()) {
                    return;
                }
                if (granted) {
                    viewModel.scanLibrary();
                } else {
                    Toast.makeText(requireContext(), R.string.permission_denied,
                            Toast.LENGTH_LONG).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(PlayerViewModel.class);

        adapter = new TrackAdapter(track -> {
            viewModel.playTrack(track);
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openPlayerTab();
            }
        });
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);

        viewModel.getLibrary().observe(getViewLifecycleOwner(), tracks -> {
            adapter.submitList(tracks);
            updateEmptyState(tracks == null || tracks.isEmpty());
            maybeAutoScan(tracks);
        });
        viewModel.getState().observe(getViewLifecycleOwner(),
                state -> adapter.setCurrentMediaId(state.mediaId));
        viewModel.getScanning().observe(getViewLifecycleOwner(), scanning -> {
            boolean running = Boolean.TRUE.equals(scanning);
            binding.progress.setVisibility(running ? View.VISIBLE : View.GONE);
            binding.scanFab.setEnabled(!running);
        });

        binding.scanFab.setOnClickListener(v -> scanWithPermission());
        binding.emptyAction.setOnClickListener(v -> scanWithPermission());
    }

    @Override
    public void onResume() {
        super.onResume();
        // 首次进入曲库时自动申请一次音频权限；之后由按钮触发
        if (!hasAudioPermission()) {
            Prefs prefs = MusicApp.get().getPrefs();
            if (!prefs.wasAudioPermissionAsked()) {
                prefs.markAudioPermissionAsked();
                permissionLauncher.launch(requiredAudioPermission());
            }
        }
    }

    private void maybeAutoScan(List<TrackEntity> tracks) {
        if (autoScanned) {
            return;
        }
        if (tracks != null && !tracks.isEmpty()) {
            autoScanned = true;
            return;
        }
        if (!hasAudioPermission()) {
            return;
        }
        autoScanned = true;
        viewModel.scanLibrary();
    }

    private void scanWithPermission() {
        if (hasAudioPermission()) {
            viewModel.scanLibrary();
            return;
        }
        String permission = requiredAudioPermission();
        if (shouldShowRequestPermissionRationale(permission)) {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.permission_title)
                    .setMessage(R.string.permission_rationale)
                    .setPositiveButton(R.string.permission_allow,
                            (dialog, which) -> permissionLauncher.launch(permission))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else {
            MusicApp.get().getPrefs().markAudioPermissionAsked();
            permissionLauncher.launch(permission);
        }
    }

    private boolean hasAudioPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), requiredAudioPermission())
                == PackageManager.PERMISSION_GRANTED;
    }

    private static String requiredAudioPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_AUDIO
                : Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private void updateEmptyState(boolean empty) {
        binding.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
