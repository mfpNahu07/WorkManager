/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.background

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.example.background.workers.BlurWorker
import com.example.background.workers.CleanupWorker
import com.example.background.workers.SaveImageToFileWorker


class BlurViewModel(application: Application) : ViewModel() {

    internal var imageUri: Uri? = null
    internal var outputUri: Uri? = null

    // New instance variable for the WorkInfo
    internal val outputWorkInfos: LiveData<List<WorkInfo>>

    private val workManager = WorkManager.getInstance(application)

    init {
        imageUri = getImageUri(application.applicationContext)

        // This transformation makes sure that whenever the current work Id changes the WorkInfo
        // the UI is listening to changes
        outputWorkInfos = workManager.getWorkInfosByTagLiveData(TAG_OUTPUT)  //Get the WorkInfo
    }

  //  private val workManager = WorkManager.getInstance(application)

    /**
     * Create the WorkRequest to apply the blur and save the resulting image
     * @param blurLevel The amount to blur the image
     */

    /*
    * Crea una cadena de WorkRequest de CleanupWorker, WorkRequest de BlurImage y WorkRequest de
    *  SaveImageToFile en applyBlur. Pasa la entrada a la WorkRequest de BlurImage
    * */

    internal fun applyBlur(blurLevel: Int) {


        // REPLACE THIS CODE:
        // Add WorkRequest to Cleanup temporary images
        // var continuation = workManager
        //            .beginWith(OneTimeWorkRequest
        //            .from(CleanupWorker::class.java))

        // WITH
        var continuation = workManager
            .beginUniqueWork(
                IMAGE_MANIPULATION_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequest.from(CleanupWorker::class.java)
            )

        // Add WorkRequests to blur the image the number of times requested
        for (i in 0 until blurLevel) {
            val blurBuilder = OneTimeWorkRequestBuilder<BlurWorker>()

            // Input the Uri if this is the first blur operation
            // After the first blur operation the input will be the output of previous
            // blur operations.
            if (i == 0) {
                blurBuilder.setInputData(createInputDataForUri())
            }

            continuation = continuation.then(blurBuilder.build())
        }

        // Add WorkRequest to save the image to the filesystem
        val save = OneTimeWorkRequestBuilder<SaveImageToFileWorker>()
            .addTag(TAG_OUTPUT) //   Tag your work
            .build()

        continuation = continuation.then(save)

        // Actually start the work
        continuation.enqueue()
    }

    private fun uriOrNull(uriString: String?): Uri? {
        return if (!uriString.isNullOrEmpty()) {
            Uri.parse(uriString)
        } else {
            null
        }
    }

    /**
     * Creates the input data bundle which includes the Uri to operate on
     * @return Data which contains the Image Uri as a String
     */
    private fun createInputDataForUri(): Data {
        val builder = Data.Builder()
        imageUri?.let {
            builder.putString(KEY_IMAGE_URI, imageUri.toString())
        }
        return builder.build()
    }

    private fun getImageUri(context: Context): Uri {
        val resources = context.resources

        val imageUri = Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(resources.getResourcePackageName(R.drawable.android_cupcake))
            .appendPath(resources.getResourceTypeName(R.drawable.android_cupcake))
            .appendPath(resources.getResourceEntryName(R.drawable.android_cupcake))
            .build()

        return imageUri
    }

    internal fun setOutputUri(outputImageUri: String?) {
        outputUri = uriOrNull(outputImageUri)
    }

    internal fun cancelWork() {
        workManager.cancelUniqueWork(IMAGE_MANIPULATION_WORK_NAME)
    }

    class BlurViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return if (modelClass.isAssignableFrom(BlurViewModel::class.java)) {
                BlurViewModel(application) as T
            } else {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}

/**
 * En lugar de llamar a workManager.enqueue(), llama a workManager.beginWith().
 * Esto mostrará un WorkContinuation, que define una cadena de WorkRequest.
 * Puedes agregar a esta cadena de solicitudes de trabajo llamando al método then(), por ejemplo,
 * si tienes tres objetos WorkRequest, workA, workB y workC, podrás hacer lo siguiente:
 *
 * val continuation = workManager.beginWith(workA)
 * continuation.then(workB) // FYI, then() returns a new WorkContinuation instance
 * .then(workC)
 * .enqueue() // Enqueues the WorkContinuation which is a chain of work
 *
 * */


/**
 * Ahora que usaste las cadenas, es hora de abordar otra poderosa función
 * de WorkManager: las cadenas de trabajo único.   <------------------
 *
 * A veces, querrás que solo una cadena de trabajo se ejecute a la vez. Por ejemplo, tal vez tengas
 * una cadena de trabajo que sincroniza tus datos locales con el servidor. Sería bueno permitir que
 * la primera sincronización de datos termine antes de comenzar una nueva. Para hacerlo, deberás
 *
 * ---------------->   usar beginUniqueWork en lugar de beginWith   <--------------------
 *
 * y proporcionarle un nombre de String único.
 * Esto nombrará la cadena completa de solicitudes de trabajo a fin de que puedas hacer consultas
 * y búsquedas en todas ellas.
 *
 * Asegúrate de que la cadena de trabajo que desenfocará tu archivo sea única por medio de
 * beginUniqueWork. Pasa IMAGE_MANIPULATION_WORK_NAME como la clave. También deberás pasar una
 * ExistingWorkPolicy. Tus opciones son REPLACE, KEEP o APPEND.
 *
 * Deberás usar REPLACE porque, si el usuario decide desenfocar otra imagen antes de que se termine
 * la actual, querremos detener la tarea actual y comenzar a desenfocar la imagen nueva.
 *
 *Blur-O-Matic ahora solo desenfocará una imagen por vez
 *
 * */

/**
 * You can get the status of any WorkRequest by getting a LiveData that holds a WorkInfo object. WorkInfo is an object that contains details about the current state of a WorkRequest, including:

Whether the work is BLOCKED, CANCELLED, ENQUEUED, FAILED, RUNNING or SUCCEEDED.
If the WorkRequest is finished, any output data from the work.
The following table shows three different ways to get LiveData<WorkInfo> or LiveData<List<WorkInfo>> objects and what each does.

Type - WorkManager Method - Description



Get work using id

getWorkInfoByIdLiveData

Each WorkRequest has a unique ID generated by WorkManager; you can use this to get a single LiveData<WorkInfo> for that exact WorkRequest.



Get work using unique chain name

getWorkInfosForUniqueWorkLiveData

As you've just seen, WorkRequests can be part of a unique chain. This returns LiveData<List<WorkInfo>> for all work in a single, unique chain of WorkRequests.



Get work using a tag

getWorkInfosByTagLiveData

Finally, you can optionally tag any WorkRequest with a String. You can tag multiple WorkRequests with the same tag to associate them. This returns the LiveData<List<WorkInfos>> for any single tag.
 */
