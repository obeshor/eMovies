package com.gdgebolowa.watchlist.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.Toolbar.OnMenuItemClickListener;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.gdgebolowa.watchlist.R;
import com.gdgebolowa.watchlist.Watchlist;
import com.gdgebolowa.watchlist.model.Review;

import butterknife.BindBool;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class ReviewDetailFragment extends Fragment implements OnMenuItemClickListener  {

    private Tracker tracker;
    private Unbinder unbinder;

    private String movieName;
    private Review review;

    @BindBool(R.bool.is_tablet)         boolean isTablet;
    @BindView(R.id.toolbar)             Toolbar toolbar;
    @BindView(R.id.review_body)         TextView reviewBody;
    @BindView(R.id.review_body_holder)  View reviewBodyHolder;

    // Fragment lifecycle
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_review_detail,container,false);
        unbinder = ButterKnife.bind(this, v);

        // Get arguments
        movieName = getArguments().getString(Watchlist.MOVIE_NAME);
        review = getArguments().getParcelable(Watchlist.REVIEW_OBJECT);
        if (review == null) {
            if (movieName.equals("null")) {
                toolbar.setTitle("");
            } else {
                toolbar.setTitle(R.string.loading);
            }
            reviewBodyHolder.setVisibility(View.GONE);
        } else {
            // Setup toolbar
            toolbar.setTitle("Review by " + review.userName);
            toolbar.inflateMenu(R.menu.menu_share);
            toolbar.setOnMenuItemClickListener(this);
            if (!isTablet) {
                toolbar.setNavigationIcon(ContextCompat.getDrawable(getActivity(), R.drawable.action_home));
                toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getActivity().finish();
                    }
                });
            }
            // Set review body
            reviewBody.setText(review.comment);
        }

        // Load Analytics Tracker
        tracker = ((Watchlist) getActivity().getApplication()).getTracker();

        return v;
    }
    @Override
    public void onResume() {
        super.onResume();
        // Send screen name to analytics
        tracker.setScreenName(getString(R.string.screen_review_detail));
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    // Toolbar options menu
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_share) {
            // Share the review
            String url = "https://trakt.tv/comments/" + review.id;
            String shareText = "A review of " + movieName + " by " + review.userName + " - " + url;
            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, movieName + " - Review");
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareText);
            startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.action_share_using)));
            // Send event to Google Analytics
            tracker.send(new HitBuilders.EventBuilder()
                    .setCategory(getString(R.string.ga_category_share))
                    .setAction(getString(R.string.ga_action_review))
                    .setLabel(url)
                    .build());
            return true;
        } else {
            return false;
        }
    }
}