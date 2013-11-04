package com.gethu.desktop;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Desktop extends Activity {

private static ArrayList<ApplicationInfo> mApplications;
//private static ArrayList<ApplicationInfo> recentApp;
protected Context mContext;
//ApplicationsAdapterRect app2;
SupplicantState supState;
WifiManager wifiManager;
WifiInfo wifiInfo;
//private AudioManager audioManager;
Thread receiverThread = null;
TextView txtCurrentTime = null;
IntentFilter mIntentFilter;
Intent wifiStatus,batteryStatus;
IntentFilter battfilter,wififilter;
LinearLayout startMenu=null;
ListView lView = null;
ApplicationsAdapter app=null;
AccessibilityServiceInfo MyAccessibilityServiceInfo=null;
ImageButton settings=null;
ImageButton notification = null;
ImageButton homeButton = null;
ImageButton startButton = null;
WifiConnectionReceiver wcr;
PowerConnectionReceiver pcr;

ImageButton batt = null, wifi = null, volume = null;
int currentVolume;
AudioManager audio;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_desktop);
    setInitialAssignments();
    setInitialVisibility();
    setInitiateListeners();
    wcr.onReceive(getApplicationContext(), wifiStatus);
    pcr.onReceive(getApplicationContext(), batteryStatus);
}

void setInitialVisibility(){
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
        settings.setVisibility(View.GONE);
    } else {
        settings.setVisibility(View.VISIBLE);
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
        homeButton.setVisibility(View.GONE);
    } else {
        homeButton.setVisibility(View.VISIBLE);

    }
}

void setInitiateListeners(){
    startButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (startMenu.getVisibility() == View.INVISIBLE) {
                startMenu.setVisibility(View.VISIBLE);
            } else {
                startMenu.setVisibility(View.INVISIBLE);
            }
        }
    });

    lView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                long arg3) {
            ApplicationInfo app = (ApplicationInfo) mApplications.get(arg2);
            startMenu.setVisibility(View.INVISIBLE);
            startActivity(app.intent);
            // recentApp.add(app);
            // app2 = new ApplicationsAdapterRect(mContext, recentApp);
            // favGrid.setAdapter(app2);
        }

    });


    homeButton.setOnClickListener(new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_HOME);
            startActivity(i);
        }
    });

    notification.setOnClickListener(new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            try {
                Object sbservice = getSystemService("statusbar");
                Class<?> statusbarManager = null;
                statusbarManager = Class
                                           .forName("android.app.StatusBarManager");
                Method showsb = null;
                if (Build.VERSION.SDK_INT >= 17) {
                    showsb = statusbarManager
                                     .getMethod("expandNotificationsPanel");
                } else {
                    showsb = statusbarManager.getMethod("expand");
                }
                showsb.invoke(sbservice);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }

        }

    });

    settings.setOnClickListener(new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            try {
                Object sbservice = getSystemService("statusbar");
                Class<?> statusbarManager = null;
                statusbarManager = Class
                                           .forName("android.app.StatusBarManager");
                Method showsb = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    showsb = statusbarManager
                                     .getMethod("expandSettingsPanel");
                } else {
                    showsb = statusbarManager.getMethod("expand");
                }
                showsb.invoke(sbservice);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }

        }

    });
}

void setInitialAssignments(){
    startButton = (ImageButton) findViewById(R.id.startbutton);
    startMenu = (LinearLayout) findViewById(R.id.startmenu);
    notification = (ImageButton) findViewById(R.id.notification);
    homeButton = (ImageButton) findViewById(R.id.homebutton);
    settings = (ImageButton) findViewById(R.id.quicksettings);
    batt = (ImageButton) findViewById(R.id.batt);
    wifi = (ImageButton) findViewById(R.id.wifi);
    lView = (ListView) findViewById(R.id.startlist);
    addApplicationlist();
    lView.setAdapter(app);
}

