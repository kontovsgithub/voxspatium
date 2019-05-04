package org.voxspatium;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.HashMap;
import java.util.Locale;

/**
 * Finally, Service in Android is a singleton. There is only one instance of each service in the system.
 * It starts on demand and handles all pending Intents / bound clients. Once it's done or explicitly stopped, it will be destroyed.
 * http://stackoverflow.com/questions/35024311/using-service-as-singleton-in-android
 */
public class SelectAndPlayService extends Service implements TextToSpeech.OnInitListener {

    String komunikat;
    String jezyk;
    boolean isTTSready;
    TextToSpeech mTTS;

    @Override
    public void onCreate() {
        super.onCreate();
        isTTSready=false;
        //utworzenie syntetyzatora
        mTTS = new TextToSpeech(getApplicationContext(), this);
        //utworzenie monitorowania syntezy przez obiekt mTTS_progress
        mTTS.setOnUtteranceProgressListener(new mTTS_progress());

        MyApplication.writeLog(getString(R.string.SelectAndPlayService_create), getBaseContext(),3);
    }

    /**
     * The system invokes this method by calling startService() when another component (such as an activity) requests that the service be started.
     * When this method executes, the service is started and can run in the background indefinitely.
     * If you implement this, it is your responsibility to stop the service when its work is complete by calling stopSelf() or stopService().
     * If you only want to provide binding, you don't need to implement this method.
     * <p/>
     * Notice that the onStartCommand() method must return an integer.
     * The integer is a value that describes how the system should continue the service in the event that the system kills it.
     * <p/>
     * If the system kills the service after onStartCommand() returns, recreate the service and call onStartCommand() with the last intent that was delivered to the service.
     * Any pending intents are delivered in turn.
     * This is suitable for services that are actively performing a job that should be immediately resumed, such as downloading a file.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        MyApplication.writeLog(getString(R.string.SelectAndPlayService_start)+" flags:" + String.valueOf(flags) + " startId:" + String.valueOf(startId), getBaseContext(),3);

        //odczyt co zagrać
        String MajorValue = intent.getStringExtra(MyApplication.PACKAGE_NAME+".Major");
        String MinorValue = intent.getStringExtra(MyApplication.PACKAGE_NAME+".Minor");
        String Kanal = intent.getStringExtra(MyApplication.PACKAGE_NAME+".Kanal");
        MyApplication.writeLog(getString(R.string.SelectAndPlayService_major_recesived) + MajorValue + getString(R.string.SelectAndPlayService_minor_recesived) + MinorValue + getString(R.string.SelectAndPlayService_kanal_recesived) + Kanal, getBaseContext(),4);

        //wyszukanie nagrania
        wyszukaj_nagranie(MajorValue, MinorValue, Kanal);

        //odtworzenie nagrania
        if (komunikat!=null && jezyk!=null) {
            if (isTTSready == true) {
                MyApplication.writeLog(getString(R.string.SelectAndPlayService_select_result) + " " + komunikat + "," + jezyk, getBaseContext(),5);
                // zmienna jezyk musi być specified IETF BCP 47 language tag string. https://tools.ietf.org/html/bcp47
                // kod language: shortest ISO 639 code: http://www-01.sil.org/iso639-3/codes.asp?order=639_1&letter=e
                // zainstalowanie biblioteki danego języka na smartfonie: http://mojaszuflada.pl/google-tts-mowi-po-polsku/
                // ewentualnie sprawdzanie czy dany jezyk syntetyzatora jest zainstalowany https://android-developers.googleblog.com/2009/09/introduction-to-text-to-speech-in.html
                Locale loc;
                loc = Locale.forLanguageTag(jezyk);
                mTTS.setLanguage(loc);
                HashMap<String, String> myHashStream = new HashMap<String, String>();
                myHashStream.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_NOTIFICATION));
                myHashStream.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, getString(R.string.app_name)+ " " + komunikat + " " + jezyk);
                mTTS.speak(komunikat, TextToSpeech.QUEUE_ADD, myHashStream);
            } else {
                MyApplication.writeLog(getString(R.string.SelectAndPlayService_waiting_for_player), getBaseContext(),1);
            }
        }

        return START_STICKY;
    }

    /**
     * The system invokes this method by calling bindService() when another component wants to bind with the service (such as to perform RPC).
     * In your implementation of this method, you must provide an interface that clients use to communicate with the service by returning an IBinder.
     * You must always implement this method; however, if you don't want to allow binding, you should return null.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * The system invokes this method when the service is no longer used and is being destroyed.
     * Your service should implement this to clean up any resources such as threads, registered listeners, or receivers.
     * This is the last call that the service receives.
     */
    @Override
    public void onDestroy() {
        if (mTTS != null) {
            mTTS.shutdown();
        }
        MyApplication.writeLog(getString(R.string.SelectAndPlayService_stop), getBaseContext(),3);
        super.onDestroy();
    }

    //obsługa bazy danych
    public void wyszukaj_nagranie(String Major, String Minor, String Kanal) {

        //kolumny frazy SELECT
        String[] projection = {"_ID","KOMUNIKAT","LANG"};
        //fraza WHERE
        String selection = "MAJOR = ? AND MINOR = ? AND KANAL = ?";
        String[] selectionArgs = { Major, Minor, Kanal };
        //fraza ORDER BY
        String OrderBy = "CREATED DESC";

        Cursor c = getContentResolver().query(DataBaseProvider.CONTENT_URI, projection, selection, selectionArgs, OrderBy);

        if (c.moveToFirst()) {
            MyApplication.writeLog(getString(R.string.SelectAndPlayService_select_result)+" row id "+c.getString(c.getColumnIndex("_ID")), getBaseContext(),4);
            komunikat= c.getString(c.getColumnIndex( "KOMUNIKAT"));
            jezyk= c.getString(c.getColumnIndex( "LANG"));
            c.close();
        }
        else {
            MyApplication.writeLog(getString(R.string.SelectAndPlayService_select_result)+" no rows", getBaseContext(),1);
            komunikat=null;
            jezyk=null;
            c.close();
        }
    }

    //ustawienie, że TTS jest zainicjalizowany
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
          isTTSready=true;
        }
    }

    //obsługa końca syntezy danej frazy
    public class mTTS_progress extends UtteranceProgressListener {

        @Override
        public void onDone(String utteranceId) {
            MyApplication.writeLog(getString(R.string.SelectAndPlayService_playing_finished), getBaseContext(),3);
        }

        @Override
        public void onStart(String utteranceId) {
            MyApplication.writeLog(getString(R.string.SelectAndPlayService_playing_started) + ": " + utteranceId, getBaseContext(),3);
        }

        @Override
        public void onError(String utteranceId) {
            MyApplication.writeLog(getString(R.string.SelectAndPlayService_player_error), getBaseContext(),1);
        }
    }
}
