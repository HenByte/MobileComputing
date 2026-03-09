package com.example.soloproject

import android.util.Log
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

private const val TAG = "FineliAPI"

//This handles the network calls.
object FineliModule {
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d(TAG, message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    //This creates the HTTP-client.
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1")
                    .build()
            )
        }
        .addInterceptor(loggingInterceptor)
        .build()
    //This creates the retrofit client that uses the HTTP-client.
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://fineli.fi/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    //This is the API what the app uses
    val fineliApi: FineliApi = retrofit.create(FineliApi::class.java)
}

//This describes endpoint in fineli-API that returns the nutritionvalues.
interface FineliApi {
    @GET("fineli/api/v1/foods/{id}")
    suspend fun getFood(@Path("id") foodId: Long): FineliFoodResponse
}
//This is the response from the API.
data class FineliFoodResponse(
    @SerializedName("carbohydrate") val carbohydrate: Double
) {

    //This is amount of carbs that has been fetched from the Fineli-API.
    val carbohydratePer100g: Int
        get() = carbohydrate.toInt().coerceAtLeast(0)
}
