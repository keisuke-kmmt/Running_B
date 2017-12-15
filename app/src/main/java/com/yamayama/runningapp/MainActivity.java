package com.yamayama.runningapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
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

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements LocationListener , SensorEventListener{

    //センサー関係
    SensorManager manager;
    Sensor sensor;

    //GPS関係
    LocationManager locationManager;

    //記録フラグ
    private boolean record = false;


    //端末が実際に取得した加速度値。重力加速度も含まれる。This values include gravity force.
    private float[] currentOrientationValues = { 0.0f, 0.0f, 0.0f };
    //ローパス、ハイパスフィルタ後の加速度値 Values after low pass and high pass filter
    private float[] currentAccelerationValues = { 0.0f, 0.0f, 0.0f };

    //取得した値の保存リスト
    private ArrayList<Float> AccelerationX = new ArrayList<Float>();
    private ArrayList<Float> AccelerationY = new ArrayList<Float>();
    private ArrayList<Float> AccelerationZ = new ArrayList<Float>();

    //GPSの取得値リスト
    private ArrayList<Double> GPS_lat = new ArrayList<>();
    private ArrayList<Double> GPS_long = new ArrayList<>();

    //GPSの最新の値
    private double nowGPS_lat = 0.0;
    private double nowGPS_long = 0.0;

    //GPS値取得可能flag
    private boolean GPSenable = false;

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


    //センサーの前回の取得時間
    private long beforeTime = 0;


    // スライド用の部品
    private ViewPager mPager;

    //GPSの値取得handler
    private int GPS_period = 5000;
    private final Handler GPS_handler = new Handler();
    private Runnable GPS_runnable;

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

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        //fragment関係
        mPager = (ViewPager) findViewById(R.id.viewpager);
        mPager.setAdapter(new MyFragmentPagerAdapter(getSupportFragmentManager(),
                MainActivity.this));
        // 上部にタブをセットする
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mPager);


        //たぶんセンサーの初期化？？
        manager = (SensorManager)getSystemService(SENSOR_SERVICE);
        sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    public void onStart(){
        super.onStart();
        EventBus.getDefault().register(this);
        manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);

        //許可を申請する処理
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
            Log.d("debug","permission OK");


            // LocationManager インスタンス生成
            locationManager =
                    (LocationManager) getSystemService(LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        }else{
            //まだ許可を求める前の時、許可を求めるダイアログを表示します。
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }

        GPStimerSet();
    }

    @Override
    public void onStop(){
        EventBus.getDefault().unregister(this);
        super.onStop();
    }


    //buttonが押されたら動作する
    @Subscribe
    public void onButtonEvent(ButtonEvent event){
        switch (event.ButtonMessage) {
            case "start":
                Log.d("debug","start, Perform action on click");
                if(record == false) {
                    record = true;
                    toastMake("測定を開始しました");
                }else{
                    toastMake("測定中です");
                }
                break;
            case "stop":
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
                    toastMake("測定を終了しました");
                }else{
                    toastMake("測定を開始してください");
                }
                break;
        }
    }

    //おそらくセンサーを受信したら呼ばれる関数
    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            /*
            //インターバルを出力
            if(beforeTime != 0){
                Log.d("debug",String.valueOf(System.currentTimeMillis() - beforeTime));
            }
            //センサーを取得した時間の取得
            beforeTime = System.currentTimeMillis();
            */
            //ローパスフィルタ　重力の値を検出
            currentOrientationValues[0] = event.values[0] * 0.1f + currentOrientationValues[0] * (1.0f -0.1f);
            currentOrientationValues[1] = event.values[1] * 0.1f + currentOrientationValues[1] * (1.0f -0.1f);
            currentOrientationValues[2] = event.values[2] * 0.1f + currentOrientationValues[2] * (1.0f -0.1f);
            //重力の除去
            currentAccelerationValues[0] = event.values[0] - currentOrientationValues[0];
            currentAccelerationValues[1] = event.values[1] - currentOrientationValues[1];
            currentAccelerationValues[2] = event.values[2] - currentOrientationValues[2];

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

            EventBus.getDefault().post(new MySensorEvent(currentAccelerationValues[0],currentAccelerationValues[1],currentAccelerationValues[2]));

            if(record == true) {
                AccelerationX.add(currentAccelerationValues[0]);
                AccelerationY.add(currentAccelerationValues[1]);
                AccelerationZ.add(currentAccelerationValues[2]);
            }
        }
    }

    private void toastMake(String message){
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        // 位置調整
        //toast.setGravity(Gravity.BOTTOM, x, y);
        toast.show();
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        manager.unregisterListener(this);
        if(locationManager != null) {
            locationManager.removeUpdates(this);
        }

        GPSstopTimerTask();

    }

    //GPSなどの位置情報が入手可能かどうか
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        switch (status) {
            case LocationProvider.AVAILABLE:
                Log.d("debug", "LocationProvider.AVAILABLE");
                GPSenable = true;
                break;
            case LocationProvider.OUT_OF_SERVICE:
                Log.d("debug", "LocationProvider.OUT_OF_SERVICE");
                GPSenable = false;
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                Log.d("debug", "LocationProvider.TEMPORARILY_UNAVAILABLE");
                GPSenable = false;
                break;
        }
    }

    //位置が変わったかどうか?
    @Override
    public void onLocationChanged(Location location) {
        //GPS受信イベントの発動
        EventBus.getDefault().post(new GPSEvent(location.getLatitude(),location.getLongitude()));
        nowGPS_lat = location.getLatitude();
        nowGPS_long = location.getLongitude();

        GPSenable = true;

    }

    private void GPStimerSet(){
        GPS_runnable = new Runnable(){
            @Override
            public void run(){
                if(GPSenable == true) {
                    //GPSの値を保存
                    GPS_lat.add(nowGPS_lat);
                    GPS_long.add(nowGPS_long);
                    Log.d("debug", String.valueOf(nowGPS_lat) + String.valueOf(nowGPS_long));
                }
                GPS_handler.postDelayed(this,GPS_period);
            }
        };

        GPS_handler.post(GPS_runnable);
    }

    private void GPSstopTimerTask(){
        GPS_handler.removeCallbacks(GPS_runnable);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }



}



