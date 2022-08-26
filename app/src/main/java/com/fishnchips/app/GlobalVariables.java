package com.fishnchips.app;

import android.app.Application;
import android.content.Context;

public class GlobalVariables extends Application {
    // Reference: https://stackoverflow.com/a/9445685/8819252

    private static GlobalVariables instance;

    public static GlobalVariables getInstance() {
        return instance;
    }

    public static Context getContext(){
//        return instance;
        return instance.getApplicationContext();
    }

    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
    }
}
