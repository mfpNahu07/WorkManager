package com.example.background.workers

import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.background.KEY_IMAGE_URI
import com.example.background.R

class BlurWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val appContext = applicationContext

        val resourceUri = inputData.getString(KEY_IMAGE_URI)

        makeStatusNotification("Blurring image", appContext)

        // ADD THIS TO SLOW DOWN THE WORKER
        sleep()

        return try {
            // REMOVE THIS
            //    val picture = BitmapFactory.decodeResource(
            //            appContext.resources,
            //            R.drawable.android_cupcake)

            if (TextUtils.isEmpty(resourceUri)) {
                Log.e(TAG, "Invalid input uri")
                throw IllegalArgumentException("Invalid input uri")
            }

            val resolver = appContext.contentResolver

            val picture = BitmapFactory.decodeStream(
                resolver.openInputStream(Uri.parse(resourceUri)))

            val output = blurBitmap(picture, appContext)

            // Write bitmap to a temp file
            val outputUri = writeBitmapToFile(appContext, output)

            val outputData = workDataOf(KEY_IMAGE_URI to outputUri.toString())

            Result.success(outputData)
            //Result.success()

        } catch (throwable: Throwable) {
            Log.e(TAG, "Error applying blur")
            throwable.printStackTrace()
            Result.failure()
        }
    }


}


/**
 *
 * Es hora de agregar la capacidad de desenfocar una imagen varias veces.
 * Toma el parámetro blurLevel que pasaste a applyBlur y agrega esa cantidad de operaciones
 * WorkRequest de desenfoque a la cadena. Solo la primera WorkRequest necesitará contar con la entrada de URI.
 *
 * Ten en cuenta que mostramos algo un poco forzado con fines de aprendizaje. Llamar a nuestro
 * código de desenfoque tres veces es menos eficiente que permitir que BlurWorker tome una entrada
 * que controle el "grado" de desenfoque. Sin embargo, esto nos permite mostrar la flexibilidad de
 * las cadenas de WorkManager.
 *
 *
 * */