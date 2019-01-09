package com.example.yukiiwamoto.testtripapp.trip;

import android.graphics.Bitmap;

public class Trip {
    private String id;
    private String title;
    private Bitmap img;
    private String start_date;
    private String end_date;

    public Trip(String id, String title, Bitmap img, String start_date, String end_date) {
        this.id = id;
        this.title = title;
        this.img = img;
        this.start_date = start_date;
        this.end_date = end_date;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Bitmap getImg() {
        return img;
    }

    public void setImg(Bitmap img) {
        this.img = img;
    }

    public String getStart_date() {
        return start_date;
    }

    public void setStart_date(String start_date) {
        this.start_date = start_date;
    }

    public String getEnd_date() {
        return end_date;
    }

    public void setEnd_date(String end_date) {
        this.end_date = end_date;
    }
}
