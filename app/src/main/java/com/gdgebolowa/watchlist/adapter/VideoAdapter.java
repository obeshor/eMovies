package com.gdgebolowa.watchlist.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.gdgebolowa.watchlist.R;
import com.gdgebolowa.watchlist.model.Video;
import com.gdgebolowa.watchlist.util.VolleySingleton;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class VideoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    public ArrayList<Video> videoList;
    private final OnVideoClickListener onVideoClickListener;

    // Constructor
    public VideoAdapter(Context context, ArrayList<Video> videoList, OnVideoClickListener onVideoClickListener) {
        this.context = context;
        this.videoList = videoList;
        this.onVideoClickListener = onVideoClickListener;
    }

    // RecyclerView methods
    @Override
    public int getItemCount() {
        return videoList.size();
    }
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewGroup v = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video, parent, false);
        return new VideoViewHolder(v, onVideoClickListener);
    }
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        Video video = videoList.get(position);
        VideoViewHolder holder = (VideoViewHolder) viewHolder;
        holder.videoImage.setImageUrl(video.imageURL, VolleySingleton.getInstance(context).imageLoader);
        holder.videoName.setText(video.title);
        holder.videoSize.setText(video.size);
    }

    // ViewHolder
    public class VideoViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.video_item)      View videoItem;
        @BindView(R.id.video_image)     NetworkImageView videoImage;
        @BindView(R.id.video_name)      TextView videoName;
        @BindView(R.id.video_size)      TextView videoSize;

        public VideoViewHolder(final ViewGroup itemView, final OnVideoClickListener onVideoClickListener) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            videoItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onVideoClickListener.onVideoClicked(getAdapterPosition());
                }
            });
        }
    }

    // Click listener interface
    public interface OnVideoClickListener {
        void onVideoClicked(final int position);
    }
}