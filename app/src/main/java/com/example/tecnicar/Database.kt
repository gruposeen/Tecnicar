package com.example.tecnicar

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object MongoRepository {

    suspend fun getTecnicos(): List<Tecnico> = withContext(Dispatchers.IO) {
        val url = URL("https://tecnicar-server.vercel.app/tecnicos")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        return@withContext connection.inputStream.bufferedReader().use { reader ->
            val response = reader.readText()
            val gson = Gson()
            val tipo = object : TypeToken<List<Tecnico>>() {}.type
            gson.fromJson(response, tipo)
        }
    }
}


