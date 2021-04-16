package motionapps.besafebox.activities.options.functional;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import java.util.Objects;

import motionapps.besafebox.R;

/**
 *  Sign in/out to Google's account from settings
 */

public class GoogleClass {

    public static final int RC_SIGN_IN = 123, SIGN_CANCELED = 12501,
            SIGN_IN_CURRENTLY_IN_PROGRESS = 12502, SIGN_IN_FAILED = 12500;

    /**
     * offers a way to sign in if account does not exist
     * @param activity - activity object
     */
    public static void GoogleInit(Activity activity, TextView textView, Button button){

        final GoogleSignInAccount googleSignInAccount = GoogleSignIn.getLastSignedInAccount(activity);

//        this.textView = personalInfo.findViewById(R.id.personalInfo_google);
//        this.button = personalInfo.findViewById(R.id.personalInfo_google_button);

        if(googleSignInAccount != null){
            textView.setText(googleSignInAccount.getEmail());
        }


        button.setOnClickListener(v -> {

            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();
            GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(activity, gso);

            if(googleSignInAccount != null){
                // sign out operation
                mGoogleSignInClient.signOut().addOnCompleteListener(activity, task -> {
                            textView.setText(R.string.personalInfo_mail_null);
                            button.setText(R.string.signin);
                        });
            }else{
                // sign in operation
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                activity.startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });

    }

    /**
     * @param context - of activity
     * @param data - intent data from google sign in process - tries to get mail, if fails, the error is raised
     */
    public static void processIntent(Context context, Intent data,
                                     @NonNull TextView textView, @NonNull Button button){
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            // trying to update textview
            GoogleSignInAccount googleSignInAccount  = task.getResult(ApiException.class);
            textView.setText(Objects.requireNonNull(googleSignInAccount).getEmail());
            button.setText(R.string.personal_info_signout);

        } catch (ApiException e) {
            // error catching
            switch (e.getStatusCode()) {
                case SIGN_CANCELED:
                    Toast.makeText(context, R.string.cancel, Toast.LENGTH_SHORT).show();
                case SIGN_IN_CURRENTLY_IN_PROGRESS:
                    return;
                case SIGN_IN_FAILED:
                    Toast.makeText(context, R.string.something_wrong, Toast.LENGTH_SHORT).show();
                    break;
                default:
            }
        }
    }

}
