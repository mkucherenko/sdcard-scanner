package com.mkucherenko.sdcard_scanner.ui.fragment;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.mkucherenko.sdcard_scanner.App;
import com.mkucherenko.sdcard_scanner.R;
import com.mkucherenko.sdcard_scanner.adapter.ScanResultsAdapter;
import com.mkucherenko.sdcard_scanner.model.ScanResults;
import com.mkucherenko.sdcard_scanner.service.ScanFilesService;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.observers.SafeSubscriber;
import rx.observers.Subscribers;
import rx.schedulers.Schedulers;

/**
 * Created by Zim on 4/25/2016.
 */
public class ScanFilesFragment extends Fragment{

    private RecyclerView mListView;
    private View mProgressBlock;
    private TextView mProgressTextView;
    private ProgressBar mProgressBar;
    private SafeSubscriber<ScanResults> mSafeSubscriber;
    private Observable<ScanResults> scanResultsObservable;
    private ShareActionProvider mShareActionProvider;
    private boolean mIsScanning = false;
    private ScanResultsAdapter mAdapter;
    private LocalBroadcastManager mLocalBroadcastManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mIsScanning = false;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.fragment_scan_files, container, false);
        mProgressBlock = view.findViewById(R.id.progressBlock);
        mListView = (RecyclerView)view.findViewById(R.id.result_list);
        mProgressTextView = (TextView)view.findViewById(R.id.progress_text_view);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        mLocalBroadcastManager.unregisterReceiver(mProgressReceiver);
    }

    public boolean isScanning(){
        return mIsScanning;
    }

    public void stopScan() {
        mIsScanning = false;
        mProgressBlock.setVisibility(View.GONE);
        if(mSafeSubscriber != null) {
            mSafeSubscriber.unsubscribe();
            mSafeSubscriber = null;
        }
    }

    public void startScan(){
        mIsScanning = true;
        scanResultsObservable = mBoundService.getFilesObservable()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());

        Subscriber<ScanResults> subscriber = Subscribers.create(new Action1<ScanResults>() {

            @Override
            public void call(ScanResults scanResults) {
                mIsScanning = false;
                updateScanResults(scanResults);
                setShareIntent(scanResults);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                mIsScanning = false;
                Log.e("Tag", "Failed to process files", throwable);
            }
        });
        mSafeSubscriber = new SafeSubscriber<>(subscriber);
        scanResultsObservable.subscribe(mSafeSubscriber);
        scanResultsObservable.cache();
        mProgressBlock.setVisibility(View.VISIBLE);
    }

    private void updateScanResults(ScanResults results){
        mProgressBlock.setVisibility(View.GONE);
        if(results == null){
            mListView.setAdapter(null);
        }else{
            mAdapter = new ScanResultsAdapter(getContext(), results);
            mListView.setAdapter(mAdapter);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(App.getInstance().getApplicationContext());
        mLocalBroadcastManager.registerReceiver(mProgressReceiver, new IntentFilter(ScanFilesService.ACTION_SCAN_STARTED));
        mLocalBroadcastManager.registerReceiver(mProgressReceiver, new IntentFilter(ScanFilesService.ACTION_SCAN_FINISHED));
        mLocalBroadcastManager.registerReceiver(mProgressReceiver, new IntentFilter(ScanFilesService.ACTION_SCAN_PROGRESS));

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        mListView.setLayoutManager(mLayoutManager);
        mListView.setAdapter(mAdapter);

        mProgressBlock.setVisibility(isScanning() ? View.VISIBLE : View.GONE);
        doBindService();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main_menu, menu);

        MenuItem shareMenuItem = menu.findItem(R.id.menu_item_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareMenuItem);
    }

    private void setShareIntent(ScanResults results){
        Intent shareIntent = new Intent(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_TEXT, new Gson().toJson(results))
                .setType("text/plain");
        mShareActionProvider.setShareIntent(shareIntent);
    }

    private ScanFilesService mBoundService;
    private boolean mIsBound;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((ScanFilesService.LocalBinder)service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
        }
    };

    void doBindService() {
        App.getInstance().getApplicationContext().bindService(new Intent(getActivity(), ScanFilesService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            App.getInstance().getApplicationContext().unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        doUnbindService();
        if(mSafeSubscriber != null) {
            mSafeSubscriber.unsubscribe();
        }
    }

    private BroadcastReceiver mProgressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(ScanFilesService.ACTION_SCAN_STARTED.equals(intent.getAction())){
                mProgressBar.setIndeterminate(true);
                mProgressBlock.setVisibility(View.VISIBLE);
                mProgressTextView.setText(R.string.label_obtaining_file_list);
            }else if(ScanFilesService.ACTION_SCAN_FINISHED.equals(intent.getAction())){
                mProgressBlock.setVisibility(View.GONE);
            }else if(ScanFilesService.ACTION_SCAN_PROGRESS.equals(intent.getAction())){
                int progress = intent.getIntExtra(ScanFilesService.EXTRA_PROGRESS, 0);
                mProgressTextView.setText(getString(R.string.label_processing_d, progress));
                mProgressBar.setIndeterminate(false);
                mProgressBar.setProgress(progress);
            }
        }
    };
}
