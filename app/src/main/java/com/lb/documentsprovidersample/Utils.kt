package com.lb.documentsprovidersample

import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.os.Build.VERSION_CODES
import android.provider.MediaStore
import com.lb.common_utils.queryIntentActivitiesCompat

object Utils {
    /**
     * Get all Gallery intents for getting image from one of the apps of the device that handle
     * images. Intent.ACTION_GET_CONTENT and then Intent.ACTION_PICK
     */
    private fun getGalleryIntents(packageManager: PackageManager, action: String, includeDocuments: Boolean, type: String = "image/*"): List<Intent> {
        val intents = ArrayList<Intent>()
        val galleryIntent = if (action == Intent.ACTION_GET_CONTENT)
            Intent(action)
        else
            Intent(action, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        setTypesForIntent(galleryIntent, type)
        val listGallery = packageManager.queryIntentActivitiesCompat(galleryIntent)
        for (res in listGallery) {
            val intent = Intent(galleryIntent)
            intent.component = ComponentName(res.activityInfo.packageName, res.activityInfo.name)
            intent.`package` = res.activityInfo.packageName
            intents.add(intent)
        }
        // remove documents intent
        if (!includeDocuments) {
            for (intent in intents) {
                if (intent.component!!.className == "com.android.documentsui.DocumentsActivity") {
                    intents.remove(intent)
                    break
                }
            }
        }
        return intents
    }

    /**
     * Create a chooser intent to select the source to get image from.<br></br>
     * The source can be camera's (ACTION_IMAGE_CAPTURE) or gallery's (ACTION_GET_CONTENT).<br></br>
     * All possible sources are added to the intent chooser.
     *
     * @param context          used to access Android APIs, like content resolve, it is your
     * activity/fragment/widget.
     * @param title            the title to use for the chooser UI
     * @param includeDocuments if to include KitKat documents activity containing all sources
     */
    private fun getPickImageChooserIntent(context: Context, title: CharSequence?, includeDocuments: Boolean, type: String = "image/*", extraIntents: ArrayList<Intent>? = null): Intent {
        val packageManager = context.packageManager
        var galleryIntents =
            getGalleryIntents(packageManager, Intent.ACTION_GET_CONTENT, includeDocuments, type)
        if (galleryIntents.isEmpty()) {
            // if no intents found for get-content try pick intent action (Huawei P9).
            galleryIntents =
                getGalleryIntents(packageManager, Intent.ACTION_PICK, includeDocuments, type)
        }
        val allIntents = ArrayList(galleryIntents)
        val target: Intent
        if (allIntents.isEmpty()) {
            target = Intent()
        } else {
            target = allIntents[allIntents.size - 1]
            allIntents.removeAt(allIntents.size - 1)
        }
        // Create a chooser from the main  intent
        val chooserIntent = Intent.createChooser(target, title)
        if (!extraIntents.isNullOrEmpty())
            allIntents.addAll(extraIntents)
        // Add all other intents
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toTypedArray<Parcelable>())
        return chooserIntent
    }

    private fun setTypesForIntent(intent: Intent, type: String = "image/*") {
        val split = type.split('|')
        if (split.size <= 1)
            intent.type = type
        else {
            //            galleryIntent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_MIME_TYPES, split.toTypedArray())
        }
    }

    fun getGalleryIntent(context: Context, title: CharSequence?, type: String = "image/*", extraIntents: ArrayList<Intent>? = null): Intent? {
        if (Build.VERSION.SDK_INT < VERSION_CODES.Q)
            return getPickImageChooserIntent(context, title, true, type, extraIntents)
        val packageManager = context.packageManager
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
        setTypesForIntent(galleryIntent, type)
        if (packageManager.queryIntentActivitiesCompat(galleryIntent).isEmpty()) {
            galleryIntent.action = Intent.ACTION_PICK
            galleryIntent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        if (packageManager.queryIntentActivitiesCompat(galleryIntent).isEmpty()) {
            if (extraIntents.isNullOrEmpty())
                return null
            galleryIntent.action = ""
        }
        val chooserIntent = Intent.createChooser(galleryIntent, title)
        if (!extraIntents.isNullOrEmpty())
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents.toTypedArray<Parcelable>())
        return chooserIntent
    }
}
