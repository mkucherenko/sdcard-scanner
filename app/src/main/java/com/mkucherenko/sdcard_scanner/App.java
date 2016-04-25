package com.mkucherenko.sdcard_scanner;

import android.app.Application;

/**
 * Created by Zim on 4/26/2016.
 */
public class App extends Application {

    private static App sInstance;

    public static App getInstance(){
        return sInstance;
    }

    public App(){
        super();
        sInstance = this;
    }
}
