package org.voxspatium;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

// do aktualizacji bazy komunikatów
public class UpdateFilesService extends IntentService {

    public String NazwaPlikuObecnejBazy = "";         //inicjalizowane w metodzie czy_jest_nowa_wersja_bazy_danych()
    public String NazwaPlikuNowejWersjiBazy = "";     //inicjalizowane w metodzie czy_jest_nowa_wersja_bazy_danych()

    public UpdateFilesService() {
        super("UpdateFilesService");
    }

    /**
     * Called when the service is being created.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        MyApplication.writeLog(getString(R.string.UpdateFilesService_created),getBaseContext(),3);
    }

    /**
     * The service is starting, due to a call to startService()
     */
    @Override
    public void onHandleIntent(Intent intent)  {

            MyApplication.writeLog(getString(R.string.UpdateFilesService_start),getBaseContext(),3);

            if (czy_jest_nowa_wersja_bazy_danych()) {
                if (pobranie_nowej_wersji_bazy_danych()) {
                    if (zamkniecie_i_zablokowanie_dostepu_do_obecnej_bazy_danych()){
                        if (DataBaseProvider.UpdateDB(NazwaPlikuNowejWersjiBazy,getApplicationContext())) {
                            usuniecie_pliku_zrodlowego_obecnej_bazy_danych();
                        }
                        else {
                            usuniecie_pliku_zrodlowego_nowej_wersji_bazy_danych();
                        }
                    }
                    else {
                        usuniecie_pliku_zrodlowego_nowej_wersji_bazy_danych();
                    }
                    otworzenie_i_odblokowanie_dostepu_do_bazy_danych();
                }
            }

            UstawienieTimeraUpdateFilesServices();
    }

    /**
     * A client is binding to the service with bindService()
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Called when The service is no longer used and is being destroyed
     */
    @Override
    public void onDestroy() {
        MyApplication.writeLog(getString(R.string.UpdateFilesService_stop),getBaseContext(),3);
        super.onDestroy();
    }

    public boolean czy_jest_nowa_wersja_bazy_danych() {

        // Cancel background network operation if we do not have network connectivity.
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
        if (netInfo == null || !netInfo.isConnected() || (netInfo.getType() != ConnectivityManager.TYPE_WIFI && netInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
            MyApplication.writeLog(getString(R.string.UpdateFilesService_NetworkOff),getBaseContext(),1);
            return false;
        }

        //odczytanie obecnej wersji bazy danych,domyślnie baza generyczna
        long numer_obecnej_bazy = 0;
        String fileName;
        File[] files = this.getFilesDir().listFiles();
        for (int i = 0; i < files.length; i++)
        {
            fileName = files[i].getName();
            if (fileName.endsWith(".sql")){
                NazwaPlikuObecnejBazy = fileName;
                fileName = fileName.replace(".sql","");
                fileName = fileName.replace(getString(R.string.UpdateFilesService_DBfilenameMask),"");
                numer_obecnej_bazy = Long.parseLong(fileName);
            }
        }

        //odczytanie nowej wersji bazy danych w sieci,domyślnie baza generyczna
        long numer_nowej_wersji_bazy_w_sieci = 0;
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(getString(R.string.UpdateFilesService_HostName), 21);
            ftpClient.login(getString(R.string.UpdateFilesService_User), getString(R.string.UpdateFilesService_Password));
            ftpClient.enterLocalPassiveMode();

            // Lists files and directories by listNames()
            ftpClient.changeWorkingDirectory(getString(R.string.UpdateFilesService_HostNameDirectory));
            String[] NazwyPlikowKatalogow = ftpClient.listNames();
            if (NazwyPlikowKatalogow != null && NazwyPlikowKatalogow.length > 0) {
                for (String PlikLubKatalog: NazwyPlikowKatalogow) {
                    if (PlikLubKatalog.endsWith(".sql")) {
                        NazwaPlikuNowejWersjiBazy = PlikLubKatalog;
                        PlikLubKatalog = PlikLubKatalog.replace(".sql", "");
                        PlikLubKatalog = PlikLubKatalog.replace(getString(R.string.UpdateFilesService_DBfilenameMask), "");
                        numer_nowej_wersji_bazy_w_sieci = Long.parseLong(PlikLubKatalog);
                    }
                }
            }

        } catch (IOException ex) {
            numer_nowej_wersji_bazy_w_sieci = 0;
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            MyApplication.writeLog(getString(R.string.UpdateFilesService_ReadDBVersionError)+ System.getProperty("line.separator")+errors.toString(),getBaseContext(),1);
        } finally {
            // logs out and disconnects from server
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                numer_nowej_wersji_bazy_w_sieci = 0;
                StringWriter errors = new StringWriter();
                ex.printStackTrace(new PrintWriter(errors));
                MyApplication.writeLog(getString(R.string.UpdateFilesService_connection_close_error)+ System.getProperty("line.separator")+errors.toString(),getBaseContext(),1);
            }
        }

