package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity()
{
    //// Class Members ///////////////////////////////////////////////////////////////////////////

    companion object {
        private val TAG = AuthenticationActivity::class.java.simpleName
        private const val SIGN_IN_RESULT_CODE = 1001
    }

    //// Object Members //////////////////////////////////////////////////////////////////////////

    // Get a reference to the ViewModel scoped to this Fragment.
    private val viewModel by viewModels<AuthenticationViewModel>()

    //// Override ////////////////////////////////////////////////////////////////////////////////

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)

        val binding = ActivityAuthenticationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // If the user was authenticated, send him to RemindersActivity
        viewModel.authenticationState.observe(this, Observer {
            when (it) {
                AuthenticationViewModel.AuthenticationState.AUTHENTICATED -> {
                    val intent = Intent(this, RemindersActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                AuthenticationViewModel.AuthenticationState.UNAUTHENTICATED -> {
                    Toast.makeText(this, "unauthenticated !", Toast.LENGTH_LONG).show()
                }
                else -> {
                    Log.e(TAG, "Authentication state that doesn't require any UI change $it")
                }
            }
        })

        binding.btnLogin.setOnClickListener { launchSignInFlow() }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SIGN_IN_RESULT_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in user.
                Log.i(TAG, "Successfully signed in user " +
                        "${FirebaseAuth.getInstance().currentUser?.displayName}!")
            } else {
                // Sign in failed. If response is null the user canceled the sign-in flow using
                // the back button. Otherwise check response.getError().getErrorCode() and handle
                // the error.
                Log.i(TAG, "Sign in unsuccessful ${response?.error?.errorCode}")
            }
        }
    }

    //// Functions ///////////////////////////////////////////////////////////////////////////////

    // Implement the create account and sign in using FirebaseUI, use sign in using email and sign in using Google
    private fun launchSignInFlow() {
        // Give users the option to sign in / register with their email or Google account. If users
        // choose to register with their email, they will need to create a password as well.
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build()
        )

        // Create and launch sign-in intent. We listen to the response of this activity with the
        // SIGN_IN_RESULT_CODE code.
        startActivityForResult(
            AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(
                providers
            ).build(), SIGN_IN_RESULT_CODE
        )
    }

    // TODO: a bonus is to customize the sign in flow to look nice using :
    //https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md#custom-layout

}
