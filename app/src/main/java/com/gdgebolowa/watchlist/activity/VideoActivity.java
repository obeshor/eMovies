package com.gdgebolowa.watchlist.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.gdgebolowa.watchlist.R;
import com.gdgebolowa.watchlist.Watchlist;
import com.gdgebolowa.watchlist.adapter.VideoAdapter;
import com.gdgebolowa.watchlist.adapter.VideoAdapter.OnVideoClickListener;
import com.gdgebolowa.watchlist.model.Video;
import com.gdgebolowa.watchlist.util.ApiHelper;
import com.gdgebolowa.watchlist.util.VolleySingleton;
import com.gdgebolowa.watchlist.util.YoutubeHelper;
import com.gdgebolowa.watchlist.widget.ItemPaddingDecoration;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.BindBool;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class VideoActivity extends AppCompatActivity implements OnVideoClickListener {

    private Tracker tracker;

    private String movieId;
    private VideoAdapter adapter;

    private boolean isLoading = false;
    @BindBool(R.bool.is_tablet) boolean isTablet;

    @BindView(R.id.toolbar)             Toolbar toolbar;
    @BindView(R.id.toolbar_title)       TextView toolbarTitle;
    @BindView(R.id.toolbar_subtitle)    TextView toolbarSubtitle;
    @BindView(R.id.video_list)          RecyclerView videoList;
    @BindView(R.id.error_message)       View errorMessage;
    @BindView(R.id.progress_circle)     View progressCircle;
    @BindView(R.id.no_results)          View noResults;
    @BindView(R.id.no_results_message)  TextView noResultsMessage;

    // Activity lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        ButterKnife.bind(this);

        movieId = getIntent().getStringExtra(Watchlist.MOVIE_ID);
        String movieName = getIntent().getStringExtra(Watchlist.MOVIE_NAME);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
        toolbarTitle.setText(R.string.videos_title);
        toolbarSubtitle.setText(movieName);

        final int numOfColumns = getNumberOfColumns();
        GridLayoutManager layoutManager = new GridLayoutManager(this, numOfColumns);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position == adapter.videoList.size()) {
                    return numOfColumns;
                } else {
                    return 1;
                }
            }
        });
        adapter = new VideoAdapter(this, new ArrayList<Video>(), this);
        videoList.setHasFixedSize(true);
        videoList.setLayoutManager(layoutManager);
        videoList.addItemDecoration(new ItemPaddingDecoration(this, R.dimen.dist_small));
        videoList.setAdapter(adapter);

        if (savedInstanceState == null) {
            downloadVideosList();
        }

        // Lock orientation for tablets
        if (isTablet) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        // Load Analytics Tracker
        tracker = ((Watchlist) getApplication()).getTracker();
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Send screen name to analytics
        tracker.setScreenName(getString(R.string.screen_movie_videos));
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
    }
    @Override
    protected void onStop() {
        super.onStop();
        VolleySingleton.getInstance(this).requestQueue.cancelAll(this.getClass().getName());
    }

    // Save/restore state
    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (adapter != null) {
            outState.putParcelableArrayList(Watchlist.VIDEO_LIST, adapter.videoList);
            outState.putBoolean(Watchlist.IS_LOADING, isLoading);
        }
        super.onSaveInstanceState(outState);
    }
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        adapter.videoList = savedInstanceState.getParcelableArrayList(Watchlist.VIDEO_LIST);
        isLoading = savedInstanceState.getBoolean(Watchlist.IS_LOADING);
        // If activity was previously downloading and it stopped, download again
        if (isLoading) {
            downloadVideosList();
        } else {
            onDownloadSuccessful();
        }
    }

    // Helper method
    public int getNumberOfColumns() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float widthPx = displayMetrics.widthPixels;
        float desiredPx = getResources().getDimensionPixelSize(R.dimen.video_item_width);
        int columns = Math.round(widthPx / desiredPx);
        if (columns <= 1) {
            return 1;
        } else {
            return columns;
        }
    }

    // Toolbar actions
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else {
            return false;
        }
    }

    // JSON parsing and display
    private void downloadVideosList() {
        isLoading = true;
        if (adapter == null) {
            adapter = new VideoAdapter(this, new ArrayList<Video>(), this);
            videoList.setAdapter(adapter);
        }
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, ApiHelper.getVideosLink(this, movieId), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject object) {
                        try {
                            JSONArray results = object.getJSONArray("results");
                            for (int i = 0; i < results.length(); i++) {
                                JSONObject vid = results.getJSONObject(i);
                                if (vid.getString("site").equals("YouTube")) {
                                    String title = vid.getString("name");
                                    String key = vid.getString("key");
                                    String subtitle = vid.getString("size") + "p";
                                    Video video = new Video(title, subtitle, key, YoutubeHelper.getThumbnailURL(key), YoutubeHelper.getVideoURL(key));
                                    adapter.videoList.add(video);
                                }
                            }
                            onDownloadSuccessful();
                        } catch (Exception ex) {
                            onDownloadFailed();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        onDownloadFailed();
                    }
                });
        request.setTag(getClass().getName());
        VolleySingleton.getInstance(this).requestQueue.add(request);
    }
    private void onDownloadSuccessful() {
        isLoading = false;
        if (adapter.videoList.size() == 0) {
            noResultsMessage.setText(R.string.videos_no_results);
            noResults.setVisibility(View.VISIBLE);
            errorMessage.setVisibility(View.GONE);
            progressCircle.setVisibility(View.GONE);
            videoList.setVisibility(View.GONE);
        } else {
            errorMessage.setVisibility(View.GONE);
            progressCircle.setVisibility(View.GONE);
            videoList.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }
    private void onDownloadFailed() {
        isLoading = false;
        errorMessage.setVisibility(View.VISIBLE);
        progressCircle.setVisibility(View.GONE);
        videoList.setVisibility(View.GONE);
    }

    // Click events
    @OnClick(R.id.try_again)
    public void onTryAgainClicked() {
        videoList.setVisibility(View.GONE);
        errorMessage.setVisibility(View.GONE);
        progressCircle.setVisibility(View.VISIBLE);
        adapter = null;
        downloadVideosList();
    }
    @Override
    public void onVideoClicked(int position) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + adapter.videoList.get(position).youtubeID));
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v="  + adapter.videoList.get(position).youtubeID));
            startActivity(intent);
        }
    }
}
