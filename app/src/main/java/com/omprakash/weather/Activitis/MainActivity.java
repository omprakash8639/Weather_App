package com.omprakash.weather.Activitis;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.omprakash.weather.Adapters.HourlyAdapter;
import com.omprakash.weather.Domains.Hourly;
import com.omprakash.weather.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private RecyclerView.Adapter adapterHourly;
    private RecyclerView recyclerView;
    private ArrayList<Hourly> items = new ArrayList<>();
    AutoCompleteTextView Entered_loc;
    ImageView search;
    TextView curr_location, curr_condition;
    ImageView curr_cond_img;
    TextView curr_temp;
    TextView High_low;
    TextView Rain_percentage, Wind_speed, Humidity_percentage,curr_date;
    private ProgressDialog progressDialog;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private FusedLocationProviderClient fusedLocationClient;

    @SuppressLint({"WrongViewCast", "MissingInflatedId"})
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Resources res = getResources();
        String[] dest = res.getStringArray(R.array.destination_array);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, dest);

        SimpleDateFormat ft
                = new SimpleDateFormat("dd-MM-yyyy");

        String str = ft.format(new Date());

        Entered_loc = findViewById(R.id.searchView);
        Entered_loc.setAdapter(arrayAdapter);

        curr_date = findViewById(R.id.date_now);
        curr_date.setText(str);

        search = findViewById(R.id.search);
        curr_location = findViewById(R.id.location);
        curr_condition = findViewById(R.id.condition_now);
        curr_cond_img = findViewById(R.id.cond_img);
        curr_temp = findViewById(R.id.temp_now);
        High_low = findViewById(R.id.h_l);
        Rain_percentage = findViewById(R.id.rain_perc);
        Wind_speed = findViewById(R.id.wind_speed);
        Humidity_percentage = findViewById(R.id.humidity_perc);

        initRecyclerview();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permissions
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            // Permissions are already granted
            getLastLocation();
        }

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Entered_loc.getText().toString().isEmpty()) {
                    fetchWeatherData(Entered_loc.getText().toString());
                }else if (progressDialog != null || progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }else {
                    Toast.makeText(MainActivity.this, "Please Enter a location", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        fusedLocationClient.getLastLocation()
                .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            Location location = task.getResult();
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            Log.d("MainActivity", "Lat: " + latitude + ", Lon: " + longitude);
                            // Use the latitude and longitude to fetch weather data
                            fetchWeatherData(latitude, longitude);
                        } else {
                            Log.w("MainActivity", "Failed to get location.");
                            Toast.makeText(MainActivity.this, "Failed to get location.", Toast.LENGTH_SHORT).show();
                            fetchWeatherData("Hyderabad");
                        }
                    }
                });
    }

    private void fetchWeatherData(double latitude, double longitude) {
        String location = latitude + "," + longitude;
        // Use this location string to fetch weather data as before
        fetchWeatherData(location);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initRecyclerview() {
        recyclerView = findViewById(R.id.view1);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapterHourly = new HourlyAdapter(items);
        recyclerView.setAdapter(adapterHourly);
    }

    private void fetchWeatherData(String q) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://weatherapi-com.p.rapidapi.com/forecast.json?q=" + q + "&days=1")
                .get()
                .addHeader("x-rapidapi-key", "Enter your api key")
                .addHeader("x-rapidapi-host", "weatherapi-com.p.rapidapi.com")
                .build();

        // Show the progress dialog
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setMessage("Loading...");
                progressDialog.setCancelable(false);
                progressDialog.show();
            }
        });

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Log.e("MainActivity", "Error fetching weather data", e);
                // Dismiss the progress dialog on failure
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(MainActivity.this, "Failed to fetch weather data", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("MainActivity", "Unexpected code " + response);

                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }

                    throw new IOException("Unexpected code " + response);

                } else {
                    String responseData = response.body().string();
                    if (responseData != null) {
                        parseWeatherData(responseData);

                    } else {
                        Log.e("MainActivity", "Response body is null");
                    }
                }

                // Dismiss the progress dialog on success
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                    }
                });
            }
        });
    }

    private void parseWeatherData(String responseData) {
        try {
            JSONObject jsonObject = new JSONObject(responseData);
            JSONObject location = jsonObject.getJSONObject("location");
            JSONObject current = jsonObject.getJSONObject("current");
            JSONObject condition = current.getJSONObject("condition");
            JSONObject forecast = jsonObject.getJSONObject("forecast");
            JSONArray forecastday = forecast.getJSONArray("forecastday");
            JSONObject day = forecastday.getJSONObject(0);
            JSONObject dayInfo = day.getJSONObject("day");

            final String city = location.getString("name");
            final String region = location.getString("region");
            final String country = location.getString("country");
            final String conditionText = condition.getString("text");
            final String conditionIcon = "https:" + condition.getString("icon");
            int temp = current.getInt("temp_c");
            final int maxTemp = dayInfo.getInt("maxtemp_c");
            final int minTemp = dayInfo.getInt("mintemp_c");
            final int rain = dayInfo.getInt("daily_chance_of_rain");
            final int windSpeed = current.getInt("wind_kph");
            final int humidity = current.getInt("humidity");

            // Update UI components on the main thread
            int finalTemp = temp;
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    curr_location.setText(city);
                    curr_condition.setText(conditionText);
                    curr_temp.setText(finalTemp + "°C");
                    High_low.setText("H-" + maxTemp + "  L-" + minTemp);
                    Rain_percentage.setText(rain + "%");
                    Wind_speed.setText(windSpeed + " km/h");
                    Humidity_percentage.setText(humidity + "%");

                    Glide.with(MainActivity.this).load(conditionIcon).into(curr_cond_img);

                }
            });

            // Clear the old items
            items.clear();

            // Update hourly forecast data
            JSONArray hourArray = day.getJSONArray("hour");
            Calendar calendar = Calendar.getInstance();

            int currentHour = calendar.get(Calendar.HOUR_OF_DAY);

            for (int i = 0; i < hourArray.length(); i++) {
                JSONObject hourObject = hourArray.getJSONObject(i);
                String time = hourObject.getString("time");
                int hour = Integer.parseInt(time.split(" ")[1].split(":")[0]); // Extract the hour from time

                if (hour >= currentHour) {
                    temp = hourObject.getInt("temp_c");
                    String iconUrl = "https:" + hourObject.getJSONObject("condition").getString("icon");

                    String f_t;
                    String t1= time.substring(11,13);
                    int t2= Integer.parseInt(t1);
                    if(t2>12)
                    {
                        t2=t2-12;
                        f_t= t2+"PM";
                    }else {
                        f_t = t2 + "AM";
                    }
                    items.add(new Hourly(f_t, temp, iconUrl));
                }
            }

            // Notify the adapter on the main thread
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    adapterHourly.notifyDataSetChanged();
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("MainActivity", "Error parsing weather data", e);
        }
    }
}