void addApplicationlist(){
    PackageManager pm = this.getPackageManager();
    Intent intent = new Intent(Intent.ACTION_MAIN, null);
    List<ResolveInfo> list = pm.queryIntentActivities(intent, PackageManager.PERMISSION_GRANTED);
    //List<PackageInfo> list = pm.getInstalledPackages(0);
    Collections.sort(list, new ResolveInfo.DisplayNameComparator(pm));
    if (list != null) {
        final int count = list.size();

        if (mApplications == null) {
            mApplications = new ArrayList<ApplicationInfo>(count);
        }
        mApplications.clear();

        for (ResolveInfo rInfo : list) {
            ApplicationInfo application = new ApplicationInfo();
            application.title = (rInfo.activityInfo.applicationInfo
                                         .loadLabel(pm).toString());
            application.setActivity(new ComponentName(
                                                             rInfo.activityInfo.applicationInfo.packageName,
                                                             rInfo.activityInfo.name), Intent.FLAG_ACTIVITY_NEW_TASK
                                                                                               | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            application.icon = (rInfo.activityInfo.applicationInfo
                                        .loadIcon(pm));
            mApplications.add(application);

        }
    }
    app = new ApplicationsAdapter(this,
                                         mApplications);
}

@Override
public void onStart() {
    super.onStart();
    registerReceivers();
    Toast.makeText(getApplicationContext(), "OnStart0", Toast.LENGTH_LONG).show();
    Runnable runnable = new BroadcastReceiverRunner();
    Toast.makeText(getApplicationContext(), "OnStart1", Toast.LENGTH_LONG).show();
    receiverThread = new Thread(runnable);
    Toast.makeText(getApplicationContext(), "OnStart2", Toast.LENGTH_LONG).show();
    receiverThread.start();
    Toast.makeText(getApplicationContext(), "OnStart3", Toast.LENGTH_LONG).show();
}

@Override
public void onStop() {
    super.onStop();
    unregisterReceivers();
}

@Override
public void onResume() {
    super.onResume();
    registerReceivers();
    Toast.makeText(getApplicationContext(), "OnResume", Toast.LENGTH_LONG).show();
    Runnable runnable = new BroadcastReceiverRunner();
    receiverThread = new Thread(runnable);
    receiverThread.start();
}

@Override
public void onPause() {
    super.onPause();
    unregisterReceivers();
}

public void registerReceivers(){
    registerPowerReceiver();
    registerWifiReceiver();
}

public void unregisterReceivers(){
    unregisterReceiver(pcr);
    unregisterReceiver(wcr);
}

public void registerPowerReceiver(){
    pcr = new PowerConnectionReceiver();
    battfilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    battfilter.addAction(Intent.ACTION_BATTERY_LOW);
    battfilter.addAction(Intent.ACTION_BATTERY_OKAY);
    battfilter.addAction(Intent.ACTION_POWER_CONNECTED);
    battfilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
    batteryStatus = registerReceiver(null, battfilter);
}

public void registerWifiReceiver(){
    wcr = new WifiConnectionReceiver();
    wififilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
    wififilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
    wififilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    wififilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
    wifiStatus = registerReceiver(null, wififilter);
}

public class WifiConnectionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NetworkInfo mWifi = ((ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE)).getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        int state = wifiManager.getWifiState();
        if (state == WifiManager.WIFI_STATE_ENABLED) {
            if (mWifi.isConnected()) {
                List<ScanResult> results = wifiManager.getScanResults();
                if (results != null) {
                    for (ScanResult result : results) {
                        if (result.BSSID.equals(wifiManager.getConnectionInfo().getBSSID())) {
                            int level = WifiManager.calculateSignalLevel(wifiManager.getConnectionInfo().getRssi(), result.level);
                            int difference = level * 100 / result.level;
                            wifi.setImageResource(R.drawable.wireless0);
                            if (difference >= 100)
                                wifi.setImageResource(R.drawable.wirelessfull);
                            else if (difference >= 75)
                                wifi.setImageResource(R.drawable.wireless75);
                            else if (difference >= 50)
                                wifi.setImageResource(R.drawable.wireless50);
                            else if (difference >= 25)
                                wifi.setImageResource(R.drawable.wireless25);
                        }
                    }
                } else {
                    // No wifi network available
                    wifi.setImageResource(R.drawable.wireless0);
                }
            } else {
                // Wifi switched on but no connected but networks available
                wifi.setImageResource(R.drawable.wirelessna);
            }

        } else {
            // Wifi switched off
            wifi.setVisibility(View.GONE);
        }
        wifi.setVisibility(View.VISIBLE);
    }

}

public class PowerConnectionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPct = (level / (float) scale) * 100;

        //Toast.makeText(getApplicationContext(), "Battery Level:", Toast.LENGTH_LONG).show();

        if (chargePlug == 0) {
            if (batteryPct > 80 && batteryPct <= 100) {
                batt.setImageResource(R.drawable.battery100);
            }
            if (batteryPct > 60 && batteryPct <= 80) {
                batt.setImageResource(R.drawable.battery80);
            }
            if (batteryPct > 40 && batteryPct <= 60) {
                batt.setImageResource(R.drawable.battery60);
            }
            if (batteryPct > 20 && batteryPct <= 40) {
                batt.setImageResource(R.drawable.battery40);
            }
            if (batteryPct > 0 && batteryPct <= 20) {
                batt.setImageResource(R.drawable.battery0);
            }
        }
        if (chargePlug != 0) {
            if (batteryPct > 80 && batteryPct <= 100) {
                batt.setImageResource(R.drawable.chrgbattery100);
            }
            if (batteryPct > 60 && batteryPct <= 80) {
                batt.setImageResource(R.drawable.chrgbattery80);
            }
            if (batteryPct > 40 && batteryPct <= 60) {
                batt.setImageResource(R.drawable.chrgbattery60);
            }
            if (batteryPct > 20 && batteryPct <= 40) {
                batt.setImageResource(R.drawable.chrgbattery40);
            }
            if (batteryPct > 0 && batteryPct <= 20) {
                batt.setImageResource(R.drawable.chrgbattery0);
            }
        }
        batt.setVisibility(View.VISIBLE);
    }
}

