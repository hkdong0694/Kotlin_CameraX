# Kotlin_CameraX
CameraX API 를 통해 실시간 이미지 분석 예제

## 미리보기, 이미지 분석, 이미지 캡처 기능

* 미리보기 -> 화면에 이미지를 가져온다. ( PreView )
* 이미지 분석 -> MLKit로 전달하는 경우와 같이 알고리즘에 사용할 수 있도록 버퍼에 원할하게 엑세스 가능 ( 머신러닝, 분석 )
* 이미지 캡처 -> 고화질 이미지를 저장한다.

Application 이 onResume(), onPause() 에 특정 시작 및 중지 메서드 호출을 배치하지 않고 cameraProvider.bindToLifecycle() 을 사용하여 카메라와 연결할 수명 주기를 지정한다.


## Preview 기본 코드

~~~kotlin

// Preview -> 카메라 미리보기를 구현해주는 것 , Builder 패턴 구현
// 개체를 초기화하고 빌드를 호출하고 뷰 파인더에서 표면 공급자를 가져온 다음 미리보기에 설정한다.
val preview = Preview.Builder().build().also {
// setSurfaceProvider 함수 -> PreviewView 에 SurfaceProvider 제공
it.setSurfaceProvider(viewFinder.surfaceProvider)

~~~

## ImageCapture 코드 설명 ( Preview 미리보기 )

~~~kotlin

// Image Capture
imageCapture = ImageCapture.Builder().build()

~~~

## ImageAnaylze 기본 코드 설명 ( Preview 미리보기 )

~~~kotlin

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

~~~

### CameraX 전체 Flow


~~~kotlin

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


~~~