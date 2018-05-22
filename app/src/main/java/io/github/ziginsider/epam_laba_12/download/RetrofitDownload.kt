package io.github.ziginsider.epam_laba_12.download

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * Declaration a Retrofit interface for file downloading with possibility of showing progress
 * of download
 *
 * @since 2018-04-10
 * @author Alex Kisel
 */
interface RetrofitDownload {

    @GET
    @Streaming
    fun downloadFile(@Url url: String): Call<ResponseBody>
}
