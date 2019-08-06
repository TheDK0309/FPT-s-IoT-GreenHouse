package com.example.t_anh.iot_aws_v2;

import android.content.Intent;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;

import java.security.KeyStore;
import java.util.UUID;

import static com.example.t_anh.iot_aws_v2.PubSubJSON.publish;
import static com.example.t_anh.iot_aws_v2.PubSubJSON.subscribeNumber;

public class SplashActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener{

    private static int SPLASH_TIME_OUT = 1500;
    static final String LOG_TAG = SplashActivity.class.getCanonicalName();

    // IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a1eyssf4fxagbt.iot.us-east-1.amazonaws.com";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    public static final String COGNITO_POOL_ID = "us-east-1:33d3b73a-67dd-469d-825c-b944e8a4e6f1";
    // Name of the AWS IoT policy to attach to numberOfNoti newly created certificate
    private static final String AWS_IOT_POLICY_NAME = "IOT_PubSub";
    // Note that spaces are not allowed in the table name
    public static final String TABLE_NAME = "Device-0";

    // Region of AWS IoT
    public static final Regions MY_REGION = Regions.US_EAST_1;
    // Filename of KeyStore file on the filesystem
    private static final String KEYSTORE_NAME = "iot_keystore";
    // Password for the private key in the KeyStore
    private static final String KEYSTORE_PASSWORD = "password";
    // Certificate and key aliases in the KeyStore
    private static final String CERTIFICATE_ID = "default";

    static AWSIotClient mIotAndroidClient;
    static AWSIotMqttManager mqttManager;
    static String clientId;
    static String keystorePath;
    String keystoreName;
    String keystorePassword;

    KeyStore clientKeyStore = null;
    String certificateId;

    SwipeRefreshLayout swipe_splash;

    static CognitoCachingCredentialsProvider credentialsProvider;
    static String context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        context = String.valueOf(SplashActivity.this);

        swipe_splash = (SwipeRefreshLayout) findViewById(R.id.swipe_splash);
        swipe_splash.setOnRefreshListener(this);

