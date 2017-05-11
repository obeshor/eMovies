package com.gdgebolowa.watchlist.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.gdgebolowa.watchlist.R;
import com.gdgebolowa.watchlist.model.Credit;
import com.gdgebolowa.watchlist.util.ApiHelper;
import com.gdgebolowa.watchlist.util.TextUtils;
import com.gdgebolowa.watchlist.util.VolleySingleton;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class CreditAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    public ArrayList<Credit> creditList;
    private OnCreditClickListener onCreditClickListener;

    // Constructor
    public CreditAdapter(Context context, ArrayList<Credit> creditList, OnCreditClickListener onCreditClickListener) {
        this.context = context;
        this.creditList = creditList;
        this.onCreditClickListener = onCreditClickListener;
    }

    // RecyclerView methods
    @Override
    public int getItemCount() {
        return creditList.size();
    }
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewGroup v = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_credit, parent, false);
        return new CreditViewHolder(v, onCreditClickListener);
    }
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        Credit credit = creditList.get(position);
        final CreditViewHolder holder = (CreditViewHolder) viewHolder;
        int imageSize = (int) context.getResources().getDimension(R.dimen.detail_cast_image_width);

        if (TextUtils.isNullOrEmpty(credit.imagePath)) {
            holder.creditImage.setImageResource(R.drawable.default_cast_square);
        } else {
            VolleySingleton.getInstance(context).imageLoader.get(ApiHelper.getImageURL(credit.imagePath, imageSize),
                    new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            holder.creditImage.setImageBitmap(imageContainer.getBitmap());
                        }
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            holder.creditImage.setImageResource(R.drawable.default_cast_square);
                        }
                    });
        }

        holder.creditName.setText(credit.name);
        if (TextUtils.isNullOrEmpty(credit.role)) {
            holder.creditRole.setVisibility(View.GONE);
        } else {
            holder.creditRole.setText(credit.role);
            holder.creditRole.setVisibility(View.VISIBLE);
        }
    }

    // ViewHolder
    public class CreditViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.credit_item)     View creditItem;
        @BindView(R.id.credit_image)    CircleImageView creditImage;
        @BindView(R.id.credit_name)     TextView creditName;
        @BindView(R.id.credit_role)     TextView creditRole;

        public CreditViewHolder(final ViewGroup itemView, final OnCreditClickListener onCreditClickListener) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            creditItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onCreditClickListener.onCreditClicked(getAdapterPosition());
                }
            });
        }
    }

    // Click listener interface
    public interface OnCreditClickListener {
        void onCreditClicked(final int position);
    }
}