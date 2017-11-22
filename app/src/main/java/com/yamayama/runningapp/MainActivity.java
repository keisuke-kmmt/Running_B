package com.yamayama.runningapp;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.annotation.IdRes;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static com.yamayama.runningapp.R.id.view;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //センサー関係
    SensorManager manager;
    Sensor sensor;
    TextView xTextView;
    TextView yTextView;
    TextView zTextView;

    //記録フラグ
    private boolean record = false;

    //low pass filter alpha ローパスフィルタのアルファ値
    protected final static float alpha= 0.8f;


    //端末が実際に取得した加速度値。重力加速度も含まれる。This values include gravity force.
    private float[] currentOrientationValues = { 0.0f, 0.0f, 0.0f };
    //ローパス、ハイパスフィルタ後の加速度値 Values after low pass and high pass filter
    private float[] currentAccelerationValues = { 0.0f, 0.0f, 0.0f };

    //取得した値の保存リスト
    private ArrayList<Float> AccelerationX = new ArrayList<Float>();
    private ArrayList<Float> AccelerationY = new ArrayList<Float>();
    private ArrayList<Float> AccelerationZ = new ArrayList<Float>();


    //previous data 1つ前の値
    private float old_x=0.0f;
    private float old_y=0.0f;
    private float old_z=0.0f;

    //ローパスフィルタの係数
    private float LPF_alpha = 0.8f;

    //ノイズ対策
    boolean noiseflg=true;
    //ベクトル量(最大値)
    private double vectorSize_max=0;

    private boolean first_get = true;

    //ラインチャート関係
    LineChart mChart;

    String[] names = new String[]{"x-value", "y-value", "z-value"};
    int[] colors = new int[]{Color.RED, Color.GREEN, Color.BLUE};


    /*
    //ベクトル量
    private double vectorSize=0;

    //カウンタ
    long counter=0;

    //一回目のゆれを省くカウントフラグ（一回の端末の揺れで2回データが取れてしまうのを防ぐため）
    //count flag to prevent aquiring data twice with one movement of a device
    boolean counted=false;

    // X軸加速方向
    boolean vecx = true;
    // Y軸加速方向
    boolean vecy = true;
    // Z軸加速方向
    boolean vecz = true;

     //diff 差分
    private float dx=0.0f;
    private float dy=0.0f;
    private float dz=0.0f;

    //THRESHOLD ある値以上を検出するための閾値
    protected final static double THRESHOLD=4.0;
    protected final static double THRESHOLD_MIN=1;
    */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //テキストの紐付け
        xTextView = (TextView)findViewById(R.id.xvalue);
        yTextView = (TextView)findViewById(R.id.yvalue);
        zTextView = (TextView)findViewById(R.id.zvalue);


        //たぶんセンサーの初期化？？
        manager = (SensorManager)getSystemService(SENSOR_SERVICE);
        sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //グラフの初期化??定義
        mChart = (LineChart) findViewById(R.id.lineChart);
        Description description = new Description();
        description.setTextColor(ColorTemplate.VORDIPLOM_COLORS[2]);
        description.setText("");
        mChart.setDescription(description);
        mChart.setData(new LineData());  //空のLineData型インスタンスを追加

        //ボタンの紐付け
        final Button recordStart = (Button) findViewById(R.id.record_start);
        recordStart.setOnClickListener(buttonClick);

        final Button recordStop = (Button) findViewById(R.id.record_stop);
        recordStop.setOnClickListener(buttonClick);

    }
    private View.OnClickListener buttonClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.record_start:
                    Log.d("debug","start, Perform action on click");
                    if(record == false) {
                        record = true;
                        toastMake("測定を開始しました", 0, -200);
                        break;
                    }
                case R.id.record_stop:
                    Log.d("debug","stop, Perform action on click");
                    if(record == true){
                        record = false;

                        WriteDataCsv WriteData = new WriteDataCsv();
                        ArrayList<String> Outdata = new ArrayList<String>();
                        Integer lenth_acc = AccelerationX.size();
                        Outdata.add("X,Y,Z");
                        for(int i = 0;i < lenth_acc;i++){
                            Outdata.add( String.valueOf( AccelerationX.get( i)) + "," + String.valueOf( AccelerationY.get( i)) + "," + String.valueOf( AccelerationZ.get( i)));
                        }
                        Log.d("debug", Outdata.toString());
                        Date nowdate = new Date();
                        SimpleDateFormat dateformat1 = new SimpleDateFormat("yyyy_MM_dd_H_mm");
                        String filename = dateformat1.format(nowdate);
                        WriteData.Write(Outdata,"/Acceleration" + filename + ".csv");

                        //リストを空にする
                        AccelerationX.clear();
                        AccelerationY.clear();
                        AccelerationZ.clear();
                        toastMake("測定を終了しました", 0, -200);
                    }
                    break;
            }
        }
    };

    //おそらくセンサーを受信したら呼ばれる関数
    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){

            //ローパスフィルタ　重力の値を検出
            currentOrientationValues[0] = event.values[0] * 0.1f + currentOrientationValues[0] * (1.0f -0.1f);
            currentOrientationValues[1] = event.values[1] * 0.1f + currentOrientationValues[1] * (1.0f -0.1f);
            currentOrientationValues[2] = event.values[2] * 0.1f + currentOrientationValues[2] * (1.0f -0.1f);
            //重力の除去
            currentAccelerationValues[0] = event.values[0] - currentOrientationValues[0];
            currentAccelerationValues[1] = event.values[1] - currentOrientationValues[1];
            currentAccelerationValues[2] = event.values[2] - currentOrientationValues[2];

            LineData data = mChart.getLineData();
            if(first_get == true) {
                currentAccelerationValues[0] = event.values[0];
                currentAccelerationValues[1] = event.values[1];
                currentAccelerationValues[2] = event.values[2];
                first_get = false;
            }else{
                /*
                //ローパスフィルタ(滑らかになるはず)
                currentAccelerationValues[0] = LPF_alpha *  old_x + (1.0f - LPF_alpha) * (event.values[0]);
                currentAccelerationValues[1] = LPF_alpha *  old_y + (1.0f - LPF_alpha) * (event.values[1]);
                currentAccelerationValues[2] = LPF_alpha *  old_z + (1.0f - LPF_alpha) * (event.values[2]);
                */
                currentAccelerationValues[0] = LPF_alpha *  old_x + (1.0f - LPF_alpha) * (currentAccelerationValues[0]);
                currentAccelerationValues[1] = LPF_alpha *  old_y + (1.0f - LPF_alpha) * (currentAccelerationValues[1]);
                currentAccelerationValues[2] = LPF_alpha *  old_z + (1.0f - LPF_alpha) * (currentAccelerationValues[2]);

            }
            old_x = currentAccelerationValues[0];
            old_y = currentAccelerationValues[1];
            old_z = currentAccelerationValues[2];
            xTextView.setText("X:" + String.valueOf(currentAccelerationValues[0]));
            yTextView.setText("Y:" + String.valueOf(currentAccelerationValues[1]));
            zTextView.setText("Z:" + String.valueOf(currentAccelerationValues[2]));
            if(record) {
                AccelerationX.add(currentAccelerationValues[0]);
                AccelerationY.add(currentAccelerationValues[1]);
                AccelerationZ.add(currentAccelerationValues[2]);
            }
            //グラフ関係処理
            if(data != null){
                for(int i = 0;i < 3;i++){
                    ILineDataSet set = data.getDataSetByIndex(i);
                    if(set == null){
                        set = createSet(names[i],colors[i]); ///初期化メゾットは作った
                        data.addDataSet(set);
                    }

                    data.addEntry(new Entry(set.getEntryCount(), currentAccelerationValues[i]), i);
                    data.notifyDataChanged();
                }
                mChart.notifyDataSetChanged();   //表示更新の通知
                mChart.setVisibleXRangeMaximum(50);//表示幅の決定
                mChart.moveViewToX(data.getEntryCount());//最新データまで移動
            }
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

    private void toastMake(String message, int x, int y){
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        // 位置調整
        toast.setGravity(Gravity.CENTER, x, y);
        toast.show();
    }


    @Override
    protected void onResume() {
        super.onResume();
        manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        manager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }


}
