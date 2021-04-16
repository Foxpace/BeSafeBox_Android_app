package motionapps.besafebox.activities.options.functional;

import android.content.Context;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;

import java.util.regex.Pattern;

import es.dmoral.toasty.Toasty;
import motionapps.besafebox.activities.options.preferences.PreferencesHolder;
import motionapps.besafebox.R;

public class ContactCheckers {


    /**
     * @param context - any
     * @param nameText - string with name of user
     * @return if is valid
     */
    public static boolean isValidName(Context context, String nameText) {
        Pattern p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
        return !p.matcher(nameText).find() && nameText.length() >= 3 && nameText.length() <= 10 &&
                !context.getString(R.string.example_name).equals(nameText);
    }


    /**
     * @param context - any
     * @param toCheck - string with name of user
     * @return if is valid
     */
    public static boolean isValidPhone(Context context, String toCheck){
        return PhoneNumberUtils.isGlobalPhoneNumber(toCheck.replace(" ", "")) &&
                toCheck.length() > 7 &&
                !context.getString(R.string.example_number).equals(toCheck);
    }

    /**
     * @param context - any
     * @param mail - string
     * @return if is valid
     */
    public static boolean isValidEmail(Context context, String mail) {
        return (!TextUtils.isEmpty(mail) && Patterns.EMAIL_ADDRESS.matcher(mail).matches()) &&
                mail.length() >= 6 &&
                !context.getString(R.string.example_mail).equals(mail);
    }

    /**
     * Checks validity of the data to store
     * @param context - Android activity
     * @param name - name of the contact
     * @param mail - mail of the contact
     * @param number - number of the contact
     * @return boolean if all data are valid
     */
    public static boolean checkNameMailNumber(Context context, String name, String mail, String number) {
        return isValidName(context, name) &&
                isValidPhone(context, number) &&
                isValidEmail(context, mail);
    }

    /**
     * Checks validity of the data to store
     * @param context - Android activity
     * @param name - name of the contact
     * @param mail - mail of the contact
     * @return boolean if all data are valid
     */
    public static boolean checkNameMail(Context context, String name, String mail) {
        return isValidName(context, name) &&
                isValidEmail(context, mail);
    }

    /**
     * Checks validity of the data to store
     * @param context - Android activity
     * @param name - name of the contact
     * @param number - number of the contact
     * @return boolean if all data are valid
     */
    public static boolean checkNameNumber(Context context, String name, String number) {
        return isValidName(context, name) &&
                isValidPhone(context, number);
    }

    /**
     * Shows toast if some input is invalid
     * @param context - Android activity
     * @param name - name of the contact
     * @param mail - mail of the contact
     * @param number - number of the contact
     */
    public static void showToast(Context context, String name, String mail, String number){
        if (!isValidName(context, name)) {
            Toasty.error(context, R.string.name_invalid, Toast.LENGTH_SHORT, true).show();
        } else if (!isValidEmail(context, mail)) {
            Toasty.error(context, R.string.mail_invalid, Toast.LENGTH_SHORT, true).show();
        } else if(isValidPhone(context, number)){
            Toasty.error(context, R.string.phone_invalid, Toast.LENGTH_SHORT, true).show();
        }
    }

    /**
     * Shows toast if some input is invalid
     * @param context - Android activity
     * @param name - name of the contact
     * @param mail - mail of the contact
     */
    public static boolean showToast(Context context, String name, String mail){
        if (!isValidName(context, name)) {
            Toasty.error(context, R.string.name_invalid, Toast.LENGTH_SHORT, true).show();
            return true;
        } else if (!isValidEmail(context, mail)) {
            Toasty.error(context, R.string.mail_invalid, Toast.LENGTH_SHORT, true).show();
            return true;
        }
        return false;
    }

    public static boolean isEmpty(String name, String mail, String number){
        return name.equals("") && mail.equals("") && number.equals("");
    }

    /**
     * reset of the contacts
     * @param context - Android context
     */
    public static void cleanContacts(Context context) {
        PreferencesHolder.onChange(context, PreferencesHolder.NAME, "");
        PreferencesHolder.onChange(context, PreferencesHolder.NUMBER, "");
        PreferencesHolder.onChange(context, PreferencesHolder.MAIL, "");
    }


    public static final int NO_CONTACTS = 0, NAME_SMS = 1, NAME_MAIL = 2, ALL_CONTACTS = 3;

    public static int getContactCompletion(Context context){
        String name = PreferencesHolder.getString(context, PreferencesHolder.NAME, context.getString(R.string.example_name));
        String mail = PreferencesHolder.getString(context, PreferencesHolder.MAIL, context.getString(R.string.example_mail));
        String number = PreferencesHolder.getString(context, PreferencesHolder.NUMBER, context.getString(R.string.example_number));

        if(checkNameMailNumber(context, name, mail, number)) return ALL_CONTACTS;
        if(checkNameMail(context, name, mail)) return NAME_MAIL;
        if(checkNameNumber(context, name, number)) return NAME_SMS;

        return NO_CONTACTS;

    }
}
