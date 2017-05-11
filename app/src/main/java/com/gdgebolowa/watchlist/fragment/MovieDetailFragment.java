package com.gdgebolowa.watchlist.fragment;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.Toolbar.OnMenuItemClickListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.gdgebolowa.watchlist.R;
import com.gdgebolowa.watchlist.Watchlist;
import com.gdgebolowa.watchlist.activity.CreditActivity;
import com.gdgebolowa.watchlist.activity.PhotoActivity;
import com.gdgebolowa.watchlist.activity.ReviewActivity;
import com.gdgebolowa.watchlist.activity.VideoActivity;
import com.gdgebolowa.watchlist.database.MovieColumns;
import com.gdgebolowa.watchlist.database.MovieProvider;
import com.gdgebolowa.watchlist.model.Credit;
import com.gdgebolowa.watchlist.model.MovieDetail;
import com.gdgebolowa.watchlist.util.ApiHelper;
import com.gdgebolowa.watchlist.util.TextUtils;
import com.gdgebolowa.watchlist.util.VolleySingleton;
import com.gdgebolowa.watchlist.util.YoutubeHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.BindBool;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class MovieDetailFragment extends Fragment implements OnMenuItemClickListener {

    private Tracker tracker;
    private Unbinder unbinder;

    private String id;
    private MovieDetail movie;
    private boolean isMovieWatched;
    private boolean isMovieToWatch;

    private boolean isVideoAvailable = false;
    @BindBool(R.bool.is_tablet) boolean isTablet;

    // Toolbar
    @BindView(R.id.toolbar)                 Toolbar toolbar;
    @BindView(R.id.toolbar_text_holder)     View toolbarTextHolder;
    @BindView(R.id.toolbar_title)           TextView toolbarTitle;
    @BindView(R.id.toolbar_subtitle)        TextView toolbarSubtitle;

    // Main views
    @BindView(R.id.progress_circle)         View progressCircle;
    @BindView(R.id.error_message)           View errorMessage;
    @BindView(R.id.movie_detail_holder)     NestedScrollView movieHolder;
    @BindView(R.id.fab_menu)                FloatingActionMenu floatingActionsMenu;
    @BindView(R.id.fab_watched)             FloatingActionButton watchedButton;
    @BindView(R.id.fab_to_see)              FloatingActionButton toWatchButton;

    // Image views
    @BindView(R.id.backdrop_image)          NetworkImageView backdropImage;
    @BindView(R.id.backdrop_image_default)  ImageView backdropImageDefault;
    @BindView(R.id.backdrop_play_button)    View backdropPlayButton;
    @BindView(R.id.poster_image)            NetworkImageView posterImage;
    @BindView(R.id.poster_image_default)    ImageView posterImageDefault;

    // Basic info
    @BindView(R.id.movie_title)             TextView movieTitle;
    @BindView(R.id.movie_subtitle)          TextView movieSubtitle;
    @BindView(R.id.movie_rating_holder)     View movieRatingHolder;
    @BindView(R.id.movie_rating)            TextView movieRating;
    @BindView(R.id.movie_vote_count)        TextView movieVoteCount;

    // Overview
    @BindView(R.id.movie_overview_holder)   View movieOverviewHolder;
    @BindView(R.id.movie_overview_value)    TextView movieOverviewValue;

    // Crew
    @BindView(R.id.movie_crew_holder)       View movieCrewHolder;
    @BindView(R.id.movie_crew_see_all)      View movieCrewSeeAllButton;
    @BindViews({R.id.movie_crew_value1, R.id.movie_crew_value2}) List<TextView> movieCrewValues;

    // Cast
    @BindView(R.id.movie_cast_holder)       View movieCastHolder;
    @BindView(R.id.movie_cast_see_all)      View movieCastSeeAllButton;
    @BindViews({R.id.movie_cast_item1, R.id.movie_cast_item2, R.id.movie_cast_item3}) List<View> movieCastItems;
    @BindViews({R.id.movie_cast_image1, R.id.movie_cast_image2, R.id.movie_cast_image3}) List<NetworkImageView> movieCastImages;
    @BindViews({R.id.movie_cast_name1, R.id.movie_cast_name2, R.id.movie_cast_name3}) List<TextView> movieCastNames;
    @BindViews({R.id.movie_cast_role1, R.id.movie_cast_role2, R.id.movie_cast_role3}) List<TextView> movieCastRoles;

    // Fragment lifecycle
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_movie_detail, container, false);
        unbinder = ButterKnife.bind(this, v);

        // Setup toolbar
        toolbar.setTitle(R.string.loading);
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

        // Download movie details if new instance, else restore from saved instance
        if (savedInstanceState == null || !(savedInstanceState.containsKey(Watchlist.MOVIE_ID)
                && savedInstanceState.containsKey(Watchlist.MOVIE_OBJECT))) {
            id = getArguments().getString(Watchlist.MOVIE_ID);
            if (TextUtils.isNullOrEmpty(id)) {
                progressCircle.setVisibility(View.GONE);
                toolbarTextHolder.setVisibility(View.GONE);
                toolbar.setTitle("");
            } else {
                downloadMovieDetails(id);
            }
        } else {
            id = savedInstanceState.getString(Watchlist.MOVIE_ID);
            movie = savedInstanceState.getParcelable(Watchlist.MOVIE_OBJECT);
            onDownloadSuccessful();
        }

        // Setup FAB
        updateFABs();
        movieHolder.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (oldScrollY < scrollY) {
                    floatingActionsMenu.hideMenuButton(true);
                } else {
                    floatingActionsMenu.showMenuButton(true);
                }
            }
        });

        // Load Analytics Tracker
        tracker = ((Watchlist) getActivity().getApplication()).getTracker();

        return v;
    }
    @Override
    public void onResume() {
        super.onResume();
        // Send screen name to analytics
        tracker.setScreenName(getString(R.string.screen_movie_detail));
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (movie != null && id != null) {
            outState.putString(Watchlist.MOVIE_ID, id);
            outState.putParcelable(Watchlist.MOVIE_OBJECT, movie);
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        VolleySingleton.getInstance(getActivity()).requestQueue.cancelAll(this.getClass().getName());
        unbinder.unbind();
    }

    // Toolbar menu click
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_share) {
            if (movie != null) {
                // Share the movie
                String shareText = getString(R.string.action_share_text, movie.title, ApiHelper.getMovieShareURL(movie.id));
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, movie.title);
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareText);
                startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.action_share_using)));
                // Send event to Google Analytics
                tracker.send(new HitBuilders.EventBuilder()
                        .setCategory(getString(R.string.ga_category_share))
                        .setAction(getString(R.string.ga_action_movie))
                        .setLabel(movie.title)
                        .build());
            }
            return true;
        } else {
            return false;
        }
    }

    // JSON parsing and display
    private void downloadMovieDetails(String id) {
        String urlToDownload = ApiHelper.getMovieDetailLink(getActivity(), id);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, urlToDownload, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        try {
                            String backdropImage = jsonObject.getString("backdrop_path");
                            String id = jsonObject.getString("id");
                            String imdbId = jsonObject.getString("imdb_id");
                            String overview = jsonObject.getString("overview");
                            String posterImage = jsonObject.getString("poster_path");
                            String releaseDate = jsonObject.getString("release_date");
                            String runtime = jsonObject.getString("runtime");
                            String tagline = jsonObject.getString("tagline");
                            String title = jsonObject.getString("title");
                            String voteAverage = jsonObject.getString("vote_average");
                            String voteCount = jsonObject.getString("vote_count");
                            ArrayList<Credit> cast = new ArrayList<>();
                            JSONArray castArray = jsonObject.getJSONObject("credits").getJSONArray("cast");
                            for (int i = 0; i < castArray.length(); i++) {
                                JSONObject object = (JSONObject) castArray.get(i);
                                String role = object.getString("character");
                                String person_id = object.getString("id");
                                String name = object.getString("name");
                                String profileImage = object.getString("profile_path");
                                cast.add(new Credit(person_id, name, role, profileImage));
                            }
                            ArrayList<Credit> crew = new ArrayList<>();
                            JSONArray crewArray = jsonObject.getJSONObject("credits").getJSONArray("crew");
                            for (int i = 0; i < crewArray.length(); i++) {
                                JSONObject object = (JSONObject) crewArray.get(i);
                                String person_id = object.getString("id");
                                String role = object.getString("job");
                                String name = object.getString("name");
                                String profileImage = object.getString("profile_path");
                                crew.add(new Credit(person_id, name, role, profileImage));
                            }
                            String video = "";
                            JSONArray videoArray = jsonObject.getJSONObject("trailers").getJSONArray("youtube");
                            if (videoArray.length() > 0) {
                                video = videoArray.getJSONObject(0).getString("source");
                            }

                            movie = new MovieDetail(id, imdbId, title, tagline, releaseDate, runtime, overview, voteAverage,
                                    voteCount, backdropImage, posterImage, video, cast, crew);

                            onDownloadSuccessful();

                        } catch (Exception ex) {
                            // Parsing error
                            onDownloadFailed();
                            Log.d("Parse Error", ex.getMessage(), ex);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        // Network error
                        onDownloadFailed();
                    }
                });
        request.setTag(this.getClass().getName());
        VolleySingleton.getInstance(getActivity()).requestQueue.add(request);
    }
    private void onDownloadSuccessful() {

        // Toggle visibility
        progressCircle.setVisibility(View.GONE);
        errorMessage.setVisibility(View.GONE);
        movieHolder.setVisibility(View.VISIBLE);
        floatingActionsMenu.setVisibility(View.VISIBLE);

        // Set title and tagline
        if (TextUtils.isNullOrEmpty(movie.tagline)) {
            toolbar.setTitle(movie.title);
            toolbarTextHolder.setVisibility(View.GONE);
        } else {
            toolbar.setTitle("");
            toolbarTextHolder.setVisibility(View.VISIBLE);
            toolbarTitle.setText(movie.title);
            toolbarSubtitle.setText(movie.tagline);
        }

        // Add share button to toolbar
        toolbar.inflateMenu(R.menu.menu_share);

        // Backdrop image
        if (!TextUtils.isNullOrEmpty(movie.backdropImage)) {
            int headerImageWidth = (int) getResources().getDimension(R.dimen.detail_backdrop_width);
            backdropImage.setImageUrl(ApiHelper.getImageURL(movie.backdropImage, headerImageWidth),
                    VolleySingleton.getInstance(getActivity()).imageLoader);
            if (movie.video.length() == 0) {
                isVideoAvailable = false;
            } else {
                backdropPlayButton.setVisibility(View.VISIBLE);
                isVideoAvailable = true;
            }
        } else {
            if (movie.video.length() == 0) {
                backdropImage.setVisibility(View.GONE);
                backdropImageDefault.setVisibility(View.VISIBLE);
                isVideoAvailable = false;
            } else {
                backdropImage.setImageUrl(YoutubeHelper.getThumbnailURL(movie.video),
                        VolleySingleton.getInstance(getActivity()).imageLoader);
                backdropPlayButton.setVisibility(View.VISIBLE);
                isVideoAvailable = true;
            }
        }

        // Basic info
        if (!TextUtils.isNullOrEmpty(movie.posterImage)) {
            int posterImageWidth = (int) getResources().getDimension(R.dimen.movie_list_poster_width);
            posterImage.setImageUrl(ApiHelper.getImageURL(movie.posterImage, posterImageWidth),
                    VolleySingleton.getInstance(getActivity()).imageLoader);
        } else {
            posterImageDefault.setVisibility(View.VISIBLE);
            posterImage.setVisibility(View.GONE);
        }
        movieTitle.setText(movie.title);
        movieSubtitle.setText(movie.getSubtitle());
        if (TextUtils.isNullOrEmpty(movie.voteAverage) || movie.voteAverage.equals("0.0")) {
            movieRatingHolder.setVisibility(View.GONE);
        } else {
            movieRating.setText(movie.voteAverage);
            movieVoteCount.setText(getString(R.string.detail_vote_count, movie.voteCount));
        }

        // Overview
        if (TextUtils.isNullOrEmpty(movie.overview)) {
            movieOverviewHolder.setVisibility(View.GONE);
        } else {
            movieOverviewValue.setText(movie.overview);
        }

        // Crew
        if (movie.crew.size() == 0) {
            movieCrewHolder.setVisibility(View.GONE);
        } else if (movie.crew.size() == 1) {
            // Set value
            movieCrewValues.get(0).setText(getString(R.string.detail_crew_format, movie.crew.get(0).role, movie.crew.get(0).name));
            // Hide views
            movieCrewValues.get(1).setVisibility(View.GONE);
            movieCrewSeeAllButton.setVisibility(View.GONE);
            // Fix padding
            int padding = getResources().getDimensionPixelSize(R.dimen.dist_large);
            movieCrewHolder.setPadding(padding, padding, padding, padding);
        } else if (movie.crew.size() >= 2) {
            // Set values
            movieCrewValues.get(0).setText(getString(R.string.detail_crew_format, movie.crew.get(0).role, movie.crew.get(0).name));
            movieCrewValues.get(1).setText(getString(R.string.detail_crew_format, movie.crew.get(1).role, movie.crew.get(1).name));
            // Hide views
            if (movie.crew.size() == 2) {
                int padding = getResources().getDimensionPixelSize(R.dimen.dist_large);
                movieCrewHolder.setPadding(padding, padding, padding, padding);
                movieCrewSeeAllButton.setVisibility(View.GONE);
            }
        }

        // Cast
        if (movie.cast.size() == 0) {
            movieCastHolder.setVisibility(View.GONE);
        } else if (movie.cast.size() == 1) {
            int castImageWidth = (int) getResources().getDimension(R.dimen.detail_cast_image_width);
            // 0
            movieCastImages.get(0).setDefaultImageResId(R.drawable.default_cast);
            movieCastImages.get(0).setImageUrl(ApiHelper.getImageURL(movie.cast.get(0).imagePath, castImageWidth),
                    VolleySingleton.getInstance(getActivity()).imageLoader);
            movieCastNames.get(0).setText(movie.cast.get(0).name);
            movieCastRoles.get(0).setText(movie.cast.get(0).role);
            // Hide views
            movieCastSeeAllButton.setVisibility(View.GONE);
            movieCastItems.get(2).setVisibility(View.GONE);
            movieCastItems.get(1).setVisibility(View.GONE);
            // Fix padding
            int padding = getResources().getDimensionPixelSize(R.dimen.dist_large);
            movieCastHolder.setPadding(padding, padding, padding, padding);
        } else if (movie.cast.size() == 2) {
            int castImageWidth = (int) getResources().getDimension(R.dimen.detail_cast_image_width);
            // 1
            movieCastImages.get(1).setDefaultImageResId(R.drawable.default_cast);
            movieCastImages.get(1).setImageUrl(ApiHelper.getImageURL(movie.cast.get(1).imagePath, castImageWidth),
                    VolleySingleton.getInstance(getActivity()).imageLoader);
            movieCastNames.get(1).setText(movie.cast.get(1).name);
            movieCastRoles.get(1).setText(movie.cast.get(1).role);
            // 0
            movieCastImages.get(0).setDefaultImageResId(R.drawable.default_cast);
            movieCastImages.get(0).setImageUrl(ApiHelper.getImageURL(movie.cast.get(0).imagePath, castImageWidth),
                    VolleySingleton.getInstance(getActivity()).imageLoader);
            movieCastNames.get(0).setText(movie.cast.get(0).name);
            movieCastRoles.get(0).setText(movie.cast.get(0).role);
            // Hide views
            movieCastSeeAllButton.setVisibility(View.GONE);
            movieCastItems.get(2).setVisibility(View.GONE);
            // Fix padding
            int padding = getResources().getDimensionPixelSize(R.dimen.dist_large);
            movieCastHolder.setPadding(padding, padding, padding, padding);
        } else if (movie.cast.size() >= 3) {
            int castImageWidth = (int) getResources().getDimension(R.dimen.detail_cast_image_width);
            // 2
            movieCastImages.get(2).setDefaultImageResId(R.drawable.default_cast);
            movieCastImages.get(2).setImageUrl(ApiHelper.getImageURL(movie.cast.get(2).imagePath, castImageWidth),
                    VolleySingleton.getInstance(getActivity()).imageLoader);
            movieCastNames.get(2).setText(movie.cast.get(2).name);
            movieCastRoles.get(2).setText(movie.cast.get(2).role);
            // 1
            movieCastImages.get(1).setDefaultImageResId(R.drawable.default_cast);
            movieCastImages.get(1).setImageUrl(ApiHelper.getImageURL(movie.cast.get(1).imagePath, castImageWidth),
                    VolleySingleton.getInstance(getActivity()).imageLoader);
            movieCastNames.get(1).setText(movie.cast.get(1).name);
            movieCastRoles.get(1).setText(movie.cast.get(1).role);
            // 0
            movieCastImages.get(0).setDefaultImageResId(R.drawable.default_cast);
            movieCastImages.get(0).setImageUrl(ApiHelper.getImageURL(movie.cast.get(0).imagePath, castImageWidth),
                    VolleySingleton.getInstance(getActivity()).imageLoader);
            movieCastNames.get(0).setText(movie.cast.get(0).name);
            movieCastRoles.get(0).setText(movie.cast.get(0).role);
            // Hide show all button
            if (movie.cast.size() == 3) {
                int padding = getResources().getDimensionPixelSize(R.dimen.dist_large);
                movieCastHolder.setPadding(padding, padding, padding, padding);
                movieCastSeeAllButton.setVisibility(View.GONE);
            }
        }
    }
    private void onDownloadFailed() {
        errorMessage.setVisibility(View.VISIBLE);
        progressCircle.setVisibility(View.GONE);
        movieHolder.setVisibility(View.GONE);
        toolbarTextHolder.setVisibility(View.GONE);
        toolbar.setTitle("");
    }

    // Click events
    @OnClick(R.id.button_photos)
    public void onPhotosButtonClicked() {
        Intent intent = new Intent(getContext(), PhotoActivity.class);
        intent.putExtra(Watchlist.MOVIE_ID, movie.id);
        intent.putExtra(Watchlist.MOVIE_NAME, movie.title);
        startActivity(intent);
    }
    @OnClick(R.id.button_reviews)
    public void onReviewsButtonClicked() {
        Intent intent = new Intent(getContext(), ReviewActivity.class);
        intent.putExtra(Watchlist.MOVIE_ID, movie.imdbId);
        intent.putExtra(Watchlist.MOVIE_NAME, movie.title);
        startActivity(intent);
    }
    @OnClick(R.id.button_videos)
    public void onVideosButtonClicked() {
        Intent intent = new Intent(getContext(), VideoActivity.class);
        intent.putExtra(Watchlist.MOVIE_ID, movie.id);
        intent.putExtra(Watchlist.MOVIE_NAME, movie.title);
        startActivity(intent);
    }
    @OnClick(R.id.try_again)
    public void onTryAgainClicked() {
        errorMessage.setVisibility(View.GONE);
        progressCircle.setVisibility(View.VISIBLE);
        downloadMovieDetails(id);
    }
    @OnClick(R.id.backdrop_play_button)
    public void onTrailedPlayClicked() {
        if (isVideoAvailable) {
            try{
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + movie.video));
                startActivity(intent);
            } catch (ActivityNotFoundException ex) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=" + movie.video));
                startActivity(intent);
            }
        }
    }
    @OnClick(R.id.movie_crew_see_all)
    public void onSeeAllCrewClicked() {
        Intent intent = new Intent(getContext(), CreditActivity.class);
        intent.putExtra(Watchlist.CREDIT_TYPE, Watchlist.CREDIT_TYPE_CREW);
        intent.putExtra(Watchlist.MOVIE_NAME, movie.title);
        intent.putExtra(Watchlist.CREDIT_LIST, movie.crew);
        startActivity(intent);
    }
    @OnClick(R.id.movie_cast_see_all)
    public void onSeeAllCastClicked() {
        Intent intent = new Intent(getContext(), CreditActivity.class);
        intent.putExtra(Watchlist.CREDIT_TYPE, Watchlist.CREDIT_TYPE_CAST);
        intent.putExtra(Watchlist.MOVIE_NAME, movie.title);
        intent.putExtra(Watchlist.CREDIT_LIST, movie.cast);
        startActivity(intent);
    }
    @OnClick(R.id.movie_cast_item1)
    public void onFirstCastItemClicked() {
        // TODO
    }
    @OnClick(R.id.movie_cast_item2)
    public void onSecondCastItemClicked() {
        // TODO
    }
    @OnClick(R.id.movie_cast_item3)
    public void onThirdCastItemClicked() {
        // TODO
    }

    // FAB related functions
    private void updateFABs() {
        final String movieId = id;
        // Look in WATCHED table
        getLoaderManager().initLoader(42, null, new LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                return new CursorLoader(getContext(),
                        MovieProvider.Watched.CONTENT_URI,
                        new String[]{ },
                        MovieColumns.TMDB_ID + " = '" + movieId + "'",
                        null, null);
            }
            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                if (data.getCount() > 0) {
                    isMovieWatched = true;
                    watchedButton.setLabelText(getString(R.string.detail_fab_watched_remove));
                    toWatchButton.setVisibility(View.GONE);
                } else {
                    isMovieWatched = false;
                    watchedButton.setLabelText(getString(R.string.detail_fab_watched_add));
                    toWatchButton.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
            }
        });
        // Look in TO_SEE table
        getLoaderManager().initLoader(43, null, new LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                return new CursorLoader(getContext(),
                        MovieProvider.ToSee.CONTENT_URI,
                        new String[]{ },
                        MovieColumns.TMDB_ID + " = '" + movieId + "'",
                        null, null);
            }
            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                if (data.getCount() > 0) {
                    isMovieToWatch = true;
                    toWatchButton.setLabelText(getString(R.string.detail_fab_to_watch_remove));
                } else {
                    isMovieToWatch = false;
                    toWatchButton.setLabelText(getString(R.string.detail_fab_to_watch_add));
                }
            }
            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
            }
        });
    }
    @OnClick(R.id.fab_watched)
    public void onWatchedButtonClicked() {
        if (!isMovieWatched) {
            // Add movie to WATCHED table
            ContentValues values = new ContentValues();
            values.put(MovieColumns.TMDB_ID, movie.id);
            values.put(MovieColumns.TITLE, movie.title);
            values.put(MovieColumns.YEAR, movie.getYear());
            values.put(MovieColumns.OVERVIEW, movie.overview);
            values.put(MovieColumns.RATING, movie.voteAverage);
            values.put(MovieColumns.POSTER, movie.posterImage);
            values.put(MovieColumns.BACKDROP, movie.backdropImage);
            getContext().getContentResolver().insert(MovieProvider.Watched.CONTENT_URI, values);
            Toast.makeText(getContext(), R.string.detail_watched_added, Toast.LENGTH_SHORT).show();
            // Send added event to Google Analytics
            tracker.send(new HitBuilders.EventBuilder()
                    .setCategory(getString(R.string.ga_category_add))
                    .setAction(getString(R.string.ga_action_watched))
                    .setLabel(movie.title)
                    .build());
            // Remove from "TO_SEE" table
            if (isMovieToWatch) {
                getContext().getContentResolver().
                        delete(MovieProvider.ToSee.CONTENT_URI,
                        MovieColumns.TMDB_ID + " = '" + id + "'",
                        null);
                // Send removed event to Google Analytics
                tracker.send(new HitBuilders.EventBuilder()
                        .setCategory(getString(R.string.ga_category_remove))
                        .setAction(getString(R.string.ga_action_to_watch))
                        .setLabel(movie.title)
                        .build());
            }
        } else {
            // Remove from WATCHED table
            getContext().getContentResolver().
                    delete(MovieProvider.Watched.CONTENT_URI,
                            MovieColumns.TMDB_ID + " = '" + id + "'",
                            null);
            Toast.makeText(getContext(), R.string.detail_watched_removed, Toast.LENGTH_SHORT).show();
            // Send removed event to Google Analytics
            tracker.send(new HitBuilders.EventBuilder()
                    .setCategory(getString(R.string.ga_category_remove))
                    .setAction(getString(R.string.ga_action_watched))
                    .setLabel(movie.title)
                    .build());
        }
        // Update FABs
        updateFABs();
    }
    @OnClick(R.id.fab_to_see)
    public void onToWatchButtonClicked() {
        if (!isMovieToWatch) {
            // Add movie to "TO SEE" table
            ContentValues values = new ContentValues();
            values.put(MovieColumns.TMDB_ID, movie.id);
            values.put(MovieColumns.TITLE, movie.title);
            values.put(MovieColumns.YEAR, movie.getYear());
            values.put(MovieColumns.OVERVIEW, movie.overview);
            values.put(MovieColumns.RATING, movie.voteAverage);
            values.put(MovieColumns.POSTER, movie.posterImage);
            values.put(MovieColumns.BACKDROP, movie.backdropImage);
            getContext().getContentResolver().insert(MovieProvider.ToSee.CONTENT_URI, values);
            Toast.makeText(getContext(), R.string.detail_to_watch_added, Toast.LENGTH_SHORT).show();
            // Send added event to Google Analytics
            tracker.send(new HitBuilders.EventBuilder()
                    .setCategory(getString(R.string.ga_category_add))
                    .setAction(getString(R.string.ga_action_to_watch))
                    .setLabel(movie.title)
                    .build());
        } else {
            // Remove from "TO SEE" table
            getContext().getContentResolver().
                    delete(MovieProvider.ToSee.CONTENT_URI,
                            MovieColumns.TMDB_ID + " = '" + id + "'",
                            null);
            Toast.makeText(getContext(), R.string.detail_to_watch_removed, Toast.LENGTH_SHORT).show();
            // Send removed event to Google Analytics
            tracker.send(new HitBuilders.EventBuilder()
                    .setCategory(getString(R.string.ga_category_remove))
                    .setAction(getString(R.string.ga_action_to_watch))
                    .setLabel(movie.title)
                    .build());
        }
        // Update FABs
        updateFABs();
    }
}