package com.example.ramya.downloadnotification;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Streaming;

public interface APIInterface {

    @GET("sample-videos/small.mp4")
    @Streaming
    Call<ResponseBody> downloadFile();
}
