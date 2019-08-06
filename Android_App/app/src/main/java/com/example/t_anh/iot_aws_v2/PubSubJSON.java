
package com.example.t_anh.iot_aws_v2;

import android.util.Log;

import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static com.example.t_anh.iot_aws_v2.DevicesList.alarmedDevice;
import static com.example.t_anh.iot_aws_v2.DevicesList.devices;

import static com.example.t_anh.iot_aws_v2.MainActivity.getDeviceNumber;
import static com.example.t_anh.iot_aws_v2.MainActivity.setDeviceNumber;
import static com.example.t_anh.iot_aws_v2.SplashActivity.mqttManager;

public class PubSubJSON {

    public static final String STATE = "state";
    public static final String METADATA = "metadata";
    public static final String REPORTED = "reported";
    public static final String TEMPERATURE = "Temperature";
    public static final String HUMIDITY = "Humidity";
    public static final String LIGHTINTENSITY = "LightIntensity";
    public static final String SOILMOISTURE = "SoilMoisture";
    public static final String VPIN = "Vpin";
    public static final String PUMP = "Pump";
    public static final String LED = "LED";
    public static final String LAT = "Lat";
    public static final String LNG = "Lng";
    public static final String TIMESTAMP = "timestamp";

    public static final String NOTI = "noti";
    public static final String NOTISUB = "notiSub";
    public static final String NOTISUB2 = "notiSub2";
    public static final String NOTIICON = "notiIcon";

    private static String LOG_TAG = PubSubJSON.class.getCanonicalName();
    static int numberOfNoti = 0;

