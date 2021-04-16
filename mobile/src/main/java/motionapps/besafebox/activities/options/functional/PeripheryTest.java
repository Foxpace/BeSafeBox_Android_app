package motionapps.besafebox.activities.options.functional;

import android.content.Context;
import android.location.Location;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.lang.ref.WeakReference;

import motionapps.besafebox.activities.options.preferences.PreferencesHolder;
import motionapps.besafebox.gps.GPSCallback;
import motionapps.besafebox.notifications.Alerts;
import motionapps.besafebox.R;

import static motionapps.besafebox.activities.options.preferences.PreferencesHolder.MAIL;
import static motionapps.besafebox.activities.options.preferences.PreferencesHolder.NAME;
import static motionapps.besafebox.activities.options.preferences.PreferencesHolder.NUMBER;

/**
 * User can test to send sms and mail to its contact
 */
public class PeripheryTest implements GPSCallback.OnLastLocation {

    public static final int SMS_BUTTON = 1, MAIL_BUTTON = 2;

    private int action;
    private String contact = "";
    private String name;
    private WeakReference<Context> contextWeakReference;


    /**
     * call to send message
     * @param context any
     * @param action - 1 for SMS and 2 for mail
     */
    public void tryToSendMessage(Context context, int action){
        contextWeakReference = new WeakReference<>(context);
        name = PreferencesHolder.getString(contextWeakReference.get(), NAME, "Unknown");
        this.action = action;
        switch (action) {
            case SMS_BUTTON:
                contact = PreferencesHolder.getString(context, NUMBER, "");
                break;
            case MAIL_BUTTON:
                contact = PreferencesHolder.getString(context, MAIL, "");
                break;
        }

        if(contact.equals("")){
            Toast.makeText(context, R.string.options_error, Toast.LENGTH_SHORT).show();
        }else{
            GPSCallback.getLastLocation(context, this);
        }
    }

    @Override
    public void onLastLocation(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        String message = Alerts.generateSMS(contextWeakReference.get().getResources(), latLng, name, -1);
        switch (action) {
            case SMS_BUTTON:
                Alerts.sendSMS(contextWeakReference.get(), contact, message, true);
                break;
            case MAIL_BUTTON:
                Alerts.sendMail(contextWeakReference.get(), contact, message, true);
                break;
        }
    }
}
