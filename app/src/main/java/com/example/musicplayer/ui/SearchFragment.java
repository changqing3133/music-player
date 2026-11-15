package com.example.musicplayer.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.musicplayer.R;
import com.example.musicplayer.data.TrackEntity;
import com.example.musicplayer.databinding.FragmentSearchBinding;

import java.util.List;

/**
 * 搜索页：按标题、艺术家、专辑或文件名做模糊查询。
 *
 * <p>关键词为空时展示整个曲库；输入停顿 {@link #DEBOUNCE_MS} 毫秒后再查库，
 * 避免每按一个键都触发一次数据库查询。</p>
 */
public class SearchFragment extends Fragment {

    private static final long DEBOUNCE_MS = 200L;

    private FragmentSearchBinding binding;
    private PlayerViewModel viewModel;
    private TrackAdapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable applyQuery = new Runnable() {
        @Override
        public void run() {
            if (binding == null) {
                return;
            }
            Editable text = binding.searchInput.getText();
            viewModel.setSearchQuery(text == null ? "" : text.toString());
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
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

        // 恢复上次的关键词（旋转屏幕或从播放页返回时视图可能重建）
        String query = viewModel.getCurrentSearchQuery();
        binding.searchInput.setText(query);
        if (!query.isEmpty()) {
            binding.searchInput.setSelection(query.length());
        }
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                handler.removeCallbacks(applyQuery);
                handler.postDelayed(applyQuery, DEBOUNCE_MS);
            }
        });

        viewModel.getSearchResults().observe(getViewLifecycleOwner(), this::showResults);
        viewModel.getState().observe(getViewLifecycleOwner(),
                state -> adapter.setCurrentMediaId(state.mediaId));
    }

    private void showResults(@Nullable List<TrackEntity> tracks) {
        if (binding == null) {
            return;
        }
        int count = tracks == null ? 0 : tracks.size();
        adapter.submitList(tracks);

        boolean empty = count == 0;
        binding.recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.resultCount.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) {
            binding.emptyText.setText(viewModel.getCurrentSearchQuery().isEmpty()
                    ? R.string.search_library_empty : R.string.search_empty);
        } else {
            binding.resultCount.setText(getString(R.string.search_result_count, count));
        }
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(applyQuery);
        super.onDestroyView();
        binding = null;
    }
}
