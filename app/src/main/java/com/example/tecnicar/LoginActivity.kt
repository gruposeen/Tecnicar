package com.example.tecnicar

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity(){

    object Global{
        var prefences = "sharedPreferences"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        verificarSesion()

        var email = findViewById<EditText>(R.id.etCorreo)
        var password = findViewById<EditText>(R.id.etPassword)
        var btnLogin = findViewById<Button>(R.id.btnLogin)
        var btnCrearCuenta = findViewById<Button>(R.id.btnCrearCuenta)
        var btnGoogleSignIn = findViewById<Button>(R.id.btnGoogleSignIn)

        btnLogin.setOnClickListener{
            if(email.text.toString().isNotEmpty() && password.text.toString().isNotEmpty()){
                loginFirebase(email.text.toString(), password.text.toString())
            }
            else{
                Toast.makeText(this, "Por favor, ingrese correo y contraseña", Toast.LENGTH_SHORT).show()
            }
        }

        btnCrearCuenta.setOnClickListener{
            DialogoCrearCuenta().show(supportFragmentManager, null)
        }

        btnGoogleSignIn.setOnClickListener{
            setContent{
                loginGoogle()
            }
        }
    }

    @SuppressLint("CoroutineCreationDuringComposition")
    @Composable
    private fun loginGoogle(){
        val context = LocalContext.current
        val coroutineScope: CoroutineScope = rememberCoroutineScope()
        val credentialManager: CredentialManager = CredentialManager.create(context)

        val signInWithGoogleOption: GetSignInWithGoogleOption = GetSignInWithGoogleOption.Builder(getString(R.string.web_client))
            .setNonce("nonce")
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        coroutineScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = context,
                )
                handleSignIn(result)
            } catch (e: GetCredentialException) {
                Toast.makeText(context, "Error al iniciar sesión con Google: " +e, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleSignIn(result: GetCredentialResponse) {
        // Handle the successfully returned credential.
        val credential = result.credential

        when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        // Use googleIdTokenCredential and extract id to validate and
                        // authenticate on your server.
                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.data)

                        val credential:AuthCredential= GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken,null)

                        FirebaseAuth.getInstance().signInWithCredential(credential)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    val intent = Intent(this, MenuActivity::class.java)
                                    intent.putExtra("email",task.result.user?.email)
                                    intent.putExtra("Proveedor", "Google")
                                    Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                                    startActivity(intent)

                                    guardarSesion(task.result.user?.email.toString(), "Google")
                                } else {
                                    Toast.makeText(this, "Error al iniciar sesión", Toast.LENGTH_SHORT).show()
                                }
                            }

                    } catch (e: GoogleIdTokenParsingException) {
                        //Log.e(TAG, "Received an invalid google id token response", e)
                        Toast.makeText(this, "Received an invalid google id token response", Toast.LENGTH_SHORT).show()
                    }
                }
                else {
                    // Catch any unrecognized credential type here.
                    //Log.e(TAG, "Unexpected type of credential")
                    Toast.makeText(this, "Unexpected type of credential", Toast.LENGTH_SHORT).show()
                }
            }

            else -> {
                // Catch any unrecognized credential type here.
                //Log.e(TAG, "Unexpected type of credential")
                Toast.makeText(this, "Unexpected type of credential", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loginFirebase(email: String, password: String){
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val intent = Intent(this, MenuActivity::class.java)
                    intent.putExtra("email", task.result.user?.email)
                    intent.putExtra("Proveedor", "Usuario/Contraseña")

                    guardarSesion(task.result.user?.email.toString(), "Usuario/Contraseña")

                    Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Error al iniciar sesión", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun verificarSesion(){
        var sesionAbierta:SharedPreferences = this.getSharedPreferences(Global.prefences, MODE_PRIVATE)

        var correo = sesionAbierta.getString("Correo", null)
        var proveedor = sesionAbierta.getString("Proveedor", null)

        if(correo != null && proveedor != null){
            var intent = Intent(this, MenuActivity::class.java)
            intent.putExtra("email", correo)
            intent.putExtra("Proveedor", proveedor)
            startActivity(intent)
        }



    }

    fun guardarSesion(correo:String, proveedor:String){
        var guardarSesion:SharedPreferences.Editor = this.getSharedPreferences(Global.prefences, MODE_PRIVATE).edit()
        guardarSesion.putString("Correo", correo)
        guardarSesion.putString("Proveedor", proveedor)
        guardarSesion.apply()
        guardarSesion.commit()
    }
}