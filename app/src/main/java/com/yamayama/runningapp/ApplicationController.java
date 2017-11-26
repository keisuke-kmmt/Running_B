package com.yamayama.runningapp;

import android.app.Application;

/**
 * Created by keisuke on 2017/11/23.
 */

public class ApplicationController extends Application {
    private static ApplicationController instance = null;
    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
    }

    public static ApplicationController getInstance() {
        return instance;
    }
}
