package org.voxspatium;

import android.Manifest;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

//Wyświetlanie komunikatu o sieci, logu, wolnej pamięci i bazie danych na ekranie
public class MainActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //pasek gornego menu
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView view = (TextView) findViewById(R.id.log_screen);
        view.setMovementMethod(ScrollingMovementMethod.getInstance());

        DisplayInfo(view);
    }

    @Override
    public void onResume() {

        super.onResume();

        TextView view = (TextView) findViewById(R.id.log_screen);
        view.setMovementMethod(ScrollingMovementMethod.getInstance());

        DisplayInfo(view);
    }

    public void DisplayInfo(TextView view) {

        //czyszczenie ekranu
        view.setText("");

        //wyświetlenie statusu BLE
        view.append(System.getProperty("line.separator"));
        final BluetoothManager BLEManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (BLEManager.getAdapter() == null || !BLEManager.getAdapter().isEnabled()) {
            view.append(getString(R.string.ACT_BluetoothOff) + System.getProperty("line.separator"));
        } else {
            view.append(getString(R.string.ACT_BluetoothOn) + System.getProperty("line.separator"));
        }

        //wyświetlenie statusu GPS
        view.append(System.getProperty("line.separator"));
        final LocationManager manager = (LocationManager) getBaseContext().getSystemService( Context.LOCATION_SERVICE );
        if (!manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            view.append(getString(R.string.ACT_GPSOff)+ System.getProperty("line.separator"));
        } else {
            view.append(getString(R.string.ACT_GPSOn) + System.getProperty("line.separator"));
        }

        //wyświetlenie statusu uprawnienień do odczytu GPS
        view.append(System.getProperty("line.separator"));
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {
            view.append(getString(R.string.ACT_GPS_permission_denied_1)+getString(R.string.app_name)+" "+getString(R.string.ACT_GPS_permission_denied_2)+ System.getProperty("line.separator"));
        } else {
            view.append(getString(R.string.ACT_GPS_permission_granted) + System.getProperty("line.separator"));
        }

        /* pierwotny kod sprawdzał czy jest połączenie sieciowe
        //wyświetlenie statusu sieci
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
        if (netInfo == null || !netInfo.isConnected() || (netInfo.getType() != ConnectivityManager.TYPE_WIFI && netInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
        */
        //wyświetlenie statusu MOBILE
        view.append(System.getProperty("line.separator"));
        TelephonyManager mMOBILE = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (        mMOBILE.getNetworkType() ==  TelephonyManager.NETWORK_TYPE_GPRS
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
            view.append(getString(R.string.ACT_MOBILEOn) + System.getProperty("line.separator"));
        } else {
            view.append(getString(R.string.ACT_MOBILEOff) + System.getProperty("line.separator"));
        }

        //wyświetlenie statusu WiFi
        view.append(System.getProperty("line.separator"));
        WifiManager mWiFi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if ( mWiFi == null || mWiFi.isWifiEnabled() == false ){
            view.append(getString(R.string.ACT_WifiOff) + System.getProperty("line.separator"));
        } else {
            view.append(getString(R.string.ACT_WifiOn) + System.getProperty("line.separator"));
        }

        //wyświetlenie parametrów pamięci
        view.append(System.getProperty("line.separator"));
        File plik = new File(this.getFilesDir(), getString(R.string.log_file_name));
        Long rozmiar_pliku = plik.length()/1000000;
        Long wolna_pamiec = plik.getFreeSpace()/1000000;
        view.append("Free memory: "+wolna_pamiec.toString()+"MB" + System.getProperty("line.separator"));
        view.append("Used memory: log file "+rozmiar_pliku.toString()+"MB");

        //wyświetlenie parametrów bazy danych komunikatów
        //rozmiar
        plik = this.getDatabasePath(DataBaseProvider.DATABASE_NAME);
        Long rozmiar_bazy_danych = plik.length()/1000000;
        //timestamp
        SimpleDateFormat dataiczas = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        Date d = new Date(plik.lastModified ());
        //wersja
        String wersjaDB ="generic";
        File[] pliki = this.getFilesDir().listFiles();
        for (int i = 0; i < pliki.length; i++)
        {
            if (pliki[i].getName().endsWith(".sql")){
                wersjaDB = pliki[i].getName().replace(".sql","");
                wersjaDB = wersjaDB.replace(getString(R.string.UpdateFilesService_DBfilenameMask),"");
            }
        }

        view.append(" and database "+ rozmiar_bazy_danych.toString()+"MB" + System.getProperty("line.separator"));
        view.append(System.getProperty("line.separator"));
        view.append("Database version "+wersjaDB/*+dataiczas.format(d)*/+ System.getProperty("line.separator"));

        //wyświetlenie wersji i ID instalacji alias numer seryjny
        String id_instalacji="Unknown";
        
        File[] files = this.getFilesDir().listFiles();
        for(int i=0; i<files.length; i++)
        {
            File file = files[i];
            /*It's assumed that all file in the path are in supported type*/
            String fileName = file.getName();
            if(fileName.endsWith(".id")) // Condition to check .id file extension
                id_instalacji=fileName.replace(".id","");
        }
        view.append(System.getProperty("line.separator"));
        view.append("Version "+getString(R.string.app_version)+" Serial number:"+ id_instalacji + System.getProperty("line.separator"));

        //wyświetlenie informacji o dostępności nowej wersji aplikacji
        if (MyApplication.numer_dostepnej_wersji > Integer.parseInt(getString(R.string.app_version))){
            view.append(System.getProperty("line.separator"));
            view.append("Version "+MyApplication.numer_dostepnej_wersji+ " available. Visit Google Play and upgrade application."+ System.getProperty("line.separator"));
        }

        //wyświetlenie kontaktu serwisowego
        view.append(System.getProperty("line.separator"));
        view.append(getString(R.string.ACT_ServiceInfo)+ System.getProperty("line.separator"));

        //wyświetlenie licencji
        view.append(System.getProperty("line.separator"));
        view.append(getString(R.string.ACT_LicenseInfo_Title)+ System.getProperty("line.separator"));
        view.setLinkTextColor(Color.BLUE);
        //view.append(Html.fromHtml(getString(R.string.ACT_LicenseInfo_Body))+ System.getProperty("line.separator"));
        view.append(Html.fromHtml("Software is available under the <a href=\"https://www.gnu.org/licenses/gpl.html\">GNU General Public License</a>, includes Apache Commons Net library available under <a href=\"http://www.apache.org/licenses/LICENSE-2.0\">Apache License version 2.0, January 2004</a>"));
        view.setClickable(true);
        view.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
