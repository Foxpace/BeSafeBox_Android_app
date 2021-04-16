package motionapps.besafebox.activities.options;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.afollestad.materialdialogs.MaterialDialog;

import es.dmoral.toasty.Toasty;
import motionapps.besafebox.activities.Alarm;
import motionapps.besafebox.activities.main.Main;
import motionapps.besafebox.activities.options.functional.ContactCheckers;
import motionapps.besafebox.activities.options.functional.PeripheryTest;
import motionapps.besafebox.activities.options.preferences.PreferencesHolder;
import motionapps.besafebox.dialogs.dialogs_jv.DialogCustomNumber;
import motionapps.besafebox.R;
import motionapps.besafebox.dialogs.dialogs_kt.DialogEditTextKt;
import motionapps.besafebox.notifications.Notify;
import motionapps.besafebox.tools.PermissionHandler;

import static motionapps.besafebox.activities.Alarm.REAL_ALARM;

/**
 * Main settings fragment to show all the SharedPreferences, which can user modify
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.settings);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sendBroadcast(new Intent(Main.BROADCAST_RESTART));
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        MaterialDialog materialDialog;
        Handler handler;
        Dialog dialog;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            setUpContacts();
            setUpTests();
            setUpAlarm();
        }


        /**
         * App needs to be able to control sound and notifications, so it can override silence mode in phone
         */
        private void setUpAlarm() {
            Preference preference = findPreference(PreferencesHolder.ALARM_ME);
            preference.setOnPreferenceChangeListener((preference1, newValue) -> Notify.enableNotifications(getContext()));

            preference = findPreference(PreferencesHolder.ALARM_ASSIST);
            preference.setOnPreferenceChangeListener((preference1, newValue) -> {

                String name = PreferencesHolder.getString(getContext(), PreferencesHolder.NAME, getString(R.string.example_name));
                String mail = PreferencesHolder.getString(getContext(), PreferencesHolder.MAIL, getString(R.string.example_mail));
                String number = PreferencesHolder.getString(getContext(), PreferencesHolder.NUMBER, getString(R.string.example_number));
//                //renew to use with mail
//                if(ContactCheckers.checkNameMailNumber(getContext(), name, mail, number)) return Notify.enableNotifications(getContext());
//                if(ContactCheckers.checkNameMail(getContext(), name, mail)) return Notify.enableNotifications(getContext());
                if(ContactCheckers.checkNameNumber(getContext(), name, number)) return Notify.enableNotifications(getContext());

                Toasty.error(getContext(), R.string.settings_empty_contacts, Toasty.LENGTH_LONG, true).show();

                return false;
            });
        }

        /**
         * sets up buttons to show dialogs to change name, number or mail - dialogs handle storing info and summary
         */
        private void setUpContacts() {

            // name of the user
            Preference preference = findPreference(PreferencesHolder.NAME);
            preference.setSummary(PreferencesHolder.getStringNone(requireContext(), PreferencesHolder.NAME, getString(R.string.none)));
            preference.setOnPreferenceClickListener(p -> {
                materialDialog = DialogEditTextKt.editTextDialog(requireContext(), p, getString(R.string.example_name),
                        text -> ContactCheckers.isValidName(requireContext(), text), R.string.add_user);
                return true;
            });

            // mail of the user's contact
            preference = findPreference(PreferencesHolder.MAIL);
            preference.setSummary(PreferencesHolder.getStringNone(requireContext(), PreferencesHolder.MAIL, getString(R.string.none)));
            preference.setOnPreferenceClickListener(p -> {
                materialDialog = DialogEditTextKt.editTextDialog(requireContext(), p, getString(R.string.example_mail),
                        text -> ContactCheckers.isValidEmail(requireContext(), text), R.string.add_mail);
                return true;
            });

            // phone of the user's contact
            preference = findPreference(PreferencesHolder.NUMBER);
            preference.setSummary(PreferencesHolder.getStringNone(requireContext(), PreferencesHolder.NUMBER, getString(R.string.none)));
            Preference finalPreference = preference;
            preference.setOnPreferenceClickListener(p -> {
                dialog = new DialogCustomNumber(getContext(), finalPreference);
                dialog.show();
                return true;
            });

            // disable message below
            preference = findPreference(getString(R.string.settings_contact_warning));
            preference.setEnabled(false);

        }

        /**
         * buttons to send SMS / mail and start example alarm
         */
        private void setUpTests(){

            // send SMS - wait 1s to re-enable the button with handler
            Preference preference = findPreference(getString(R.string.settings_test_sms));
            preference.setOnPreferenceClickListener(p -> {

                if(!PermissionHandler.checkSMS(getContext(), true)) return false;
                if(!PermissionHandler.checkLocation(getContext(), true)) return false;

                String number = PreferencesHolder.getString(requireContext(), PreferencesHolder.NUMBER, "");
                if(ContactCheckers.isValidPhone(requireContext(), number)){
                    p.setEnabled(false);
                    new PeripheryTest().tryToSendMessage(requireContext(), PeripheryTest.SMS_BUTTON);
                    if(handler == null){
                        handler = new Handler(Looper.getMainLooper());
                    }
                    handler.postDelayed(() -> p.setEnabled(true), 1000L);
                }else{
                    Toasty.error(requireContext(), R.string.settings_error_sms, Toasty.LENGTH_SHORT, true).show();
                }

                return true;
            });

            // send mail via MailDroid
            preference = findPreference(getString(R.string.settings_test_mail));
            preference.setOnPreferenceClickListener(p -> {
                if(!PermissionHandler.checkLocation(getContext(), true)) return false;
                String number = PreferencesHolder.getString(requireContext(), PreferencesHolder.MAIL, "");
                if(ContactCheckers.isValidEmail(requireContext(), number)){
                    new PeripheryTest().tryToSendMessage(requireContext(), PeripheryTest.MAIL_BUTTON);
                }else{
                    Toasty.error(requireContext(), R.string.settings_error_mail, Toasty.LENGTH_SHORT, true).show();
                }
                return true;
            });

            // start activity with test alarm
            preference = findPreference(getString(R.string.settings_test_alarm));
            preference.setOnPreferenceClickListener(p -> {
                Intent intent = new Intent(requireContext(), Alarm.class);
                intent.putExtra(REAL_ALARM, false);
                startActivity(intent);
                return true;
            });
        }


        @Override
        public void onDestroy() {
            super.onDestroy();
            if(materialDialog != null){
                materialDialog.dismiss();
                materialDialog = null;
            }

            if(dialog != null){
                dialog.dismiss();
                dialog = null;
            }

            if(handler != null){
                handler.removeCallbacksAndMessages(null);
                handler = null;
            }


        }
    }

    @Override
    public void onBackPressed() {
        finish();
        startActivity(new Intent(this, Main.class));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            startActivity(new Intent(this, Main.class));
        }
        return true;
    }
}