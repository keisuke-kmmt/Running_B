package com.yamayama.runningapp;

/**
 * Created by cyber on 2017/12/11.
 */

public class MySensorEvent {

    public final Float xValue;
    public final Float yValue;
    public final Float zValue;

    public MySensorEvent(Float x,Float y,Float z){
        this.xValue = x;
        this.yValue = y;
        this.zValue = z;
    }

}

