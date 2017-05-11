package com.gdgebolowa.watchlist.activity;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.gdgebolowa.watchlist.R;
import com.gdgebolowa.watchlist.Watchlist;
import com.gdgebolowa.watchlist.fragment.CreditFragment;

import butterknife.BindBool;
import butterknife.ButterKnife;

public class CreditActivity extends AppCompatActivity {

    @BindBool(R.bool.is_tablet) boolean isTablet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credit);
        ButterKnife.bind(this);

        if (savedInstanceState == null) {
            CreditFragment fragment = new CreditFragment();

            Bundle args = new Bundle();
            args.putInt(Watchlist.CREDIT_TYPE, getIntent().getIntExtra(Watchlist.CREDIT_TYPE, 0));
            args.putString(Watchlist.MOVIE_NAME, getIntent().getStringExtra(Watchlist.MOVIE_NAME));
            args.putParcelableArrayList(Watchlist.CREDIT_LIST, getIntent().getParcelableArrayListExtra(Watchlist.CREDIT_LIST));
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction().replace(R.id.credit_container, fragment).commit();

            if (isTablet) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }
    }

}
