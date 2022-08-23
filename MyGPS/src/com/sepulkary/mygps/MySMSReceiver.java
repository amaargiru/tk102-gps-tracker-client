package com.sepulkary.mygps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MySMSReceiver extends BroadcastReceiver {
    static double EPSILON = 0.00000001;

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


    @Override
    public void onReceive(Context context, Intent intent){
        Object[] pdus=(Object[])intent.getExtras().get("pdus");
        SmsMessage shortMessage= SmsMessage.createFromPdu((byte[]) pdus[0]);

        String sMessageSender = shortMessage.getOriginatingAddress();
        String sMessagetext = shortMessage.getDisplayMessageBody();

        Log.d("TrackerApp", "SMS message sender: " + sMessageSender);
        Log.d("TrackerApp", "SMS message text: " + sMessagetext);

        ArrayList<String> floatsFromSMS = parseIntsAndFloats(sMessagetext);
        String[] stockArr = new String[2];
        stockArr = floatsFromSMS.toArray(stockArr);

        float lat = 0;
        float lon = 0;

        try {
            lat = Float.parseFloat(stockArr[0]);
            lon = Float.parseFloat(stockArr[1]);
        }
        catch(Exception ex) {
        }

        Log.d("TrackerApp", "lat: " + Float.toString(lat));
        Log.d("TrackerApp", "lon: " + Float.toString(lon));

        // Пришли обе координаты, обе в диапазоне + сообщение длинное, не служебное (не "t021s001n ok!") и в теле сообщения есть служебные слова
        if ((Math.abs(lat) > EPSILON) && (Math.abs(lon) > EPSILON) && lat >= -180 && lat <= 180 && lon >= -90 && lon <= 90 && sMessagetext.length()>=25 && sMessagetext.contains("lat:") && sMessagetext.contains("long:") && sMessagetext.contains("speed:"))
        {
            Intent intent2open = new Intent(context, MainActivity.class);
            intent2open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent2open.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            String name = "SMS sender";
            String value = shortMessage.getOriginatingAddress();
            intent2open.putExtra(name, value);

            name = "SMS text";
            value = shortMessage.getDisplayMessageBody();
            intent2open.putExtra(name, value);

            context.startActivity(intent2open);
        }
    }
}