        Connectd();
        connectAWS();
    }

    protected void Connectd() {
        // MQTT client IDs are required to be unique per AWS IoT account.
        // This UUID is "practically unique" but does not _guarantee_
        // uniqueness.
        clientId = UUID.randomUUID().toString();

        // Initialize the AWS Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(), // context
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );

        Region region = Region.getRegion(MY_REGION);

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);

        // Set keepalive to 10 seconds.  Will recognize disconnects more quickly but will also send
        // MQTT pings every 10 seconds.
        mqttManager.setKeepAlive(10);

        // Set Last Will and Testament for MQTT.  On an unclean disconnect (loss of connection)
        // AWS IoT will publish this message to alert other clients.
        AWSIotMqttLastWillAndTestament lwt = new AWSIotMqttLastWillAndTestament("my/lwt/topic",
                "Android client lost connection", AWSIotMqttQos.QOS0);
        mqttManager.setMqttLastWillAndTestament(lwt);

        // IoT Client (for creation of certificate if needed)
        mIotAndroidClient = new AWSIotClient(credentialsProvider);
        mIotAndroidClient.setRegion(region);

        keystorePath = getFilesDir().getPath();
        keystoreName = KEYSTORE_NAME;
        keystorePassword = KEYSTORE_PASSWORD;
        certificateId = CERTIFICATE_ID;

        // To load cert/key from keystore on filesystem
        try {
            if (AWSIotKeystoreHelper.isKeystorePresent(keystorePath, keystoreName)) {
                if (AWSIotKeystoreHelper.keystoreContainsAlias(certificateId, keystorePath,
                        keystoreName, keystorePassword)) {
                    Log.i(LOG_TAG, "Certificate " + certificateId
                            + " found in keystore - using for MQTT.");
                    // load keystore from file into memory to pass on connection
                    clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                            keystorePath, keystoreName, keystorePassword);
                } else {
                    Log.i(LOG_TAG, "Key/cert " + certificateId + " not found in keystore.");
                }
            } else {
                Log.i(LOG_TAG, "Keystore " + keystorePath + "/" + keystoreName + " not found.");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "An error occurred retrieving cert/key from keystore.", e);
        }

        if (clientKeyStore == null) {
            Log.i(LOG_TAG, "Cert/key was not found in keystore - creating new key and certificate.");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Create numberOfNoti new private key and certificate. This call
                        // creates both on the server and returns them to the
                        // device.
                        CreateKeysAndCertificateRequest createKeysAndCertificateRequest =
                                new CreateKeysAndCertificateRequest();
                        createKeysAndCertificateRequest.setSetAsActive(true);
                        final CreateKeysAndCertificateResult createKeysAndCertificateResult;
                        createKeysAndCertificateResult =
                                mIotAndroidClient.createKeysAndCertificate(createKeysAndCertificateRequest);
                        Log.i(LOG_TAG,
                                "Cert ID: " +
                                        createKeysAndCertificateResult.getCertificateId() +
                                        " created.");

                        // store in keystore for use in MQTT client
                        // saved as alias "default" so numberOfNoti new certificate isn't
                        // generated each run of this application
                        AWSIotKeystoreHelper.saveCertificateAndPrivateKey(certificateId,
                                createKeysAndCertificateResult.getCertificatePem(),
                                createKeysAndCertificateResult.getKeyPair().getPrivateKey(),
                                keystorePath, keystoreName, keystorePassword);

                        // load keystore from file into memory to pass on
                        // connection
                        clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                                keystorePath, keystoreName, keystorePassword);

                        // Attach numberOfNoti policy to the newly created certificate.
                        // This flow assumes the policy was already created in
                        // AWS IoT and we are now just attaching it to the
                        // certificate.
                        AttachPrincipalPolicyRequest policyAttachRequest =
                                new AttachPrincipalPolicyRequest();
                        policyAttachRequest.setPolicyName(AWS_IOT_POLICY_NAME);
                        policyAttachRequest.setPrincipal(createKeysAndCertificateResult
                                .getCertificateArn());
                        mIotAndroidClient.attachPrincipalPolicy(policyAttachRequest);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                            }
                        });
                    } catch (Exception e) {
                        Log.e(LOG_TAG,
                                "Exception occurred when generating new private key and certificate.",
                                e);
                    }
                }
            }).start();
        }
        Log.d(LOG_TAG, "clientId = " + clientId);
    }

    private void connectAWS() {
        Log.d(LOG_TAG, "clientId = " + clientId);

        try {
            mqttManager.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status,
                                            final Throwable throwable) {
                    Log.d(LOG_TAG, "Status = " + String.valueOf(status));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (status == AWSIotMqttClientStatus.Connecting) {
                                Toast.makeText(SplashActivity.this, "Connecting", Toast.LENGTH_SHORT).show();
                            } else if (status == AWSIotMqttClientStatus.Connected) {
                                Toast.makeText(SplashActivity.this, " Connected", Toast.LENGTH_SHORT).show();

                                subscribeNumber(MainActivity.GETACCEPTED);
                                publish(MainActivity.GET, "");
                                if (context.equals(String.valueOf(SplashActivity.this))) {
                                    final Intent homeIntent = new Intent(SplashActivity.this, MainActivity.class);
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            //if (previousDeviceNumber != getDeviceNumber()){
                                            startActivity(homeIntent);
                                            finish();
                                            //}
                                        }
                                    }, SPLASH_TIME_OUT);
                                }
                            } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                                if (throwable != null) {
                                    Toast.makeText(SplashActivity.this, "Reconnecting", Toast.LENGTH_SHORT).show();
                                    Log.e(LOG_TAG, "Connection error.", throwable);
                                }
                            } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                                if (throwable != null) {
                                    Toast.makeText(SplashActivity.this, "Connection Lost", Toast.LENGTH_SHORT).show();
                                    Log.e(LOG_TAG, "Connection error.", throwable);
                                }
                            } else {

                            }
                        }
                    });
                }
            });
        } catch (final Exception e) {
            Log.e(LOG_TAG, "Connection error.", e);
        }
    }

    private void reconnectAWS(){
        try{
            mqttManager.resetReconnect();
        } catch (final Exception e) {
            Log.e(LOG_TAG, "Connection error.", e);
        }
    }

    @Override
    public void onRefresh() {
        if (swipe_splash.isRefreshing()) {
            swipe_splash.setRefreshing(false);
        }
        reconnectAWS();
    }
}
