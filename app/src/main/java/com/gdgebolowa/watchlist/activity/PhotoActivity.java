package com.gdgebolowa.watchlist.activity;

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
import com.gdgebolowa.watchlist.adapter.PhotoAdapter;
import com.gdgebolowa.watchlist.adapter.PhotoAdapter.OnPhotoClickListener;
import com.gdgebolowa.watchlist.util.ApiHelper;
import com.gdgebolowa.watchlist.util.VolleySingleton;
import com.gdgebolowa.watchlist.widget.ItemPaddingDecoration;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.BindBool;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PhotoActivity extends AppCompatActivity implements OnPhotoClickListener {

    private Tracker tracker;

    private String movieId;
    private PhotoAdapter adapter;

    private boolean isLoading = false;
    @BindBool(R.bool.is_tablet) boolean isTablet;

    @BindView(R.id.toolbar)             Toolbar toolbar;
    @BindView(R.id.toolbar_title)       TextView toolbarTitle;
    @BindView(R.id.toolbar_subtitle)    TextView toolbarSubtitle;
    @BindView(R.id.photo_list)          RecyclerView photoList;
    @BindView(R.id.error_message)       View errorMessage;
    @BindView(R.id.progress_circle)     View progressCircle;
    @BindView(R.id.no_results)          View noResults;
    @BindView(R.id.no_results_message)  TextView noResultsMessage;

    // Activity lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        ButterKnife.bind(this);

        movieId = getIntent().getStringExtra(Watchlist.MOVIE_ID);
        String movieName = getIntent().getStringExtra(Watchlist.MOVIE_NAME);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
        toolbarTitle.setText(R.string.photos_title);
        toolbarSubtitle.setText(movieName);

        GridLayoutManager layoutManager = new GridLayoutManager(this,getNumberOfColumns());
        adapter = new PhotoAdapter(this, new ArrayList<String>(), this);
        photoList.setHasFixedSize(true);
        photoList.setLayoutManager(layoutManager);
        photoList.addItemDecoration(new ItemPaddingDecoration(this, R.dimen.dist_xxsmall));
        photoList.setAdapter(adapter);

        if (savedInstanceState == null) {
            downloadPhotosList();
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
        tracker.setScreenName(getString(R.string.screen_movie_photos));
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
            outState.putStringArrayList(Watchlist.PHOTO_LIST, adapter.photoList);
            outState.putBoolean(Watchlist.IS_LOADING, isLoading);
        }
        super.onSaveInstanceState(outState);
    }
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        adapter.photoList = savedInstanceState.getStringArrayList(Watchlist.PHOTO_LIST);
        isLoading = savedInstanceState.getBoolean(Watchlist.IS_LOADING);
        // If activity was previously downloading and it stopped, download again
        if (isLoading) {
            downloadPhotosList();
        } else {
            onDownloadSuccessful();
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

    // Helper method
    public int getNumberOfColumns() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float widthPx = displayMetrics.widthPixels;
        float desiredPx = getResources().getDimensionPixelSize(R.dimen.photo_item_width);
        int columns = Math.round(widthPx / desiredPx);
        if (columns <= 1) {
            return 1;
        } else {
            return columns;
        }
    }

    // JSON parsing and display
    private void downloadPhotosList() {
        isLoading = true;
        if (adapter == null) {
            adapter = new PhotoAdapter(this, new ArrayList<String>(), this);
            photoList.setAdapter(adapter);
        }
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, ApiHelper.getPhotosLink(this, movieId), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject object) {
                        try {
                            JSONArray backdrops = object.getJSONArray("backdrops");
                            for (int i = 0; i < backdrops.length(); i++) {
                                adapter.photoList.add(backdrops.getJSONObject(i).getString("file_path"));
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
        if (adapter.photoList.size() == 0) {
            noResultsMessage.setText(R.string.photos_no_results);
            noResults.setVisibility(View.VISIBLE);
            errorMessage.setVisibility(View.GONE);
            progressCircle.setVisibility(View.GONE);
            photoList.setVisibility(View.GONE);
        } else {
            errorMessage.setVisibility(View.GONE);
            progressCircle.setVisibility(View.GONE);
            photoList.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }
    private void onDownloadFailed() {
        isLoading = false;
        errorMessage.setVisibility(View.VISIBLE);
        progressCircle.setVisibility(View.GONE);
        photoList.setVisibility(View.GONE);
    }

    // Click events
    @OnClick(R.id.try_again)
    public void onTryAgainClicked() {
        photoList.setVisibility(View.GONE);
        errorMessage.setVisibility(View.GONE);
        progressCircle.setVisibility(View.VISIBLE);
        adapter = null;
        downloadPhotosList();
    }
    @Override
    public void onPhotoClicked(int position) {
        String url = ApiHelper.getOriginalImageURL(adapter.photoList.get(position));
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }
}
