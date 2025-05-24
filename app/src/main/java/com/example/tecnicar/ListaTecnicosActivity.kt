package com.example.tecnicar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class ListaTecnicosActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TecnicoAdapter
    private lateinit var searchView: SearchView
    private val tecnicosList = mutableListOf<Tecnico>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_tecnicos)

        recyclerView = findViewById(R.id.rvTecnicos)
        searchView = findViewById(R.id.searchViewTecnicos)

        // Configurar el RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TecnicoAdapter(tecnicosList) { tecnico ->
            mostrarBottomSheet(tecnico)
        }
        recyclerView.adapter = adapter

        // Configurar el SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filtrarTecnicos(newText)
                return true
            }
        })

        // Cargar los técnicos desde MongoDB
        cargarTecnicosDesdeMongoDB()

        // Botón para volver al mapa
        findViewById<Button>(R.id.btnVolverAlMapa).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun cargarTecnicosDesdeMongoDB() {
        lifecycleScope.launch {
            try {
                val tecnicos = MongoRepository.getTecnicos()
                tecnicosList.clear()
                tecnicosList.addAll(tecnicos)
                adapter.notifyDataSetChanged()
                Log.d("ListaTecnicosActivity", "Técnicos cargados: ${tecnicos.size}")
            } catch (e: Exception) {
                Log.e("ListaTecnicosActivity", "Error al cargar técnicos", e)
                Toast.makeText(
                    this@ListaTecnicosActivity,
                    "Error al cargar técnicos: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun filtrarTecnicos(query: String?) {
        lifecycleScope.launch {
            try {
                val filteredList = if (query.isNullOrEmpty()) {
                    MongoRepository.getTecnicos()
                } else {
                    MongoRepository.getTecnicos().filter { tecnico ->
                        tecnico.nombre.contains(query, ignoreCase = true) ||
                                tecnico.tipo.contains(query, ignoreCase = true)
                    }
                }

                tecnicosList.clear()
                tecnicosList.addAll(filteredList)
                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Log.e("ListaTecnicosActivity", "Error al filtrar técnicos", e)
            }
        }
    }

    private fun mostrarBottomSheet(tecnico: Tecnico) {
        val bottomSheetView = LayoutInflater.from(this)
            .inflate(R.layout.bottom_sheet_tecnico, null)

        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(bottomSheetView)

        // Configurar la información del técnico
        bottomSheetView.findViewById<TextView>(R.id.tvPresentacion).text = "Hola! Soy un tecnico de la App y me encantaria poder ayudarte"
        bottomSheetView.findViewById<TextView>(R.id.tvNombre).text = "Mi nombre: ${tecnico.nombre}"
        bottomSheetView.findViewById<TextView>(R.id.tvEspecialidad).text = "Mi especialidad: ${tecnico.tipo}"

        // Configurar botones de acción
        bottomSheetView.findViewById<Button>(R.id.btnVerPerfil).setOnClickListener {
            bottomSheetDialog.dismiss()
            mostrarPerfilTecnico(tecnico)
        }

        bottomSheetView.findViewById<Button>(R.id.btnContratarTecnico).setOnClickListener {
            bottomSheetDialog.dismiss()
            mostrarContratarTecnico(tecnico)
        }

        bottomSheetDialog.show()
    }

    private fun mostrarPerfilTecnico(tecnico: Tecnico) {
        try {
            val perfilView = LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_perfil_tecnico, null)

            val perfilDialog = BottomSheetDialog(this)
            perfilDialog.setContentView(perfilView)

            // Configurar la información detallada del técnico
            perfilView.findViewById<TextView>(R.id.tvNombreCompleto).text = tecnico.nombre
            perfilView.findViewById<TextView>(R.id.tvTipoTecnico).text = tecnico.tipo
            perfilView.findViewById<TextView>(R.id.tvDescripcion).text =
                "Técnico especializado con amplia experiencia en solucionar problemas relacionados con ${tecnico.tipo}."
            perfilView.findViewById<TextView>(R.id.tvCalificacion).text = "Calificación: 4.5/5"
            perfilView.findViewById<TextView>(R.id.tvTrabajos).text = "Trabajos completados: 28"

            perfilView.findViewById<Button>(R.id.btnVolver).setOnClickListener {
                perfilDialog.dismiss()
                mostrarBottomSheet(tecnico)
            }

            perfilDialog.show()
        } catch (e: Exception) {
            Log.e("ListaTecnicosActivity", "Error al mostrar perfil del técnico", e)
            Toast.makeText(this, "Error al mostrar perfil: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarContratarTecnico(tecnico: Tecnico) {
        try {
            val contratarView = LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_contratar_tecnico, null)

            val contratarDialog = BottomSheetDialog(this)
            contratarDialog.setContentView(contratarView)

            // Configurar la información para contratar al técnico
            contratarView.findViewById<TextView>(R.id.tvNombreTecnico).text = "Técnico: ${tecnico.nombre}"
            contratarView.findViewById<TextView>(R.id.tvEspecialidadTecnico).text = "Especialidad: ${tecnico.tipo}"
            contratarView.findViewById<TextView>(R.id.tvPrecioHora).text = "Precio por hora: $1500"

            contratarView.findViewById<Button>(R.id.btnConfirmarContratacion).setOnClickListener {
                Toast.makeText(this, "Contratación enviada a ${tecnico.nombre}", Toast.LENGTH_LONG).show()
                contratarDialog.dismiss()
            }

            contratarView.findViewById<Button>(R.id.btnCancelar).setOnClickListener {
                contratarDialog.dismiss()
                mostrarBottomSheet(tecnico)
            }

            contratarDialog.show()
        } catch (e: Exception) {
            Log.e("ListaTecnicosActivity", "Error al mostrar contratar técnico", e)
            Toast.makeText(this, "Error al mostrar pantalla de contratación: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}