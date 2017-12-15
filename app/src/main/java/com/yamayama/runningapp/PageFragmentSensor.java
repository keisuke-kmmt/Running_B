package com.yamayama.runningapp;

import android.graphics.Color;
import android.hardware.Sensor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by cyber on 2017/12/10.
 */

public class PageFragmentSensor extends Fragment{

    //テキスト関係
    TextView xTextView;
    TextView yTextView;
    TextView zTextView;
    TextView  GpsTextView;

    //グラフ関係
    String[] names = new String[]{"x-value", "y-value", "z-value"};
    int[] colors = new int[]{Color.RED, Color.GREEN, Color.BLUE};
    private LineChart mChart;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_sensor, container, false);

        //テキストの紐付け
        xTextView = view.findViewById(R.id.xvalue);
        yTextView = view.findViewById(R.id.yvalue);
        zTextView = view.findViewById(R.id.zvalue);

        GpsTextView = view.findViewById(R.id.Gps);


        //グラフの初期化??定義
        mChart = (LineChart) view.findViewById(R.id.lineChart);
        Description description = new Description();
        description.setTextColor(ColorTemplate.VORDIPLOM_COLORS[2]);
        description.setText("");
        mChart.setDescription(description);
        mChart.setData(new LineData());  //空のLineData型インスタンスを追加


        //ボタンの紐付け
        final Button recordStart = (Button) view.findViewById(R.id.record_start);
        recordStart.setOnClickListener(buttonClick);

        final Button recordStop = (Button) view.findViewById(R.id.record_stop);
        recordStop.setOnClickListener(buttonClick);

        return view;
    }

    @Override
    public void onStart(){
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop(){
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Subscribe
    public void onMessageEvent(MessageEvent event){
        //do something
        Log.d("debug",event.message);
    }

    @Subscribe
    public void onMySensorEvent(MySensorEvent event){
        //センサーの値がpostされたとき
        xTextView.setText("X:" + String.format("%.4f",event.xValue));
        yTextView.setText("Y:" + String.format("%.4f",event.yValue));
        zTextView.setText("Z:" + String.format("%.4f",event.zValue));

        //グラフ関係処理
        LineData data = mChart.getLineData();
        if(data != null){
            Float sensordata[] = new Float[3];
            sensordata[0] = event.xValue;
            sensordata[1] = event.yValue;
            sensordata[2] = event.zValue;

            for(int i = 0;i < 3;i++){
                ILineDataSet set = data.getDataSetByIndex(i);
                if(set == null){
                    set = createSet(names[i],colors[i]); ///初期化メゾットは作った
                    data.addDataSet(set);
                }

                data.addEntry(new Entry(set.getEntryCount(), sensordata[i]), i);
                data.notifyDataChanged();
            }
            mChart.notifyDataSetChanged();   //表示更新の通知
            mChart.setVisibleXRangeMaximum(50);//表示幅の決定
            mChart.moveViewToX(data.getEntryCount());//最新データまで移動
        }
    }

    private LineDataSet createSet(String label, int color){
        LineDataSet set = new LineDataSet(null, label);
        set.setLineWidth(2.5f);
        set.setColor(color);
        set.setDrawCircles(false); //グラフのポイント点
        set.setDrawValues(false);  //値を表示しない

        return set;
    }

    //クリックした時に動作する関数
    private View.OnClickListener buttonClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.record_start:
                    Log.d("debug","start, Perform action on click");
                    EventBus.getDefault().post(new ButtonEvent("start"));

                    break;
                case R.id.record_stop:
                    Log.d("debug","stop, Perform action on click");
                    EventBus.getDefault().post(new ButtonEvent("stop"));
                    break;
            }
        }
    };

    //GPSの値を受信したとき
    @Subscribe
    public void onGPSEvent(GPSEvent event){
        GpsTextView.setText("lat:" + String.format("%.4f",event.Latitude) + "long:" + String.format("%.4f",event.Longitude));
    }

}
