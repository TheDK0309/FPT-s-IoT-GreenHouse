package com.example.t_anh.iot_aws_v2;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import static com.example.t_anh.iot_aws_v2.DeviceActivity.deviceNameValue;
import static com.example.t_anh.iot_aws_v2.MainActivity.getDeviceNumber;
import static com.example.t_anh.iot_aws_v2.PubSubJSON.publish;
import static com.example.t_anh.iot_aws_v2.PubSubJSON.subscribeDevice;
import static com.example.t_anh.iot_aws_v2.PubSubJSON.subscribeNumber;

public class NotiList extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private boolean start = true;

    public static Bundle noti = new Bundle();
    public static Bundle notiSub = new Bundle();
    public static Bundle notiSub2 = new Bundle();
    public static Bundle notiIcon = new Bundle();
    String[] notif = new String[noti.size()];

    CustomAdapter adapter;

    private SwipeRefreshLayout swipeRefreshLayout;
    ListView notiList;
    static TextView alarmText;

    int delayed = 300000;
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            new refreshNoti().execute();
            timerHandler.postDelayed(this, delayed);
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_noti_list, container, false);
        swipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_layout);
        notiList = (ListView) v.findViewById(R.id.notiList);
        alarmText = (TextView) v.findViewById(R.id.alarmText);

        alarmText.setText(noti.size() + " alarms");
        if (noti.size() > 0) {
            DevicesList.alarmText.setTextColor(Color.RED);
            alarmText.setTextColor(Color.RED);
        } else {
            DevicesList.alarmText.setTextColor(Color.BLACK);
            alarmText.setTextColor(Color.BLACK);
        }

        /*
        subscribeNumber(topic);
        publish(Ptopic, msg);
        subscribeNumber("$aws/things/IoT/shadow/update/accepted");
        */
        try {
            for (int a = 0; a < noti.size(); a++) {
                notif[a] = (String) noti.getCharSequence(PubSubJSON.NOTI + String.valueOf(a));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        adapter = new CustomAdapter(getActivity(), notif);
        notiList.setAdapter(adapter);

        notiList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                timerHandler.removeCallbacks(timerRunnable);

                deviceNameValue = String.valueOf(parent.getItemAtPosition(position));

                subscribeDevice(MainActivity.UPDATEACCEPTED, deviceNameValue);
                publish(MainActivity.GET, "");

                Intent deviceIntent = new Intent(getActivity(), DeviceActivity.class);
                startActivity(deviceIntent);
                MainActivity.tabCurrentPosition = 2;
            }
        });

        swipeRefreshLayout.setOnRefreshListener(this);
        if (start) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    new refreshNoti().execute();
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
        if (getDeviceNumber().equals(1000)) {
            getActivity().recreate();
        }
        ((MainActivity)getActivity()).notiTabIcon();
        new refreshNoti().execute();
        timerHandler.postDelayed(timerRunnable, delayed);
    }

    private class refreshNoti extends AsyncTask<Void, Void, Void> {
        int alarmColor;

        @Override
        protected Void doInBackground(Void... voids) {
            publish(MainActivity.GET, "");
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            if (noti.size() > 0) {
                alarmColor = Color.RED;
            } else {
                alarmColor = Color.BLACK;
            }
            notif = new String[noti.size()];
            try {
                for (int a = 0; a < noti.size(); a++) {
                    notif[a] = (String) noti.getCharSequence(PubSubJSON.NOTI + String.valueOf(a));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            alarmText.setText(noti.size() + " alarms");

            DevicesList.alarmText.setTextColor(alarmColor);
            alarmText.setTextColor(alarmColor);
            adapter = new CustomAdapter(getActivity(), notif);
            notiList.setAdapter(adapter);
        }
    }

    class CustomAdapter extends ArrayAdapter<String> {

        public CustomAdapter(Context context, String[] data) {
            super(context, R.layout.activity_noti_listview, data);
        }

        @Override
        public int getCount() {
            return notif.length;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater mInflater = LayoutInflater.from(getContext());
            View customView = convertView;
            if (customView == null) {
                customView = mInflater.inflate(R.layout.activity_noti_listview, parent, false);
            }

            ImageView color_status = (ImageView) customView.findViewById(R.id.color_status);
            ImageView deviceIcon = (ImageView) customView.findViewById(R.id.deviceIcon);
            TextView textView = (TextView) customView.findViewById(R.id.textView);
            TextView subTextView = (TextView) customView.findViewById(R.id.subTextView);
            TextView subTextView2 = (TextView) customView.findViewById(R.id.subTextView2);

            String deviceItem = "";
            try {
                deviceItem = getItem(position);
            } catch (Exception e) {
                e.printStackTrace();
            }
            color_status.setBackgroundColor(Color.parseColor("#22FF1111"));
            deviceIcon.setImageResource(notiIcon.getInt(PubSubJSON.NOTIICON + String.valueOf(position)));
            textView.setText(deviceItem);
            subTextView.setText(notiSub.getCharSequence(PubSubJSON.NOTISUB + String.valueOf(position)));
            subTextView2.setText(notiSub2.getCharSequence(PubSubJSON.NOTISUB2 + String.valueOf(position)));

            return customView;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }
}
