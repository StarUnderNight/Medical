package com.dkss.medical;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.dkss.medical.service.MainService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;


public class MainActivity extends Activity {

    private Button startServiceBtn;
    private Button closeServiceBtn;
    private Button modifyConfigBtn;
    private Intent serviceIntent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startServiceBtn = findViewById(R.id.startService);
        closeServiceBtn = findViewById(R.id.closeService);
        modifyConfigBtn = findViewById(R.id.modifyConfig);

        serviceIntent = new Intent(this,MainService.class);


        startServiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("service","启动服务");
                startService(serviceIntent);
            }
        });

        closeServiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("service","关闭服务");
                stopService(serviceIntent);
               // android.os.Process.killProcess(android.os.Process.myPid());
            }
        });

        modifyConfigBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });



        startService(serviceIntent);

    }

//    static{
//        System.loadLibrary("Max485Serial");
//    }

}
