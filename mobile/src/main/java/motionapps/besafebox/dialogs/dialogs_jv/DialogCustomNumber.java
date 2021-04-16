package motionapps.besafebox.dialogs.dialogs_jv;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import com.hbb20.CountryCodePicker;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import es.dmoral.toasty.Toasty;
import motionapps.besafebox.R;
import motionapps.besafebox.activities.options.preferences.PreferencesHolder;

/**
 * custom dialog to show ccp view for number prefixes with number validation
 * if the input is incorrect, the toast is shown
 */

public class DialogCustomNumber  extends Dialog {

    private final Preference preference;
    private CountryCodePicker ccp;


    public DialogCustomNumber(@NonNull Context context, Preference preference) {
        super(context);
        this.preference = preference;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setContentView(R.layout.activity_custom_number);

        // changing colors of the dialog - dialog is shown on top of dialog
        ccp = findViewById(R.id.introduction_number); // binding to lib
        ccp.registerCarrierNumberEditText(findViewById(R.id.custom_phone_edit));
        ccp.setDialogBackgroundColor(Color.parseColor("#333333"));
        ccp.setDialogTextColor(Color.parseColor("#FFFFFF"));
        ccp.setDialogSearchEditTextTintColor(Color.parseColor("#000000"));
        ccp.setFastScrollerBubbleColor(Color.parseColor("#000000"));
        ccp.setFastScrollerHandleColor(Color.parseColor("#FFFFFF"));


        Button button = findViewById(R.id.save_custon_number);
        button.setOnClickListener(v -> processClick());
    }

    private void processClick() {

        if(ccp.isValidFullNumber()){ // lib controls input by nationality picked
            preference.setSummary(ccp.getFullNumberWithPlus());
            PreferencesHolder.onChange(getContext(), preference.getKey(), ccp.getFullNumberWithPlus());
            dismiss();

        }else{
            Toasty.error(getContext(), R.string.phone_invalid, Toast.LENGTH_SHORT, true).show();
        }
    }
}
