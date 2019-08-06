package com.example.t_anh.iot_aws_v2;

import android.graphics.Color;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity{

    static String LOG_TAG = MainActivity.class.getCanonicalName();

    public static final String GETACCEPTED = "$aws/things/IoT/shadow/get/accepted";
    public static final String UPDATEACCEPTED = "$aws/things/IoT/shadow/update/accepted";
    public static final String GET = "$aws/things/IoT/shadow/get";
    public static final String UPDATE = "$aws/things/IoT/shadow/update";

    private static Integer deviceNumber = 0;
    static Integer tabCurrentPosition = 0;
    int noti, noti_selected;
    boolean haveNoti = false;
    boolean preHaveNoti = false;
    static String reportedJSON = "{\"state\":{\"reported\":{";
    Button btnMenu;
    private SectionsPageAdapter mSectionsPageAdapter;
    private ViewPager mViewPager;
    private TabLayout tabLayout;

    public static AmazonClientManager clientManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SplashActivity.context = String.valueOf(MainActivity.this);

        clientManager = new AmazonClientManager(this);

        mSectionsPageAdapter = new SectionsPageAdapter(getSupportFragmentManager());
        btnMenu = (Button) findViewById(R.id.btnMenu);

        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final PopupMenu popupMenu = new PopupMenu(MainActivity.this, btnMenu);
                popupMenu.getMenuInflater().inflate(R.menu.menu_main,popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getTitle().equals("About")){
                            Toast.makeText(MainActivity.this, "1st developer: Nguyen Doan Tan Anh", Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    }
                });
                popupMenu.show();
            }
        });
        noti = R.drawable.notification;
        noti_selected = R.drawable.notification_selected;

        mViewPager = (ViewPager) findViewById(R.id.container);
        setupViewPager(mViewPager);
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        tabLayout.getTabAt(0).setIcon(R.drawable.dashboard_selected);
        tabLayout.getTabAt(1).setIcon(R.drawable.devices);
        tabLayout.setTabTextColors(Color.parseColor("#727272"), Color.parseColor("#626262"));
        tabLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));
        notiTabIcon();

        tabLayout.getTabAt(2).setIcon(noti);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()){
                    case 0:
                        tab.setIcon(R.drawable.dashboard_selected);
                        break;
                    case 1:
                        tab.setIcon(R.drawable.devices_selected);
                        break;
                    case 2:
                        tab.setIcon(noti_selected);
                        break;
                    default:
                        break;
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                switch (tab.getPosition()){
                    case 0:
                        tab.setIcon(R.drawable.dashboard);
                        break;
                    case 1:
                        tab.setIcon(R.drawable.devices);
                        break;
                    case 2:
                        tab.setIcon(noti);
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        mViewPager.setCurrentItem(tabCurrentPosition);
        //timerHandler.postDelayed(timerRunnable, 2000);
    }

    public void notiTabIcon(){
        if (NotiList.noti.size()>0){
            haveNoti = true;
        } else {
            haveNoti = false;
        }
        if(haveNoti != preHaveNoti){
            if (NotiList.noti.size()>0){
                noti = R.drawable.notification_red;
                noti_selected = R.drawable.notification_red_selected;
            } else {
                noti = R.drawable.notification;
                noti_selected = R.drawable.notification_selected;
            }
            preHaveNoti = haveNoti;
        }
    }

    private void setupViewPager(final ViewPager viewPager){
        final SectionsPageAdapter adapter = new SectionsPageAdapter(getSupportFragmentManager());

        adapter.addFragment(new DashBoard(), "Dashboard");
        adapter.addFragment(new DevicesList(), "Devices");
        adapter.addFragment(new NotiList(), "Notifications");
        viewPager.setAdapter(adapter);
    }

    public static Integer getDeviceNumber() {
        return deviceNumber;
    }

    public static void setDeviceNumber(Integer deviceNumber) {
        MainActivity.deviceNumber = deviceNumber;
    }
}
