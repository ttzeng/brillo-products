package com.intel.otc.brillo.examples.demo.companion;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private OcClient ocClient;
    private ResourceAdapter mResourceAdapter;
    private ScrollView viewScroll;
    private TextView viewConsole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewScroll = (ScrollView) findViewById(R.id.scrollView);
        viewScroll.fullScroll(View.FOCUS_DOWN);
        viewConsole = (TextView) findViewById(R.id.consoleTextView);
        viewConsole.setMovementMethod(new ScrollingMovementMethod());

        RecyclerView viewResources = (RecyclerView)findViewById(R.id.resourceList);
        viewResources.setHasFixedSize(true);
        viewResources.setLayoutManager(new LinearLayoutManager(this));
        // Adapters provide a binding from an app-specific data set to views that are displayed within a RecyclerView
        mResourceAdapter = new ResourceAdapter(this);
        viewResources.setAdapter(mResourceAdapter);

        display("Configuring platform...");
        ocClient = new OcClient(this, mResourceAdapter);

        if (null == savedInstanceState) {
            // This app is started 1st time
        } else {
            viewConsole.setText(savedInstanceState.getString("consoleDump"));
        }
    }

    @Override
    protected void onResume() {
        // User returns to the activity
        Log.d(TAG, "onResume");
        super.onResume();

        // Finding Brightness resources
        findResource(CardBrightness.RESOURCE_TYPE);
        // Finding Audio Control resources
        findResource(CardAudioControl.RESOURCE_TYPE);
        // Finding vendor audio player resources
        findResource(CardMp3Player.RESOURCE_TYPE);
    }

    @Override
    protected void onPause() {
        // Another activity comes into the foreground
        Log.d(TAG, "onPause");
        mResourceAdapter.clear();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
        outState.putString("consoleDump", viewConsole.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        // Retain console output on screen orientation changed
        Log.d(TAG, "onRestoreInstanceState");
        super.onRestoreInstanceState(savedInstanceState);
        viewConsole.setText(savedInstanceState.getString("consoleDump"));
    }

    private void findResource(String ocResourceType) {
        display("Finding resource of type \"" + ocResourceType + "\"...");
        ocClient.findResources(ocResourceType);
    }

    public synchronized void display(final String text) {
        Log.i(TAG, text);
        viewConsole.append("\n" + text);
        viewScroll.fullScroll(View.FOCUS_DOWN);
    }
}
