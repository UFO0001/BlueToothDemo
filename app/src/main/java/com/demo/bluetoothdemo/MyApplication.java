package com.demo.bluetoothdemo;

import android.app.Application;

/**
 * Created by THF on 17/4/6.
 */

public class MyApplication extends Application {
    private static MyApplication mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }
    public static MyApplication getInstance(){
        return mInstance;
    }
}
