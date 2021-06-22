# Kotlin_CameraX
CameraX API 를 통해 실시간 이미지 분석 예제

## 미리보기, 이미지 분석, 이미지 캡처 기능

* 미리보기 -> 화면에 이미지를 가져온다. ( PreView )
* 이미지 분석 -> MLKit로 전달하는 경우와 같이 알고리즘에 사용할 수 있도록 버퍼에 원할하게 엑세스 가능 ( 머신러닝, 분석 )
* 이미지 캡처 -> 고화질 이미지를 저장한다.

Application 이 onResume(), onPause() 에 특정 시작 및 중지 메서드 호출을 배치하지 않고 cameraProvider.bindToLifecycle() 을 사용하여 카메라와 연결할 수명 주기를 지정한다.


## CameraX 기본 코드 설명

~~~kotlin

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
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))

    }

~~~