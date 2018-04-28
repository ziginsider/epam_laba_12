package io.github.ziginsider.epam_laba_12.retrofit

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Streaming

interface RetrofitService {

    @GET("files/Node-Android-Chat.zip")
    @Streaming
    fun downloadFile(): Call<ResponseBody>
}