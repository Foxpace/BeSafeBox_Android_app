package motionapps.besafebox.activities.introduction;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.github.appintro.SlidePolicy;
import com.hbb20.CountryCodePicker;

import es.dmoral.toasty.Toasty;
import motionapps.besafebox.activities.options.functional.ContactCheckers;
import motionapps.besafebox.activities.options.preferences.PreferencesHolder;
import motionapps.besafebox.R;


public class ContactFragment extends Fragment implements SlidePolicy {

    public ContactFragment() {}

    private EditText name, mail, phone;
    private CountryCodePicker ccp;

    public static ContactFragment newInstance() {
        return new ContactFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // getting reference to all edittexts, phone is binded to country code picker
        View view = inflater.inflate(R.layout.fragment_contact, container, false);
        name = view.findViewById(R.id.introduction_name);
        mail = view.findViewById(R.id.introduction_mail);
        phone = view.findViewById(R.id.introduction_phone);
        ccp = view.findViewById(R.id.introduction_country_code);
        ccp.registerCarrierNumberEditText(phone);
        return view;
    }

    /**
     * @return true, if the name and mail or phone is valid
     */
    @Override
    public boolean isPolicyRespected() {

        String name = this.name.getText().toString();
        String mail = this.mail.getText().toString();
        String phone = this.phone.getText().toString();

        if(ContactCheckers.isEmpty(name, mail, phone)){
            ContactCheckers.cleanContacts(getContext());
            return true;
        }

        if(ContactCheckers.checkNameMail(getContext(), name, mail) && ccp.isValidFullNumber()){
            saveContacts();
            return true;
        }else{
            // if one is invalid, it is ok, but toast will raise
            if(ContactCheckers.isValidName(getContext(), name) &&
                    (ContactCheckers.isValidEmail(getContext(), mail)
                            || ccp.isValidFullNumber())){
                saveContacts();
                return true;
            }
        }

        return false;
    }

    /**
     * save data to Preferences
     */
    private void saveContacts(){

        String name = this.name.getText().toString();
        String mail = this.mail.getText().toString();

        PreferencesHolder.onChange(requireContext(), PreferencesHolder.NAME, name);
        if(ContactCheckers.isValidEmail(requireContext(), mail)){
            PreferencesHolder.onChange(requireContext(), PreferencesHolder.MAIL, mail);
        }else{
            PreferencesHolder.onChange(requireContext(), PreferencesHolder.MAIL, "");
        }

        if(ccp.isValidFullNumber()){
            PreferencesHolder.onChange(requireContext(), PreferencesHolder.NUMBER, ccp.getFullNumberWithPlus());
        }else{
            PreferencesHolder.onChange(requireContext(), PreferencesHolder.NUMBER, "");
        }
    }

    /**
     * Show toast of the invalid input
     */
    @Override
    public void onUserIllegallyRequestedNextPage() {
        String name = this.name.getText().toString();
        String mail = this.mail.getText().toString();
        if(!ContactCheckers.showToast(requireContext(), name, mail)){
            if(!ccp.isValidFullNumber()){
                Toasty.error(getContext(), R.string.phone_invalid, Toast.LENGTH_SHORT, true).show();
            }
        }
    }
}