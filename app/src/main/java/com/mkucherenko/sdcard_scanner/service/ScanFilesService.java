package com.mkucherenko.sdcard_scanner.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.mkucherenko.sdcard_scanner.R;
import com.mkucherenko.sdcard_scanner.model.ScanResults;
import com.mkucherenko.sdcard_scanner.ui.MainActivity;
import com.mkucherenko.sdcard_scanner.utils.MapValueComparator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.comparator.SizeFileComparator;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;

public class ScanFilesService extends Service {

    public static final String ACTION_SCAN_STARTED = "com.mkucherenko.sdcard_scanner.ACTION_SCAN_STARTED";
    public static final String ACTION_SCAN_FINISHED = "com.mkucherenko.sdcard_scanner.ACTION_SCAN_FINISHED";
    public static final String ACTION_SCAN_PROGRESS = "com.mkucherenko.sdcard_scanner.ACTION_SCAN_PROGRESS";
    public static final String EXTRA_PROGRESS = "extra_progress";

    private static final int ID_NOTIFICATION = R.string.label_notification_title;
    private static final int LARGEST_FILE_COUNT = 10;
    private static final int EXT_COUNT = 5;

    private NotificationManager mNotificationManager;
    private Notification.Builder mNotificationBuilder;
    public LocalBroadcastManager mLocalBroadcastManager;
    private Intent mBroadcastIntent;

    public class LocalBinder extends Binder {
        public ScanFilesService getService() {
            return ScanFilesService.this;
        }
    }

    @Override
    public void onCreate() {
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mBroadcastIntent = new Intent();
    }

    private void sendBroadcast(String action){
        mBroadcastIntent.setAction(action);
        mLocalBroadcastManager.sendBroadcast(mBroadcastIntent);
    }

    private void sendProgressBroadcast(int progress){
        mBroadcastIntent.setAction(ACTION_SCAN_PROGRESS);
        mBroadcastIntent.putExtra(EXTRA_PROGRESS, progress);
        mLocalBroadcastManager.sendBroadcast(mBroadcastIntent);
    }

    public Observable<ScanResults> getFilesObservable(){
        Observable<List<File>> filesObservable = Observable.create(new Observable.OnSubscribe<List<File>>(){

            @Override
            public void call(Subscriber<? super List<File>> subscriber) {
                sendBroadcast(ACTION_SCAN_STARTED);
                mNotificationBuilder = prepareNotification();
                mNotificationBuilder.setContentText(getString(R.string.label_obtaining_file_list));
                showNotification(mNotificationBuilder);

                Collection<File> files = FileUtils.listFiles(
                        Environment.getExternalStorageDirectory(),
                        new RegexFileFilter("^(.*?)"),
                        DirectoryFileFilter.DIRECTORY
                );
                List<File> filesList = new ArrayList<>(files);
                Collections.sort(filesList, SizeFileComparator.SIZE_REVERSE);
                subscriber.onNext(filesList);
                subscriber.onCompleted();
            }
        });
        Observable<ScanResults> resultObservable = filesObservable.map(new Func1<List<File>, ScanResults>() {
            @Override
            public ScanResults call(List<File> filesList) {
                HashMap<String, Integer> mFilesExtension = new HashMap<>();
                long filesSize = 0;
                double avgFileSize = 0;
                int progress = 0;
                for(int index = 0; index < filesList.size(); index++){
                    File file = filesList.get(index);
                    int currentProgress = (int)(((double)index / filesList.size()) * 100);
                    if(currentProgress != progress) {
                        progress = currentProgress;
                        sendProgressBroadcast(progress);
                        mNotificationBuilder.setContentText(getString(R.string.label_processing));
                        mNotificationBuilder.setProgress(100, progress, false);
                        mNotificationBuilder.setNumber(progress);
                        showNotification(mNotificationBuilder);
                    }

                    filesSize = filesSize + file.length();
                    String ext = FilenameUtils.getExtension(file.getName()).toLowerCase();
                    if(mFilesExtension.containsKey(ext)){
                        int extCount = mFilesExtension.get(ext);
                        extCount = extCount + 1;
                        mFilesExtension.put(ext, extCount);
                    }else{
                        mFilesExtension.put(ext, 1);
                    }
                }
                if(filesList.size() > 0) {
                    avgFileSize = filesSize / filesList.size();
                }

                int largeFilesCount = filesList.size() > LARGEST_FILE_COUNT ? LARGEST_FILE_COUNT : filesList.size();
                List<File> largestFiles = filesList.subList(0, largeFilesCount);

                List<Map.Entry<String, Integer>> extList = new ArrayList<>(mFilesExtension.entrySet());
                Comparator<Map.Entry<String, Integer>> reverseComparator = Collections.<Map.Entry<String,Integer>>reverseOrder(new MapValueComparator<String, Integer>());
                Collections.sort(extList, reverseComparator);
                int extCount = extList.size() > EXT_COUNT ? EXT_COUNT : extList.size();
                extList = extList.subList(0, extCount);

                sendBroadcast(ACTION_SCAN_FINISHED);
                return new ScanResults(avgFileSize, largestFiles, extList);
            }
        });
        resultObservable = resultObservable.doOnCompleted(new Action0() {
            @Override
            public void call() {
                mNotificationManager.cancelAll();
            }
        });
        resultObservable = resultObservable.doOnUnsubscribe(new Action0() {
            @Override
            public void call() {
                mNotificationManager.cancelAll();
            }
        });
        return resultObservable;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        mNotificationManager.cancel(ID_NOTIFICATION);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    private final IBinder mBinder = new LocalBinder();

    private Notification.Builder prepareNotification() {
        CharSequence text = getText(R.string.label_notification_title);

        Intent launchActivityIntent = new Intent(this, MainActivity.class);
        launchActivityIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, launchActivityIntent, 0);

        Notification.Builder notificationBuilder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(text)
                .setOnlyAlertOnce(true)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(getText(R.string.label_notification_title))
                .setContentText(text)
                .setOngoing(true)
                .setContentIntent(contentIntent)
                .setProgress(0, 0, true);

        return notificationBuilder;
    }

    private void showNotification(Notification.Builder notificationBuilder){
        mNotificationManager.notify(ID_NOTIFICATION, notificationBuilder.getNotification());
    }
}