class ApplicationsAdapter extends ArrayAdapter<ApplicationInfo> {
    private Rect mOldBounds = new Rect();

    public ApplicationsAdapter(Context context,
                               ArrayList<ApplicationInfo> apps) {
        super(context, 0, apps);
    }

    @SuppressWarnings("deprecation")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ApplicationInfo info = mApplications.get(position);

        if (convertView == null) {
            final LayoutInflater inflater = getLayoutInflater();
            convertView = inflater.inflate(R.layout.application, parent,
                                                  false);
        }

        Drawable icon = info.icon;

        if (!info.filtered) {
            // final Resources resources = getContext().getResources();
            int width = 36;// (int)
            // resources.getDimension(android.R.dimen.app_icon_size);
            int height = 36;// (int)
            // resources.getDimension(android.R.dimen.app_icon_size);

            final int iconWidth = icon.getIntrinsicWidth();
            final int iconHeight = icon.getIntrinsicHeight();

            if (icon instanceof PaintDrawable) {
                PaintDrawable painter = (PaintDrawable) icon;
                painter.setIntrinsicWidth(width);
                painter.setIntrinsicHeight(height);
            }

            if (width > 0 && height > 0
                        && (width < iconWidth || height < iconHeight)) {
                final float ratio = (float) iconWidth / iconHeight;

                if (iconWidth > iconHeight) {
                    height = (int) (width / ratio);
                } else if (iconHeight > iconWidth) {
                    width = (int) (height * ratio);
                }

                final Bitmap.Config c = icon.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                                                : Bitmap.Config.RGB_565;
                final Bitmap thumb = Bitmap.createBitmap(width, height, c);
                final Canvas canvas = new Canvas(thumb);
                canvas.setDrawFilter(new PaintFlagsDrawFilter(
                                                                     Paint.DITHER_FLAG, 0));
                // Copy the old bounds to restore them later
                // If we were to do oldBounds = icon.getBounds(),
                // the call to setBounds() that follows would
                // change the same instance and we would lose the
                // old bounds
                mOldBounds.set(icon.getBounds());
                icon.setBounds(0, 0, width, height);
                icon.draw(canvas);
                icon.setBounds(mOldBounds);
                icon = info.icon = new BitmapDrawable(thumb);
                info.filtered = true;
            }
        }
        final TextView textView = (TextView) convertView
                                                     .findViewById(R.id.label);
        ImageView imageView = (ImageView) convertView
                                                  .findViewById(R.id.image);
        // textView.setCompoundDrawablesWithIntrinsicBounds(null, icon,
        // null,
        // null);
        imageView.setImageDrawable(icon);
        textView.setText(info.title);

        return convertView;
    }

}

class BroadcastReceiverRunner implements Runnable {
    @Override
    public void run() {
        //android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        while (!Thread.currentThread().isInterrupted()) {
            Log.i("BatteryLevelReceiver", "Battery Level: ");
            try {
                wcr.onReceive(getApplicationContext(), wifiStatus);
                pcr.onReceive(getApplicationContext(), batteryStatus);
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                //Toast.makeText(getApplicationContext(), "Thread Interrupted", Toast.LENGTH_LONG).show();
            }
        }
        //Thread.currentThread();
    }
}

public class MyAccessibilityService extends AccessibilityService {
    final AccessibilityManager accessibilityManager =
            (AccessibilityManager) (getApplicationContext()).getSystemService(Context.ACCESSIBILITY_SERVICE);

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Toast.makeText(getApplicationContext(), "Notification received", Toast.LENGTH_LONG).show();
        if(event.getEventType()== 64 ) {
            long eventTime = event.getEventTime();
            String application = event.getClass().toString();
            List<CharSequence> notification = event.getText();
            String packagename = event.getPackageName().toString();
            Parcelable notificationparcel = event.getParcelableData();
        }
    }

    @Override
    public void onInterrupt() {
        Toast.makeText(getApplicationContext(), "Interrupt received", Toast.LENGTH_LONG).show();
    }
    // MyAccessibilityServiceInfo= new AccessibilityServiceInfo();
    //android.accessibilityservice.AccessibilityService.setServiceInfo(MyAccessibilityServiceInfo);
    //AccessibilityService MyAccessibilityService = new AccessibilityService();
    //accessibilityManager.addAccessibilityStateChangeListener(new AccessibilityManager.AccessibilityStateChangeListener(){
    //  @Override
    //public void onAccessibilityStateChanged(boolean enabled){
    //      MyAccessibilityService.getServiceInfo();
    //     }
    //  });


}

}