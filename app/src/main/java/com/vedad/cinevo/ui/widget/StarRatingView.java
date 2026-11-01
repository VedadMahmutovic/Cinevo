package com.vedad.cinevo.ui.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.color.MaterialColors;
import com.vedad.cinevo.R;

public class StarRatingView extends LinearLayout {

    public interface OnRatingChangeListener {
        void onRatingChanged(float rating);
    }

    private static final int STAR_COUNT = 5;

    private final ImageView[] stars = new ImageView[STAR_COUNT];
    private final int filledColor;
    private final int emptyColor;
    private float rating = 0f;
    private float dragRating = 0f;
    private OnRatingChangeListener listener;

    public StarRatingView(Context context) {
        this(context, null);
    }

    public StarRatingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);

        filledColor = ContextCompat.getColor(context, R.color.cinevo_star);
        emptyColor = MaterialColors.getColor(context,
                com.google.android.material.R.attr.colorOutline, 0xFF888888);

        int size = dp(38);
        int gap = dp(3);
        for (int i = 0; i < STAR_COUNT; i++) {
            ImageView star = new ImageView(context);
            LayoutParams params = new LayoutParams(size, size);
            params.setMargins(gap, 0, gap, 0);
            star.setLayoutParams(params);
            addView(star);
            stars[i] = star;
        }
        render(0f);
    }

    public void setOnRatingChangeListener(OnRatingChangeListener listener) {
        this.listener = listener;
    }

    public void setRating(float value) {
        rating = value;
        render(value);
    }

    public float getRating() {
        return rating;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                dragRating = ratingAt(event.getX());
                render(dragRating);
                return true;
            case MotionEvent.ACTION_MOVE:
                dragRating = ratingAt(event.getX());
                render(dragRating);
                return true;
            case MotionEvent.ACTION_UP:
                rating = dragRating;
                render(rating);
                if (listener != null) listener.onRatingChanged(rating);
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                render(rating);
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private float ratingAt(float x) {
        float width = Math.max(1, getWidth());
        float position = Math.max(0f, Math.min(x, width)) / (width / STAR_COUNT);
        float half = (float) Math.ceil(position * 2) / 2f;
        return Math.max(0.5f, Math.min(5f, half));
    }

    private void render(float value) {
        for (int i = 0; i < STAR_COUNT; i++) {
            ImageView star = stars[i];
            if (value >= i + 1) {
                star.setImageResource(R.drawable.ic_star_full);
                star.setImageTintList(ColorStateList.valueOf(filledColor));
            } else if (value >= i + 0.5f) {
                star.setImageResource(R.drawable.ic_star_half);
                star.setImageTintList(ColorStateList.valueOf(filledColor));
            } else {
                star.setImageResource(R.drawable.ic_star_empty);
                star.setImageTintList(ColorStateList.valueOf(emptyColor));
            }
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}