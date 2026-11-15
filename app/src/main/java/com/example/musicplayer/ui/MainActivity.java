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
    private static final String TAG_SEARCH = "search";
    private static final String TAG_PLAYER = "player";
    private static final String STATE_TAB_ID = "state_tab_id";

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
        int initialTab = initialTab(savedInstanceState, viewUri);
        if (initialTab != R.id.nav_library) {
            // 在注册监听之前恢复选中项，避免重复添加 Fragment
            binding.bottomNav.setSelectedItemId(initialTab);
        }

        binding.bottomNav.setOnItemSelectedListener(item -> {
            switchTab(item.getItemId());
            return true;
        });

        switchTab(initialTab);

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

    /** 恢复上次所在的页签；冷启动来自“打开方式”时直接进入播放页。 */
    private static int initialTab(@Nullable Bundle savedState, @Nullable Uri viewUri) {
        if (viewUri != null) {
            return R.id.nav_player;
        }
        if (savedState == null) {
            return R.id.nav_library;
        }
        int tab = savedState.getInt(STATE_TAB_ID, R.id.nav_library);
        return tab == R.id.nav_player || tab == R.id.nav_search ? tab : R.id.nav_library;
    }

    private void switchTab(int itemId) {
        if (itemId == R.id.nav_player) {
            showTab(TAG_PLAYER, R.string.nav_player);
        } else if (itemId == R.id.nav_search) {
            showTab(TAG_SEARCH, R.string.nav_search);
        } else {
            showTab(TAG_LIBRARY, R.string.nav_library);
        }
    }

    private static Fragment createFragment(String tag) {
        if (TAG_PLAYER.equals(tag)) {
            return new PlayerFragment();
        }
        if (TAG_SEARCH.equals(tag)) {
            return new SearchFragment();
        }
        return new LibraryFragment();
    }

    private void showTab(String tag, int titleRes) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment target = fragmentManager.findFragmentByTag(tag);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (target == null) {
            target = createFragment(tag);
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
        setTitle(titleRes);
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
        outState.putInt(STATE_TAB_ID,
                binding == null ? R.id.nav_library : binding.bottomNav.getSelectedItemId());
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
