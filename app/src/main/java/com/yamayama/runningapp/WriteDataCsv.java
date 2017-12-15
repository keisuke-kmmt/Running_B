package com.yamayama.runningapp;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Created by keisuke Watanabe on 2017/11/22.
 */

public class WriteDataCsv {
    //private FileOutputStream mOut;

    public static void Write(ArrayList<String> data, String filename){
        try {
            String folder = Environment.getExternalStorageDirectory().getPath()+"/RUNAPP";
            File dir = new File(folder);
            // フォルダが無ければ作成
            if(!dir.exists()){
                boolean result = dir.mkdirs();
            }

            //indexに登録してすぐpcに接続した時にアクセスできるようにする
            Context context = ApplicationController.getInstance().getApplicationContext();
            MediaScannerConnection.scanFile(context, new String[] { folder + filename }, null, null);

            //出力先を作成する
            FileWriter fw = new FileWriter(folder + filename, false);
            PrintWriter pw = new PrintWriter(new BufferedWriter(fw));



            //内容を指定する
            for(int i = 0;i<data.size();i++) {
                pw.println(data.get(i));
            }

            pw.flush();
            //ファイルに書き出す
            pw.close();


        //終了メッセージを画面に出力する
            Log.d("debug","出力が完了しました。");

        } catch (IOException ex) {
            //例外時処理
            ex.printStackTrace();
        }

    }
}