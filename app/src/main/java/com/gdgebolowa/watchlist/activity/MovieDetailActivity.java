package com.gdgebolowa.watchlist.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.gdgebolowa.watchlist.R;
import com.gdgebolowa.watchlist.Watchlist;
import com.gdgebolowa.watchlist.fragment.MovieDetailFragment;

public class MovieDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        if (savedInstanceState == null) {
            String movieId;
            Intent intent = getIntent();
            Uri data = intent.getData();
            if (data == null) {
                // Not loading from deep link
                movieId = getIntent().getStringExtra(Watchlist.MOVIE_ID);
                loadMovieDetailsOf(movieId);
            } else {
                // Loading from deep link
                String[] parts = data.toString().split("/");
                movieId = parts[parts.length - 1];
                switch (movieId) {
                    // Load Movie Lists
                    case "movie":
                        loadMoviesOfType(0);
                        break;
                    case "top-rated":
                        loadMoviesOfType(1);
                        break;
                    case "upcoming":
                        loadMoviesOfType(2);
                        break;
                    case "now-playing":
                        loadMoviesOfType(3);
                        break;
                    // Load details of a particular movie
                    default:
                        int dashPosition = movieId.indexOf("-");
                        if (dashPosition != -1) {
                            movieId = movieId.substring(0, dashPosition);
                        }
                        loadMovieDetailsOf(movieId);
                        break;
                }
            }
        }
    }

    private void loadMovieDetailsOf(String movieId) {
        MovieDetailFragment fragment = new MovieDetailFragment();

        Bundle args = new Bundle();
        args.putString(Watchlist.MOVIE_ID, movieId);
        fragment.setArguments(args);

        getSupportFragmentManager().beginTransaction().replace(R.id.movie_detail_container, fragment).commit();
    }

    @SuppressLint("CommitPrefEdits")
    private void loadMoviesOfType(int viewType) {
        SharedPreferences.Editor editor = getSharedPreferences(Watchlist.TABLE_USER, MODE_PRIVATE).edit();
        editor.putInt(Watchlist.LAST_SELECTED, viewType);
        editor.commit();
        startActivity(new Intent(this, MovieActivity.class));
        finish();
    }

}
