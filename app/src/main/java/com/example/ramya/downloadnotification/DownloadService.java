package com.example.ramya.downloadnotification;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;
import retrofit2.Call;

public class DownloadService extends IntentService {

    private Notification.Builder notificationBuilder;
    private NotificationManager notificationManager;
    APIInterface apiInterface;

    private int totalFileSize;
    String filePath;

    public DownloadService() {
        super("Download Service");
        apiInterface = APIClient.getClient().create(APIInterface.class);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "my_channel_01";
            String channelName = "channel";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(channel);
        }

        notificationBuilder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Download")
                .setContentText("Downloading File")
                .setOngoing(true)
                .setAutoCancel(true);

//                .setProgress(100, 0, false)
//                .setContentText("Downloading file " + 0 + "/" + totalFileSize + " MB");
        notificationManager.notify(0, notificationBuilder.build());

        initDownload();
    }

    private void initDownload() {
        Call<ResponseBody> call = apiInterface.downloadFile();
        try {
            downloadFile(call.execute().body());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void downloadFile(ResponseBody responseBody) throws IOException {
        int count;
        byte data[] = new byte[1024 * 4];
        long fileSize = responseBody.contentLength();
        InputStream inputStream = new BufferedInputStream(responseBody.byteStream(), 1024 * 8);
        File file = new File(getCacheDir(), "file.mp4");
        OutputStream output = new FileOutputStream(file);
        filePath = file.getPath();

        long total = 0;
        long startTime = System.currentTimeMillis();
        int timeCount = 1;
        while ((count = inputStream.read(data)) != -1) {

            total += count;
            totalFileSize = (int) (fileSize / (Math.pow(1024, 2)));
            double current = Math.round(total / (Math.pow(1024, 2)));

            int progress = (int) ((total * 100) / fileSize);

            long currentTime = System.currentTimeMillis() - startTime;

            Download download = new Download();
            download.setTotalFileSize(totalFileSize);

            if (currentTime > 1000 * timeCount) {

                download.setCurrentFileSize((int) current);
                download.setProgress(progress);
                sendNotification(download);
                timeCount++;
            }

            output.write(data, 0, count);
        }
        onDownloadComplete();
        output.flush();
        output.close();
        inputStream.close();
    }

    private void sendNotification(Download download) {

        sendIntent(download);
        notificationBuilder.setProgress(100, download.getProgress(), false);
        notificationBuilder.setContentText("Downloading file " + download.getCurrentFileSize() + "/" + totalFileSize + " MB");
        notificationManager.notify(0, notificationBuilder.build());
    }

    private void sendIntent(Download download) {

        Intent intent = new Intent(MainActivity.MESSAGE_PROGRESS);
        intent.putExtra("download", download);
        LocalBroadcastManager.getInstance(DownloadService.this).sendBroadcast(intent);
    }

    private void onDownloadComplete() {

        Download download = new Download();
        download.setProgress(100);
        sendIntent(download);

        notificationManager.cancel(0);
        notificationBuilder.setProgress(0, 0, false)
                .setOngoing(false)
                .setContentText("File Downloaded");

        Intent intent = getOpenFileIntent(filePath);
        if (intent != null) {
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            notificationBuilder.setContentIntent(pendingIntent);
        }

        notificationManager.notify(0, notificationBuilder.build());

    }

    private Intent getOpenFileIntent(String path) {
        File file = new File(path);
        Uri uri = FileProvider.getUriForFile(this, "com.example.ramya.downloadnotification.fileProvider", file);
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        String extension = path.substring(path.lastIndexOf("."));
        String type = mime.getMimeTypeFromExtension(extension);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        intent.setDataAndType(uri, "application/*");
        intent.setDataAndType(uri, type);
        return Intent.createChooser(intent, "Open file");

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        notificationManager.cancelAll();
    }
}