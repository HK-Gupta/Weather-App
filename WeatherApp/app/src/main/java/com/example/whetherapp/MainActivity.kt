package com.example.whetherapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.widget.SearchView
import com.example.whetherapp.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.util.Date
import java.util.Locale

//
class MainActivity : AppCompatActivity() {

    private val API_KEY = BuildConfig.API_KEY
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        fetchWeatherData("New Delhi")
        searchCity()
    }

    private fun searchCity() {
        val searchView = binding.searchBarMain
        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    fetchWeatherData(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }

        })
    }

    private fun fetchWeatherData(cityName: String) {
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .build().create(ApiInterface::class.java)
        val response = retrofit.getWeatherData("Delhi", API_KEY, "metric")
        response.enqueue(object: Callback<WeatherApp> {
            override fun onResponse(call: Call<WeatherApp>, response: Response<WeatherApp>) {
                val resBody = response.body()
                if(response.isSuccessful && resBody != null) {

                    // Setting up all the view by binding
                    val condition = resBody.weather.firstOrNull()?.main?: "unKnown"
                    val temperatureMain = resBody.main.temp
                    val maxTemp = resBody.main.temp_max
                    val minTemp = resBody.main.temp_min
                    val humidity = resBody.main.humidity
                    val windSpeed = resBody.wind.speed
                    val sunRise = resBody.sys.sunrise.toLong()
                    val sunSet = resBody.sys.sunset.toLong()
                    val seaLevel = resBody.main.pressure


                    binding.txtDayWeather.text = condition
                    binding.txtTemperatureMain.text = "$temperatureMain °C"
                    binding.txtMaxTemperature.text = "$maxTemp °C"
                    binding.txtMinTemperature.text = "$minTemp °C"
                    binding.txtHumidity.text = "$humidity%"
                    binding.txtWindSpeed.text = "$windSpeed m/s"
                    binding.txtSunrise.text = "${getTime(sunRise)}"
                    binding.txtSunset.text = "${getTime(sunSet)}"
                    binding.txtSeaLevel.text = "$seaLevel hPa"
                    binding.txtCondition.text = condition
                    binding.txtDay.text = getDay()
                    binding.txtDate.text = getDate(System.currentTimeMillis())
                    binding.txtCityName.text = cityName

                    changeWeatherBackground(condition)
                }
            }

            override fun onFailure(call: Call<WeatherApp>, t: Throwable) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun changeWeatherBackground(condition: String) {
        when(condition) {
            "Partly Clouds", "Clouds", "Overcast", "Mist", "Foggy" -> {
                binding.root.setBackgroundResource(R.drawable.cloud_bg)
                binding.lottieAnim.setAnimation(R.raw.cloud_anim)
            }

            "Light Rain", "Drizzle", "Moderate Rain", "Showers", "Heavy Rain" -> {
                binding.root.setBackgroundResource(R.drawable.rain_bg)
                binding.lottieAnim.setAnimation(R.raw.rain_anim)
            }

            "Light Snow", "Moderate Snow", "Heavy Snow", "Blizzard" -> {
                binding.root.setBackgroundResource(R.drawable.snow_bg)
                binding.lottieAnim.setAnimation(R.raw.snow_anim)
            }
            else -> {
                binding.root.setBackgroundResource(R.drawable.sunny_bg)
                binding.lottieAnim.setAnimation(R.raw.sun_anim)
            }
        }
        binding.lottieAnim.playAnimation()
    }

    private fun getDay(): String {
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
        return sdf.format(Date())

    }

    private fun getDate(timeStamp: Long): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getTime(timeStamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timeStamp*1000))
    }
}