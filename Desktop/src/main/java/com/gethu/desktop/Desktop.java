package com.gethu.desktop;

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
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class Desktop extends Activity {

private static ArrayList<ApplicationInfo> mApplications;
//private static ArrayList<ApplicationInfo> recentApp = null;
protected Context mContext;
ApplicationsAdapterRect app2;
SupplicantState supState;
WifiManager wifiManager;
WifiInfo wifiInfo;
private AudioManager audioManager;
Thread myThread = null;
TextView txtCurrentTime = null;
IntentFilter mIntentFilter;
Intent wifiStatus;
IntentFilter ifilter;
Intent batteryStatus;

ImageButton batt = null, wifi = null, volume = null;
PowerConnectionReceiver pcr, pcr2;
WifiConnectionReceiver wcr, wcr2;
int currentVolume;
AudioManager audio;

@SuppressWarnings("deprecation")
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_desktop);

    ImageButton notification = (ImageButton) findViewById(R.id.notification);
    ImageButton startButton = (ImageButton) findViewById(R.id.startbutton);
    final ListView lView = (ListView) findViewById(R.id.startlist);
    final LinearLayout startMenu = (LinearLayout) findViewById(R.id.startmenu);
    ImageButton settings = (ImageButton) findViewById(R.id.quicksettings);
    ImageButton homeButton = (ImageButton) findViewById(R.id.homebutton);
    //final GridView favGrid = (GridView) findViewById(R.id.fav_apps);
    audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

    PackageManager pm = this.getPackageManager();
    Intent intent = new Intent(Intent.ACTION_MAIN, null);
    mContext = this;

    batt = (ImageButton) new ImageButton(this);
    // batt.setImageResource(R.drawable.nobattery);
    batt.setBackgroundDrawable(null);
    batt.setClickable(true);
    ((LinearLayout) findViewById(R.id.systemtray)).addView((View) batt,
                                                                  new LayoutParams(36, LayoutParams.WRAP_CONTENT));

    wifi = (ImageButton) new ImageButton(this);
    wifi.setBackgroundDrawable(null);
    wifi.setClickable(true);
    ((LinearLayout) findViewById(R.id.systemtray)).addView((View) wifi,
                                                                  new LayoutParams(36, LayoutParams.WRAP_CONTENT));

    audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    currentVolume = audio.getStreamVolume(AudioManager.STREAM_RING);

    volume = (ImageButton) new ImageButton(this);
    volume.setBackgroundDrawable(null);
    volume.setClickable(true);
    setVolume();

    ((LinearLayout) findViewById(R.id.systemtray)).addView(
                                                                  (View) volume, new LayoutParams(36, LayoutParams.WRAP_CONTENT));

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

//		List<PackageInfo> list = pm.getInstalledPackages(0);
//
//		procInfos = actvityManager.getRunningAppProcesses();
//		for (int i = 0; i < procInfos.size(); i++) 
//		{
//		            runningApplist.add(procInfos.get(i).processName);
//		            runningApplistpid.add(procInfos.get(i).pid);
//		}

    intent.addCategory(Intent.CATEGORY_LAUNCHER);

    List<ResolveInfo> list = pm.queryIntentActivities(intent,
                                                             PackageManager.PERMISSION_GRANTED);
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

    final ApplicationsAdapter app = new ApplicationsAdapter(this,
                                                                   mApplications);
    startButton.setOnClickListener(new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (startMenu.getVisibility() == View.INVISIBLE) {
                lView.setAdapter(app);
                startMenu.setVisibility(View.VISIBLE);
            } else {
                startMenu.setVisibility(View.INVISIBLE);
            }
        }
    });

    homeButton.setOnClickListener(new OnClickListener() {

        @Override
        public void onClick(View v) {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_HOME);
            startActivity(i);
        }
    });

    notification.setOnClickListener(new OnClickListener() {

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

    settings.setOnClickListener(new OnClickListener() {

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

    lView.setOnItemClickListener(new OnItemClickListener() {
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

    lView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> av, View v, int pos,
                                       long id) {
            onLongListItemClick(v, pos, id);
            return false;
        }
    });

    wifi.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        }
    });

    batt.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    });
}

