package com.example.t_anh.iot_aws_v2;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import static com.example.t_anh.iot_aws_v2.DevicesList.alarmedDevice;
import static com.example.t_anh.iot_aws_v2.MainActivity.reportedJSON;
import static com.example.t_anh.iot_aws_v2.PubSubJSON.publish;
import static com.example.t_anh.iot_aws_v2.PubSubJSON.subscribeDevice;

public class DeviceActivity extends FragmentActivity {
    static String LOG_TAG = DeviceActivity.class.getCanonicalName();

    private GoogleMap mMap;
    Calendar calendar = Calendar.getInstance();
    Date c = calendar.getTime();
    SimpleDateFormat df = new SimpleDateFormat("hh:mm  dd-MM-yyyy");
    String formattedDate = df.format(c);
    LineGraphSeries<DataPoint> series;
    String[] timeInterval = new String[8];
    int timeStep = Calendar.DATE;
    int step = 1;
    SimpleDateFormat d = new SimpleDateFormat("EEE");
    SimpleDateFormat dt = new SimpleDateFormat("dd/MM/yyyy");
    SimpleDateFormat ddt = new SimpleDateFormat("HH mm dd MMM yyyy");
    DataPoint[] data = new DataPoint[timeInterval.length];
    Integer lastVal = values.getInt("Temperature");

    public static String deviceNameValue = "";
    final String msg = "";
    static Bundle values = new Bundle();
    private String pumpState = "OFF";
    private String ledState = "OFF";
    private String nameOfChart = "Temperature chart";
    private String durationOfChart = "DATE";
    private String startDate = "", endDate = "", ddtDate = "";

    private ArrayList<Long> timestampDB = null;
    private ArrayList<String> labelDB = null;
    private ArrayList<String> temperatureDB = null;
    private ArrayList<String> humidityDB = null;
    private ArrayList<String> soilMoistureDB = null;
    private ArrayList<String> lightIntensityDB = null;
    private ArrayList<DynamoDBManager.IotHistory> itemDB = null;

    Double preLat = 0.0;
    Double preLng = 0.0;

    Integer delayed = 500;

    Random r = new Random();

    Button btnBack;
    Button chartName;
    Button chartDuration;
    Button btnRefresh;
    Button btnPump;
    Button btnLED;
    Button btnUpdate;

    TextView dateTime;
    TextView deviceName;
    TextView temperatureValue;
    TextView humidityValue;
    TextView moistValue;
    TextView lightValue;
    TextView pumpText;
    TextView ledText;
    TextView batteryText;
    TextView alarmDevice;
    TextView chartTime;
    TextView locationText;

    ImageView batteryIcon;
    ImageView AlarmIcon;
    GraphView graph;

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            new getDeviceInShadow().execute();
            timerHandler.postDelayed(this, delayed);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        btnBack = (Button) findViewById(R.id.btnBack);
        chartName = (Button) findViewById(R.id.chartName);
        chartDuration = (Button) findViewById(R.id.chartDuration);
        btnRefresh = (Button) findViewById(R.id.btnRefresh);
        btnPump = (Button) findViewById(R.id.btnPump);
        btnLED = (Button) findViewById(R.id.btnLED);
        btnUpdate = (Button) findViewById(R.id.btnUpdate);

        dateTime = (TextView) findViewById(R.id.dateTime);
        deviceName = (TextView) findViewById(R.id.deviceName);
        temperatureValue = (TextView) findViewById(R.id.temperatureValue);
        humidityValue = (TextView) findViewById(R.id.humidityValue);
        moistValue = (TextView) findViewById(R.id.moistValue);
        lightValue = (TextView) findViewById(R.id.lightValue);
        pumpText = (TextView) findViewById(R.id.pumpText);
        ledText = (TextView) findViewById(R.id.ledText);
        batteryText = (TextView) findViewById(R.id.batteryText);
        alarmDevice = (TextView) findViewById(R.id.alarmDevice);
        chartTime = (TextView) findViewById(R.id.chartTime);
        locationText = (TextView) findViewById(R.id.locationText);

