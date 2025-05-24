package com.example.tecnicar

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private val annotationsMap = mutableMapOf<String, Tecnico>() // Mapa para asociar anotaciones con técnicos
    private var initialLocationObtained = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            initializeLocationComponent()
        } else {
            Toast.makeText(
                this,
                "Se requieren permisos de ubicación para mostrar tu posición",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)

        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) { style ->
            val annotationApi = mapView.annotations
            pointAnnotationManager = annotationApi.createPointAnnotationManager()

            // Configura el click listener una sola vez
            pointAnnotationManager.addClickListener(
                OnPointAnnotationClickListener { clickedAnnotation ->
                    annotationsMap[clickedAnnotation.id]?.let { tecnico ->
                        mostrarBottomSheet(tecnico)
                    }
                    true
                }
            )

            // Cargar los iconos en el estilo
            cargarIconos(style)

            // Inicializar la ubicación del usuario
            checkLocationPermission()

            cargarTecnicosDesdeMongoDB()
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permiso ya concedido
                initializeLocationComponent()
            }
            else -> {
                // Solicitar permisos
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun initializeLocationComponent() {
        try {
            val locationComponentPlugin = mapView.location
            locationComponentPlugin.updateSettings {
                this.enabled = true
                this.pulsingEnabled = true
            }

            locationComponentPlugin.addOnIndicatorPositionChangedListener(
                OnIndicatorPositionChangedListener { point ->
                    if (!initialLocationObtained) {
                        initialLocationObtained = true
                        mapView.getMapboxMap().setCamera(
                            CameraOptions.Builder()
                                .center(point)
                                .zoom(15.0)
                                .build()
                        )
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al inicializar el componente de ubicación", e)
            Toast.makeText(
                this,
                "No se pudo inicializar la ubicación: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun cargarIconos(style: Style) {
        // Obtenemos todos los tipos de técnicos que queremos precargar
        val tiposConocidos = listOf("electricista", "plomero", "carpintero", "mecanico", "gasista",
            "cerrajero", "pintor", "albañil", "pc")

        // Cargamos cada icono
        tiposConocidos.forEach { tipo ->
            val resourceId = resources.getIdentifier(tipo, "drawable", packageName)
            if (resourceId != 0) {
                val drawable = ContextCompat.getDrawable(this, resourceId)
                if (drawable != null) {
                    val bitmap = drawableToBitmap(drawable)
                    style.addImage(tipo, bitmap)
                }
            }
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    private fun cargarTecnicosDesdeMongoDB() {
        lifecycleScope.launch {
            try {
                val tecnicos = MongoRepository.getTecnicos()
                agregarTecnicos(tecnicos)
                Log.d("MainActivity", "Técnicos cargados: ${tecnicos.size}")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error al cargar técnicos", e)
                Toast.makeText(
                    this@MainActivity,
                    "Error al cargar técnicos: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun agregarTecnicos(tecnicos: List<Tecnico>) {
        tecnicos.forEach { tecnico ->
            val punto = Point.fromLngLat(tecnico.lng, tecnico.lat)

            // Obtenemos el nombre del icono basado en el tipo del técnico
            val iconName = tecnico.tipo.lowercase()

            // Verificamos si el icono existe como recurso
            val iconResourceId = resources.getIdentifier(iconName, "drawable", packageName)

            // Creamos la anotación con el icono correspondiente
            val annotationOptions = PointAnnotationOptions()
                .withPoint(punto)
                .withIconImage(if (iconResourceId != 0) iconName else "marker")

            val annotation = pointAnnotationManager.create(annotationOptions)

            // Guardamos la relación entre el ID de la anotación y el técnico
            annotationsMap[annotation.id] = tecnico
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
            Log.e("MainActivity", "Error al mostrar perfil del técnico", e)
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
            Log.e("MainActivity", "Error al mostrar contratar técnico", e)
            Toast.makeText(this, "Error al mostrar pantalla de contratación: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

}