protected void onLongListItemClick(View v, int pos, long id) {
    String[] popupstart = {"Open","Pin to desktop","Pin to Favorites"};
    ListView popupList = new ListView(this);
    popupList.setAdapter(new ArrayAdapter<String>(this,R.layout.popup_noimages,popupstart));
    PopupWindow popupMenu = new PopupWindow((View)popupList);
    popupMenu.showAtLocation(v, Gravity.CENTER, 0, 0);
    popupMenu.setWidth(LinearLayout.LayoutParams.WRAP_CONTENT);
    popupMenu.setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
    popupMenu.setFocusable(true);
    //	popupMenu.show();


}

@Override
public void onStart() {
    super.onStart();

    // Intent for Battery receiver
    ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    batteryStatus = registerReceiver(null, ifilter);
    pcr = new PowerConnectionReceiver();

    // Intent for wifi receiver
    mIntentFilter = new IntentFilter();
    mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
    mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
    mIntentFilter
            .addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
    wifiStatus = registerReceiver(null, mIntentFilter);
    wcr = new WifiConnectionReceiver();

    Runnable runnable = new CountDownRunner();
    myThread = new Thread(runnable);

    myThread.start();

}

@Override
public void onStop() {
    super.onStop();
    try {
        unregisterReceiver(pcr);
        unregisterReceiver(wcr);
    } catch (IllegalStateException e) {
        e.printStackTrace();
    }

}

@Override
public void onResume() {
    super.onResume();
    ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    batteryStatus = getApplicationContext().registerReceiver(null, ifilter);
    pcr = new PowerConnectionReceiver();
    pcr.onReceive(getApplicationContext(), batteryStatus);

    // Intent for wifi receiver
    mIntentFilter = new IntentFilter();
    mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
    mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
    mIntentFilter
            .addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
    wifiStatus = registerReceiver(wcr2, mIntentFilter);

    Runnable runnable = new CountDownRunner();
    myThread = new Thread(runnable);

    myThread.start();
}

@Override
public void onPause() {
    super.onPause();
    try {
        unregisterReceiver(pcr);
        unregisterReceiver(wcr);
    } catch (IllegalStateException e) {
        e.printStackTrace();
    }
}

@Override
public boolean onKeyDown(int keyCode, KeyEvent event) {

    switch (keyCode) {
        case KeyEvent.KEYCODE_VOLUME_UP:
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                                                   AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
            // Raise the Volume Bar on the Screen
            setVolume();
            return true;
        case KeyEvent.KEYCODE_VOLUME_DOWN:
            // Adjust the Volume
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                                                   AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
            // Lower the VOlume Bar on the Screen
            setVolume();
            return true;
        default:
            return false;
    }
}

@Override
public boolean onKeyLongPress(int keyCode, KeyEvent event) {

    onKeyDown(keyCode, event);

    return super.onKeyLongPress(keyCode, event);
}

	/*
	 * @Override public boolean onCreateOptionsMenu(Menu menu) { // Inflate the
	 * menu; this adds items to the action bar if it is present.
	 * getMenuInflater().inflate(R.menu.desktop, menu); return true; }
	 */

private class ApplicationsAdapter extends ArrayAdapter<ApplicationInfo> {
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

private class ApplicationsAdapterRect extends ArrayAdapter<ApplicationInfo> {
    private Rect mOldBounds = new Rect();

    public ApplicationsAdapterRect(Context context,
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
        textView.setCompoundDrawablesWithIntrinsicBounds(null, icon, null,
                                                                null);
        imageView.setImageDrawable(icon);
        return convertView;
    }

}

public class PowerConnectionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        //boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
        //		|| status == BatteryManager.BATTERY_STATUS_FULL;
        int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED,
                                                   -1);

        //boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
        //boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPct = (level / (float) scale) * 100;
        Log.i("BatteryLevelReceiver", "Battery Level: " + batteryPct);
        //Toast.makeText(getApplicationContext(),
        //	"Battery Level:" + batteryPct, Toast.LENGTH_SHORT).show();

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
    }
}

