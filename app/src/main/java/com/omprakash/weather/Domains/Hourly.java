package com.omprakash.weather.Domains;

public class Hourly {
    private String hour;
    private int temp;
    private String imageUrl;

    public Hourly(String hour, int temp, String imageUrl) {
        this.hour = hour;
        this.temp = temp;
        this.imageUrl = imageUrl;
    }

    public String getHour() {
        return hour;
    }

    public int getTemp() {
        return temp;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}