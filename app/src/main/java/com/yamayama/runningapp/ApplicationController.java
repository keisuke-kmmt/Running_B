package com.yamayama.runningapp;

import android.app.Application;

import java.util.ArrayList;

/**
 * Created by keisuke Watanabe on 2017/11/23.
 */

public class ApplicationController extends Application {
    private static ApplicationController instance = null;

    /*グローバル変数(こんなことしていいのか？笑)*/



    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
    }

    public static ApplicationController getInstance() {
        return instance;
    }
}
