package filaroid.co.kr.camera_exercise

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import android.view.TextureView
import kotlinx.android.synthetic.main.activity_main.*
import android.os.Build
import android.support.annotation.RequiresApi
import android.view.Surface

class MainActivity : AppCompatActivity(), CameraUtil.Camera2Interface, TextureView.SurfaceTextureListener {
    private lateinit var mCamera: CameraUtil
    private var facing: Int = CameraCharacteristics.LENS_FACING_BACK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mCamera = CameraUtil(this)

        c_iv_change.setOnCheckedChangeListener { _, checked ->
            facing = if (checked) CameraCharacteristics.LENS_FACING_FRONT else CameraCharacteristics.LENS_FACING_BACK
            closeCamera()
            openCamera()
        }
    }

    /* Surface Callbacks */
    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, i: Int, i1: Int) {
        openCamera()
    }

    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, i: Int, i1: Int) {

    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        return true
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {

    }

    override fun onResume() {
        super.onResume()

        if (mTextureView.isAvailable) {
            openCamera();
        } else {
            mTextureView.surfaceTextureListener = this;
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun openCamera() {
        val cameraManager = mCamera._1_getCameraManager(this)
        val cameraId = mCamera._2_getCameraId(cameraManager, facing)
        mCamera._3_getCameraDevice(cameraManager, cameraId!!)


    }

    override fun onCameraDeviceOpened(cameraDevice: CameraDevice, cameraSize: Size?) {
        val texture = mTextureView.surfaceTexture
        texture.setDefaultBufferSize(cameraSize!!.width, cameraSize.height)
        val surface = Surface(texture)

        mCamera._4_createCameraSession(cameraDevice, surface)
        mCamera._5_captureRequest(cameraDevice, surface)
    }

    override fun onPause() {
        closeCamera()
        super.onPause()
    }

    // 카메라 종료
    private fun closeCamera() {
        mCamera.closeCamera()
    }
}
