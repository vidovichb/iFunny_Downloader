package com.vidovichb.ifunnydownloader;

import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.io.File;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class iFunnyDownloadService extends Service {

    String VideoUrltmp;
    String VideoUrl;

    private long downloadID;

    private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Fetching the download id received with the broadcast
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            //Checking if the received broadcast is for our enqueued download by matching download id
            if (downloadID == id) {
                //Toast.makeText(iFunnyDownloadService.this, "Download Completed", Toast.LENGTH_SHORT).show();
                stopSelf();
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String essence = intent.getStringExtra("VideoUrl");

        OkHttpClient okHttpClient = new OkHttpClient().newBuilder().addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(ScalarsConverterFactory.create())
                .baseUrl(essence)
                .client(okHttpClient).build();

        final ApiService apiService = retrofit.create(ApiService.class);

        Call<String> stringCall = apiService.getStringResponse();

        stringCall.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    String responseString = response.body();

                    int indexOfUrl = responseString.indexOf("<meta property=\"og:video:url\" content=")+38;
                    VideoUrltmp = responseString.substring(indexOfUrl,indexOfUrl+150);
                    VideoUrl = VideoUrltmp.substring(VideoUrltmp.indexOf("\"")+1,VideoUrltmp.indexOf("mp4\"")+3);

                    //try to download now
                    beginDownload(VideoUrl);
                    //mp4load(VideoUrl);
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {

            }
        });

        return START_NOT_STICKY;
    }

    private void beginDownload(String videoUrl){
        String downloadsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        File file=new File(downloadsPath,"ifunnyVideo.mp4");
        /*
        Create a DownloadManager.Request with all the information necessary to start the download
         */
        DownloadManager.Request request=new DownloadManager.Request(Uri.parse(videoUrl))
                .setTitle("iFunny Download")// Title of the Download Notification
                .setDescription("Downloading")// Description of the Download Notification
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)// Visibility of the download Notification
                .setDestinationUri(Uri.fromFile(file))// Uri of the destination file
                .setRequiresCharging(false)// Set if charging is required to begin the download
                .setAllowedOverMetered(true)// Set if download is allowed on Mobile network
                .setAllowedOverRoaming(true);// Set if download is allowed on roaming network
        DownloadManager downloadManager= (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        downloadID = downloadManager.enqueue(request);// enqueue puts the download request in the queue.
    }


    @Override
    public void onCreate() {
    }


    @Override
    public void onDestroy() {
        unregisterReceiver(onDownloadComplete);
    }
}
