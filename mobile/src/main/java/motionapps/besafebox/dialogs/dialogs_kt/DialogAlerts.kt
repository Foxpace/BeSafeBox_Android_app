package motionapps.besafebox.dialogs.dialogs_kt

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.afollestad.materialdialogs.MaterialDialog
import motionapps.besafebox.R
import motionapps.besafebox.activities.options.preferences.PreferencesHolder


/**
 * Creates dialog to turn on GPS, if it is absent
 *
 * @param context - any
 * @return MaterialDialog reference
 */
fun createNoGPSDialog(context: Context): MaterialDialog = MaterialDialog(context).show {
    title(R.string.no_gps)
    message(R.string.warning_nogps)
    cancelOnTouchOutside(false)
    positiveButton(R.string.yes){
        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }
    negativeButton(R.string.No) {
        dismiss()
    }
}

/**
 * Creates dialog for battery optimization - needed for app to work properly
 *
 * @param context - any
 * @return MaterialDialog reference
 */
fun batteryOptimizationDialog(context: Context): MaterialDialog = MaterialDialog(context).show {
    title(R.string.battery_ignore_title)
    message(R.string.battery_ignore_text)
    cancelOnTouchOutside(false)
    positiveButton(R.string.ignore){
        PreferencesHolder.onChange(context, PreferencesHolder.IGNORE_BATTERY_OPTIMIZER, true)
        dismiss()
    }
    negativeButton(R.string.open_settings) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
        dismiss()
    }
    icon(R.drawable.warning_white)
}