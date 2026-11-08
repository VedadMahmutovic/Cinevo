package com.vedad.cinevo.util;

import android.content.Context;

import com.vedad.cinevo.R;
import com.vedad.cinevo.recommendation.RecommendationEngine;

public class ResourceExplainText implements RecommendationEngine.ExplainText {

    private final Context context;

    public ResourceExplainText(Context context) {
        this.context = context;
    }

    @Override
    public String because(String firstTitle, int others) {
        return others == 0
                ? context.getString(R.string.why_because, firstTitle)
                : context.getString(R.string.why_because_more, firstTitle, others);
    }

    @Override
    public String directedBy(String directorName) {
        return context.getString(R.string.directed_by, directorName);
    }

    @Override
    public String genres(String genreList) {
        return context.getString(R.string.why_genre, genreList);
    }

    @Override
    public String highlyRated(double rating) {
        return context.getString(R.string.why_rating, rating);
    }

    @Override
    public String genreName(int genreId) {
        return GenreLabels.of(context, genreId);
    }
}