package com.example.tecnicar

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.tecnicar.LoginActivity.Global
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        val btnMapaActivity = findViewById<Button>(R.id.btnMapaActivity)
        val btnListaTecnicosActivity = findViewById<Button>(R.id.btnListaTecnicosActivity)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        var intent = getIntent()
        var email = intent.getStringExtra("email")
        var proveedor = intent.getStringExtra("Proveedor")
        var tvBienvenido = findViewById<TextView>(R.id.tvBienvenido)
        tvBienvenido.text = "Bienvenido $email"



        btnMapaActivity.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        btnListaTecnicosActivity.setOnClickListener {
            val intent = Intent(this, ListaTecnicosActivity::class.java)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)

            setContent{
                borrarSesion()
            }
        }

    }


    @SuppressLint("CoroutineCreationDuringComposition")
    @Composable
    private fun borrarSesion(){
        var borrarSesion: SharedPreferences.Editor = this.getSharedPreferences(Global.prefences, MODE_PRIVATE).edit()
        borrarSesion.clear()
        borrarSesion.apply()
        borrarSesion.commit()

        Firebase.auth.signOut()

        val context = LocalContext.current
        val coroutineScope: CoroutineScope = rememberCoroutineScope()
        val credentialManager = CredentialManager.create(context)

        coroutineScope.launch {
            val clearRequest = ClearCredentialStateRequest()
            credentialManager.clearCredentialState(clearRequest)
        }

    }
}

