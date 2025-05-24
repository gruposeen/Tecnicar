package com.example.tecnicar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth


class DialogoCrearCuenta : DialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view:View = inflater.inflate(R.layout.fragment_dialogo__crear__cuenta, container, false)

        var email = view.findViewById<EditText>(R.id.etCorreo)
        var password = view.findViewById<EditText>(R.id.etPassword)
        var btnCrearCuenta = view.findViewById<Button>(R.id.btnCrearCuenta)

        btnCrearCuenta.setOnClickListener{
            if(email.text.toString().isNotEmpty() && password.text.toString().isNotEmpty()){
                crearCuentaFirebase(email.text.toString(), password.text.toString())
            }
            else{
                Toast.makeText(requireContext(), "Por favor, ingrese correo y contraseña", Toast.LENGTH_SHORT).show()
            }
        }


        return view
    }

    private fun crearCuentaFirebase(email: String, password: String){
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val intent = Intent(requireContext(), MenuActivity::class.java)
                    intent.putExtra("email", task.result.user?.email)
                    intent.putExtra("Proveedor", "Usuario/Contraseña")

                    var guardarSesion: LoginActivity = activity as LoginActivity
                    guardarSesion.guardarSesion(task.result.user?.email.toString(), "Usuario/Contraseña")


                    Toast.makeText(requireContext(), "Cuenta creada con exito, bienvenido a TecnicAr!", Toast.LENGTH_SHORT).show()
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "Error al crear cuenta: Es muy corta o el correo ya existe", Toast.LENGTH_SHORT).show()
                }
            }
    }
}