package org.voxspatium;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

// do restartu i przesyłania logu do centrum oraz przesylania danych o stanie baterii do centrum
public class SendFilesService extends IntentService {

    public SendFilesService() {
        super("SendFilesService");
    }

    /**
     * Called when the service is being created.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        MyApplication.writeLog(getString(R.string.SendFilesService_created),getBaseContext(),3);
    }

    /**
     * The service is starting, due to a call to startService()
     */
    @Override
    public void onHandleIntent(Intent intent)  {
       MyApplication.writeLog(getString(R.string.SendFilesService_start),getBaseContext(),3);
       if (WyslijLog()) {
           File plik = new File(getBaseContext().getFilesDir(), getBaseContext().getString(R.string.log_file_name));
           plik.delete();
       }
       UstawienieTimeraSendFilesServices();
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
        MyApplication.writeLog(getString(R.string.SendFilesService_stop),getBaseContext(),3);
        super.onDestroy();
    }

    public boolean WyslijLog() {
        // Cancel background network operation if we do not have network connectivity.
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
        if (netInfo == null || !netInfo.isConnected() || (netInfo.getType() != ConnectivityManager.TYPE_WIFI && netInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
            MyApplication.writeLog(getString(R.string.SendFilesService_NetworkOff),getBaseContext(),1);
            return false;
        }

        //ustawienie nazwy pliku na serwerze
        String nazwaPlikuNaSerwerze = getString(R.string.log_file_name);
        //pobranie ID instalacji alias numer seryjny
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
        nazwaPlikuNaSerwerze = nazwaPlikuNaSerwerze+id_instalacji;
        //dodanie Timestamp
        SimpleDateFormat dataiczas = new SimpleDateFormat("yyMMddHHmmss", Locale.US);
        String currentDateandTime = dataiczas.format(new Date());
        nazwaPlikuNaSerwerze = nazwaPlikuNaSerwerze+"_"+currentDateandTime;

        //wysyłanie pliku
        InputStream inputStream = null;
        boolean wynikTransmsji=false;
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(getString(R.string.SendFilesService_HostName), 21);
            ftpClient.login(getString(R.string.SendFilesService_User), getString(R.string.SendFilesService_Password));
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            File LocalFile = new File(getBaseContext().getFilesDir(), getBaseContext().getString(R.string.log_file_name));
            inputStream = new FileInputStream(LocalFile);

            boolean done = ftpClient.storeFile(getString(R.string.SendFilesService_HostNameDirectory)+"/"+nazwaPlikuNaSerwerze, inputStream);

            if (done) {
                MyApplication.writeLog(getString(R.string.SendFilesService_file_send), getBaseContext(), 1);
                wynikTransmsji=true;
            } else {
                MyApplication.writeLog(getString(R.string.SendFilesService_transmission_error),getBaseContext(),1);
                wynikTransmsji=false;
            }
        }
        catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            MyApplication.writeLog(getString(R.string.SendFilesService_transmission_error)+ System.getProperty("line.separator")+errors.toString(),getBaseContext(),1);
            wynikTransmsji=false;
        }
        finally {
            try {
                inputStream.close();
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
                return wynikTransmsji;
            } catch (Exception e1) {
                StringWriter errors = new StringWriter();
                e1.printStackTrace(new PrintWriter(errors));
                MyApplication.writeLog(getString(R.string.SendFilesService_connection_close_error)+ System.getProperty("line.separator")+errors.toString(),getBaseContext(),1);
                return false;
            }
        }
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
}




