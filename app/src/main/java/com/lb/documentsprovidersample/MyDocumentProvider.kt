package com.lb.documentsprovidersample

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.database.*
import android.graphics.*
import android.media.ThumbnailUtils
import android.os.*
import android.provider.*
import android.provider.DocumentsContract.*
import androidx.annotation.*
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import java.io.*


//https://medium.com/androiddevelopers/building-a-documentsprovider-f7f2fb38e86a
//https://developer.android.com/reference/android/provider/DocumentsProvider
//https://developer.android.com/guide/topics/providers/create-document-provider
//https://android.googlesource.com/platform/development/+/android-5.0.0_r2/samples/Vault/src/com/example/android/vault/VaultProvider.java
//https://github.com/magnusja/libaums/blob/92b60a99d356fd165810aa91c07d09b4d3749cbf/storageprovider/src/main/java/me/jahnen/libaums/storageprovider/UsbDocumentProvider.java#L344

class MyDocumentProvider : DocumentsProvider() {
    @UiThread
    override fun onCreate(): Boolean {
        //        Log.d("AppLog", "MyDocumentProvider onCreate ")
        return true
    }

    @WorkerThread
    override fun queryRoots(projection: Array<String>?): Cursor {
        //        Log.d("AppLog", "MyDocumentProvider queryRoots projection:$projection ")
        val defaultRootProjection = arrayOf(
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_SUMMARY,
            Root.COLUMN_FLAGS,
            Root.COLUMN_TITLE,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_ICON,
            Root.COLUMN_AVAILABLE_BYTES)
        val result = MatrixCursor(projection ?: defaultRootProjection)
        val row = result.newRow()
        row.add(Root.COLUMN_ROOT_ID, DEFAULT_ROOT_ID)
        row.add(Root.COLUMN_ICON, R.mipmap.ic_launcher)
        row.add(Root.COLUMN_TITLE,
            context!!.getString(R.string.app_name))
        row.add(Root.COLUMN_FLAGS, Root.FLAG_LOCAL_ONLY)
        row.add(Root.COLUMN_DOCUMENT_ID, DEFAULT_DOCUMENT_ID)
        return result
    }

    @WorkerThread
    private fun includeFile(matrixCursor: MatrixCursor, documentId: String) {
        val context = context!!
        val row = matrixCursor.newRow()
        val isRoot = documentId == DEFAULT_DOCUMENT_ID

        matrixCursor.columnNames.forEach { columnName ->
            when (columnName) {
                Document.COLUMN_DOCUMENT_ID -> row.add(Document.COLUMN_DOCUMENT_ID, documentId)
                Document.COLUMN_DISPLAY_NAME -> {
                    val name = if (isRoot) DEFAULT_ROOT_ID else SOME_FILE_FILENAME
                    row.add(Document.COLUMN_DISPLAY_NAME, name)
                }

                Document.COLUMN_SIZE -> {
                    if (isRoot)
                        row.add(Document.COLUMN_SIZE, 0L)
                    else {
                        val length = File(context.filesDir, SOME_FILE_FILENAME).length()
                        row.add(Document.COLUMN_SIZE, length)
                    }
                }

                Document.COLUMN_MIME_TYPE -> {
                    val mimeType = if (isRoot) Document.MIME_TYPE_DIR else "image/png"
                    row.add(Document.COLUMN_MIME_TYPE, mimeType)
                }

                Document.COLUMN_FLAGS -> {
                    if (isRoot)
                        row.add(Document.COLUMN_FLAGS, Document.FLAG_DIR_PREFERS_GRID)
                    else
                        row.add(Document.COLUMN_FLAGS, Document.FLAG_SUPPORTS_THUMBNAIL)
                }

                Document.COLUMN_LAST_MODIFIED -> {
                    if (isRoot)
                        row.add(Document.COLUMN_LAST_MODIFIED, 0L)
                    else {
                        val lastModified = File(context.filesDir, SOME_FILE_FILENAME).lastModified()
                        row.add(Document.COLUMN_LAST_MODIFIED, lastModified)
                    }
                }
            }


        }
    }

    @WorkerThread
    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        //        Log.d("AppLog", "MyDocumentProvider queryDocument documentId:$documentId projection:$projection ")
        prepareFileIfNeeded(context!!)
        val matrixCursor = MatrixCursor(resolveDocumentProjection(projection))
        includeFile(matrixCursor, documentId)
        return matrixCursor
    }

    @WorkerThread
    override fun openDocumentThumbnail(documentId: String, sizeHint: Point, signal: CancellationSignal?): AssetFileDescriptor {
        val context = context!!
        prepareFileIfNeeded(context)
        val originalFile = File(context.filesDir, SOME_FILE_FILENAME)
        val thumbnail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ThumbnailUtils.createImageThumbnail(originalFile, android.util.Size(sizeHint.x, sizeHint.y), signal)
        } else
            Glide.with(context).load(originalFile).centerCrop().skipMemoryCache(true).submit(sizeHint.x, sizeHint.y).get().toBitmap()
        val cachedFile = File(context.cacheDir, CACHED_FILE_FILENAME)
        thumbnail.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(cachedFile))
        val pfd = ParcelFileDescriptor.open(cachedFile, ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(pfd, 0, cachedFile.length())
    }

    @WorkerThread
    override fun queryChildDocuments(parentDocumentId: String?, projection: Array<out String>?, sortOrder: String?): Cursor {
        prepareFileIfNeeded(context!!)
        //        Log.d("AppLog", "MyDocumentProvider queryChildDocuments parentDocumentId:$parentDocumentId projection:$projection sortOrder:$sortOrder ")
        val result = MatrixCursor(resolveDocumentProjection(projection))
        if (parentDocumentId != DEFAULT_DOCUMENT_ID)
            return result
        includeFile(result, SOME_FILE_DOCUMENT_ID)
        return result
    }

    @WorkerThread
    override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
        //        Log.d("AppLog", "MyDocumentProvider openDocument documentId:$documentId mode:$mode signal:$signal ")
        if (documentId == SOME_FILE_DOCUMENT_ID) {
            val context = context!!
            prepareFileIfNeeded(context)
            val file = File(context.filesDir, SOME_FILE_FILENAME)
            return ParcelFileDescriptorUtil.pipeFrom(FileInputStream(file))

        }
        TODO("Not yet implemented")
    }

    companion object {
        const val DEFAULT_ROOT_ID = "Root"
        const val DEFAULT_DOCUMENT_ID = "0"
        const val SOME_FILE_DOCUMENT_ID = "1"
        const val SOME_FILE_FILENAME = "someFile.png"
        const val CACHED_FILE_FILENAME = "cached.png"

        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf<String>(
            Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE, Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED, Document.COLUMN_FLAGS, Document.COLUMN_SIZE)

        private fun resolveDocumentProjection(projection: Array<out String>?): Array<out String> {
            return projection ?: DEFAULT_DOCUMENT_PROJECTION
        }

        @Synchronized
        private fun prepareFileIfNeeded(context: Context) {
            val file = File(context.filesDir, SOME_FILE_FILENAME)
            if (!file.exists()) {
                //prepare the file if needed
                val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.image)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(file))
            }
        }
    }
}
