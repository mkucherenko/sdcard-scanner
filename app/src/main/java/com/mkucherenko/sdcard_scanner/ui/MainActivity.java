package com.mkucherenko.sdcard_scanner.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.mkucherenko.sdcard_scanner.R;
import com.mkucherenko.sdcard_scanner.service.ScanFilesService;
import com.mkucherenko.sdcard_scanner.ui.fragment.ScanFilesFragment;

public class MainActivity extends AppCompatActivity {

    private FloatingActionButton mFab;
    private ScanFilesFragment mScanFragment;
    private LocalBroadcastManager mLocalBroadcastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        mFab = (FloatingActionButton) findViewById(R.id.fab);
        if (mFab != null) {
            mFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!mScanFragment.isScanning()){
                        startScan();
                    }else {
                        stopScan();
                    }
                }
            });
            if(savedInstanceState == null){
                mScanFragment = new ScanFilesFragment();
                getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, mScanFragment, "tag").commit();
            }else{
                mScanFragment = (ScanFilesFragment)(getSupportFragmentManager().findFragmentByTag("tag"));
            }
            if(!mScanFragment.isScanning()) {
                mFab.setImageResource(R.drawable.ic_scan);
            }else {
                mFab.setImageResource(R.drawable.ic_stop);
            }

        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLocalBroadcastManager.registerReceiver(mProgressReceiver, new IntentFilter(ScanFilesService.ACTION_SCAN_STARTED));
        mLocalBroadcastManager.registerReceiver(mProgressReceiver, new IntentFilter(ScanFilesService.ACTION_SCAN_FINISHED));
    }

    @Override
    protected void onStop() {
        super.onStop();
        mLocalBroadcastManager.unregisterReceiver(mProgressReceiver);
    }

    private void stopScan() {
        mFab.setImageResource(R.drawable.ic_scan);
        mScanFragment.stopScan();
    }

    private void startScan() {
        mFab.setImageResource(R.drawable.ic_stop);
        mScanFragment.startScan();
    }

    private BroadcastReceiver mProgressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(ScanFilesService.ACTION_SCAN_STARTED.equals(intent.getAction())){
                mFab.setImageResource(R.drawable.ic_stop);
            }else if(ScanFilesService.ACTION_SCAN_FINISHED.equals(intent.getAction())){
                mFab.setImageResource(R.drawable.ic_scan);
            }
        }
    };
}
