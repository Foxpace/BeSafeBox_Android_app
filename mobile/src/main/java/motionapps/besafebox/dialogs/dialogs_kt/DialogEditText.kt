package motionapps.besafebox.dialogs.dialogs_kt

import android.content.Context
import androidx.preference.Preference
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import motionapps.besafebox.activities.options.preferences.PreferencesHolder
import motionapps.besafebox.R


/**
 * Interface, which is used to check output of EditText view
 */
interface TextValidator{
    fun isValid(text: String): Boolean
}

/**
 * function to create EditTextDialog, which handles preference - storage and summary
 *
 * @param context - activity / context
 * @param preference - to handle from SharedPreferences settings
 * @param hint - to show in EditText - "" to ommit
 * @param validator - interface to handle, if the input is correct - custom implementation
 * @param title - of the dialog
 */
fun editTextDialog(context: Context, preference: Preference, hint: String, validator: TextValidator, title: Int) =
        MaterialDialog(context).show {
            // check value, if it exists -> prefill
        val oldValue = PreferencesHolder.getString(context, preference.key, "")
        if(oldValue != ""){
            input(prefill = oldValue, hint = hint, waitForPositiveButton = false) { dialog, text ->
                val isValid = validator.isValid(text.toString())
                dialog.setActionButtonEnabled(WhichButton.POSITIVE, isValid)
            }
        }else{
            input(hint = hint, waitForPositiveButton = false) { dialog, text ->
                val isValid = validator.isValid(text.toString())
                dialog.setActionButtonEnabled(WhichButton.POSITIVE, isValid)
            }
        }

        title(title)
        cornerRadius(16f)
        positiveButton(R.string.ok){
            val value = it.getInputField().text.toString()
            if(validator.isValid(value)){
                PreferencesHolder.onChange(context, preference.key, value)
                preference.summary = value
                dismiss()
            }
        }
        negativeButton(R.string.cancel){ dismiss() }

    }