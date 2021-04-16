package motionapps.besafebox.activities.introduction;


import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.appintro.AppIntro;
import com.github.appintro.AppIntroFragment;

import motionapps.besafebox.activities.main.Main;
import motionapps.besafebox.activities.options.preferences.PreferencesHolder;
import motionapps.besafebox.R;
import motionapps.besafebox.services.Sampler;

public class Introduction extends AppIntro {


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setWizardMode(true);
        setIndicatorColor(
                ContextCompat.getColor(
                        this,
                        R.color.white
                ),
                Color.LTGRAY
        );

        addSlide(AppIntroFragment.newInstance(
                getString(R.string.introduction_welcome_title),
                getString(R.string.introduction_welcome_description),
                R.drawable.ic_fall,
                ContextCompat.getColor(this, R.color.black_foreground_elements),
                ContextCompat.getColor(this, R.color.white),
                ContextCompat.getColor(this, R.color.white)
        ));

        addSlide(AppIntroFragment.newInstance(
                getString(R.string.introduction_research_title),
                getString(R.string.introduction_research_description),
                R.drawable.ic_science_icon,
                ContextCompat.getColor(this, R.color.black_foreground_elements),
                ContextCompat.getColor(this, R.color.white),
                ContextCompat.getColor(this, R.color.white)
        ));

        addSlide(AppIntroFragment.newInstance(
                getString(R.string.introduction_accuracy_title),
                getString(R.string.introduction_accuracy_description),
                R.drawable.ic_accuracy,
                ContextCompat.getColor(this, R.color.black_foreground_elements),
                ContextCompat.getColor(this, R.color.white),
                ContextCompat.getColor(this, R.color.white)
        ));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            addSlide(AppIntroFragment.newInstance(
                    getString(R.string.introduction_title_SMS),
                    getString(R.string.introduction_description_SMS),
                    R.drawable.ic_sms,
                    ContextCompat.getColor(this, R.color.black_foreground_elements),
                    ContextCompat.getColor(this, R.color.white),
                    ContextCompat.getColor(this, R.color.white)
            ));

            askForPermissions(
                    new String[]{Manifest.permission.SEND_SMS},
                    4,
                    false);
            askForPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    5,
                    false);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                addSlide(AppIntroFragment.newInstance(
                        getString(R.string.introduction_title_GPS_foreground),
                        getString(R.string.introduction_GPS_description_foreground_background),
                        R.drawable.ic_baseline_gps,
                        ContextCompat.getColor(this, R.color.black_foreground_elements),
                        ContextCompat.getColor(this, R.color.white),
                        ContextCompat.getColor(this, R.color.white)
                ));
                addSlide(AppIntroFragment.newInstance(
                        getString(R.string.introduction_title_GPS_background),
                        getString(R.string.introduction_GPS_description_background),
                        R.drawable.ic_no_location,
                        ContextCompat.getColor(this, R.color.black_foreground_elements),
                        ContextCompat.getColor(this, R.color.white),
                        ContextCompat.getColor(this, R.color.white)
                ));


                askForPermissions(
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        6,
                        false);
            } else {
                addSlide(AppIntroFragment.newInstance(
                        getString(R.string.introduction_title_GPS_foreground),
                        getString(R.string.introduction_GPS_description_foreground),
                        R.drawable.ic_baseline_gps,
                        ContextCompat.getColor(this, R.color.black_foreground_elements),
                        ContextCompat.getColor(this, R.color.white),
                        ContextCompat.getColor(this, R.color.white)
                ));
            }
        }

        addSlide(ContactFragment.newInstance());

        addSlide(AppIntroFragment.newInstance(
                getString(R.string.introduction_danger_title),
                getString(R.string.introduction_danger_description),
                R.drawable.warning_white,
                ContextCompat.getColor(this, R.color.soft_red),
                ContextCompat.getColor(this, R.color.white),
                ContextCompat.getColor(this, R.color.white)
        ));

        // starts test to get sampling rates for the sensors in phone
        startService(new Intent(this, Sampler.class));
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
        if (newFragment != null) {
            View view = newFragment.getView();
            if (view != null) {
                TextView textView = view.findViewById(R.id.description);
                textView.setTextSize(15);
            }
        }

    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        PreferencesHolder.onChange(this, PreferencesHolder.FIRST_START, true);
        finish();
        Intent intent = new Intent(this, Main.class);
        startActivity(intent);
    }
}
