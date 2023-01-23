package com.lb.documentsprovidersample

import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.*


object ParcelFileDescriptorUtil {
    @Throws(IOException::class)
    fun pipeFrom(inputStream: InputStream): ParcelFileDescriptor {
        val pipe = ParcelFileDescriptor.createPipe()
        val output: OutputStream = ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])
        TransferThread(inputStream, output).start()
        return pipe[0]
    }

    @Suppress("unused")
    @Throws(IOException::class)
    fun pipeTo(outputStream: OutputStream): ParcelFileDescriptor {
        val pipe = ParcelFileDescriptor.createPipe()
        val input: InputStream = ParcelFileDescriptor.AutoCloseInputStream(pipe[0])
        TransferThread(input, outputStream).start()
        return pipe[1]
    }

    internal class TransferThread(val mIn: InputStream, val mOut: OutputStream) : Thread("ParcelFileDescriptor Transfer Thread") {
        init {
            isDaemon = true
        }

        override fun run() {
            try {
                mIn.copyTo(mOut)
                mOut.flush()
            } catch (e: IOException) {
                Log.e("TransferThread", "writing failed")
                e.printStackTrace()
            } finally {
                kotlin.runCatching {
                    mIn.close()
                }
                kotlin.runCatching {
                    mOut.close()
                }
            }
        }
    }
}
