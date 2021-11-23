package com.example.camerax

import android.Manifest
import android.content.pm.ConfigurationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
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
        if(resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_main)
        } else {

            setContentView(R.layout.activity_landscape)
        }
        btn_camera.setOnClickListener(this)
        camera_capture_button.setOnClickListener(this)
        outputDirectory = getOutputDirectory()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_camera -> {
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // 카메라 Permission 이 있을 경우!
                    // SingleThread
                    cameraExecutor = Executors.newSingleThreadExecutor()
                    startCamera()
                    camera_capture_button.visibility = View.VISIBLE
                    viewFinder.visibility = View.VISIBLE
                    btn_camera.visibility = View.GONE
                } else {
                    // 카메라 Permission 이 없을 경우 사용자에게 요청을 한다.
                    requestPermission()
                }
            }
            R.id.camera_capture_button -> {
                // Set up the listener for take photo button
                takePhoto()
            }
        }
    }

    private fun requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this@MainActivity,
                Manifest.permission.CAMERA
            )
        ) {
            // 카메라 권한 요구
            Toast.makeText(this@MainActivity, "카메라 권한이 요구됩니다.", Toast.LENGTH_SHORT).show()
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            // 다시는 보지 않기를 클릭하고 나서 요청을 보낼때!
            Toast.makeText(this@MainActivity, "카메라 허가를 받을 수 없습니다.", Toast.LENGTH_SHORT).show()
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    /**
     * 요청을 받고 나서 들어오는 Method
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == CAMERA_PERMISSION_CODE) {
            // 권한요청을 한 갯수가 1개이고, grantResult 결과가 GRANTED -> 허용일 경우 카메라를 켜준다.
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        // Image 파일명 만들기
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    // 이미지 캡처 오류
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // 이미지 캡처 성공
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    // 이미지를 캡처 후 Preview 화면 Gone 처리!
                    camera_capture_button.visibility = View.GONE
                    viewFinder.visibility = View.GONE
                    btn_camera.visibility = View.VISIBLE
                    cameraExecutor.shutdown()
                }
            })
    }

    private fun startCamera() {

        // CameraX 에서는 ViewFinder 가 촬영할 사진을 미리 볼 수 있는 역할을 한다. PreView Class 이용

        // ProcessCameraProvider
        // Camera 의 생명주기를 Activity 와 같은 LifeCycleOwner 의 생명주기에 Binding 시키는 것
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        // fun ListenableFuture.addListener(runnable: Runnable, executor: Executor)
        // MainThread 에서 작동해야 하기 때문에 끝에 ContextCompat.getMainExecutor(this) 붙여준다.!!
        cameraProviderFuture.addListener({

            // 카메라의 수명주기 LifecycleOwner 애플리케이션의 프로세스 내에서 바인딩하는데 사용한다.
            // ProcessCameraProvider -> 생명주기에 Binding 하는 객체 가져오기
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview -> 카메라 미리보기를 구현해주는 것 , Builder 패턴 구현
            // 개체를 초기화하고 빌드를 호출하고 뷰 파인더에서 표면 공급자를 가져온 다음 미리보기에서 설정한다.
            val preview = Preview.Builder().build().also {
                // setSurfaceProvider 함수 -> PreviewView 에 SurfaceProvider 제공
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }
            /**
             * SurfaceProvider 는 이미지 데이터를 받을 준비가 되었다는 신호를 카메라에게 보내주는 역할을 맡는다.
             * 매개변수 중에서 Executor 가 따로 설정되어있지 않으면 MainThread 에서 SurfaceProvider 를 제공한다.
             * 만약 null 값으로 Provider를 제거하면 카메라는 Preview 객체에서 이미지를 만드는 것을 멈춘다.
             */

            // Image Capture
            imageCapture = ImageCapture.Builder().build()

            // 이미지 분석용
            // 이미지 분석은 차단 비차단 모드 ( 2가지 기능이 있음 )
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                /**
                 * 1. ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST -> 비차단 모드 ( 이 모드에서 실행자는 analyze() 메서드가 호출되는 시점에 카메라에서 마지막으로 사용 가능한 프레임을 수신합니다. )
                 *    analyze() 메서드의 현재 프레임 속도가 단일 프레임의 지연 시간보다 느린 경우 analyze()가
                 *    다음번에 데이터를 수신할 때 카메라 파이프라인에서 사용 가능한 최신 프레임을 가져오도록 몇몇 프레임을 건너뛸 수 있습니다.
                 * 2. ImageAnalysis.STRATEGY_BLOCK_PRODUCER -> 차단 모드 ( 카메라에서 전송되는 프레임을 순차적으로 가져온다 )
                 *    이는 analyze() 메서드가 현재 프레임 속도에서 단일 프레임의 지연 시간보다 오래 걸리면 메서드가
                 *    반환할 때까지 새 프레임이 파이프라인에 진입하지 못하게 차단되므로
                 *    프레임이 더 이상 최신 상태가 아닐 수 있음을 뜻합니다.
                 */
                // .setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            // 쉽게 정리하면
            // STRATEGY_KEEP_ONLY_LATEST -> 이미지를 close 함으로써 가장 최신의 이미지를 가지고온다. ( 몇개 건너 뛸수도 있음 )
            // STRATEGY_BLOCK_PRODUCER -> 이미지를 무조건 순차적으로 가져온다 ( 건너 뛰기 x )

            // 제공되는 이미지 형식 ImageFormat.YUV_420_888
            imageAnalysis.setAnalyzer(cameraExecutor, { image ->
                // 이미지 분석!! 시작
                val rotationDegrees = image.imageInfo.rotationDegrees
                Log.d("asd", "들어와!! $rotationDegrees")

                // 이걸 안쓰면 화면 멈춤! ( 이미지를 닫아 줘야 다음 프레임을 가져오기때문에 꼭 해줘야함! )
                image.close()
            })

            // Select back camera as a default
            // 후면 카메라로 Default 설정
            // DEFAULT_FRONT_CAMERA -> 정면 카메라, DEFAULT_BACK_CAMERA -> 후면 카메라
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                // 생명주기에 binding 시키기
                // Unbind use cases before rebinding
                // Bind use cases to camera
                cameraProvider.unbindAll()
                // Preview Binding!!
                // cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis, imageCapture)
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