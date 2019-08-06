package com.example.t_anh.iot_aws_v2;

import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBScanExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DynamoDBManager {

    private static final String TAG = "DynamoDBManager";

    /*
     * Retrieves the table description and returns the table status as numberOfNoti string.
     */
    public static String getTableStatus() {

        try {
            AmazonDynamoDBClient ddb = MainActivity.clientManager.ddb();

            DescribeTableRequest request = new DescribeTableRequest().withTableName(DeviceActivity.deviceNameValue);
            DescribeTableResult result = ddb.describeTable(request);

            String status = result.getTable().getTableStatus();
            return status == null ? "" : status;

        } catch (ResourceNotFoundException e) {
        } catch (AmazonServiceException ex) {
            MainActivity.clientManager.wipeCredentialsOnAuthError(ex);
        }
        return "";
    }

    /*
     * Scans the table and returns the list of users.
     */
    @Nullable
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static ArrayList<IotHistory> getIotHistory() {

        AmazonDynamoDBClient ddb = MainActivity.clientManager.ddb();
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);

        Map<String, AttributeValue> eav = new HashMap<String, AttributeValue> ();
        eav.put(":n", new AttributeValue().withN("0"));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("time_load >= :n")
                .withExpressionAttributeValues(eav);
        try {
            PaginatedScanList<IotHistory> result = mapper.scan(
                    IotHistory.class,
                    scanExpression,
                    new DynamoDBMapperConfig(new DynamoDBMapperConfig.TableNameOverride(DeviceActivity.deviceNameValue)));

            ArrayList<IotHistory> resultList = new ArrayList<>();
            for (IotHistory up : result) {
                resultList.add(up);
            }
            int n = resultList.size();
            for (int i = 0; i < n-1; i++) {
                for (int j = 0; j < n - i - 1; j++) {
                    if (resultList.get(j).getTime1() > resultList.get(j + 1).getTime1()) {
                        // swap temp and arr[i]
                        IotHistory temp = resultList.get(j);
                        resultList.set(j, resultList.get(j+1));
                        resultList.set(j+1, temp);
                    }
                }
            }
            return resultList;
        } catch (AmazonServiceException ex) {
            MainActivity.clientManager.wipeCredentialsOnAuthError(ex);
        }
        return null;
    }

    /*
     * Retrieves all of the attribute/value pairs for the specified user.
     */
    @Nullable
    public static IotHistory getIotHistory(long time) {

        AmazonDynamoDBClient ddb = MainActivity.clientManager.ddb();
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);
        try {
            IotHistory iotHistory = mapper.load(IotHistory.class, time, new DynamoDBMapperConfig(new DynamoDBMapperConfig.TableNameOverride(DeviceActivity.deviceNameValue)));
            return iotHistory;
        } catch (AmazonServiceException ex) {
            MainActivity.clientManager.wipeCredentialsOnAuthError(ex);
        }
        return null;
    }

    @DynamoDBTable(tableName = SplashActivity.TABLE_NAME)
    public static class IotHistory {
        private Long time_load;
        private String humidity;
        //private String lat;
        private String led;
        private String lightIntensity;
        //private String lng;
        private String pump;
        private String soilMoisture;
        private String temperature;
        //private String vpin;

        @DynamoDBHashKey(attributeName = "time_load")
        public Long getTime1() {
            return time_load;
        }

        public void setTime1(Long time_load) {
            this.time_load = time_load;
        }

        @DynamoDBAttribute(attributeName = "Humidity")
        public String getHumidity() {
            return humidity;
        }

        public void setHumidity(String humidity) {
            this.humidity = humidity;
        }
/*
        @DynamoDBAttribute(attributeName = "Lat")
        public String getLat() {
            return lat;
        }

        public void setLat(String lat) {
            this.lat = lat;
        }
*/
        @DynamoDBAttribute(attributeName = "LED")
        public String getLed() {
            return led;
        }

        public void setLed(String led) {
            this.led = led;
        }

        @DynamoDBAttribute(attributeName = "LightIntensity")
        public String getLightIntensity() {
            return lightIntensity;
        }

        public void setLightIntensity(String lightIntensity) {
            this.lightIntensity = lightIntensity;
        }
/*
        @DynamoDBAttribute(attributeName = "Lng")
        public String getLng() {
            return lng;
        }

        public void setLng(String lng) {
            this.lng = lng;
        }
*/
        @DynamoDBAttribute(attributeName = "Pump")
        public String getPump() {
            return pump;
        }

        public void setPump(String pump) {
            this.pump = pump;
        }

        @DynamoDBAttribute(attributeName = "SoilMoisture")
        public String getSoilMoisture() {
            return soilMoisture;
        }

        public void setSoilMoisture(String soilMoisture) {
            this.soilMoisture = soilMoisture;
        }

        @DynamoDBAttribute(attributeName = "Temperature")
        public String getTemperature() {
            return temperature;
        }

        public void setTemperature(String temperature) {
            this.temperature = temperature;
        }
/*
        @DynamoDBAttribute(attributeName = "Vpin")
        public String getVpin() {
            return vpin;
        }

        public void setVpin(String vpin) {
            this.vpin = vpin;
        }
*/
    }
}
