package motionapps.besafebox.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;


import mehdi.sakout.aboutpage.AboutPage;
import mehdi.sakout.aboutpage.Element;
import motionapps.besafebox.BuildConfig;
import motionapps.besafebox.R;
import motionapps.besafebox.activities.main.Main;

public class About extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new AboutFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.about_besafebox);
        }
    }

    public static class AboutFragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return new AboutPage(requireContext())
                    .isRTL(false)
                    .enableDarkMode(true)
                    .setDescription(getString(R.string.about_description))
                    .setImage(R.drawable.ic_fall)
                    .addItem(websiteElement())
                    .addItem(emailElement())
                    .addItem(githubElement())
                    .addItem(versionElement())
                    .create();
        }

        private Element websiteElement(){
            String url = getString(R.string.creative_motion_app_web);
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://$url";
            }
            Element websiteElement = new Element();
            websiteElement.setTitle(getString(R.string.about_check_web));
            websiteElement.setIconDrawable(R.drawable.ic_web);
            websiteElement.setIconTint(R.color.white);
            websiteElement.setValue(url);
            Uri uri = Uri.parse(url);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
            websiteElement.setIntent(browserIntent);
            return websiteElement;
        }

        /**
         * Tab element for mail
         * @return About page Element
         */
        private Element emailElement(){
            String email = getString(R.string.creative_motion_app_mail);
            Element emailElement = new Element();
            emailElement.setTitle(getString(R.string.about_send_mail));
            emailElement.setIconDrawable(R.drawable.ic_mail_outline);
            emailElement.setIconTint(R.color.white);
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:"));
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
            emailElement.setIntent(intent);
            return emailElement;
        }

        /**
         * Tab element for github repository
         * @return About page Element
         */
        private Element githubElement(){
            Element gitHubElement = new Element();
            String id = getString(R.string.creative_motion_app_github);
            gitHubElement.setTitle(getString(mehdi.sakout.aboutpage.R.string.about_github));
            gitHubElement.setIconDrawable(R.drawable.ic_github);
            gitHubElement.setIconTint(R.color.white);
            gitHubElement.setValue(id);

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setData(Uri.parse(id));

            gitHubElement.setIntent(intent);
            return gitHubElement;
        }

        private Element versionElement(){
            // android version tab
            Element versionElement = new Element();
            versionElement.setIconDrawable(R.drawable.ic_android);
            versionElement.setIconTint(R.color.white);
            versionElement.setTitle(String.format("Version of the app: %s", BuildConfig.VERSION_NAME));
            return versionElement;
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