public class WifiConnectionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        // wifiInfo = wifiManager.getConnectionInfo();
        // supState = wifiInfo.getSupplicantState();
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager
                                    .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        // String action = intent.getAction();
        // if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
        // } else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION))
        // {
        // int iTemp =
        // intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,WifiManager.WIFI_STATE_UNKNOWN);
        // checkState(iTemp);
        // } else if
        // (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION))
        // {
        // DetailedState state =
        // WifiInfo.getDetailedStateOf((SupplicantState)
        // intent.getParcelableExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED));
        // changeState(state);
        // } else if
        // (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
        // DetailedState state = ((NetworkInfo)
        // intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)).getDetailedState();
        // changeState(state);
        // }
        int state = wifiManager.getWifiState();
        if (state == WifiManager.WIFI_STATE_ENABLED) {
            if (mWifi.isConnected()) {
                List<ScanResult> results = wifiManager.getScanResults();
                if (results != null) {
                    for (ScanResult result : results) {
                        if (result.BSSID.equals(wifiManager
                                                        .getConnectionInfo().getBSSID())) {
                            int level = WifiManager.calculateSignalLevel(
                                                                                wifiManager.getConnectionInfo()
                                                                                        .getRssi(), result.level);
                            int difference = level * 100 / result.level;
                            int signalStrangth = 0;
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
    }

    private void changeState(DetailedState aState) {
        if (aState == DetailedState.SCANNING) {
            Log.d("wifiSupplicanState", "SCANNING");
        } else if (aState == DetailedState.CONNECTING) {
            Log.d("wifiSupplicanState", "CONNECTING");
        } else if (aState == DetailedState.OBTAINING_IPADDR) {
            Log.d("wifiSupplicanState", "OBTAINING_IPADDR");
        } else if (aState == DetailedState.CONNECTED) {
            Log.d("wifiSupplicanState", "CONNECTED");
        } else if (aState == DetailedState.DISCONNECTING) {
            Log.d("wifiSupplicanState", "DISCONNECTING");
        } else if (aState == DetailedState.DISCONNECTED) {
            Log.d("wifiSupplicanState", "DISCONNECTTED");
        } else if (aState == DetailedState.FAILED) {
            Log.d("wifiSupplicanState", "FAILED");
        }
    }

    public void checkState(int aInt) {
        if (aInt == WifiManager.WIFI_STATE_ENABLING) {
            Log.d("WifiManager", "WIFI_STATE_ENABLING");
        } else if (aInt == WifiManager.WIFI_STATE_ENABLED) {
            Log.d("WifiManager", "WIFI_STATE_ENABLED");
        } else if (aInt == WifiManager.WIFI_STATE_DISABLING) {
            Log.d("WifiManager", "WIFI_STATE_DISABLING");
        } else if (aInt == WifiManager.WIFI_STATE_DISABLED) {
            Log.d("WifiManager", "WIFI_STATE_DISABLED");
        }

    }
}

public void doWork() {
    runOnUiThread(new Runnable() {
        public void run() {
            try {

                Calendar c = Calendar.getInstance();
                SimpleDateFormat df = new SimpleDateFormat(
                                                                  "yyyy-MM-dd HH:mm:ss");
                String sDate = df.format(c.getTime());
                txtCurrentTime.setText(sDate);
                txtCurrentTime.setGravity(Gravity.CENTER);
                txtCurrentTime.setTextSize(20);
                setContentView(txtCurrentTime);
                ((LinearLayout) findViewById(R.id.systemtray)).addView(
                                                                              txtCurrentTime, new LayoutParams(78,
                                                                                                                      LayoutParams.WRAP_CONTENT));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    });
}

public void setVolume() {
    currentVolume = audio.getStreamVolume(AudioManager.STREAM_RING);
    if (currentVolume > 75 && currentVolume <= 100) {
        volume.setImageResource(R.drawable.volumefull);
    }
    if (currentVolume > 50 && currentVolume <= 75) {
        volume.setImageResource(R.drawable.volumehigh);
    }
    if (currentVolume > 25 && currentVolume <= 50) {
        volume.setImageResource(R.drawable.volumelow);
    }
    if (currentVolume > 0 && currentVolume <= 25) {
        volume.setImageResource(R.drawable.volumemin);
    }
    if (currentVolume == 0) {
        volume.setImageResource(R.drawable.volumemute);
    }
}

class CountDownRunner implements Runnable {
    // @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                doWork();
                setVolume();
                wcr.onReceive(getApplicationContext(), wifiStatus);
                pcr.onReceive(getApplicationContext(), batteryStatus);
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
            }
        }
    }
}
}