        if (numer_nowej_wersji_bazy_w_sieci>numer_obecnej_bazy) {
            // jak jest nowa baza
            return true;
        }
        else {
            // gdy nie ma nowej bazy
            MyApplication.writeLog(getString(R.string.UpdateFilesService_DBversions)+Long.toString(numer_obecnej_bazy)+" found in server:"+Long.toString(numer_nowej_wersji_bazy_w_sieci), getBaseContext(), 1);
            return false;
        }
    }

    public boolean pobranie_nowej_wersji_bazy_danych() {

        // Cancel background network operation if we do not have network connectivity.
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
        if (netInfo == null || !netInfo.isConnected() || (netInfo.getType() != ConnectivityManager.TYPE_WIFI && netInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
            MyApplication.writeLog(getString(R.string.UpdateFilesService_NetworkOff),getBaseContext(),1);
            return false;
        }

        // download pliku
        boolean success = false;
        FTPClient ftpClient = new FTPClient();
        try {

            ftpClient.connect(getString(R.string.UpdateFilesService_HostName), 21);
            ftpClient.login(getString(R.string.UpdateFilesService_User), getString(R.string.UpdateFilesService_Password));
            ftpClient.enterLocalPassiveMode();
            ftpClient.changeWorkingDirectory(getString(R.string.UpdateFilesService_HostNameDirectory));
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            String remoteFile1 = NazwaPlikuNowejWersjiBazy;
            File downloadFile1 = new File(getBaseContext().getFilesDir(),NazwaPlikuNowejWersjiBazy);
            OutputStream outputStream1 = new BufferedOutputStream(new FileOutputStream(downloadFile1));
            success = ftpClient.retrieveFile(remoteFile1, outputStream1);
            outputStream1.close();

        } catch (IOException ex) {
            success = false;
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            MyApplication.writeLog(getString(R.string.UpdateFilesService_transmission_error)+ System.getProperty("line.separator")+errors.toString(),getBaseContext(),1);
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex1) {
                success = false;
                MyApplication.writeLog(getString(R.string.UpdateFilesService_connection_close_error),getBaseContext(),1);
            }
        }

        //decyzja i ewentualnie usunięcie pozostałości pliku po nieudanej transmisji
        if (success) {
            MyApplication.writeLog(getString(R.string.UpdateFilesService_file_received)+" "+NazwaPlikuNowejWersjiBazy,getBaseContext(),1);
            return true;
        }
        else {
            //usunięcie niepewnego pliku przy nieudanej transmsji lub zapisie, aby wymusić przy następnej sesji aktualizację
            File plik_to_delete = new File(getBaseContext().getFilesDir(), NazwaPlikuNowejWersjiBazy);
            plik_to_delete.delete();
            return false;
        }
    }

    public boolean zamkniecie_i_zablokowanie_dostepu_do_obecnej_bazy_danych() {

        if (DataBaseProvider.CloseDB(getApplicationContext())) {
            MyApplication.isDatabaseUpdatePending = true;
            return true;
        } else {
            MyApplication.writeLog(getString(R.string.UpdateFilesService_CloseDB_error), getBaseContext(),1);
            return false;
        }
    }

    public void otworzenie_i_odblokowanie_dostepu_do_bazy_danych() {
        //zdjęcie blokady korzystania z bazy
        MyApplication.isDatabaseUpdatePending = false;
        //otwarcie bazy
        DataBaseProvider.OpenDB(getApplicationContext());
    }

    public void usuniecie_pliku_zrodlowego_nowej_wersji_bazy_danych() {
        File plik_do_usuniecia = new File(getBaseContext().getFilesDir(), NazwaPlikuNowejWersjiBazy);
        plik_do_usuniecia.delete();
    }

    public void usuniecie_pliku_zrodlowego_obecnej_bazy_danych () {
        if (!(NazwaPlikuObecnejBazy == "")) {
            File plik_do_usuniecia = new File(getBaseContext().getFilesDir(), NazwaPlikuObecnejBazy);
            plik_do_usuniecia.delete();
        }
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
    }
}

