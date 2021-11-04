package com.example.myinstagram

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.myinstagram.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;

class LoginActivity : AppCompatActivity() {
    lateinit var auth : FirebaseAuth
    lateinit var googleSignInClient : GoogleSignInClient

    val binding by lazy { ActivityLoginBinding.inflate(layoutInflater)}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("24047239005-21ps9vc99h0knr7oe42kdrqmuh28jnm6.apps.googleusercontent.com")
            .requestEmail()
            .build()

        val googleLoginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            if(it.resultCode == Activity.RESULT_OK){
                var result = Auth.GoogleSignInApi.getSignInResultFromIntent(it.data)
                if(result.isSuccess){
                    var account = result.signInAccount
                    //Seconde step
                    firebaseAuthWithGoogle(account)
                }
            }
        }

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.emailLoginButton.setOnClickListener{
            signinAndSignup()
        }
        binding.googleSigninButton.setOnClickListener{
            val signInIntent = googleSignInClient?.signInIntent
            googleLoginLauncher.launch(signInIntent)
        }

    }

    override fun onStart() {
        super.onStart()
        moveMaininPage(auth?.currentUser)
    }

    fun firebaseAuthWithGoogle(account: GoogleSignInAccount?){
        var credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener {
                    task ->
                if(task.isSuccessful){
                    //Creating a user account
                    moveMaininPage(task.result?.user)
                }else {
                    // Login if you have account
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    fun signinAndSignup(){
        auth?.createUserWithEmailAndPassword(binding.emailEdittext.text.toString(), binding.passwordEdittext.text.toString())?.addOnCompleteListener {
            task ->
                if(task.isSuccessful){
                    //Creating a user account
                    moveMaininPage(task.result?.user)
                }else if(task.exception?.message.isNullOrEmpty()){
                    // Show the error message
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                }else {
                    // Login if you have account
                    signinemail()
                }
        }
    }

    fun signinemail(){
        auth?.signInWithEmailAndPassword(binding.emailEdittext.text.toString(), binding.passwordEdittext.text.toString())
            ?.addOnCompleteListener{ task ->
                if(task.isSuccessful){
                    //Creating a user account
                    moveMaininPage(task.result?.user)
                }else {
                    // Login if you have account
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    fun moveMaininPage(user: FirebaseUser?){
        if(user != null){
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}