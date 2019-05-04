package org.voxspatium;

import android.Manifest;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;

// do obsługi interface głosowego użytkownika
public class VoiceUserInterface extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

            MyApplication.writeLog(context.getString(R.string.VoiceUserInterface_ReceiveIntent), context, 3);

            //informacja o statusie regulacji głośności i reakcja na sygnał wyładowania baterii
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                MyApplication.writeLog(context.getString(R.string.VoiceUserInterface_ScreenOff), context, 4);
                Intent DaneDlaSelectAndPlayService = new Intent(context, SelectAndPlayService.class);
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Major", "0");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Minor", "4");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Kanal", "t");
                context.startService(DaneDlaSelectAndPlayService);
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                MyApplication.writeLog(context.getString(R.string.VoiceUserInterface_ScreenOn), context, 4);
                Intent DaneDlaSelectAndPlayService = new Intent(context, SelectAndPlayService.class);
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Major", "0");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Minor", "5");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Kanal", "t");
                context.startService(DaneDlaSelectAndPlayService);
            } else if (intent.getAction().equals(Intent.ACTION_BATTERY_LOW)) {
                MyApplication.writeLog(context.getString(R.string.VoiceUserInterface_BatteryOff), context, 3);
                Intent DaneDlaSelectAndPlayService = new Intent(context, SelectAndPlayService.class);
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Major", "0");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Minor", "6");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Kanal", "t");
                context.startService(DaneDlaSelectAndPlayService);
            }

            //informacja o poziomie baterii
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryProcentNaladowania = 100*(level / (float)scale);
            if (batteryProcentNaladowania>90) {
                Intent DaneDlaSelectAndPlayService = new Intent(context, SelectAndPlayService.class);
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Major", "0");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Minor", "7");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Kanal", "t");
                context.startService(DaneDlaSelectAndPlayService);
            }
            if (batteryProcentNaladowania>70 && batteryProcentNaladowania<=90) {
                Intent DaneDlaSelectAndPlayService = new Intent(context, SelectAndPlayService.class);
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Major", "0");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Minor", "8");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Kanal", "t");
                context.startService(DaneDlaSelectAndPlayService);
            }
            if (batteryProcentNaladowania>50 && batteryProcentNaladowania<=70) {
                Intent DaneDlaSelectAndPlayService = new Intent(context, SelectAndPlayService.class);
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Major", "0");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Minor", "9");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Kanal", "t");
                context.startService(DaneDlaSelectAndPlayService);
            }
            if (batteryProcentNaladowania>30 && batteryProcentNaladowania<=50) {
                Intent DaneDlaSelectAndPlayService = new Intent(context, SelectAndPlayService.class);
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Major", "0");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Minor", "10");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Kanal", "t");
                context.startService(DaneDlaSelectAndPlayService);
            }
            if (batteryProcentNaladowania>10 && batteryProcentNaladowania<=30) {
                Intent DaneDlaSelectAndPlayService = new Intent(context, SelectAndPlayService.class);
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Major", "0");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Minor", "11");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Kanal", "t");
                context.startService(DaneDlaSelectAndPlayService);
            }
            if (batteryProcentNaladowania<=10) {
                Intent DaneDlaSelectAndPlayService = new Intent(context, SelectAndPlayService.class);
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Major", "0");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Minor", "12");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Kanal", "t");
                context.startService(DaneDlaSelectAndPlayService);
            }

            boolean networkOK = false;
            boolean bluetoothOK = false;
            boolean GPS_OK = false;
            boolean PermissionToLocationOK = false;

            /* pierwotny kod sprawdzał czy jest połączenie sieciowe
            //sprawdzenie statusu sieci
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
            if (netInfo == null || !netInfo.isConnected() || (netInfo.getType() != ConnectivityManager.TYPE_WIFI && netInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
                //info o braku sieci
                MyApplication.writeLog(context.getString(R.string.VoiceUserInterface_NetworkOff), context, 3);
                Intent DaneDlaSelectAndPlayService = new Intent(context, SelectAndPlayService.class);
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Major", "0");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Minor", "1");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Kanal", "t");
                context.startService(DaneDlaSelectAndPlayService);
            } else {
                networkOK = true;
            }
            */

            //sprawdzenie statusu sieci MOBILE
            TelephonyManager mMOBILE = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (    mMOBILE.getNetworkType() ==  TelephonyManager.NETWORK_TYPE_GPRS
                ||  mMOBILE.getNetworkType() ==  TelephonyManager.NETWORK_TYPE_EDGE
                ||  mMOBILE.getNetworkType() ==  TelephonyManager.NETWORK_TYPE_UMTS
                ||  mMOBILE.getNetworkType() ==  TelephonyManager.NETWORK_TYPE_HSDPA
                ||  mMOBILE.getNetworkType() ==  TelephonyManager.NETWORK_TYPE_HSUPA
                ||  mMOBILE.getNetworkType() ==  TelephonyManager.NETWORK_TYPE_HSPA
                ||  mMOBILE.getNetworkType() ==  TelephonyManager.NETWORK_TYPE_CDMA
                ||  mMOBILE.getNetworkType() ==  TelephonyManager.NETWORK_TYPE_EVDO_0
                ||  mMOBILE.getNetworkType() ==  TelephonyManager.NETWORK_TYPE_EVDO_A
                ||  mMOBILE.getNetworkType() ==  TelephonyManager.NETWORK_TYPE_EVDO_B
                ||  mMOBILE.getNetworkType() ==  TelephonyManager.NETWORK_TYPE_1xRTT
                ||  mMOBILE.getNetworkType() ==  TelephonyManager.NETWORK_TYPE_IDEN
                ||  mMOBILE.getNetworkType() ==  TelephonyManager.NETWORK_TYPE_LTE
                ||  mMOBILE.getNetworkType() ==  TelephonyManager.NETWORK_TYPE_EHRPD
                ||  mMOBILE.getNetworkType() ==  TelephonyManager.NETWORK_TYPE_HSPAP)
            {
                networkOK = true;
            }

            //sprawdzenie statusu WiFi
            WifiManager mWiFi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (!( mWiFi == null || mWiFi.isWifiEnabled() == false ))
            {
                networkOK = true;
            }

            //info o braku sieci
            if (networkOK==false){
                MyApplication.writeLog(context.getString(R.string.VoiceUserInterface_NetworkOff), context, 3);
                Intent DaneDlaSelectAndPlayService = new Intent(context, SelectAndPlayService.class);
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Major", "0");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Minor", "1");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Kanal", "t");
                context.startService(DaneDlaSelectAndPlayService);
            }

            //sprawdzenie statusu BLE
            final BluetoothManager BLEManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (!(BLEManager.getAdapter() == null || !BLEManager.getAdapter().isEnabled())) {
                bluetoothOK = true;
            }

            //sprawdzenie statusu GPS
            final LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                GPS_OK = true;
            }

             //sprawdzenie statusu uprawnienień do odczytu GPS
            if (!(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED)) {
                PermissionToLocationOK=true;
            }

            //info o braku możliwości odbierania komunikatów
            if (!(bluetoothOK && GPS_OK && PermissionToLocationOK)) {
                MyApplication.writeLog(context.getString(R.string.VoiceUserInterface_NoBLEsearch), context, 3);
                Intent DaneDlaSelectAndPlayService = new Intent(context, SelectAndPlayService.class);
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Major", "0");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Minor", "0");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Kanal", "t");
                context.startService(DaneDlaSelectAndPlayService);
            }

            //info o braku bluetooth
            if (bluetoothOK==false) {
               MyApplication.writeLog(context.getString(R.string.VoiceUserInterface_BluetoothOff), context, 3);
                Intent DaneDlaSelectAndPlayService = new Intent(context, SelectAndPlayService.class);
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Major", "0");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Minor", "2");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Kanal", "t");
                context.startService(DaneDlaSelectAndPlayService);
            }

            //info o braku GPS
            if (GPS_OK==false) {
                MyApplication.writeLog(context.getString(R.string.VoiceUserInterface_GPSOff), context, 3);
                Intent DaneDlaSelectAndPlayService = new Intent(context, SelectAndPlayService.class);
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Major", "0");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Minor", "3");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Kanal", "t");
                context.startService(DaneDlaSelectAndPlayService);
            }

            //info o braku uprawnien do GPS (lokalizacji)
            if (PermissionToLocationOK==false) {
                MyApplication.writeLog(context.getString(R.string.VoiceUserInterface_GPS_permission_denied), context, 3);
                Intent DaneDlaSelectAndPlayService = new Intent(context, SelectAndPlayService.class);
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Major", "0");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Minor", "13");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Kanal", "t");
                context.startService(DaneDlaSelectAndPlayService);
            }

            //info o dostepnej nowej wersji
            if (MyApplication.numer_dostepnej_wersji > Integer.parseInt(context.getString(R.string.app_version))) {
                MyApplication.writeLog(context.getString(R.string.VoiceUserInterface_new_version_available), context, 3);
                Intent DaneDlaSelectAndPlayService = new Intent(context, SelectAndPlayService.class);
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Major", "0");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Minor", "14");
                DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Kanal", "t");
                context.startService(DaneDlaSelectAndPlayService);
            }
    }
}
