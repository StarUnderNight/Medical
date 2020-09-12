package com.dkss.medical.service;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.dkss.medical.R;
import com.dkss.medical.device.max485.Max485;
import com.dkss.medical.device.pmr.PatientMonitor;
import com.dkss.medical.device.pmr.PatientMonitor_v0;
import com.dkss.medical.server.RespiratorServer;
import com.dkss.medical.util.Protocol;
import com.dkss.medical.util.IniConfig;

import java.util.Map;

public class MainService extends Service {
    private Context context;
    private RespiratorServer respiratorServer  = null;
    private PatientMonitor_v0 patientMonitor = null;
    private Max485 max485 = null;

    public MainService() {
        context = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println("on. create");
        boolean ret = runProgram(context);
        if(!ret ){
            System.out.println("结果："+ret);
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("进入onstartComman");

        Notification notification = new NotificationCompat.Builder(this, "110")
                .setContentTitle("medical")
                .setContentText("已开启服务")
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .build();

        startForeground(1, notification);

        return  super.onStartCommand(intent,flags,startId);
    }

    @Override
    public void onDestroy() {
        System.out.println("结束前台服务");
        if(patientMonitor != null){
            patientMonitor.exit = true;
        }
        if(max485 != null){
            max485.exit = true;
        }
        if(respiratorServer != null){
            if(respiratorServer.respirator !=null){
                respiratorServer.respirator.exit = true;
            }
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        System.out.println("onBind绑定");
        return null;
    }



    private boolean runProgram(Context context) {
        AssetManager assetManager = getAssets();
        Thread pmrThread = null;
        Thread max485Thread = null;
        Thread rerThread = null;

        try {

            Map<String,Object> cfgMap =  IniConfig.readIni(assetManager.open("dkss_medical.ini"));
            Protocol config = new Protocol();
            if(!config.init(cfgMap)){
                System.out.println("配置文件缺少配置信息，请检查");
                return false;
            }

            PatientMonitor pmTest = new PatientMonitor();
            new Thread(pmTest).start();



        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


}
