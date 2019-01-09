package com.example.yukiiwamoto.testtripapp.trip;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.yukiiwamoto.testtripapp.R;

public class TripAdapter extends ArrayAdapter<Trip> {
    private LayoutInflater inflater;

    public TripAdapter(Context context) {
        super(context, 0);
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = this.inflater.inflate(R.layout.row, parent, false);
            ViewHolder vh = new ViewHolder();
            vh.titleText = convertView.findViewById(R.id.title);
            vh.dateText = convertView.findViewById(R.id.date);
            vh.image = convertView.findViewById(R.id.imageView);
            convertView.setTag(vh);
        }
        Trip trip = getItem(position);
        ViewHolder vh = (ViewHolder) convertView.getTag();
        vh.titleText.setText(trip.getTitle());
        vh.dateText.setText(trip.getStart_date() + "~" + trip.getEnd_date());
        if (trip.getImg() != null && !"".equals(trip.getImg())) {
            Log.i("getImgデータ取ってきたよ！！！！", trip.getImg().toString());
            vh.image.setImageBitmap(trip.getImg());
        }
        return convertView;
    }


    private class ViewHolder {
        private TextView titleText;
        private TextView dateText;
        private ImageView image;
    }

}
