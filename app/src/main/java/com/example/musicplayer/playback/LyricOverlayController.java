package com.example.musicplayer.playback;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 悬浮歌词窗口（TYPE_APPLICATION_OVERLAY）：
 * - 卡片可拖动，右上角 × 关闭（由播放服务取消开启状态）
 * - 只负责显示，歌词内容由 {@link PlaybackService} 按播放进度推送
 * - 窗口带 FLAG_SHOW_WHEN_LOCKED，部分机型可覆盖锁屏；不能覆盖时由锁屏歌词页兜底
 */
final class LyricOverlayController {

    interface Listener {
        void onCloseRequested();
    }

    private static final int COLOR_CARD = 0xC0071A2B;
    private static final int COLOR_STROKE = 0x6622D3EE;
    private static final int COLOR_TITLE = 0xFF7C93A9;
    private static final int COLOR_CURRENT = 0xFFE6F7FF;
    private static final int COLOR_SECONDARY = 0xFF9FB3C8;

    private final Context context;
    private final WindowManager windowManager;
    private final Listener listener;

    private LinearLayout card;
    private TextView titleView;
    private TextView currentView;
    private TextView nextView;
    private WindowManager.LayoutParams params;
    private boolean added;

    LyricOverlayController(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        this.windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    /** 是否已获得“显示在其他应用上层”权限。 */
    boolean canShow() {
        return Settings.canDrawOverlays(context);
    }

    boolean isVisible() {
        return added;
    }

    void show(String title) {
        if (added || windowManager == null || !canShow()) {
            return;
        }
        if (card == null) {
            build();
        }
        setTitle(title);
        try {
            windowManager.addView(card, params);
            added = true;
        } catch (Exception ignored) {
            // 权限被系统回收或窗口服务异常时保持隐藏，下次播放再试
        }
    }

    void hide() {
        if (!added || windowManager == null) {
            return;
        }
        try {
            windowManager.removeViewImmediate(card);
        } catch (Exception ignored) {
            // 窗口可能已被系统移除
        }
        added = false;
    }

    void setTitle(String title) {
        if (titleView != null) {
            titleView.setText(TextUtils.isEmpty(title) ? "本地音乐" : title);
        }
    }

    void setLines(String current, String next) {
        if (currentView == null || nextView == null) {
            return;
        }
        currentView.setText(TextUtils.isEmpty(current) ? "♪" : current);
        if (TextUtils.isEmpty(next)) {
            nextView.setVisibility(View.GONE);
        } else {
            nextView.setVisibility(View.VISIBLE);
            nextView.setText(next);
        }
    }

    private void build() {
        int padding = dp(14);

        card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(padding, dp(10), padding, dp(12));
        GradientDrawable background = new GradientDrawable();
        background.setColor(COLOR_CARD);
        background.setCornerRadius(dp(18));
        background.setStroke(Math.max(1, dp(1)), COLOR_STROKE);
        card.setBackground(background);

        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        titleView = new TextView(context);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        titleView.setTextColor(COLOR_TITLE);
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        header.addView(titleView, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView closeView = new TextView(context);
        closeView.setText("×");
        closeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        closeView.setTextColor(COLOR_TITLE);
        closeView.setPadding(dp(12), 0, dp(2), dp(2));
        closeView.setOnClickListener(v -> {
            hide();
            if (listener != null) {
                listener.onCloseRequested();
            }
        });
        header.addView(closeView);

        card.addView(header);

        currentView = new TextView(context);
        currentView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        currentView.setTextColor(COLOR_CURRENT);
        currentView.setTypeface(currentView.getTypeface(), android.graphics.Typeface.BOLD);
        currentView.setMaxLines(2);
        currentView.setEllipsize(TextUtils.TruncateAt.END);
        currentView.setIncludeFontPadding(false);
        LinearLayout.LayoutParams currentParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        currentParams.topMargin = dp(4);
        card.addView(currentView, currentParams);

        nextView = new TextView(context);
        nextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        nextView.setTextColor(COLOR_SECONDARY);
        nextView.setSingleLine(true);
        nextView.setEllipsize(TextUtils.TruncateAt.END);
        nextView.setIncludeFontPadding(false);
        LinearLayout.LayoutParams nextParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        nextParams.topMargin = dp(3);
        card.addView(nextView, nextParams);

        params = new WindowManager.LayoutParams();
        params.width = overlayWidth();
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        params.format = PixelFormat.TRANSLUCENT;
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = dp(24);
        params.y = dp(190);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        card.setOnTouchListener(new DragListener());
    }

    @SuppressLint("ClickableViewAccessibility")
    private final class DragListener implements View.OnTouchListener {
        private float downRawX;
        private float downRawY;
        private int startX;
        private int startY;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downRawX = event.getRawX();
                    downRawY = event.getRawY();
                    startX = params.x;
                    startY = params.y;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    params.x = startX + Math.round(event.getRawX() - downRawX);
                    params.y = Math.max(0, startY + Math.round(event.getRawY() - downRawY));
                    try {
                        windowManager.updateViewLayout(card, params);
                    } catch (Exception ignored) {
                        // 窗口已被移除
                    }
                    return true;
                default:
                    return false;
            }
        }
    }

    private int overlayWidth() {
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int maxWidth = dp(560);
        return Math.max(dp(220), Math.min(screenWidth - dp(48), maxWidth));
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
