package com.example.ramya.downloadnotification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    public static String MESSAGE_PROGRESS = "message_progress";
    Button btnDownload;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnDownload = findViewById(R.id.activity_main_btnDownload);
        textView = findViewById(R.id.activity_main_tv_showDownloaded);
        registerReceiver();

        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textView.setText("");
                startDownload();
            }
        });
    }

    private void startDownload() {

        Intent intent = new Intent(this, DownloadService.class);
        startService(intent);

    }

    private void registerReceiver() {

        LocalBroadcastManager bManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MESSAGE_PROGRESS);
        bManager.registerReceiver(broadcastReceiver, intentFilter);

    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ( intent.getAction()!= null && intent.getAction().equals(MESSAGE_PROGRESS)) {
                Download download = intent.getParcelableExtra("download");
                if(download.getProgress() == 100){
                    textView.setText(getString(R.string.download_complete));
                }
            }
        }
    };
}
