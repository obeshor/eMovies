package com.gdgebolowa.watchlist.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.gdgebolowa.watchlist.R;
import com.gdgebolowa.watchlist.Watchlist;
import com.gdgebolowa.watchlist.fragment.ReviewDetailFragment;

public class ReviewDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_detail);

        if (savedInstanceState == null) {
            ReviewDetailFragment fragment = new ReviewDetailFragment();

            Bundle args = new Bundle();
            args.putString(Watchlist.MOVIE_NAME, getIntent().getStringExtra(Watchlist.MOVIE_NAME));
            args.putParcelable(Watchlist.REVIEW_OBJECT, getIntent().getParcelableExtra(Watchlist.REVIEW_OBJECT));
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction().replace(R.id.review_detail_container, fragment).commit();
        }
    }
}