    public static void subscribeNumber(String topic) {
        Log.d(LOG_TAG, "topic = " + topic);
        final String[] message = new String[1];
        try {
            mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
                    new AWSIotMqttNewMessageCallback() {
                        @Override
                        public void onMessageArrived(final String topic, final byte[] data) {
                            try {
                                message[0] = new String(data, "UTF-8");
                                Log.d(LOG_TAG, "AWSMessage arrived:");
                                Log.d(LOG_TAG, "   Topic: " + topic);
                                Log.d(LOG_TAG, " AWSMessage: " + message[0]);
                                JSONdeviceNumber(message[0]);
                                JSONnoti(message[0]);
                            } catch (UnsupportedEncodingException e) {
                                Log.e(LOG_TAG, "AWSMessage encoding error.", e);
                            }
                        }
                    });
        } catch (Exception e) {
            Log.e(LOG_TAG, "Subscription error.", e);
        }
    }

    public static void subscribeDevice(String topic, final String device) {
        Log.d(LOG_TAG, "topic = " + topic);
        final String[] message = new String[1];
        try {
            mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
                    new AWSIotMqttNewMessageCallback() {
                        @Override
                        public void onMessageArrived(final String topic, final byte[] data) {
                            try {
                                message[0] = new String(data, "UTF-8");
                                JSONdevice(message[0], device);
                                JSONnoti(message[0]);
                            } catch (UnsupportedEncodingException e) {
                                Log.e(LOG_TAG, "AWSMessage encoding error.", e);
                            }
                        }
                        //});
                        //}
                    });
        } catch (Exception e) {
            Log.e(LOG_TAG, "Subscription error.", e);
        }
    }

    public static void publish(String topic, String msg) {
        try {
            mqttManager.publishString(msg, topic, AWSIotMqttQos.QOS0);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Publish error.", e);
        }
    }

    public static void JSONdeviceNumber(final String string) {
        try {
            JSONObject json = new JSONObject(string);
            JSONObject state = json.getJSONObject(STATE);
            JSONObject reported = state.getJSONObject(REPORTED);
            setDeviceNumber(reported.length() - 1);
            JSONArray deviceName = reported.names();
            DevicesList.latLng.clear();
            ArrayList<String> mStringList = new ArrayList<String>();
            for (int i = 0; i < reported.length(); i++) {
                mStringList.add(deviceName.optString(i));
            }
            mStringList.remove("groupID");
            devices = new String[mStringList.size()];
            devices = mStringList.toArray(devices);
            for (int i = 0; i < devices.length; i++) {
                JSONObject device = reported.getJSONObject(devices[i]);
                Double latVal = checkJSONdouble(device, LAT);
                Double lngVal = checkJSONdouble(device, LNG);
                DevicesList.latLng.putDouble(LAT + String.valueOf(i), latVal);
                DevicesList.latLng.putDouble(LNG + String.valueOf(i), lngVal);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void JSONdevice(String string, String deviceName) {
        try {
            JSONObject json = new JSONObject(string);
            JSONObject state = json.getJSONObject(STATE);
            JSONObject reported = state.getJSONObject(REPORTED);
            JSONObject device = reported.getJSONObject(deviceName);
            Integer temperatureVal = checkJSONint(device, TEMPERATURE);
            Integer humidityVal = checkJSONint(device, HUMIDITY);
            Integer LightIntensityVal = checkJSONint(device, LIGHTINTENSITY);
            Integer moistVal = checkJSONint(device, SOILMOISTURE);
            Integer batteryVal = checkJSONint(device, VPIN);
            //Integer pHVal = checkJSONint(device, "pH");
            String pumpVal = checkJSONstring(device, PUMP);
            String ledVal = checkJSONstring(device, LED);
            Double latVal = checkJSONdouble(device, LAT);
            Double lngVal = checkJSONdouble(device, LNG);

            DeviceActivity.values.putInt(TEMPERATURE, temperatureVal);
            DeviceActivity.values.putInt(HUMIDITY, humidityVal);
            DeviceActivity.values.putInt(LIGHTINTENSITY, LightIntensityVal);
            DeviceActivity.values.putInt(SOILMOISTURE, moistVal);
            DeviceActivity.values.putDouble(VPIN, batteryVal);
            //DeviceActivity.pHVal.putInt("pH", pHVal);
            DeviceActivity.values.putCharSequence(PUMP, pumpVal);
            DeviceActivity.values.putCharSequence(LED, ledVal);
            DeviceActivity.values.putDouble(LAT, latVal);
            DeviceActivity.values.putDouble(LNG, lngVal);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void JSONnoti(String message) {
        numberOfNoti = 0;
        NotiList.noti.clear();
        NotiList.notiSub.clear();
        NotiList.notiSub2.clear();
        NotiList.notiIcon.clear();
        alarmedDevice.clear();
        for (int i = 0; i < getDeviceNumber(); i++) {
            alarmedDevice.putBoolean(devices[i], false);
        }

        try {
            JSONObject json = new JSONObject(message);
            JSONObject state = json.getJSONObject(STATE);
            JSONObject reported = state.getJSONObject(REPORTED);
            for (int i = 0; i < getDeviceNumber(); i++) {
                JSONObject device = reported.getJSONObject(devices[i]);
                Integer temperatureVal = checkJSONint(device, TEMPERATURE);
                Integer humidityVal = checkJSONint(device, HUMIDITY);
                Integer LightIntensityVal = checkJSONint(device, LIGHTINTENSITY);
                Integer moistVal = checkJSONint(device, SOILMOISTURE);
                Integer batteryVal = checkJSONint(device, VPIN);
                //Integer pHVal = checkJSONint(device, "pH");
                newNoti(message, device, TEMPERATURE, i, temperatureVal);
                newNoti(message, device, HUMIDITY, i, humidityVal);
                newNoti(message, device, LIGHTINTENSITY, i, LightIntensityVal);
                newNoti(message, device, SOILMOISTURE, i, moistVal);
                newNoti(message, device, VPIN, i, batteryVal);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void newNoti(String message, JSONObject jsonObject, String string, Integer position, Integer value) throws JSONException {
        Integer min = 0;
        Integer max = 100;
        String notiSub = null;
        Integer ImageResource = null;

        if (jsonObject.has(string)) {
            switch (string) {
                case TEMPERATURE:
                    min = 10;
                    max = 45;
                    notiSub = "The temperature of this area is " + String.valueOf(value) + " \u2103";
                    ImageResource = R.drawable.temperature_alarm;
                    break;
                case HUMIDITY:
                    min = 40;
                    max = 70;
                    notiSub = "The humidity of this area is " + String.valueOf(value) + " %";
                    ImageResource = R.drawable.humidity_alarm;
                    break;
                case LIGHTINTENSITY:
                    min = 1000;
                    max = 100000;
                    notiSub = "The light intensity of this area is " + String.valueOf(value) + " lux";
                    ImageResource = R.drawable.light_alarm;
                    break;
                case SOILMOISTURE:
                    min = 10;
                    max = 50;
                    notiSub = "The soil moisture of this area is " + String.valueOf(value) + " %";
                    ImageResource = R.drawable.moisture_alarm;
                    break;
                case VPIN:
                    min = 50;
                    max = 150;
                    notiSub = "The battery of this device is " + String.valueOf(value / 10.0) + " V";
                    ImageResource = R.drawable.battery_alarm;
                    break;
                default:
                    break;
            }
            if (value < min || value > max) {
                JSONObject json = new JSONObject(message);
                JSONObject metadata = json.getJSONObject(METADATA);
                JSONObject reported = metadata.getJSONObject(REPORTED);
                JSONObject device = reported.getJSONObject(devices[position]);
                JSONObject deviceValue = device.getJSONObject(string);
                long timestamp = checkJSONint(deviceValue, TIMESTAMP) * 1000L;
                SimpleDateFormat format = new SimpleDateFormat("hh:mm  dd-MM-yyyy");

                alarmedDevice.putBoolean(devices[position], true);

                NotiList.noti.putCharSequence(NOTI + String.valueOf(numberOfNoti), devices[position]);
                NotiList.notiSub.putCharSequence(NOTISUB + String.valueOf(numberOfNoti), notiSub);
                NotiList.notiSub2.putCharSequence(NOTISUB2 + String.valueOf(numberOfNoti), format.format(new Date(timestamp)));
                NotiList.notiIcon.putInt(NOTIICON + String.valueOf(numberOfNoti), ImageResource);
                numberOfNoti++;
            }
        }
    }

    private static Integer checkJSONint(JSONObject jsonObject, String string) throws JSONException {
        Integer integer = 0;
        if (jsonObject.has(string)) {
            integer = jsonObject.getInt(string);
        }
        return integer;
    }

    private static String checkJSONstring(JSONObject jsonObject, String string) throws JSONException {
        String s = null;
        if (jsonObject.has(string)) {
            s = jsonObject.getString(string);
        }
        return s;
    }

    private static Double checkJSONdouble(JSONObject jsonObject, String string) throws JSONException {
        Double d = 0.0;
        if (jsonObject.has(string)) {
            d = jsonObject.getDouble(string);
        }
        return d;
    }
}
