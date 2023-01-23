package com.lb.documentsprovidersample

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import com.bumptech.glide.Glide

class MainActivity : AppCompatActivity() {
    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val uri = result.data?.data
            val previewImageView = findViewById<ImageView>(R.id.previewImageView)
            Log.d("AppLog", "got result:$uri")
            if (uri != null) {
                Glide.with(this).load(uri).into(previewImageView)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.openPickerButton).setOnClickListener {
            val intent = Utils.getGalleryIntent(this@MainActivity, "choose...")
            //alternative:
            //            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            //            intent.addCategory(Intent.CATEGORY_OPENABLE)
            //            intent.type = "image/*"
            resultLauncher.launch(intent)
        }
        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                val url: String = when (menuItem.itemId) {
                    R.id.menuItem_all_my_apps -> "https://play.google.com/store/apps/developer?id=AndroidDeveloperLB"
                    R.id.menuItem_all_my_repositories -> "https://github.com/AndroidDeveloperLB"
                    R.id.menuItem_current_repository_website -> "https://github.com/AndroidDeveloperLB/DocumentsProviderSample"
                    else -> return true
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                startActivity(intent)
                return true
            }
        })
    }


}
