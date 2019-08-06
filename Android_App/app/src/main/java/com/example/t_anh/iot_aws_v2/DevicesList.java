package com.example.t_anh.iot_aws_v2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import static com.example.t_anh.iot_aws_v2.DeviceActivity.deviceNameValue;
import static com.example.t_anh.iot_aws_v2.MainActivity.getDeviceNumber;
import static com.example.t_anh.iot_aws_v2.PubSubJSON.publish;
import static com.example.t_anh.iot_aws_v2.PubSubJSON.subscribeNumber;

public class DevicesList extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    static String LOG_TAG = DevicesList.class.getCanonicalName();
    private GoogleMap mMap;

    private boolean start = true;
    static String[] devices = new String[getDeviceNumber()];
    //ArrayAdapter<String> adapter = null;
    CustomAdapter adapter = null;
    static Bundle latLng = new Bundle();
    static Bundle alarmedDevice = new Bundle();

    LinearLayout refreshList;
    static ListView devicesList;
    static TextView deviceText;
    static TextView connectedText;
    static TextView disconnectedText;
    TextView mapText;
    TextView listText;
    static TextView alarmText;
    SwipeRefreshLayout swipe_refresh;
    FrameLayout mapView;
    ToggleButton tbutton;

    int delayed = 300000;
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            new refreshDevicesList().execute();
            timerHandler.postDelayed(this, delayed);
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_devices_list, container, false);
        refreshList = (LinearLayout) v.findViewById(R.id.refreshList);
        devicesList = (ListView) v.findViewById(R.id.devicesList);
        deviceText = (TextView) v.findViewById(R.id.deviceText);
        connectedText = (TextView) v.findViewById(R.id.connectedText);
        disconnectedText = (TextView) v.findViewById(R.id.disconnectedText);
        mapText = (TextView) v.findViewById(R.id.mapText);
        listText = (TextView) v.findViewById(R.id.listText);
        alarmText = (TextView) v.findViewById(R.id.alarmText);
        swipe_refresh = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh);
        mapView = (FrameLayout) v.findViewById(R.id.mapView);
        tbutton = (ToggleButton) v.findViewById(R.id.toggleButton1);

        /*
        subscribeNumber(topic);

        publish(Ptopic, msg);

        subscribeNumber("$aws/things/IoT/shadow/update/accepted");
        */
        PubSubJSON.subscribeNumber(MainActivity.GETACCEPTED);

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("Checked")) {
                listText.setTextColor(Color.parseColor("#000000"));
                mapText.setTextColor(Color.parseColor("#CCCCCC"));
                refreshList.setVisibility(View.VISIBLE);
                mapView.setVisibility(View.GONE);
            } else {
                listText.setTextColor(Color.parseColor("#CCCCCC"));
                mapText.setTextColor(Color.parseColor("#000000"));
                refreshList.setVisibility(View.GONE);
                mapView.setVisibility(View.VISIBLE);
            }
        } else {
            mapView.setVisibility(View.GONE);
        }

        tbutton.setOnClickListener(new View.OnClickListener() {

            @SuppressLint("ResourceAsColor")
            @Override
            public void onClick(View v) {
                if (tbutton.isChecked()) {
                    listText.setTextColor(Color.parseColor("#000000"));
                    mapText.setTextColor(Color.parseColor("#AAAAAA"));
                    refreshList.setVisibility(View.VISIBLE);
                    mapView.setVisibility(View.GONE);
                } else {
                    listText.setTextColor(Color.parseColor("#AAAAAA"));
                    mapText.setTextColor(Color.parseColor("#000000"));
                    refreshList.setVisibility(View.GONE);
                    mapView.setVisibility(View.VISIBLE);
                }
            }
        });

        adapter = new CustomAdapter(getActivity(), devices);
        devicesList.setAdapter(adapter);

        devicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                timerHandler.removeCallbacks(timerRunnable);

                deviceNameValue = String.valueOf(parent.getItemAtPosition(position));

                PubSubJSON.subscribeDevice(MainActivity.GETACCEPTED, deviceNameValue);
                publish(MainActivity.GET, "");

                Intent deviceIntent = new Intent(getActivity(), DeviceActivity.class);
                startActivity(deviceIntent);
                MainActivity.tabCurrentPosition = 1;
            }
        });

        swipe_refresh.setOnRefreshListener(this);
        if (start == true) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        new refreshDevicesList().execute();
                    }
                }, 600);
            start = false;
        }

        timerHandler.postDelayed(timerRunnable, delayed);
        return v;
    }

    @Override
    public void onRefresh() {
        timerHandler.removeCallbacks(timerRunnable);
        if (getDeviceNumber().equals(1000)){
            getActivity().recreate();
        }
        ((MainActivity)getActivity()).notiTabIcon();
        new refreshDevicesList().execute();
        timerHandler.postDelayed(timerRunnable, delayed);
    }

    private class refreshDevicesList extends AsyncTask<Void, Void, Void> implements OnMapReadyCallback {

        int alarmColor;
        String devicesNumber = String.valueOf(getDeviceNumber());
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapView);
        @Override
        protected Void doInBackground(Void... voids) {
            subscribeNumber(MainActivity.UPDATEACCEPTED);
            publish(MainActivity.GET, "");
            if (swipe_refresh.isRefreshing()) {
                swipe_refresh.setRefreshing(false);
            }
            if (NotiList.noti.size() > 0) {
                alarmColor = Color.RED;
            } else {
                alarmColor = Color.BLACK;
            }
            if (getDeviceNumber() >= 0 && getDeviceNumber() <= 9) {
                devicesNumber = "0" + String.valueOf(getDeviceNumber());
            }
            adapter = new CustomAdapter(getActivity(), devices);
            if (mapFragment == null) {
                FragmentManager fm = getChildFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                mapFragment = SupportMapFragment.newInstance();
                ft.replace(R.id.mapView, mapFragment).commit();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            alarmText.setText(NotiList.noti.size() + " alarms");
            alarmText.setTextColor(alarmColor);
            deviceText.setText(devicesNumber + " devices");
            connectedText.setText(devicesNumber + " connected");
            devicesList.setAdapter(adapter);
            mapFragment.getMapAsync(this);
        }

        @Override
        public void onMapReady(GoogleMap googleMap) {
            int marker;
            BitmapDrawable bitMarker;

            mMap = googleMap;
            LatLng fville;
            mMap.clear();
            LatLng defo = new LatLng(21.03053, 105.7828);
            if (getDeviceNumber() > 0) {
                for (int i = 0; i < getDeviceNumber(); i++) {
                    fville = new LatLng(latLng.getDouble("Lat" + String.valueOf(i)), latLng.getDouble("Lng" + String.valueOf(i)));
                    if(alarmedDevice.getBoolean(devices[i])){
                        marker = R.drawable.map_marker_red;
                    } else {
                        marker = R.drawable.map_marker_green;
                    }
                    bitMarker = (BitmapDrawable)getResources().getDrawable(marker);
                    Bitmap b = bitMarker.getBitmap();
                    Bitmap smallMarker = Bitmap.createScaledBitmap(b, 90, 90, false);
                    mMap.addMarker(new MarkerOptions().position(fville).title(devices[i]).icon(BitmapDescriptorFactory.fromBitmap(smallMarker)));
                }
            }
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defo, 17.5f));
        }
    }

    class CustomAdapter extends ArrayAdapter<String> {

        public CustomAdapter(Context context, String[] data) {
            super(context, R.layout.activity_listview, data);
        }

        @Override
        public int getCount() {
            return getDeviceNumber();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int tint = Color.parseColor("#ff0000");;
            LayoutInflater mInflater = LayoutInflater.from(getContext());
            View customView = convertView;
            if (customView == null) {
                customView = mInflater.inflate(R.layout.activity_listview, parent, false);
            }

            ImageView color_status = (ImageView) customView.findViewById(R.id.color_status);
            ImageView deviceIcon = (ImageView) customView.findViewById(R.id.deviceIcon);
            TextView textView = (TextView) customView.findViewById(R.id.textView);
            TextView subTextView = (TextView) customView.findViewById(R.id.subTextView);

            String deviceItem = "";
            try{
                deviceItem = getItem(position);
                if(alarmedDevice.getBoolean(devices[position])){
                    tint = Color.parseColor("#ff0000");
                } else {
                    tint = Color.parseColor("#1c7c1b");
                }
            } catch (Exception e){
                e.printStackTrace();
            }
            color_status.setBackgroundColor(tint);
            deviceIcon.setImageResource(R.drawable.device_list);
            deviceIcon.setColorFilter(tint);
            textView.setText(deviceItem);
            subTextView.setText("Gateway");

            return customView;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("Checked", tbutton.isChecked());
    }
}
