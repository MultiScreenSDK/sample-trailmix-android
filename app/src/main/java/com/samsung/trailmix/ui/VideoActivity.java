

package com.samsung.trailmix.ui;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.samsung.trailmix.R;
import com.samsung.trailmix.interceptor.AppCompatActivityMenuKeyInterceptor;


/**
 * Activity for view videos
 */
public class VideoActivity extends AppCompatActivity {

    private Toolbar toolbar;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_activity);

        //Initialize the interceptor
        AppCompatActivityMenuKeyInterceptor.intercept(this);

        //Add toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
//        toolbar.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        toolbar.setBackgroundColor(Color.GRAY);
        TextView textView = (TextView)toolbar.findViewById(R.id.appText);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        textView.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_connect:
                //When the S icon is clicked, opens the service list dialog.
//                showServiceListDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
