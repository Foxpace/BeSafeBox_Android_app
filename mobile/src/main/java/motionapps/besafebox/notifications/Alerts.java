package motionapps.besafebox.notifications;

import android.content.Context;
import android.content.res.Resources;
import android.telephony.SmsManager;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import es.dmoral.toasty.Toasty;
import motionapps.besafebox.R;
import motionapps.besafebox.activities.options.functional.ContactCheckers;
import motionapps.besafebox.activities.options.preferences.PreferencesHolder;
import motionapps.besafebox.tools.InternetTools;
import motionapps.besafebox.tools.PermissionHandler;

import static motionapps.besafebox.activities.options.preferences.PreferencesHolder.NAME;
import static motionapps.besafebox.activities.options.preferences.PreferencesHolder.NUMBER;

public class Alerts {

    public final static int FALL = 0, CAR = 1;

    /**
     * @param context any
     * @param phoneNumber - String with form "+421xxxxxxxxx"
     * @param message - to send
     * @param showToast - boolean, if the toast is required as feedback
     */
    public static void sendSMS(Context context, String phoneNumber, String message, boolean showToast) {
        if(!PermissionHandler.checkSMS(context, false)) return;
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, null, null);
        if (showToast) {
            Toasty.success(context, context.getString(R.string.settings_sms_sent_test), Toast.LENGTH_SHORT, true).show();
        }
    }

    /**
     * @param context any
     * @param recipient - its mail in form "recipient@mail.com"
     * @param body - text for the mail
     * @param showToast - show toast as feedback
     */
    public static void sendMail(Context context, String recipient, String body, boolean showToast) {

        if(!InternetTools.isConnected()){
            Toasty.error(context, context.getString(R.string.alarm_no_internet) , Toasty.LENGTH_LONG, true).show();
            return;
        }

        if(showToast){
            Toasty.warning(context, "Mail service discontinued", Toasty.LENGTH_LONG, true).show();
        }


//        new MaildroidX.Builder()
//                .smtp(MailLogins.smtp)
//                .smtpUsername(MailLogins.mail)
//                .smtpPassword(MailLogins.password)
//                .port(MailLogins.port)
//                .type(MaildroidXType.HTML)
//                .to(recipient)
//                .from(MailLogins.mail)
//                .subject(context.getString(R.string.detection))
//                .body(body)
//                .onCompleteCallback(new MaildroidX.onCompleteCallback() {
//                    @Override
//                    public void onSuccess() {
//                        if (showToast) {
//                            Toasty.success(context, context.getString(R.string.settings_mail_sent_test), Toast.LENGTH_SHORT, true).show();
//                        }
//                    }
//
//                    @Override
//                    public void onFail(@NotNull String s) {
//                        if (showToast) {
//                            Toasty.error(context, context.getString(R.string.alarm_mail_failed) , Toasty.LENGTH_LONG, true).show();
//                        }
//                    }
//
//                    @Override
//                    public long getTimeout() {
//                        return 3000;
//                    }
//                }).mail();
//
    }

    /**
     * message consists URL to google maps with marker to location and nickname, which was added at the endingIndex
     * @param resources - so the method can access strings
     * @param latLng - location of the person
     * @param nickname - name of the person
     * @param type - FALL / CAR accident
     */
    public static String generateSMS(Resources resources, LatLng latLng, String nickname, int type) {

        String text;

        switch (type) {
            case FALL:
                text = resources.getString(R.string.falldetection);
                break;
            case CAR:
                text = resources.getString(R.string.cardetection);
                break;
            default:
                text = "Something happened";
                break;
        }

        if (latLng == null) {
            return text + " " + nickname;
        }
        return text + " " + resources.getString(R.string.googleMaps) +
                latLng.latitude + "," + latLng.longitude + "\n\n" + nickname;
    }


    /**
     * method, which combines all the methods to send alert to recipient
     * @param context - any
     * @param latLng - location of the person
     * @param type - type of the alarm
     */
    public static void sendAlert(Context context, LatLng latLng, int type) {


        String number = PreferencesHolder.getString(context, NUMBER, "");
        if (ContactCheckers.isValidPhone(context, number)) {
            sendSMS(context, number, generateSMS(context.getResources(),
                    latLng, PreferencesHolder.getString(context, NAME, "Unknown"), type), false);
        }

//        if(InternetTools.isConnected()) {
//            String mail = PreferencesHolder.getString(context, MAIL, "");
//            if (ContactCheckers.isValidEmail(context, mail)) {
//                sendMail(context, generateSMS(context.getResources(), latLng,
//                        PreferencesHolder.getString(context, NAME, "Unknown"), type), mail, false);
//            }
//        }
    }
}
