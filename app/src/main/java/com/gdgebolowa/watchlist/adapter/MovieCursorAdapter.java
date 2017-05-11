package com.gdgebolowa.watchlist.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.gdgebolowa.watchlist.R;
import com.gdgebolowa.watchlist.Watchlist;
import com.gdgebolowa.watchlist.database.MovieColumns;
import com.gdgebolowa.watchlist.model.Movie;
import com.gdgebolowa.watchlist.util.ApiHelper;
import com.gdgebolowa.watchlist.util.TextUtils;
import com.gdgebolowa.watchlist.util.VolleySingleton;
import com.gdgebolowa.watchlist.widget.AutoResizeTextView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MovieCursorAdapter extends CursorRecyclerAdapter<ViewHolder> {

    private Context context;
    private int imageWidth;
    private SharedPreferences sharedPref;
    private final OnMovieClickListener onMovieClickListener;

    // Constructor
    public MovieCursorAdapter(Context context, OnMovieClickListener onMovieClickListener, Cursor cursor) {
        super(context, cursor);
        this.context = context;
        this.onMovieClickListener = onMovieClickListener;
        sharedPref = context.getSharedPreferences(Watchlist.TABLE_USER, Context.MODE_PRIVATE);
        imageWidth = sharedPref.getInt(Watchlist.THUMBNAIL_SIZE, 0);   // Load image width for grid view
    }

    // RecyclerView methods
    @Override
    public int getItemCount() {
        return super.getItemCount();
    }
    @Override
    public int getItemViewType(int position) {
        return (sharedPref.getInt(Watchlist.VIEW_MODE, Watchlist.VIEW_MODE_GRID));
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == Watchlist.VIEW_MODE_GRID) {
            // GRID MODE
            final ViewGroup v = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie_grid, parent, false);
            ViewTreeObserver viewTreeObserver = v.getViewTreeObserver();
            if (viewTreeObserver.isAlive()) {
                viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // Update width integer and save to storage for next use
                        int width = v.findViewById(R.id.movie_poster).getWidth();
                        if (width > imageWidth) {
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putInt(Watchlist.THUMBNAIL_SIZE, width);
                            editor.apply();
                        }
                        // Unregister LayoutListener
                        if (Build.VERSION.SDK_INT >= 16) {
                            v.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        } else {
                            v.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        }
                    }
                });
            }
            return new MovieGridViewHolder(v, onMovieClickListener);
        } else if (viewType == Watchlist.VIEW_MODE_LIST)  {
            // LIST MODE
            ViewGroup v = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie_list, parent, false);
            return new MovieListViewHolder(v, onMovieClickListener);
        } else {
            // COMPACT MODE
            ViewGroup v = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie_compact, parent, false);
            return new MovieCompactViewHolder(v, onMovieClickListener);
        }
    }
    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final Cursor cursor) {
        // Get data from cursor
        String id = cursor.getString(cursor.getColumnIndex(MovieColumns.TMDB_ID));
        String title = cursor.getString(cursor.getColumnIndex(MovieColumns.TITLE));
        String year = cursor.getString(cursor.getColumnIndex(MovieColumns.YEAR));
        String overview = cursor.getString(cursor.getColumnIndex(MovieColumns.OVERVIEW));
        String rating = cursor.getString(cursor.getColumnIndex(MovieColumns.RATING));
        String posterImage = cursor.getString(cursor.getColumnIndex(MovieColumns.POSTER));
        String backdropImage = cursor.getString(cursor.getColumnIndex(MovieColumns.BACKDROP));
        Movie movie = new Movie(id, title, year, overview, rating, posterImage, backdropImage);
        // Bind data to view
        int viewType = getItemViewType(0);
        if (viewType == Watchlist.VIEW_MODE_GRID) {
            // GRID MODE
            MovieGridViewHolder movieViewHolder = (MovieGridViewHolder) viewHolder;

            // Title and year
            movieViewHolder.movieName.setText(movie.title);
            movieViewHolder.releaseYear.setText(movie.year);
            // Load image
            if (!TextUtils.isNullOrEmpty(movie.backdropImage)) {
                String imageUrl = ApiHelper.getImageURL(movie.backdropImage, imageWidth);
                movieViewHolder.imageView.setImageUrl(imageUrl, VolleySingleton.getInstance(context).imageLoader);
                movieViewHolder.imageView.setVisibility(View.VISIBLE);
                movieViewHolder.defaultImageView.setVisibility(View.GONE);
            } else if (!TextUtils.isNullOrEmpty(movie.posterImage)) {
                String imageUrl = ApiHelper.getImageURL(movie.posterImage, imageWidth);
                movieViewHolder.imageView.setImageUrl(imageUrl, VolleySingleton.getInstance(context).imageLoader);
                movieViewHolder.imageView.setVisibility(View.VISIBLE);
                movieViewHolder.defaultImageView.setVisibility(View.GONE);
            } else {
                movieViewHolder.defaultImageView.setVisibility(View.VISIBLE);
                movieViewHolder.imageView.setVisibility(View.GONE);
            }
            // Display movie rating
            if (TextUtils.isNullOrEmpty(movie.rating) || movie.rating.equals("0")) {
                movieViewHolder.movieRatingIcon.setVisibility(View.GONE);
                movieViewHolder.movieRating.setVisibility(View.GONE);
            } else {
                movieViewHolder.movieRatingIcon.setVisibility(View.VISIBLE);
                movieViewHolder.movieRating.setVisibility(View.VISIBLE);
                movieViewHolder.movieRating.setText(movie.rating);
            }
        } else if (viewType == Watchlist.VIEW_MODE_LIST) {
            // LIST MODE
            MovieListViewHolder movieViewHolder = (MovieListViewHolder) viewHolder;

            // Title, year and overview
            movieViewHolder.movieName.setText(movie.title);
            movieViewHolder.releaseYear.setText(movie.year);
            movieViewHolder.overview.setText(movie.overview);
            // Load image
            if (TextUtils.isNullOrEmpty(movie.posterImage)) {
                movieViewHolder.imageView.setVisibility(View.GONE);
                movieViewHolder.defaultImageView.setVisibility(View.VISIBLE);
            } else {
                int imageSize = (int) context.getResources().getDimension(R.dimen.movie_list_poster_width);
                String imageUrl = ApiHelper.getImageURL(movie.posterImage, imageSize);
                movieViewHolder.imageView.setImageUrl(imageUrl, VolleySingleton.getInstance(context).imageLoader);
                movieViewHolder.imageView.setVisibility(View.VISIBLE);
                movieViewHolder.defaultImageView.setVisibility(View.GONE);
            }
            // Display movie rating
            if (TextUtils.isNullOrEmpty(movie.rating) || movie.rating.equals("0")) {
                movieViewHolder.movieRatingIcon.setVisibility(View.GONE);
                movieViewHolder.movieRating.setVisibility(View.GONE);
            } else {
                movieViewHolder.movieRatingIcon.setVisibility(View.VISIBLE);
                movieViewHolder.movieRating.setVisibility(View.VISIBLE);
                movieViewHolder.movieRating.setText(movie.rating);
            }
        } else {
            // COMPACT MODE
            final MovieCompactViewHolder movieViewHolder = (MovieCompactViewHolder) viewHolder;

            // Title and year
            movieViewHolder.movieName.setText(movie.title);
            movieViewHolder.movieYear.setText(movie.year);
            // Load image
            if (TextUtils.isNullOrEmpty(movie.backdropImage)) {
                movieViewHolder.movieImage.setImageResource(R.drawable.default_backdrop_circle);
            } else {
                int imageSize = (int) context.getResources().getDimension(R.dimen.movie_compact_image_size);
                String imageUrl = ApiHelper.getImageURL(movie.backdropImage, imageSize);
                VolleySingleton.getInstance(context).imageLoader.get(imageUrl, new ImageLoader.ImageListener() {
                    @Override
                    public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                        movieViewHolder.movieImage.setImageBitmap(response.getBitmap());
                    }
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        movieViewHolder.movieImage.setImageResource(R.drawable.default_backdrop_circle);
                    }
                });
            }
            // Display movie rating
            if (TextUtils.isNullOrEmpty(movie.rating) || movie.rating.equals("0")) {
                movieViewHolder.movieRatingIcon.setVisibility(View.GONE);
                movieViewHolder.movieRating.setVisibility(View.GONE);
            } else {
                movieViewHolder.movieRatingIcon.setVisibility(View.VISIBLE);
                movieViewHolder.movieRating.setVisibility(View.VISIBLE);
                movieViewHolder.movieRating.setText(movie.rating);
            }
        }
    }

    // ViewHolders
    public class MovieGridViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.movie_card)              CardView cardView;
        @BindView(R.id.movie_poster_default)    ImageView defaultImageView;
        @BindView(R.id.movie_poster)            NetworkImageView imageView;
        @BindView(R.id.movie_title)             TextView movieName;
        @BindView(R.id.movie_year)              TextView releaseYear;
        @BindView(R.id.movie_rating)            TextView movieRating;
        @BindView(R.id.rating_icon)             ImageView movieRatingIcon;

        public MovieGridViewHolder(final ViewGroup itemView, final OnMovieClickListener onMovieClickListener) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onMovieClickListener.onMovieClicked(getAdapterPosition());
                }
            });
        }
    }
    public class MovieListViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.movie_card)              CardView cardView;
        @BindView(R.id.movie_poster_default)    ImageView defaultImageView;
        @BindView(R.id.movie_poster)            NetworkImageView imageView;
        @BindView(R.id.movie_title)             TextView movieName;
        @BindView(R.id.movie_year)              TextView releaseYear;
        @BindView(R.id.movie_overview)          AutoResizeTextView overview;
        @BindView(R.id.movie_rating)            TextView movieRating;
        @BindView(R.id.rating_icon)             ImageView movieRatingIcon;

        public MovieListViewHolder(final ViewGroup itemView, final OnMovieClickListener onMovieClickListener) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onMovieClickListener.onMovieClicked(getAdapterPosition());
                }
            });
        }
    }
    public class MovieCompactViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.movie_item)              View movieItem;
        @BindView(R.id.movie_image)             ImageView movieImage;
        @BindView(R.id.movie_name)              TextView movieName;
        @BindView(R.id.movie_year)              TextView movieYear;
        @BindView(R.id.movie_rating)            TextView movieRating;
        @BindView(R.id.rating_icon)             ImageView movieRatingIcon;

        public MovieCompactViewHolder(final ViewGroup itemView, final OnMovieClickListener onMovieClickListener) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            movieItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onMovieClickListener.onMovieClicked(getAdapterPosition());
                }
            });
        }
    }

    // Click listener interface
    public interface OnMovieClickListener {
        void onMovieClicked(final int position);
    }
}
