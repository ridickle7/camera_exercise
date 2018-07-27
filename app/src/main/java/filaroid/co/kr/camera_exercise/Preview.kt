package filaroid.co.kr.camera_exercise

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Toast

import java.util.Arrays
import java.util.concurrent.Semaphore

class Preview(private val mContext: Context, private val mTextureView: TextureView) : Thread() {

    private var mPreviewSize: Size? = null
    private var mCameraDevice: CameraDevice? = null
    private var mPreviewBuilder: CaptureRequest.Builder? = null
    private var mPreviewSession: CameraCaptureSession? = null

    private val mSurfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture,
                                               width: Int, height: Int) {
            // TODO Auto-generated method stub
            Log.e(TAG, "onSurfaceTextureAvailable, width=$width,height=$height")
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture,
                                                 width: Int, height: Int) {
            // TODO Auto-generated method stub
            Log.e(TAG, "onSurfaceTextureSizeChanged")
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            // TODO Auto-generated method stub
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            // TODO Auto-generated method stub
        }
    }

    private val mStateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(camera: CameraDevice) {
            // TODO Auto-generated method stub
            Log.e(TAG, "onOpened")
            mCameraDevice = camera
            startPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            // TODO Auto-generated method stub
            Log.e(TAG, "onDisconnected")
        }

        override fun onError(camera: CameraDevice, error: Int) {
            // TODO Auto-generated method stub
            Log.e(TAG, "onError")
        }

    }

    private val mCameraOpenCloseLock = Semaphore(1)

    private fun getBackFacingCameraId(cManager: CameraManager): String? {
        try {
            for (cameraId in cManager.cameraIdList) {
                val characteristics = cManager.getCameraCharacteristics(cameraId)
                val cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING)!!
                if (cOrientation == CameraCharacteristics.LENS_FACING_BACK) return cameraId
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

        return null
    }

    fun openCamera() {
        val manager = mContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        Log.e(TAG, "openCamera E")
        try {
            val cameraId = getBackFacingCameraId(manager)
            val characteristics = manager.getCameraCharacteristics(cameraId!!)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            mPreviewSize = map!!.getOutputSizes(SurfaceTexture::class.java)[0]

            val permissionCamera = ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA)
            if (permissionCamera == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(mContext as Activity, arrayOf(Manifest.permission.CAMERA), MainActivity.REQUEST_CAMERA)
            } else {
                manager.openCamera(cameraId, mStateCallback, null)
            }
        } catch (e: CameraAccessException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

        Log.e(TAG, "openCamera X")
    }

    protected fun startPreview() {
        // TODO Auto-generated method stub
        if (null == mCameraDevice || !mTextureView.isAvailable || null == mPreviewSize) {
            Log.e(TAG, "startPreview fail, return")
        }

        val texture = mTextureView.surfaceTexture
        if (null == texture) {
            Log.e(TAG, "texture is null, return")
            return
        }

        texture.setDefaultBufferSize(mPreviewSize!!.width, mPreviewSize!!.height)
        val surface = Surface(texture)

        try {
            mPreviewBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        } catch (e: CameraAccessException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

        mPreviewBuilder!!.addTarget(surface)

        try {
            mCameraDevice!!.createCaptureSession(Arrays.asList(surface), object : CameraCaptureSession.StateCallback() {

                override fun onConfigured(session: CameraCaptureSession) {
                    // TODO Auto-generated method stub
                    mPreviewSession = session
                    updatePreview()
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    // TODO Auto-generated method stub
                    Toast.makeText(mContext, "onConfigureFailed", Toast.LENGTH_LONG).show()
                }
            }, null)
        } catch (e: CameraAccessException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

    }

    protected fun updatePreview() {
        // TODO Auto-generated method stub
        if (null == mCameraDevice) {
            Log.e(TAG, "updatePreview error, return")
        }

        mPreviewBuilder!!.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        val thread = HandlerThread("CameraPreview")
        thread.start()
        val backgroundHandler = Handler(thread.looper)

        try {
            mPreviewSession!!.setRepeatingRequest(mPreviewBuilder!!.build(), null, backgroundHandler)
        } catch (e: CameraAccessException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

    }

    fun setSurfaceTextureListener() {
        mTextureView.surfaceTextureListener = mSurfaceTextureListener
    }

    fun onResume() {
        Log.d(TAG, "onResume")
        setSurfaceTextureListener()
    }

    fun onPause() {
        // TODO Auto-generated method stub
        Log.d(TAG, "onPause")
        try {
            mCameraOpenCloseLock.acquire()
            if (null != mCameraDevice) {
                mCameraDevice!!.close()
                mCameraDevice = null
                Log.d(TAG, "CameraDevice Close")
            }
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.")
        } finally {
            mCameraOpenCloseLock.release()
        }
    }

    companion object {
        private val TAG = "Preview : "
    }
}