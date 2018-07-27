package filaroid.co.kr.camera_exercise

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Size
import android.view.Surface


class CameraUtil  {
    private var mCameraSize: Size? = null

    private var mCaptureSession: CameraCaptureSession? = null
    private var mCameraDevice: CameraDevice? = null
    private var mPreviewRequestBuilder: CaptureRequest.Builder? = null

    private lateinit var mInterface: Camera2Interface

    interface Camera2Interface {
        fun onCameraDeviceOpened(cameraDevice: CameraDevice, cameraSize: Size?)
    }

    constructor(impl: Camera2Interface) {
        mInterface = impl
    }

    // CameraManager : 사용가능한 카메라를 나열하고, CameraDevice를 취득하기 위해 필요한 변수
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun _1_getCameraManager(activity: Activity): CameraManager {
        return activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    // 사용가능한 카메라 리스트를 가져와 해당 cameraId 리턴 (후면 카메라(LENS_FACING_BACK) 사용)
    // StreamConfiguratonMap : CaptureSession 생성시 surfaces를 설정하기 위한 출력 포맷 및 사이즈등의 정보를 가지는 클래스
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun _2_getCameraId(cameraManager: CameraManager, facing : Int): String? {
        try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == facing) {
                    val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    val sizes = map!!.getOutputSizes(SurfaceTexture::class.java)

                    mCameraSize = sizes[0]

                    // 사용가능한 출력 사이즈중 가장 큰 사이즈 선택.
                    for (size in sizes) {
                        if (size.width > mCameraSize!!.width) {
                            mCameraSize = size
                        }
                    }

                    return cameraId
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

        return null
    }

    // CameraDevice : 실질적인 해당 카메라를 나타냄 (mCameraDeviceStateCallback 의 onOpened() 메소드를 통해 device 취득)
    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun _3_getCameraDevice(cameraManager: CameraManager, cameraId: String) {
        try {
            cameraManager.openCamera(cameraId, _3_getCameraDeviceCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    // onOpened()에서 취득한 CameraDevice로 CaptureSession, CaptureRequest가 이뤄지는데

    private val _3_getCameraDeviceCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            mCameraDevice = camera
            mInterface.onCameraDeviceOpened(camera, mCameraSize)    // Camera2 APIs 처리과정을 MainActivity에서 일원화하여 표현하기 위해 인터페이스로 처리.
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
        }
    }

    // CameraCaptureSession
    // CameraDevice에 의해 이미지 캡쳐를 위한 세션 연결.
    // 해당 세션이 연결될 surface를 전달.
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun _4_createCameraSession(cameraDevice: CameraDevice, surface: Surface) {
        try {
            // 세션이 생성(비동기)된 후에는 해당 CameraDevice에서 새로운 세션이 생성되거나 종료하기 이전에는 유효.
            cameraDevice.createCaptureSession(listOf(surface), _4_createCameraSession_callback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private val _4_createCameraSession_callback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object : CameraCaptureSession.StateCallback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
            try {
                mCaptureSession = cameraCaptureSession
                mPreviewRequestBuilder!!.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                cameraCaptureSession.setRepeatingRequest(mPreviewRequestBuilder!!.build(), _4_captureComplete_callback, null)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }

        override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {

        }
    }

    // 캡쳐된 이미지 정보 및 Metadata가 넘어오는데, 프리뷰에서는 딱히 처리할 작업은 없다.
    // 사진 촬영의 경우라면, onCaptureCompleted()에서 촬영이 완료되고 이미지가 저장되었다는 메세지를 띄우는 시점.
    // 캡쳐 이미지와 Metadata 매칭은 Timestamp로 매칭가능하다.
    private val _4_captureComplete_callback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object : CameraCaptureSession.CaptureCallback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        override fun onCaptureProgressed(session: CameraCaptureSession, request: CaptureRequest, partialResult: CaptureResult) {
            super.onCaptureProgressed(session, request, partialResult)
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
            super.onCaptureCompleted(session, request, result)
        }
    }


    // CaptureRequest
    // CameraDevice에 의해 Builder패턴으로 생성하며, 단일 이미지 캡쳐를 위한 하드웨어 설정(센서, 렌즈, 플래쉬) 및 출력 버퍼등의 정보(immutable).
    // 해당 리퀘스트가 연결될 세션의 surface를 타겟으로 지정.
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun _5_captureRequest(cameraDevice: CameraDevice, surface: Surface) {
        try {
            mPreviewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            mPreviewRequestBuilder!!.addTarget(surface)
            mPreviewRequestBuilder!!.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, mPreviewRequestBuilder!!.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION));
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    fun closeCamera() {
        if (null != mCaptureSession) {
            mCaptureSession!!.close()
            mCaptureSession = null
        }

        if (null != mCameraDevice) {
            mCameraDevice!!.close()
            mCameraDevice = null
        }
    }
}