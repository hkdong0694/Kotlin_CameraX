package com.example.camerax

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.ArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val CAMERA_PERMISSION_CODE = 0
    }

    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_camera.setOnClickListener(this)

        // Set up the listener for take photo button
        camera_capture_button.setOnClickListener { takePhoto() }

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_camera -> {
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    // 카메라 Permission 이 있을 경우!
                    startCamera()
                    camera_capture_button.visibility = View.VISIBLE
                    viewFinder.visibility = View.VISIBLE
                    btn_camera.visibility = View.GONE
                } else {
                    // 카메라 Permission 이 없을 경우 사용자에게 요청을 한다.
                    requestPermission()
                }
            }
        }
    }

    private fun requestPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, Manifest.permission.CAMERA)) {
            Toast.makeText(this@MainActivity, "카메라 권한이 요구됩니다.", Toast.LENGTH_SHORT).show()
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        } else {
            // 다시는 보지 않기를 클릭하고 나서 요청을 보낼때!
            Toast.makeText(this@MainActivity, "카메라 허가를 받을 수 없습니다.", Toast.LENGTH_SHORT).show()
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == CAMERA_PERMISSION_CODE) {
            if(grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                startCamera()
                camera_capture_button.visibility = View.VISIBLE
                viewFinder.visibility = View.VISIBLE
                btn_camera.visibility = View.GONE
            }
        } else {
            Toast.makeText(this@MainActivity, "카메라 권한 요청을 거절하였습니다.", Toast.LENGTH_SHORT).show()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun takePhoto() {}

    private fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }
            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))

    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}