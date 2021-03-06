package it.polito.mad.group25.lab.fragments.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.navigation.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import it.polito.mad.group25.lab.AuthenticationContext
import it.polito.mad.group25.lab.R
import it.polito.mad.group25.lab.fragments.userprofile.UserProfileViewModel

class LoginFragment : Fragment(R.layout.login_fragment) {

    private val viewModel: LoginViewModel by activityViewModels()

    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private lateinit var authenticator: FirebaseAuth

    private val authenticationContext: AuthenticationContext by activityViewModels()
    private val userProfileViewModel: UserProfileViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        viewModel.mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        authenticator = Firebase.auth

        resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
                if (res.resultCode == Activity.RESULT_OK) {
                    var task = GoogleSignIn.getSignedInAccountFromIntent(res.data)
                    handleSignInResult(task)
                }
            }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<SignInButton>(R.id.signInButton).also {
            it.setSize(SignInButton.SIZE_STANDARD)
            it.setOnClickListener { signIn() }
        }

        view.findViewById<CheckBox>(R.id.rememberMeCheck).setOnClickListener {
            authenticationContext.rememberMe = !authenticationContext.rememberMe
        }

    }

    override fun onStart() {
        super.onStart()
        authenticationContext.logoutUser()

        //check for already signed in users (only if selected)
        if (authenticationContext.rememberMe)
            authenticator.currentUser?.let {
                authenticationContext.loginUser(it)
                complete()
            }
    }

    //sign in to google
    private fun signIn() {
        var intent = viewModel.mGoogleSignInClient.signInIntent
        resultLauncher.launch(intent)
    }


    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            //google signIn success
            val account = task.result
            firebaseAuthWithGoogle(account?.idToken!!)
        } catch (e: ApiException) {
            //google signIn failed
            Log.w("signIn", "signInResult:failed code=" + e.statusCode)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credentials = GoogleAuthProvider.getCredential(idToken, null)
        authenticator.signInWithCredential(credentials)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    //success
                    authenticationContext.loginUser(authenticator.currentUser!!)

                    //if "remember me" is not selected, next time login page is visited google shouldn ask again the google account
                    if (!authenticationContext.rememberMe)
                        viewModel.signOut()
                    complete()
                }
            }
    }

    private fun complete() {
        authenticationContext.userData.observe(viewLifecycleOwner){
            if(it!=null) {
                activity?.findNavController(R.id.nav_host_fragment_content_main)
                    ?.navigate(R.id.action_LoginFragment_to_OthersTripListFragment)
            }
        }
    }

}

class LoginViewModel : ViewModel() {
    lateinit var mGoogleSignInClient: GoogleSignInClient

    //sign out from google
    fun signOut() {
        mGoogleSignInClient.signOut()
    }

}
