package com.gdgebolowa.watchlist.util;

import android.content.Context;
import android.net.Uri;
import com.gdgebolowa.watchlist.R;

public class ApiHelper {

    // API key for TMDB
    public static String getTMDBKey(Context context) {
        return context.getString(R.string.tmdb_api_key);
    }
    public static String getTraktKey(Context context) {
        return context.getString(R.string.trakt_api_key);
    }

    // API Endpoints
    public static String getMostPopularMoviesLink(Context context, int page) {
        return "http://api.themoviedb.org/3/movie/popular?&page=" + page + "&api_key=" + getTMDBKey(context);
    }
    public static String getHighestRatedMoviesLink(Context context, int page) {
        return "http://api.themoviedb.org/3/movie/top_rated?&page=" + page + "&api_key=" + getTMDBKey(context);
    }
    public static String getUpcomingMoviesLink(Context context, int page) {
        return "http://api.themoviedb.org/3/movie/upcoming?&page=" + page + "&api_key=" + getTMDBKey(context);
    }
    public static String getNowPlayingMoviesLink(Context context, int page) {
        return "http://api.themoviedb.org/3/movie/now_playing?&page=" + page + "&api_key=" + getTMDBKey(context);
    }
    public static String getSearchMoviesLink(Context context, String query, int page) {
        return Uri.parse("http://api.themoviedb.org/3/search/movie")
                .buildUpon()
                .appendQueryParameter("api_key", getTMDBKey(context))
                .appendQueryParameter("query", query)
                .appendQueryParameter("page", page + "")
                .build().toString();
    }
    public static String getMovieDetailLink(Context context, String id) {
        return "http://api.themoviedb.org/3/movie/" + id + "?api_key=" + getTMDBKey(context) + "&append_to_response=credits,trailers";
    }
    public static String getMovieReviewsLink(String imdbId, int page) {
        return "https://api-v2launch.trakt.tv/movies/" + imdbId + "/comments/newest?page=" + page;

    }
    public static String getVideosLink(Context context, String id) {
        return "http://api.themoviedb.org/3/movie/" + id + "/videos?api_key=" + getTMDBKey(context);
    }
    public static String getPhotosLink(Context context, String id) {
        return "http://api.themoviedb.org/3/movie/" + id + "/images?api_key=" + getTMDBKey(context);
    }

    // Get image of the given pixel size
    public static String getImageURL(String baseURL, int widthPx) {
        if (widthPx > 500) {
            return "http://image.tmdb.org/t/p/w780/" + baseURL;
        } else if (widthPx > 342 && widthPx <= 500) {
            return "http://image.tmdb.org/t/p/w500/" + baseURL;
        } else if (widthPx > 185 && widthPx <= 342) {
            return "http://image.tmdb.org/t/p/w342/" + baseURL;
        } else if (widthPx > 154 && widthPx <= 185) {
            return "http://image.tmdb.org/t/p/w185/" + baseURL;
        } else if (widthPx > 92 && widthPx <= 154) {
            return "http://image.tmdb.org/t/p/w154/" + baseURL;
        } else if (widthPx > 0 && widthPx <= 92) {
            return "http://image.tmdb.org/t/p/w92/" + baseURL;
        } else {
            return "http://image.tmdb.org/t/p/w185/" + baseURL;     // Default Value
        }
    }
    public static String getOriginalImageURL(String baseURL) {
        return "http://image.tmdb.org/t/p/original/" + baseURL;
    }

    // URL to share the movie
    public static String getMovieShareURL(String id) {
        return "https://www.themoviedb.org/movie/" + id;
    }
}
