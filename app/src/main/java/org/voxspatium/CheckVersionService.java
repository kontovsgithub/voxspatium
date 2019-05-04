package org.voxspatium;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;

//To check available version app e.g. new one
public class CheckVersionService extends IntentService {

    public CheckVersionService() {
        super("CheckVersionService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MyApplication.writeLog(getString(R.string.CheckVersionService_created),getBaseContext(),3);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        MyApplication.writeLog(getString(R.string.CheckVersionService_start),getBaseContext(),3);

        sprawdz_wersje();

        UstawienieTimeraCheckVersionServices();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        MyApplication.writeLog(getString(R.string.CheckVersionService_stop),getBaseContext(),3);
        super.onDestroy();
    }

    public boolean sprawdz_wersje() {

        // Cancel background network operation if we do not have network connectivity.
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
        if (netInfo == null || !netInfo.isConnected() || (netInfo.getType() != ConnectivityManager.TYPE_WIFI && netInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
            MyApplication.writeLog(getString(R.string.CheckVersionService_NetworkOff), getBaseContext(), 1);
            return false;
        }

        //start read available version via http
        InputStream stream = null;
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(getString(R.string.CheckVersionServiceURL));
            connection = (HttpsURLConnection) url.openConnection();
            // Timeout for reading InputStream arbitrarily set to 3000ms.
            connection.setReadTimeout(3000);
            // Timeout for connection.connect() arbitrarily set to 3000ms.
            connection.setConnectTimeout(3000);
            // For this use case, set HTTP method to GET.
            connection.setRequestMethod("GET");
            // Already true by default but setting just in case; needs to be true since this request
            // is carrying an input (response) body.
            connection.setDoInput(true);
            // Open communications link (network traffic occurs here).
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode);
            }
            // Retrieve the response body as an InputStream.
            stream = connection.getInputStream();
            if (stream != null) {
                //parsowanie XML'a
                String wersja="";
                try {
                    XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
                    XmlPullParser myParser = xmlFactoryObject.newPullParser();
                    myParser.setInput(stream, null);
                    int event = myParser.getEventType();
                    while (event != XmlPullParser.END_DOCUMENT)  {
                        String name=myParser.getName();
                        switch (event){
                            case XmlPullParser.START_TAG:
                                break;

                            case XmlPullParser.END_TAG:
                                if(name.equals("wersja")){
                                    wersja = myParser.getAttributeValue(null,"value");
                                }
                                break;
                        }
                        event = myParser.next();
                    }
                } catch (XmlPullParserException e) {
                    MyApplication.writeLog(getString(R.string.CheckVersionService_XML_parse_error), getBaseContext(), 1);
                    wersja="";
                }

                try{
                    MyApplication.numer_dostepnej_wersji = Integer.parseInt(wersja);
                    MyApplication.writeLog(getString(R.string.CheckVersionService_FoundVersion)+" "+MyApplication.numer_dostepnej_wersji, getBaseContext(), 1);
                }
                catch (NumberFormatException exc) {
                    MyApplication.numer_dostepnej_wersji = 0;
                    MyApplication.writeLog(getString(R.string.CheckVersionService_FoundVersion)+" not found", getBaseContext(), 1);
                }
            }
            else {
                MyApplication.writeLog(getString(R.string.CheckVersionService_StreamNull), getBaseContext(), 1);
            }
        }
        catch (IOException ex) {
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            MyApplication.writeLog(getString(R.string.CheckVersionService_transmission_error)+ System.getProperty("line.separator")+errors.toString(),getBaseContext(),1);
        }
        finally {
            // Close Stream and disconnect HTTPS connection.
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    StringWriter errors = new StringWriter();
                    e.printStackTrace(new PrintWriter(errors));
                    MyApplication.writeLog(getString(R.string.CheckVersionService_StreamClose_error)+ System.getProperty("line.separator")+errors.toString(),getBaseContext(),1);
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        return true;
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

        // Timer jobs for CheckVersionServices
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
}
