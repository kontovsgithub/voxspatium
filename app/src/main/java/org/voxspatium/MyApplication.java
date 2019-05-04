package org.voxspatium;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MyApplication extends Application {

    public static Integer numer_dostepnej_wersji = 0;

    public static String  PACKAGE_NAME;
    public static Boolean isLeScanStarted = false;
    public static Boolean isDatabaseUpdatePending = false;

    // Define a listener that responds to location updates
    static LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {}
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override
        public void onProviderEnabled(String provider) {}
        @Override
        public void onProviderDisabled(String provider) {}
    };

    @Override
    public void onCreate() {
        super.onCreate();

        PACKAGE_NAME = getApplicationContext().getPackageName();

        writeLog(getBaseContext().getString(R.string.new_session),getBaseContext(),1);
        writeLog(getString(R.string.running_version)+" "+getString(R.string.app_version), getBaseContext(), 1);

        //rejestracja VUI
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        this.registerReceiver(new VoiceUserInterface(), filter);
        //nie implementowałem unregisterReceiver() ponieważ nie znalazlem miejsca w cyklu życia Application na tą operację. Polegam na mechanizmach OS usuwających Aplikacje z pamięci

        //uruchomienie serwisu do aktualizacji plików - download
        UstawienieTimeraUpdateFilesServices();

        //uruchomienie serwisu do aktualizacji plików - upload
        UstawienieTimeraSendFilesServices();

        //uruchomienie serwisu do sprawdzenia wersji
        UstawienieTimeraCheckVersionServices();

        //uruchomienie scanowanie BLE
        UstawienieTimeraBLEscaning();

        //display notification about running
        showNotification();
    }

    public void UstawienieTimeraUpdateFilesServices() {
        //losowanie terminu uruchomienia update bazy danych
        int hour = (int)Math.round(Math.random() * (23 - 0));
        int min  = (int)Math.round(Math.random() * (59 - 0));
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.DAY_OF_MONTH, 1);                  //dodanie jednej doby do wylosowanej godziny i minuty
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, min);

        /*Do testowania UpdateFilesService ustawienie daty wstecznej
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 21);
        calendar.set(Calendar.MINUTE, 05);
        */


        // Timer jobs for UpdateFilesService
        Timer UFSTimer = new Timer();
        TimerTask UFSTask = new TimerTask() {
            @Override
            public void run() {
                startService(new Intent(getBaseContext(), UpdateFilesService.class));
            }
        };
        UFSTimer.schedule(UFSTask,calendar.getTime());

        SimpleDateFormat FormatDaty = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        String ScheduledDateandTime = FormatDaty.format(calendar.getTime());
        MyApplication.writeLog("UFS:next session at "+ScheduledDateandTime, getBaseContext(),1);
            /*
            // Schedule job for UpdateDataBaseService based on Android Alarms
            // alarm można skasować kodem: if (alarmMgr!= null) {alarmMgr.cancel(alarmIntent);} pominąłem obsługę tego,ponieważ akceptuje działanie alarmu po kill-u lub crash-u aplikacji
            // alarm odpala sie po każdej instalacji, nie czeka na wskazaną porę
            AlarmManager alarmMgr;
            PendingIntent alarmIntent;
            Intent intent = new Intent(getBaseContext(), UpdateFilesService.class);
            alarmMgr = (AlarmManager)getBaseContext().getSystemService(Context.ALARM_SERVICE);
            alarmIntent = PendingIntent.getService(getBaseContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),AlarmManager.INTERVAL_DAY, alarmIntent);
            */
    }

    public void UstawienieTimeraSendFilesServices() {
        //losowanie terminu uruchomienia update bazy danych
        int hour = (int)Math.round(Math.random() * (23 - 0));
        int min  = (int)Math.round(Math.random() * (59 - 0));
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.DAY_OF_MONTH, 1);                  //dodanie jednej doby do wylosowanej godziny i minuty
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, min);

        /*Do testowania SendFilesService
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 21);
        calendar.set(Calendar.MINUTE, 10);
        */


        // Timer jobs for SendFilesService
        Timer SFSTimer = new Timer();
        TimerTask SFSTask = new TimerTask() {
            @Override
            public void run() {
                startService(new Intent(getBaseContext(), SendFilesService.class));
            }
        };
        SFSTimer.schedule(SFSTask,calendar.getTime());

        SimpleDateFormat FormatDaty = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        String ScheduledDateandTime = FormatDaty.format(calendar.getTime());
        MyApplication.writeLog("SFS:next session at "+ScheduledDateandTime, getBaseContext(),1);
    }

    public void UstawienieTimeraCheckVersionServices() {
        //losowanie terminu uruchomienia
        int hour = (int)Math.round(Math.random() * (23 - 0));
        int min  = (int)Math.round(Math.random() * (59 - 0));
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.DAY_OF_MONTH, 1);                  //dodanie jednej doby do wylosowanej godziny i minuty
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, min);

        /*Do testowania CheckVersionService
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        calendar.set(Calendar.HOUR_OF_DAY,12);
        calendar.set(Calendar.MINUTE, 52);
        */

        // Timer jobs for CheckVersionService
        Timer CVSTimer = new Timer();
        TimerTask CVSTask = new TimerTask() {
            @Override
            public void run() {
                startService(new Intent(getBaseContext(), CheckVersionService.class));
            }
        };
        CVSTimer.schedule(CVSTask,calendar.getTime());

        SimpleDateFormat FormatDaty = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        String ScheduledDateandTime = FormatDaty.format(calendar.getTime());
        MyApplication.writeLog("CVS:next session at "+ScheduledDateandTime, getBaseContext(),1);
    }

    public void UstawienieTimeraBLEscaning(){
        // Timer jobs for BLE scaning. Kasowanie timera ze scanowaniem BLE możliwe komendą:  myTimer.cancel(); OS likwiduje timer samodzielnie przy crash-u lub kill-u aplikacji
        boolean RunAsDaemon = true;
        Timer myTimer = new Timer(RunAsDaemon);
        Resources res = getResources();
        Integer okres_scanowania = res.getInteger(R.integer.czas_szukania_milisekundy)+res.getInteger(R.integer.czas_przerwy_milisekundy);
        myTimer.schedule(SearchBLE, 0, okres_scanowania.longValue());
    }

    //procedura tworzenia i zapisu do logu
    public static void writeLog(String komunikat, Context kontekst, Integer message_level) {

        //ewentualne założenie loga jeżeli nie istnieje lub został skasowany po wysłaniu do centrum
        boolean CzyLogIstnieje = false;
        File plik = new File(kontekst.getFilesDir(), kontekst.getString(R.string.log_file_name));
        try {
            plik.createNewFile();
            CzyLogIstnieje = true;
        } catch (IOException e) {
            Toast.makeText(kontekst, kontekst.getString(R.string.log_open_exception), Toast.LENGTH_LONG).show();
        }

        //zapis tylko komunikatów z danego lub niższego poziomu. Im wyższa wartość poziomu tym mniej ważny komunikat
        if (CzyLogIstnieje==true && message_level <= kontekst.getResources().getInteger(R.integer.current_log_level)) {
            //ustawienie nazwy pliku z logiem
            String nazwa_pliku_z_logiem = kontekst.getString(R.string.log_file_name);
            //odczyt daty
            SimpleDateFormat dataiczas = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            String currentDateandTime = dataiczas.format(new Date());
            //odczyt lokalizacji
            String szerokosc_i_dlugosc = null;
            LocationManager locationManager = (LocationManager) kontekst.getSystemService(kontekst.LOCATION_SERVICE);
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 300000, 0, locationListener);
                Location lokalizacja = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lokalizacja == null) {
                    szerokosc_i_dlugosc = kontekst.getString(R.string.gps_location_not_found);
                } else {
                    szerokosc_i_dlugosc = LocationManager.GPS_PROVIDER.toUpperCase() + ":Long=" + String.format("%.4f", lokalizacja.getLongitude()) + " Lati=" + String.format("%.4f", lokalizacja.getLatitude());
                }
            } catch (Exception e) {
                // lets the user know there is a problem with the gps
                szerokosc_i_dlugosc = kontekst.getString(R.string.gps_exception);
            }
            //sprawdzenie wielkosći wolnej pamięci
            Long wolna_pamiec = plik.getFreeSpace()/1000000;
            String RAM = "Free memory: "+wolna_pamiec.toString()+"MB";
            //skompletowanie komunikatu
            String message = (currentDateandTime + " " + szerokosc_i_dlugosc + " " + RAM + " " + komunikat + System.getProperty("line.separator"));

            //zapis do logu jeżeli jest więcej niż minimalny poziom wolnej pamięci.
            Resources res = kontekst.getResources();
            Integer minimalna_poziom_wolnej_pamieci = res.getInteger(R.integer.minimalna_poziom_wolnej_pamieci);
            if (wolna_pamiec.intValue() > minimalna_poziom_wolnej_pamieci) {
                FileOutputStream outputStream;
                try {
                    outputStream = kontekst.openFileOutput(nazwa_pliku_z_logiem, Context.MODE_APPEND);
                    outputStream.write(message.getBytes());
                    outputStream.close();
                } catch (Exception e) {
                    Toast.makeText(kontekst, kontekst.getString(R.string.log_security_exception), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    //procedura pokazania informacji na ekranie, że w tle pracuje aplikacja
    private void showNotification() {
        // Unique Identification Number for the Notification. We use it on Notification start, and to cancel it.
        // Cancel jest możliwy poprzez wywołanie: mNM.cancel(NOTIFICATION); OS likwiduje notyfikajcę samodzielnie przy crash-u lub kill-u aplikacji więc nie implementowałem tego
        NotificationManager mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        int NOTIFICATION = R.string.app_name;

        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.app_name);

        // The PendingIntent to launch MainActivity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.app_name))  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .build();

        // Send the notification.
        mNM.notify(NOTIFICATION, notification);
    }

    // kod demona szukającego BLE
    TimerTask SearchBLE = new TimerTask() {
        @Override
        public void run() {

            //At the beginning of the run() method, set the thread to use background priority by calling Process.setThreadPriority() with THREAD_PRIORITY_BACKGROUND.
            //This approach reduces resource competition between the Runnable object's thread and the UI thread.
            //https://developer.android.com/training/multiple-threads/define-runnable.html
            //szukanie tylko wskazanego UUID nie działa http://code.google.com/p/android/issues/detail?id=59490&q=BLE&colspec=ID%20Type%20Status%20Owner%20Summary%20Stars
            //mBluetoothAdapter.startLeScan(new UUID[]{UUID.fromString(getString(R.string.BLE_UUID))},mLeScanCallback);
            final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();

            if (!isLeScanStarted) {
                // Ensures Bluetooth is available on the device and it is enabled.
                if (!(mBluetoothAdapter == null) && mBluetoothAdapter.isEnabled()) {
                    isLeScanStarted=mBluetoothAdapter.startLeScan(mLeScanCallback);
                    if (isLeScanStarted) { MyApplication.writeLog("Search ON", getBaseContext(),5);}
                } else {
                    //automatic start of Bluetooth
                    if (mBluetoothAdapter.enable()){
                        MyApplication.writeLog(getString(R.string.BLE_successfully_switchON), getBaseContext(),1);
                        isLeScanStarted=mBluetoothAdapter.startLeScan(mLeScanCallback);
                        if (isLeScanStarted) { MyApplication.writeLog("Search ON", getBaseContext(),5);}
                    } else {
                        MyApplication.writeLog(getString(R.string.BLE_cannot_switchON), getBaseContext(),1);
                    }
                }
            }
            else {
                MyApplication.writeLog(getString(R.string.BLE_already_started_exception), getBaseContext(),1);
            }

            try {
                Resources res = getResources();
                Integer czas_szukania = res.getInteger(R.integer.czas_szukania_milisekundy);
                Thread.sleep(czas_szukania.longValue());
                //if (isLeScanStarted) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                //}
                isLeScanStarted=false;
                MyApplication.writeLog("Search OFF", getBaseContext(),5);
            } catch (InterruptedException e) {
                MyApplication.writeLog(getString(R.string.BLE_thread_sleep_exception), getBaseContext(),1);
            }

        }
    };

    // Device scan callback. Obsługa wyniku SearchBLE
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

            // dekodowanie rozgłaszanego pakietu
            char[] hexArray = "0123456789ABCDEF".toCharArray();
            char[] hexChars = new char[scanRecord.length * 2];
            for (int j = 0; j < scanRecord.length; j++) {
                int v = scanRecord[j] & 0xFF;
                hexChars[j * 2] = hexArray[v >>> 4];
                hexChars[j * 2 + 1] = hexArray[v & 0x0F];
            }
            String wykryteUUID = new String(hexChars, 18, 32);
            String wykrytaNazwa = device.getName();

            // filtrowanie tylko BLE należące do systemu
            if (wykryteUUID.compareTo(getString(R.string.BLE_Expected_UUID).toUpperCase()) == 0)
                if (wykrytaNazwa.compareTo(getString(R.string.BLE_Expected_Name)) == 0) {
                    //odczyt wartosci Major i Minor
                    String wykrytyMajor = new String(hexChars, 50, 4);
                    String wykrytyMinor = new String(hexChars, 54, 4);
                    Integer MajorInt = Integer.parseInt(wykrytyMajor, 16);
                    Integer MinorInt = Integer.parseInt(wykrytyMinor, 16);
                    wykrytyMajor = MajorInt.toString();
                    wykrytyMinor = MinorInt.toString();
                    MyApplication.writeLog(getString(R.string.BLE_major_recesived) + wykrytyMajor + getString(R.string.BLE_minor_recesived) + wykrytyMinor, getBaseContext(),4);
                    //wywołanie odtworzenia komunikatu
                    Intent DaneDlaSelectAndPlayService = new Intent(getBaseContext(), SelectAndPlayService.class);
                    DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Major", wykrytyMajor);
                    DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Minor", wykrytyMinor);
                    DaneDlaSelectAndPlayService.putExtra(MyApplication.PACKAGE_NAME + ".Kanal", "z");
                    startService(DaneDlaSelectAndPlayService);
                }
        }
    };
}
