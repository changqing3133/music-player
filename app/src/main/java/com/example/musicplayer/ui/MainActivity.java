package com.example.musicplayer.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.musicplayer.MusicApp;
import com.example.musicplayer.R;
import com.example.musicplayer.data.Prefs;
import com.example.musicplayer.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final String TAG_LIBRARY = "library";
    private static final String TAG_PLAYER = "player";
    private static final String STATE_PLAYER_TAB = "state_player_tab";

    private ActivityMainBinding binding;
    private PlayerViewModel viewModel;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        // 通知权限被拒绝不影响播放，仅通知栏控件不可见
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        viewModel = new ViewModelProvider(this).get(PlayerViewModel.class);

        // 冷启动来自“打开方式”时直接进入播放页，避免先 add 曲库页、再叠加播放页
        Uri viewUri = savedInstanceState == null ? viewUriFrom(getIntent()) : null;
        boolean showPlayer = viewUri != null
                || (savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_PLAYER_TAB, false));
        if (showPlayer) {
            // 在注册监听之前恢复选中项，避免重复添加 Fragment
            binding.bottomNav.setSelectedItemId(R.id.nav_player);
        }

        binding.bottomNav.setOnItemSelectedListener(item -> {
            boolean player = item.getItemId() == R.id.nav_player;
            showTab(player ? TAG_PLAYER : TAG_LIBRARY, player);
            setTitle(player ? R.string.nav_player : R.string.nav_library);
            return true;
        });

        showTab(showPlayer ? TAG_PLAYER : TAG_LIBRARY, showPlayer);
        setTitle(showPlayer ? R.string.nav_player : R.string.nav_library);

        viewModel.getMessage().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                viewModel.clearMessage();
            }
        });

        requestNotificationPermissionOnce();

        if (viewUri != null) {
            viewModel.playExternalUri(viewUri);
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleViewIntent(intent);
    }

    /** 处理应用已在运行时，文件管理器再次传入的音频 URI。 */
    private void handleViewIntent(@Nullable Intent intent) {
        Uri uri = viewUriFrom(intent);
        if (uri == null) {
            return;
        }
        viewModel.playExternalUri(uri);
        openPlayerTab();
    }

    @Nullable
    private static Uri viewUriFrom(@Nullable Intent intent) {
        if (intent == null || !Intent.ACTION_VIEW.equals(intent.getAction())) {
            return null;
        }
        return intent.getData();
    }

    private void showTab(String tag, boolean player) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment target = fragmentManager.findFragmentByTag(tag);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (target == null) {
            target = player ? new PlayerFragment() : new LibraryFragment();
            transaction.add(R.id.fragment_container, target, tag);
        } else {
            transaction.show(target);
        }
        for (Fragment fragment : fragmentManager.getFragments()) {
            if (fragment != target) {
                transaction.hide(fragment);
            }
        }
        transaction.commit();
    }

    public void openPlayerTab() {
        binding.bottomNav.setSelectedItemId(R.id.nav_player);
    }

    private void requestNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        Prefs prefs = MusicApp.get().getPrefs();
        if (prefs.wasNotificationAsked()) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        prefs.markNotificationAsked();
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_PLAYER_TAB,
                binding != null && binding.bottomNav.getSelectedItemId() == R.id.nav_player);
    }

    @Override
    protected void onStart() {
        super.onStart();
        viewModel.setActive(true);
    }

    @Override
    protected void onStop() {
        viewModel.setActive(false);
        super.onStop();
    }

    @NonNull
    public PlayerViewModel getPlayerViewModel() {
        return viewModel;
    }
}