        batteryIcon = (ImageView) findViewById(R.id.batteryIcon);
        AlarmIcon = (ImageView) findViewById(R.id.AlarmIcon);

        graph = (GraphView) findViewById(R.id.chart);

        deviceName.setText(deviceNameValue);
        AlarmIcon.setImageResource(R.drawable.status_light);
        batteryIcon.setImageResource(R.drawable.status_light);
        //AlarmIcon.setColorFilter(Color.parseColor("#b1ff90"));
        alarmDevice.setText("No alarm");

        pumpState = "OFF";
        ledState = "OFF";

        if (savedInstanceState != null) {
            pumpState = String.valueOf(savedInstanceState.getCharSequence(PubSubJSON.PUMP));
            ledState = String.valueOf(savedInstanceState.getCharSequence(PubSubJSON.LED));
        }

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timerHandler.removeCallbacks(timerRunnable);
                publish(MainActivity.GET, msg);
                timerHandler.postDelayed(timerRunnable, delayed);
            }
        });

        btnPump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnPump.getText().toString() == "pump ON") {
                    pumpState = "OFF";
                    btnPump.setText("pump OFF");
                } else {
                    pumpState = "ON";
                    btnPump.setText("pump ON");
                }
            }
        });

        btnLED.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnLED.getText().toString() == "LED ON") {
                    ledState = "OFF";
                    btnLED.setText("LED OFF");
                } else {
                    ledState = "ON";
                    btnLED.setText("LED ON");
                }
            }
        });

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timerHandler.removeCallbacks(timerRunnable);
                new updateDeviceInShadow().execute();
                timerHandler.postDelayed(timerRunnable, delayed);
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timerHandler.removeCallbacks(timerRunnable);
                Intent homeIntent = new Intent(DeviceActivity.this, MainActivity.class);
                homeIntent.putExtra("backTab", 2);
                startActivity(homeIntent);
            }
        });

        chartName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final PopupMenu popupMenu = new PopupMenu(DeviceActivity.this, chartName);
                popupMenu.getMenuInflater().inflate(R.menu.menu_chart, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        nameOfChart = String.valueOf(item.getTitle());
                        chartName.setText(nameOfChart);
                        switch (nameOfChart) {
                            case "Temperature chart":
                                lastVal = values.getInt(PubSubJSON.TEMPERATURE);
                                break;
                            case "Humidity chart":
                                lastVal = values.getInt(PubSubJSON.HUMIDITY);
                                break;
                            case "Light chart":
                                lastVal = values.getInt(PubSubJSON.LIGHTINTENSITY);
                                break;
                            case "Moisture chart":
                                lastVal = values.getInt(PubSubJSON.SOILMOISTURE);
                                break;
                            default:
                                break;
                        }
                        new getGraph().execute();
                        return true;
                    }
                });
                popupMenu.show();
            }
        });

        chartDuration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final PopupMenu popupMenu = new PopupMenu(DeviceActivity.this, chartDuration);
                popupMenu.getMenuInflater().inflate(R.menu.menu_chart_time, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        durationOfChart = String.valueOf(item.getTitle());
                        chartDuration.setText(durationOfChart);
                        switch (durationOfChart) {
                            case "MINUTE":
                                timeInterval = new String[61];
                                timeStep = Calendar.MINUTE;
                                step = 1;
                                d = new SimpleDateFormat("HH:mm");
                                dt = new SimpleDateFormat("HH:mm  dd MMM yyyy");
                                break;
                            case "HOUR":
                                timeInterval = new String[25];
                                timeStep = Calendar.HOUR;
                                step = 1;
                                d = new SimpleDateFormat("HH");
                                dt = new SimpleDateFormat("HH'h': dd MMM yyyy");
                                break;
                            case "DATE":
                                timeInterval = new String[8];
                                timeStep = Calendar.DATE;
                                step = 1;
                                d = new SimpleDateFormat("EEE");
                                dt = new SimpleDateFormat("dd MMM yyyy");
                                break;
                            case "MONTH":
                                timeInterval = new String[13];
                                timeStep = Calendar.MONTH;
                                step = 1;
                                d = new SimpleDateFormat("MM");
                                dt = new SimpleDateFormat("MMM yyyy");
                                break;
                            default:
                                break;
                        }
                        new getGraph().execute();
                        return true;
                    }
                });
                popupMenu.show();
            }
        });

        //subcribe
        Log.d(LOG_TAG, "topic = " + MainActivity.UPDATEACCEPTED);
        subscribeDevice(MainActivity.GETACCEPTED, deviceNameValue);
        timerHandler.postDelayed(timerRunnable, delayed);
    }

    private class getGraph extends AsyncTask<Void, Void, String> {
        int maxY = 10;
        StaticLabelsFormatter staticLabelsFormatter;
        String tempDate = "";
        double tempVal = 0;
        Integer tempDivide = 1;

        @RequiresApi(api = Build.VERSION_CODES.O)
        protected String doInBackground(Void... voids) {
            String tableStatus = DynamoDBManager.getTableStatus();
            if (tableStatus.equalsIgnoreCase("ACTIVE")) {
                calendar = Calendar.getInstance();
                if (!ddtDate.equals(ddt.format(calendar.getTime()))) {
                    timestampDB = new ArrayList<>();
                    temperatureDB = new ArrayList<>();
                    humidityDB = new ArrayList<>();
                    soilMoistureDB = new ArrayList<>();
                    lightIntensityDB = new ArrayList<>();
                    itemDB = DynamoDBManager.getIotHistory();
                    for (DynamoDBManager.IotHistory up : itemDB) {
                        timestampDB.add(up.getTime1());
                        temperatureDB.add(up.getTemperature());
                        humidityDB.add(up.getHumidity());
                        soilMoistureDB.add(up.getSoilMoisture());
                        lightIntensityDB.add(up.getLightIntensity());
                    }
                    ddtDate = ddt.format(calendar.getTime());
                }
                endDate = dt.format(calendar.getTime());
                data = new DataPoint[timeInterval.length];
                calendar.add(timeStep, -timeInterval.length * step + step);
                startDate = dt.format(calendar.getTime());
                for (int i = 0; i < timeInterval.length; i++) {
                    tempVal = 0;
                    tempDivide = 1;
                    timeInterval[i] = d.format(calendar.getTime());
                    if(durationOfChart.equals("HOUR")){
                        if ((i%2) != 0){
                            timeInterval[i] = "";
                        }
                    } else if(durationOfChart.equals("MINUTE")) {
                        if ((i%10) != 0){
                            timeInterval[i] = "";
                        }
                    }
                    for (int u = 0; u < timestampDB.size(); u++) {
                        tempDate = dt.format(new Date(timestampDB.get(u)));
                        if (!tempDate.equals(dt.format(calendar.getTime()))) {
                            continue;
                        } else {
                            switch (nameOfChart) {
                                case "Temperature chart":
                                    tempVal = ((tempDivide - 1) * tempVal + Integer.parseInt(temperatureDB.get(u))) / tempDivide;
                                    break;
                                case "Humidity chart":
                                    tempVal = ((tempDivide - 1) * tempVal + Integer.parseInt(humidityDB.get(u))) / tempDivide;
                                    break;
                                case "Light chart":
                                    tempVal = ((tempDivide - 1) * tempVal + Integer.parseInt(lightIntensityDB.get(u))) / tempDivide;
                                    break;
                                case "Moisture chart":
                                    tempVal = ((tempDivide - 1) * tempVal + Integer.parseInt(soilMoistureDB.get(u))) / tempDivide;
                                    break;
                                default:
                                    break;
                            }
                            if (maxY < (int) tempVal) {
                                maxY = (int) tempVal;
                            }
                            tempDivide++;
                        }
                    }
                    data[i] = new DataPoint(i, tempVal);
                    calendar.add(timeStep, step);
                }
            } else {
                for (int i = 0; i < timeInterval.length; i++) {
                    data[i] = new DataPoint(i, 0);
                }
            }
            maxY = (Math.abs(maxY * 5 / 4) + 10);
            //data[timeInterval.length - 1] = new DataPoint(timeInterval.length - 1, lastVal);
            series = new LineGraphSeries<>(data);
            staticLabelsFormatter = new StaticLabelsFormatter(graph);

            return null;
        }

        protected void onPostExecute(String result) {
            chartTime.setText(startDate + " - " + endDate);

            //if (tableStatus.equalsIgnoreCase("ACTIVE")) {
            staticLabelsFormatter.setHorizontalLabels(timeInterval);
            graph.getViewport().setYAxisBoundsManual(true);
            //graph.getViewport().setMinY(0);
            graph.getViewport().setMaxY(maxY);
            graph.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);
            //graph.getGridLabelRenderer().setNumVerticalLabels(8);

            series.setDrawBackground(true);
            series.setColor(Color.parseColor("#FF0000"));
            series.setThickness(6);
            series.setBackgroundColor(Color.parseColor("#55FF0000"));

            graph.removeAllSeries();
            graph.addSeries(series);
        }
    }

    private class getDeviceInShadow extends AsyncTask<Void, Void, Void> implements OnMapReadyCallback {
        String t = "";
        String t2 = "";
        SpannableStringBuilder alarmText;
        int alarmColor;
        int batteryColor;
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        protected Void doInBackground(Void... voids) {

            if (alarmedDevice.getBoolean(deviceNameValue)) {
                for (int i = 0; i < NotiList.noti.size(); i++) {
                    if (String.valueOf(NotiList.noti.getCharSequence(PubSubJSON.NOTI + String.valueOf(i))).equals(deviceNameValue)) {
                        t = t + "\n" + NotiList.notiSub.getCharSequence(PubSubJSON.NOTISUB + String.valueOf(i));
                        t2 = (String) NotiList.notiSub2.getCharSequence(PubSubJSON.NOTISUB2 + String.valueOf(i));
                    }
                }
                alarmColor = Color.parseColor("#FF0000");
                alarmText = new SpannableStringBuilder("Alarm:" + t);
                alarmText.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                alarmColor = Color.parseColor("#b1ff90");
                alarmText = new SpannableStringBuilder("Everything is fine");
                t2 = formattedDate;
            }
            if (values.getDouble(PubSubJSON.VPIN) < 50.0 || values.getDouble(PubSubJSON.VPIN) > 150.0) {
                batteryColor = Color.parseColor("#FF0000");
            } else {
                batteryColor = Color.parseColor("#b1ff90");
            }
            return null;
        }

        protected void onPostExecute(Void result) {
            DecimalFormat df1 = new DecimalFormat(".#");
            temperatureValue.setText(String.valueOf(values.getInt(PubSubJSON.TEMPERATURE)) + " \u2103");
            humidityValue.setText(String.valueOf(values.getInt(PubSubJSON.HUMIDITY)) + " %");
            moistValue.setText(String.valueOf(values.getInt(PubSubJSON.SOILMOISTURE)) + " %");
            lightValue.setText(String.valueOf(df1.format(values.getInt(PubSubJSON.LIGHTINTENSITY) / 1000.0)) + "k lux");
            pumpText.setText("Pump: " + String.valueOf(values.getCharSequence(PubSubJSON.PUMP)));
            ledText.setText("LED: " + String.valueOf(values.getCharSequence(PubSubJSON.LED)));
            batteryText.setText("Battery: " + String.valueOf(values.getDouble(PubSubJSON.VPIN) / 10) + " V");
            dateTime.setText(t2);
            alarmDevice.setText(alarmText);
            batteryIcon.setColorFilter(batteryColor);
            AlarmIcon.setColorFilter(alarmColor);
            //fville = new LatLng(values.getDouble("Lat"), values.getDouble("Lng"));

            if (preLat != values.getDouble(PubSubJSON.LAT) || preLng != values.getDouble(PubSubJSON.LNG)) {
                mapFragment.getMapAsync(this);
                preLat = values.getDouble(PubSubJSON.LAT);
                preLng = values.getDouble(PubSubJSON.LNG);
            }
        }

        @Override
        public void onMapReady(GoogleMap googleMap) {
            int marker;
            mMap = googleMap;
            locationText.setText("Location ");
            LatLng fville = new LatLng(values.getDouble(PubSubJSON.LAT), values.getDouble(PubSubJSON.LNG));
            mMap.clear();
            if (alarmedDevice.getBoolean(deviceNameValue)) {
                marker = R.drawable.map_marker_red;
            } else {
                marker = R.drawable.map_marker_green;
            }
            mMap.addMarker(new MarkerOptions().position(fville).title(deviceNameValue).icon(BitmapDescriptorFactory.fromResource(marker)));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fville, 18.0f));
        }
    }

    private class updateDeviceInShadow extends AsyncTask<Void, Void, String> {

        protected String doInBackground(Void... voids) {
            publish(MainActivity.UPDATE, reportedJSON + "\"" + deviceNameValue + "\":{\"Pump\":\"" + pumpState + "\"}}}}");
            publish(MainActivity.UPDATE, reportedJSON + "\"" + deviceNameValue + "\":{\"LED\":\"" + ledState + "\"}}}}");
            //subscribeDevice(MainActivity.GETACCEPTED, deviceNameValue);
            publish(MainActivity.GET, msg);
            return null;
        }

        protected void onPostExecute(String result) {
            Toast.makeText(DeviceActivity.this, "update", Toast.LENGTH_SHORT).show();
        }
    }

    private class getDB extends AsyncTask<Void, Void, String> {
        long timeDB = 0;
        String s = "";
        String message = "";
        SimpleDateFormat format = new SimpleDateFormat("hh:mm  dd-MM-yyyy");

        @RequiresApi(api = Build.VERSION_CODES.O)
        protected String doInBackground(Void... voids) {
            String tableStatus = DynamoDBManager.getTableStatus();
            message = tableStatus;
            if (tableStatus.equalsIgnoreCase("ACTIVE")) {
                timestampDB = new ArrayList<>();
                labelDB = new ArrayList<>();
                itemDB = DynamoDBManager.getIotHistory();
                for (DynamoDBManager.IotHistory up : itemDB) {
                    timestampDB.add(up.getTime1());
                    labelDB.add(up.getHumidity());
                }

                for (int i = 0; i < itemDB.size(); i++) {
                    timeDB = timestampDB.get(i);
                    s = labelDB.get(i);
                    message = message + "\n" + String.valueOf(format.format(new Date(timeDB))) + "   " + s;
                }
                //timeDB = timestampDB.get(0);
                //message = String.valueOf(timeDB) + " " + String.valueOf(format.format(new Date(timeDB))) + " " + s;
            }
            return null;
        }

        protected void onPostExecute(String result) {

        }
    }

    /*
    @Override
    protected void onStop() {
        super.onStop();
        LoadingActivity.disconnectAWS();
    }
*/

    @Override
    protected void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        timerHandler.postDelayed(timerRunnable, delayed);
    }

    @Override
    public void onBackPressed() {
        timerHandler.removeCallbacks(timerRunnable);
        Intent homeIntent = new Intent(DeviceActivity.this, MainActivity.class);
        homeIntent.putExtra("backTab", 2);
        startActivity(homeIntent);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence("pump", pumpState);
        outState.putCharSequence("LED", ledState);
    }
}




try {
    mqttManager.publishString("Hello to all subscribers!", "myTopic", AWSIotMqttQos.QOS0);
} catch (Exception e) {
    Log.e(LOG_TAG, "Publish error: ", e);
}

