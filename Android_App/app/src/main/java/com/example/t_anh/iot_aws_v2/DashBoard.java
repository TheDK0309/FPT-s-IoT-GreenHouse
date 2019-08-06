package com.example.t_anh.iot_aws_v2;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import static com.example.t_anh.iot_aws_v2.MainActivity.getDeviceNumber;
import static com.example.t_anh.iot_aws_v2.PubSubJSON.publish;
import static com.example.t_anh.iot_aws_v2.PubSubJSON.subscribeNumber;

public class DashBoard extends Fragment implements SwipeRefreshLayout.OnRefreshListener{
    private boolean start = true;

    SwipeRefreshLayout refreshDashboard;
    static ImageView statusIcon;
    static TextView statusText;
    TextView deviceNumber;
    TextView connectedNumber;
    TextView disConnectedNumber;
    Button btnUp;
    int n = getDeviceNumber();

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            new setRefreshDashboard().execute();
            timerHandler.postDelayed(this, 300000);
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, final Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_dashboard, container,false);
        refreshDashboard = (SwipeRefreshLayout) v.findViewById(R.id.refreshDashboard);
        statusIcon = (ImageView) v.findViewById(R.id.statusIcon);
        statusText = (TextView) v.findViewById(R.id.statusText);
        deviceNumber = (TextView) v.findViewById(R.id.deviceNumber);
        connectedNumber = (TextView) v.findViewById(R.id.connectedNumber);
        disConnectedNumber = (TextView) v.findViewById(R.id.disConnectedNumber);
        btnUp = (Button) v.findViewById(R.id.btnUp);

        refreshDashboard.setOnRefreshListener(this);

        /*
        subscribeNumber(topic);

        publish(Ptopic, msg);
        subscribeNumber("$aws/things/IoT/shadow/update/accepted");
*/
        String devicesNumber = String.valueOf(n);
        if(n >= 0 && n <= 9){
            devicesNumber = "0" + String.valueOf(n);
        }

        deviceNumber.setText(devicesNumber);
        connectedNumber.setText(devicesNumber);

        if (start == true){
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        new setRefreshDashboard().execute();
                    }
                }, 300);

            start = false;
        }

        //timerHandler.postDelayed(timerRunnable, 300000);
        return v;
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("key", n);
    }

    @Override
    public void onRefresh() {
        if (getDeviceNumber().equals(1000)){
            getActivity().recreate();
        }
        ((MainActivity)getActivity()).notiTabIcon();
        new setRefreshDashboard().execute();
    }

    private class setRefreshDashboard extends AsyncTask<Void, Void, Void> {
        String devicesNumber = String.valueOf(n);
        Integer alarmColor;
        String alarmText;
        @Override
        protected Void doInBackground(Void... voids) {

            publish(MainActivity.GET, "");
            if (refreshDashboard.isRefreshing()) {
                refreshDashboard.setRefreshing(false);
            }
            n = getDeviceNumber();
            if(n >= 0 && n <= 9){
                devicesNumber = "0" + String.valueOf(n);
            }
            if (NotiList.noti.size() > 0){
                alarmColor = Color.parseColor("#FF0000");
                alarmText = "Something is wrong";
            } else {
                alarmColor= Color.parseColor("#b1ff90");
                alarmText = "Everything is fine";
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            DashBoard.statusIcon.setColorFilter(alarmColor);
            DashBoard.statusText.setText(alarmText);
            deviceNumber.setText(devicesNumber);
            connectedNumber.setText(devicesNumber);
        }
    }
}
