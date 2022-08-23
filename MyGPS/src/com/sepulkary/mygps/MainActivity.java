package com.sepulkary.mygps;

import android.app.*;
import android.content.*;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private String sMessageSender;
    private String sMessagetext;

    static Context context;
    public static final String PREFS_NAME = "MyPrefsFile";

    String sTarget1Number = "";
    String sTarget1Pass = "";
    String sTarget2Number = "";
    String sTarget2Pass = "";

    // Индикатор состояния программы
    final static int STATE_BASE = 0; // Ничего особо не делаем, отображаем последние полученные координаты
    final static int STATE_REQUEST = 1; // Послан запрос на трекер, но ответ еще не получен

    private MapView mapView; // Вывод карты с местоположением пользователя
    private ImageButton settingsButton; // Настройки
    private ImageButton reloadButton1; // Получить свежие координаты трекера 1
    private ImageButton reloadButton2; // Получить свежие координаты трекера 2

    static final String TAG = "TrackerApp";

    static double EPSILON = 0.00001;

    MyItemizedOverlay myItemizedOverlay1 = null; // Цель 1
    MyItemizedOverlay myItemizedOverlay2 = null; // Цель 2
    private static int DEFAULT_ZOOM = 16; // Увеличение карты OpenStreetMap по умолчанию
    float initial1Lat = 0.0f;
    float initial1Long = 0.0f;
    float initial2Lat = 0.0f;
    float initial2Long = 0.0f;

    private void Messaging() {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
        r.play();

        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator.hasVibrator()) {
            long pattern[] = {0, 300, 200, 300, 50, 50, 50, 50, 50, 50, 50, 50};
            vibrator.vibrate(pattern, -1);
        }
    }

    // http://stackoverflow.com/questions/12234963/java-searching-float-number-in-string
    public ArrayList<String> parseIntsAndFloats(String raw) {
        ArrayList<String> listBuffer = new ArrayList<String>();
        Pattern p = Pattern.compile("[-]?[0-9]*\\.?[0-9]+");
        Matcher m = p.matcher(raw);
        while (m.find()) {
            listBuffer.add(m.group());
        }
        return listBuffer;
    }

    private void sendSMS(String phoneNumber, String message) {
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, null, null);
    }

    void SaveSettings() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString("sTarget1Number", sTarget1Number);
        editor.putString("sTarget1Pass", sTarget1Pass);
        editor.putString("sTarget2Number", sTarget2Number);
        editor.putString("sTarget2Pass", sTarget2Pass);
        editor.putFloat("initial1Lat", initial1Lat);
        editor.putFloat("initial1Long", initial1Long);
        editor.putFloat("initial2Lat", initial2Lat);
        editor.putFloat("initial2Long", initial2Long);

        editor.commit();
    }

    void RestoreSettings() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        sTarget1Number = settings.getString("sTarget1Number", "");
        sTarget1Pass = settings.getString("sTarget1Pass", "");
        sTarget2Number = settings.getString("sTarget2Number", "");
        sTarget2Pass = settings.getString("sTarget2Pass", "");
        initial1Lat = settings.getFloat("initial1Lat", 0);
        initial1Long = settings.getFloat("initial1Long", 0);
        initial2Lat = settings.getFloat("initial2Lat", 0);
        initial2Long = settings.getFloat("initial2Long", 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;

        requestWindowFeature(Window.FEATURE_NO_TITLE); // Приложение без заголовка
        setContentView(R.layout.activity_main); // Выбираем UI

        mapView = (MapView) findViewById(R.id.mapview); // Вывод карты с местоположением пользователя
        settingsButton = (ImageButton) findViewById(R.id.settingsButton); // Кнопка "Настройки"
        reloadButton1 = (ImageButton) findViewById(R.id.reloadButton1);
        reloadButton2 = (ImageButton) findViewById(R.id.reloadButton2);

        RestoreSettings();

        mapView.getController().setZoom(DEFAULT_ZOOM);
        mapView.setMultiTouchControls(true);

        Handler handler = new Handler();    // Без задержки карта центрируется некорректно
        handler.postDelayed(new Runnable() {
            public void run() {
                if ((initial1Lat > EPSILON) || (initial1Long > EPSILON))    // Рисуем последнее известное местоположение трекера 1
                {
                    mapView.getOverlays().remove(myItemizedOverlay1);

                    Drawable target1Marker = getResources().getDrawable(R.drawable.map_marker_target1);
                    int target1MarkerWidth = target1Marker.getIntrinsicWidth();
                    int target1MarkerHeight = target1Marker.getIntrinsicHeight();
                    target1Marker.setBounds(0, target1MarkerHeight, target1MarkerWidth, 0);

                    org.osmdroid.ResourceProxy resourceProxy = new DefaultResourceProxyImpl(getApplicationContext());
                    myItemizedOverlay1 = new MyItemizedOverlay(target1Marker, resourceProxy);
                    mapView.getOverlays().add(myItemizedOverlay1);

                    GeoPoint target1Point = new GeoPoint((int) (initial1Lat * 1000000), (int) (initial1Long * 1000000)); // Центрируем карту по местоположению объекта и выводим маркер
                    mapView.getController().animateTo(target1Point);
                    myItemizedOverlay1.addItem(target1Point, "target1Point", "target1Point");
                }

                if ((initial2Lat > EPSILON) || (initial2Long > EPSILON))    // Рисуем последнее известное местоположение трекера 1
                {
                    mapView.getOverlays().remove(myItemizedOverlay2);

                    Drawable target2Marker = getResources().getDrawable(R.drawable.map_marker_target2);
                    int target2MarkerWidth = target2Marker.getIntrinsicWidth();
                    int target2MarkerHeight = target2Marker.getIntrinsicHeight();
                    target2Marker.setBounds(0, target2MarkerHeight, target2MarkerWidth, 0);

                    org.osmdroid.ResourceProxy resourceProxy = new DefaultResourceProxyImpl(getApplicationContext());
                    myItemizedOverlay2 = new MyItemizedOverlay(target2Marker, resourceProxy);
                    mapView.getOverlays().add(myItemizedOverlay2);

                    GeoPoint target2Point = new GeoPoint((int) (initial2Lat * 1000000), (int) (initial2Long * 1000000)); // Центрируем карту по местоположению объекта и выводим маркер
                    mapView.getController().animateTo(target2Point);
                    myItemizedOverlay2.addItem(target2Point, "target2Point", "target2Point");
                }
            }
        }, 1000);

        settingsButton.setOnClickListener(new Button.OnClickListener() { // Обработчик кнопки "Настройки"
            @Override
            public void onClick(View arg0) {
                Animation animAlpha = AnimationUtils.loadAnimation(MainActivity.this, R.anim.anim_alpha);
                arg0.startAnimation(animAlpha);

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        Intent intent = new Intent(MainActivity.this, UsersActivity.class);
                        intent.putExtra("sTarget1Number", sTarget1Number);
                        intent.putExtra("sTarget1Pass", sTarget1Pass);
                        intent.putExtra("sTarget2Number", sTarget2Number);
                        intent.putExtra("sTarget2Pass", sTarget2Pass);
                        startActivityForResult(intent, 0);
                    }
                }, 350);
            }
        });

        reloadButton1.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Animation animAlpha = AnimationUtils.loadAnimation(MainActivity.this, R.anim.anim_alpha);
                arg0.startAnimation(animAlpha);

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        if (sTarget1Number.isEmpty())
                            Toast.makeText(MainActivity.this, "Введите телефонный номер трекера 1", Toast.LENGTH_LONG).show();
                        else if (sTarget1Pass.isEmpty())
                            Toast.makeText(MainActivity.this, "Введите пароль для трекера 1", Toast.LENGTH_LONG).show();
                        else {// Можно начинать работать
                            sendSMS(sTarget1Number, "t021s001n" + sTarget1Pass);
                        }
                    }
                }, 350);
            }
        });

        reloadButton2.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Animation animAlpha = AnimationUtils.loadAnimation(MainActivity.this, R.anim.anim_alpha);
                arg0.startAnimation(animAlpha);

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        if (sTarget2Number.isEmpty())
                            Toast.makeText(MainActivity.this, "Введите телефонный номер трекера 2", Toast.LENGTH_LONG).show();
                        else if (sTarget2Pass.isEmpty())
                            Toast.makeText(MainActivity.this, "Введите пароль для трекера 2", Toast.LENGTH_LONG).show();
                        else {// Можно начинать работать
                            sendSMS(sTarget2Number, "t021s001n" + sTarget2Pass);
                        }
                    }
                }, 350);
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        sMessageSender = intent.getStringExtra("SMS sender");
        sMessagetext = intent.getStringExtra("SMS text");

        Log.d("SMSReceiver", "SMS message sender main: " + sMessageSender);
        Log.d("SMSReceiver", "SMS message text main: " + sMessagetext);

        ArrayList<String> floatsFromSMS = parseIntsAndFloats(sMessagetext);
        String[] stockArr = new String[2];
        stockArr = floatsFromSMS.toArray(stockArr);
        float lat = Float.parseFloat(stockArr[0]);      // Получили координаты точки
        float lon = Float.parseFloat(stockArr[1]);

        Log.d("TrackerApp", "latmain: " + Float.toString(lat));
        Log.d("TrackerApp", "lonmain: " + Float.toString(lon));

        if ((sMessageSender.length() > 10) && (sTarget1Number.length() > 10))
            if (sMessageSender.substring(sMessageSender.length() - 10).equals(sTarget1Number.substring(sTarget1Number.length() - 10))) // Сравниваем только последние 10 цифр номеров, чтобы избежать путаницы с +7... и 8...
            {   // Сообщение пришло от трекера 1
                mapView.getOverlays().remove(myItemizedOverlay1);

                Drawable target1Marker = getResources().getDrawable(R.drawable.map_marker_target1);
                int target1MarkerWidth = target1Marker.getIntrinsicWidth();
                int target1MarkerHeight = target1Marker.getIntrinsicHeight();
                target1Marker.setBounds(0, target1MarkerHeight, target1MarkerWidth, 0);

                org.osmdroid.ResourceProxy resourceProxy = new DefaultResourceProxyImpl(getApplicationContext());
                myItemizedOverlay1 = new MyItemizedOverlay(target1Marker, resourceProxy);
                mapView.getOverlays().add(myItemizedOverlay1);

                GeoPoint target1Point = new GeoPoint((int) (lat * 1000000), (int) (lon * 1000000)); // Центрируем карту по местоположению объекта и выводим маркер
                mapView.getController().animateTo(target1Point);
                myItemizedOverlay1.addItem(target1Point, "target1Point", "target1Point");

                initial1Lat = lat;
                initial1Long = lon;

                Messaging();
            }

        if ((sMessageSender.length() > 10) && (sTarget2Number.length() > 10))
            if (sMessageSender.substring(sMessageSender.length() - 10).equals(sTarget2Number.substring(sTarget2Number.length() - 10))) // Сравниваем только последние 10 цифр номеров, чтобы избежать путаницы с +7... и 8...
            {   // Сообщение пришло от трекера 2
                mapView.getOverlays().remove(myItemizedOverlay2);

                Drawable target2Marker = getResources().getDrawable(R.drawable.map_marker_target2);
                int target2MarkerWidth = target2Marker.getIntrinsicWidth();
                int target2MarkerHeight = target2Marker.getIntrinsicHeight();
                target2Marker.setBounds(0, target2MarkerHeight, target2MarkerWidth, 0);

                org.osmdroid.ResourceProxy resourceProxy = new DefaultResourceProxyImpl(getApplicationContext());
                myItemizedOverlay2 = new MyItemizedOverlay(target2Marker, resourceProxy);
                mapView.getOverlays().add(myItemizedOverlay2);

                GeoPoint target2Point = new GeoPoint((int) (lat * 1000000), (int) (lon * 1000000)); // Центрируем карту по местоположению объекта и выводим маркер
                mapView.getController().animateTo(target2Point);
                myItemizedOverlay2.addItem(target2Point, "target2Point", "target2Point");

                initial2Lat = lat;
                initial2Long = lon;

                Messaging();
            }

        super.onNewIntent(intent);
    } // End of onNewIntent(Intent intent)

    @Override
    public void onPause() {
        super.onPause();

        SaveSettings();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {
            return;
        }
        sTarget1Number = data.getStringExtra("sTarget1Number");
        sTarget1Pass = data.getStringExtra("sTarget1Pass");
        sTarget2Number = data.getStringExtra("sTarget2Number");
        sTarget2Pass = data.getStringExtra("sTarget2Pass");
    }
}
