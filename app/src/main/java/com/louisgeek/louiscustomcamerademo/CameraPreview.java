package com.louisgeek.louiscustomcamerademo;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import java.io.IOException;
import java.util.List;

/**
 * A simple wrapper around a Camera and a SurfaceView that renders a centered preview of the Camera
 * to the surface. We need to center the SurfaceView because not all devices have cameras that
 * support preview sizes at the same aspect ratio as the device's display.
 * <p/>
 */
public class CameraPreview extends ViewGroup implements SurfaceHolder.Callback, Camera.AutoFocusCallback {
    private final String TAG = "Preview";

    /**
     * 图片的偏移
     */
    public int moveX = 0;
    public int moveY = 0;

    public SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    public Camera.Size mPreviewSize;
    private List<Camera.Size> mSupportedPreviewSizes;

    public Camera.Size mPictureSize;
    private List<Camera.Size> mSupportedPictureSizes;

    private Camera mCamera;

    CameraPreview(Context context) {
        super(context);
        init(context);
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CameraPreview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public SurfaceHolder getLouisSurfaceHolder() {
        return mHolder;
    }

    private void init(Context context) {
        mSurfaceView = new SurfaceView(context);
        ViewGroup.LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(mSurfaceView, layoutParams);
        // mSurfaceView.setBackgroundColor(Color.RED);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setCamera(Camera camera) {
        mCamera = camera;
        if (mCamera != null) {
            try {
                mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
                mSupportedPictureSizes = mCamera.getParameters().getSupportedPictureSizes();
                for (Camera.Size size : mSupportedPreviewSizes) {
                    Log.d(TAG, "Preview for mPreviewSize w - h : " + size.width + " - " + size.height);
                }
                for (Camera.Size size : mSupportedPictureSizes) {
                    Log.d(TAG, "Preview for mPictureSize w - h : " + size.width + " - " + size.height);
                }
                mCamera.setDisplayOrientation(90);
            } catch (Exception e) {
                e.printStackTrace();
            }
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);
        Log.d(TAG, "Preview w - h : " + width + " - " + height);
        if (mSupportedPreviewSizes != null) {
            // 需要宽高切换 因为相机有90度的角度
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, height, width);
            Log.d(TAG, "Preview mPreviewSize w - h : " + mPreviewSize.width + " - " + mPreviewSize.height);
        }
        if (mSupportedPictureSizes != null) {
            mPictureSize = getOptimalPreviewSize(mSupportedPictureSizes, height, width);
            Log.d(TAG, "Preview mPictureSize w - h : " + mPictureSize.width + " - " + mPictureSize.height);
        }

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);

            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null) {
                previewWidth = mPreviewSize.height;
                previewHeight = mPreviewSize.width;
            }

            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {

                final int scaleWidth = width;
                final int scaleHeight = width * previewHeight / previewWidth;
                moveX = 0;
                moveY = (scaleHeight - height) / 2;
                if (moveY < 0) {
                    moveY = 0;
                }
                child.layout(-moveX, -moveY, scaleWidth, scaleHeight);
            } else {

                final int scaleHeight = height;
                final int scaleWidth = height * previewWidth / previewHeight;
                moveX = (scaleWidth - width) / 2;
                moveY = 0;
                if (moveX < 0) {
                    moveX = 0;
                }
                child.layout(-moveX, -moveY, scaleWidth, scaleHeight);
            }

        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("", "surface surfaceCreated()");
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("", "surface surfaceDestroyed()");
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.d("", "surface surfaceChanged()");
        if (mCamera == null) {
            return;
        }
        try {
            // Now that the size is known, set up the camera parameters and begin
            // the preview.
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            parameters.setPictureSize(mPictureSize.width, mPictureSize.height);
            requestLayout();

            mCamera.setParameters(parameters);
            mCamera.startPreview();
            mCamera.autoFocus(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {

    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

}
