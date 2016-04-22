package com.louisgeek.louiscustomcamerademo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 遇到的问题：显示相机SurfaceView尺寸，相机预览尺寸 和 相机保存图片尺寸 三者不一致
 */
public class MainActivity extends Activity implements Camera.PictureCallback, Camera.ShutterCallback {

    public static final int FLAG_CHOOCE_PICTURE = 2001;
    private final int FLAG_AUTO_FOCUS = 1001;

    private final int FOCUS_DURATION = 3000;//延迟聚焦

    // private View centerWindowView;
    private int mScreenHeight, mScreenWidth;
    private int viewHeight;

    public static final int ZOOM_FACTOR = 5;//缩放因子 
    private int zoomValue = 0;
    private boolean safeToTakePicture = true;

    private CameraPreview mPreview;
    Camera mCamera;
    int numberOfCameras;
    int cameraCurrentlyLocked;


    private ImageView preview_iv;
    private Handler handler;
    Bitmap rightBitmap;
    ImageView id_iv_flash_switch;
    int Request_Code_Camera = 10;
    CameraLine mCameraLine;
    // 两个相机的情况下
    // The first rear facing camera
    private int defaultCameraId = 1;
    int cameraPosition = 1;

    private ScreenSwitchUtils mScreenSwitchInstance;
    private boolean isPortrait=true;
    private int orientationState=ScreenSwitchUtils.ORIENTATION_HEAD_IS_UP;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hide the window title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);//强制横屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//【旋转问题】首先强制竖屏，手机横过来时候 控件不变

        setContentView(R.layout.activity_main);

        //【重力感应处理】 app内锁定横屏 或用户锁定横屏时候获得方向
        mScreenSwitchInstance=ScreenSwitchUtils.init(getApplicationContext());
        //android M
        //判断是否有权限
      /*  if (ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED){

        }else{

            //请求权限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    Request_Code_Camera);
        }
        //判断是否需要 向用户解释，为什么要申请该权限
        ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA);*/


        if (!Utils.checkCameraHardware(this)) {
            Toast.makeText(MainActivity.this, "设备没有摄像头", Toast.LENGTH_SHORT).show();
            return;
        }
        mCameraLine= (CameraLine) findViewById(R.id.id_cl);
        //
        preview_iv = (ImageView) findViewById(R.id.id_preview_iv);
        RelativeLayout id_rl_cp_view = (RelativeLayout) findViewById(R.id.id_rl_cp_view);
        DoubleClickConfig.registerDoubleClickListener(id_rl_cp_view, new DoubleClickConfig.OnDoubleClickListener() {

            @Override
            public void OnSingleClick(View v) {
                // TODO Auto-generated method stub
                zoomDown();//淡季缩小
            }

            @Override
            public void OnDoubleClick(View v) {
                // TODO Auto-generated method stub
                zoomUp();//双击放大
            }
        });

        id_iv_flash_switch = (ImageView) findViewById(R.id.id_iv_flash_switch);

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == FLAG_AUTO_FOCUS) {
                    if (mCamera != null && safeToTakePicture && !TextUtils.isEmpty(mCamera.getParameters().getFlashMode())) {
                        mCamera.startPreview();
                        mCamera.autoFocus(null);
                        //### Toast.makeText(MainActivity.this, "auto focus", Toast.LENGTH_SHORT).show();
                    }
                    handler.sendEmptyMessageDelayed(FLAG_AUTO_FOCUS, FOCUS_DURATION);
                }
            }
        };

        // centerWindowView = findViewById(R.id.center_window_view);
        Log.d("CameraSurfaceView", "CameraSurfaceView onCreate currentThread : " + Thread.currentThread());
        // 得到屏幕的大小
        WindowManager wManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wManager.getDefaultDisplay();
        mScreenHeight = display.getHeight();
        mScreenWidth = display.getWidth();
        viewHeight = mScreenWidth / 2;
        // centerWindowView.getLayoutParams().width = viewHeight;
        //  centerWindowView.getLayoutParams().height = viewHeight;

        // Create a RelativeLayout container that will hold a SurfaceView,
        // and set it as the content of our activity.
        mPreview = (CameraPreview) findViewById(R.id.camera_preview);

        startCamera();

    }
    @Override
    protected void onStart() {
        super.onStart();
        //【重力感应处理】
        mScreenSwitchInstance.start(this);
    }
    @Override
    protected void onStop() {
        super.onStop();
        //【重力感应处理】
        mScreenSwitchInstance.stop();
    }





    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.id_iv_shutter:
             /*   // 拍照,设置相关参数
                Camera.Parameters params = mCamera.getParameters();
                params.setPictureFormat(ImageFormat.JPEG);
                params.setPreviewSize(800, 400);
                // 自动对焦
                params.setFocusMode(Parameters.FOCUS_MODE_AUTO);
                mCamera.setParameters(params);
                mCamera.takePicture(null, null, mPictureCallback);*/
                //快门
                stopFocus();
                mCamera.autoFocus(new Camera.AutoFocusCallback() {//自动对焦
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        // TODO Auto-generated method stub
                        if (success) {
                          /*  //设置参数，并拍照
                            Camera.Parameters params = camera.getParameters();
                            params.setPictureFormat(PixelFormat.JPEG);//图片格式
                            params.setPreviewSize(800, 480);//图片大小
                            camera.setParameters(params);//将参数设置到我的camera
                            camera.takePicture(null, null, mPictureCallback);//将拍摄到的照片给自定义的对象*/
                            // camera.takePicture(null, null, MainActivity.this);
                             takePicture(null, null, MainActivity.this);
                        }
                    }
                });
                // takePicture(null, null, this);
                break;
            case R.id.id_iv_flash_switch:
                toggleFlash();
                break;
            case R.id.id_iv_config_line:
                mCameraLine.changeLineStyle();
                break;
            /*case R.id.id_iv_config_line:
                //###choosePicture();
                break;*/
            case R.id.id_iv_change:
               // changeCamera();
                changeCameraTwo();

                break;
            default:

                break;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

       startCamera();
    }
    private void changeCamera(){
        // Find the total number of cameras available
        numberOfCameras = Camera.getNumberOfCameras();

        // Find the ID of the default camera
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if(defaultCameraId == 1) {
                if(cameraInfo.facing  == Camera.CameraInfo.CAMERA_FACING_FRONT) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置

                    releaseCamera();
                    mCamera = Camera.open(i);//打开当前选中的摄像头

                    defaultCameraId = 0;
                    break;
                }
        }else {
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置

                    mCamera.release();//释放资源
                    mCamera = Camera.open(i);//打开当前选中的摄像头

                    defaultCameraId = 1;
                    break;
                }
            }}
    }
    void changeCameraTwo(){
        //切换前后摄像头
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();//得到摄像头的个数
        for(int i = 0; i < cameraCount; i++   ) {
            Camera.getCameraInfo(i, cameraInfo);//得到每一个摄像头的信息
            if(cameraPosition == 1) {
                Toast.makeText(this, "1现在是后置，变更为前置", Toast.LENGTH_SHORT).show();
                //现在是后置，变更为前置
                if(cameraInfo.facing  == Camera.CameraInfo.CAMERA_FACING_FRONT) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置
                    mCamera.stopPreview();//停掉原来摄像头的预览
                   /*
                    mCamera.release();//释放资源
                    mCamera = null;//取消原来摄像头*/
                    releaseCamera();
                    mCamera = Camera.open(i);//打开当前选中的摄像头
                    try {
                        mCamera.setPreviewDisplay(mPreview.getLouisSurfaceHolder());//通过surfaceview显示取景画面
                       // mCamera.setDisplayOrientation(90); //
                        mPreview.setCamera(mCamera);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    mCamera.startPreview();//开始预览
                    cameraPosition = 0;
                    break;
                }
            } else {
                Toast.makeText(this, "2现在是前置， 变更为后置", Toast.LENGTH_SHORT).show();
                //现在是前置， 变更为后置
                if(cameraInfo.facing  == Camera.CameraInfo.CAMERA_FACING_BACK) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置
                    mCamera.stopPreview();//停掉原来摄像头的预览
                    /*
                    mCamera.release();//释放资源
                    mCamera = null;//取消原来摄像头*/
                    releaseCamera();
                    mCamera = Camera.open(i);//打开当前选中的摄像头
                    try {
                        mCamera.setPreviewDisplay(mPreview.getLouisSurfaceHolder());//通过surfaceview显示取景画面
                        // mCamera.setDisplayOrientation(90); //
                        mPreview.setCamera(mCamera);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    mCamera.startPreview();//开始预览
                    cameraPosition = 1;
                    break;
                }
            }

        }
    }
    private void startCamera() {
        // Open the default i.e. the first rear facing camera.
        try {
            if (mCamera == null) {
                mCamera = Camera.open();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "启动照相机失败，请检查设备并打开权限", Toast.LENGTH_SHORT).show();
        }
        cameraCurrentlyLocked = defaultCameraId;
        mPreview.setCamera(mCamera);

        startFocus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopFocus();
        releaseCamera();
    }

    /**
     * 开启自动对焦
     */
    private void startFocus() {
        stopFocus();
        handler.sendEmptyMessageDelayed(FLAG_AUTO_FOCUS, FOCUS_DURATION);
    }

    /**
     * 关闭自动对焦
     */
    private void stopFocus() {
        handler.removeMessages(FLAG_AUTO_FOCUS);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
    }
    /**
     * 释放mCamera
     */
    private void releaseCamera() {
        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
        if (mCamera != null) {
            mPreview.setCamera(null);
            mCamera.release();
            mCamera = null;
        }
    }
    /**
     * 释放mCamera
     */
    private void releaseCameraTwo() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();// 停掉原来摄像头的预览
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * 拍照
     *
     * @param shutter
     * @param raw
     * @param jpeg
     */
    public void takePicture(Camera.ShutterCallback shutter, Camera.PictureCallback raw,
                            Camera.PictureCallback jpeg) {
        if (mCamera != null) {
            if (safeToTakePicture) {
                mCamera.takePicture(shutter, raw, jpeg);
                safeToTakePicture = false;
            }
        }
    }

    /**
     * 缩小
     */
    public void zoomDown() {
        if (mCamera == null) {
            return;
        }
        Camera.Parameters p = mCamera.getParameters();
        if (!p.isZoomSupported()) return;

        if (zoomValue > 0) {
            zoomValue--;
        } else {
            zoomValue = 0;
            return;
        }
        int value = (int) (1F * zoomValue / ZOOM_FACTOR * p.getMaxZoom());
        p.setZoom(value);
        mCamera.setParameters(p);
    }

    /**
     * 放大
     */
    public void zoomUp() {
        if (mCamera == null) {
            return;
        }
        Camera.Parameters p = mCamera.getParameters();
        if (!p.isZoomSupported()) return;

        if (zoomValue < ZOOM_FACTOR) {
            zoomValue++;
        } else {
            zoomValue = ZOOM_FACTOR;
            Toast.makeText(getApplicationContext(), "已放大最大级别", Toast.LENGTH_SHORT).show();
            return;
        }
        int value = (int) (1F * zoomValue / ZOOM_FACTOR * p.getMaxZoom());
        p.setZoom(value);
        mCamera.setParameters(p);
    }

    /**
     * 开关闪光灯
     * <p/>
     * 持续的亮灯FLASH_MODE_TORCH
     * 闪一下FLASH_MODE_ON
     * 关闭模式FLASH_MODE_OFF
     * 自动感应是否要用闪光灯FLASH_MODE_AUTO
     */
    public void toggleFlash() {
        if (mCamera == null) {
            return;
        }

        Camera.Parameters p = mCamera.getParameters();
        if (Camera.Parameters.FLASH_MODE_OFF.equals(p.getFlashMode())) {
            p.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
            mCamera.setParameters(p);
            id_iv_flash_switch.setImageDrawable(getResources().getDrawable(R.mipmap.camera_flash_on));
        } else if (Camera.Parameters.FLASH_MODE_ON.equals(p.getFlashMode())) {
            p.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
            mCamera.setParameters(p);
            id_iv_flash_switch.setImageDrawable(getResources().getDrawable(R.mipmap.camera_flash_auto));
        } else if (Camera.Parameters.FLASH_MODE_AUTO.equals(p.getFlashMode())) {
            p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);//持续的亮灯
            mCamera.setParameters(p);
            id_iv_flash_switch.setImageDrawable(getResources().getDrawable(R.mipmap.camera_flash_light));
        } else if (Camera.Parameters.FLASH_MODE_TORCH.equals(p.getFlashMode())) {
            p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(p);
            id_iv_flash_switch.setImageDrawable(getResources().getDrawable(R.mipmap.camera_flash_off));
        } else {
            Toast.makeText(this, "Flash mode setting is not supported.", Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * 选择图片
     */
    private void choosePicture() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, FLAG_CHOOCE_PICTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FLAG_CHOOCE_PICTURE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            String imgPath = getUrl(uri);
            Log.d("", "CameraSurfaceView imgPath : " + imgPath);
        }
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {

        // cameraSurfaceView.restartPreview();
        if (mCamera != null) {
            mCamera.startPreview();
            if (!TextUtils.isEmpty(mCamera.getParameters().getFlashMode())) {
                mCamera.autoFocus(mPreview);
            }
        }

        if (data == null || data.length <= 0) {
            safeToTakePicture = true;
            return;
        }

        Log.d("CameraSurfaceView", "CameraSurfaceView onPictureTaken data.length : " + data.length);
        Toast.makeText(this, "data.length : " + data.length, Toast.LENGTH_SHORT).show();
        //获取方向
        //int previewDegree=getDisplayRotation(this);
        //获取配置的方向
        //int xx=getRequestedOrientation();

    /*    Configuration cf= this.getResources().getConfiguration();
        int ori = cf.orientation ;
        if(ori == cf.ORIENTATION_LANDSCAPE){
        // 横屏
            Toast.makeText(this,"1横屏",Toast.LENGTH_SHORT).show();
        }else if(ori == cf.ORIENTATION_PORTRAIT){
        // 竖屏
            Toast.makeText(this,"1竖屏",Toast.LENGTH_SHORT).show();
        }
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
       int mWidth = dm.widthPixels;
        int mHeight = dm.heightPixels;

        if (mHeight > mWidth){
        // 竖屏
            Toast.makeText(this,"2竖屏",Toast.LENGTH_SHORT).show();
        }else{
        // 横屏
            Toast.makeText(this,"2横屏",Toast.LENGTH_SHORT).show();
        }
*/

        isPortrait=mScreenSwitchInstance.isPortrait();
        orientationState=mScreenSwitchInstance.getOrientationState();
        Log.i("xxx","louis==xx==isPortrait："+isPortrait);
        Log.i("xxx","louis==xx==orientationState："+orientationState);
        // 保存图片
        final byte[] b = data;
        new Thread(new Runnable() {
            @Override
            public void run() {
                handleAndSaveBitmap(b);
            }
        }).start();

        safeToTakePicture = true;
    }

    @Override
    public void onShutter() {
        Log.d("CameraSurfaceView", "CameraSurfaceView onShutter");
    }

    /**
     * 处理拍照图片并保存
     *
     * @param data
     */
    private synchronized void handleAndSaveBitmap(byte[] data) {
        // 保存图片
       //### Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length);
        Bitmap b = Utils.byteToBitmap(data);



        if(cameraPosition==1) {
            //后置摄像头
            //rightBitmap = Utils.rotate(b, 0);
             rightBitmap = Utils.rotate(b, 90);//摆正位置
            //rightBitmap = Utils.rotate(b, 180);
            // rightBitmap = Utils.rotate(b, 270);
            //根据重力感应 更正旋转
            switch (orientationState){
                case ScreenSwitchUtils.ORIENTATION_HEAD_IS_UP:
                    break;
                case ScreenSwitchUtils.ORIENTATION_HEAD_IS_DOWN:
                    rightBitmap = Utils.rotate(rightBitmap, 180);
                    break;
                case ScreenSwitchUtils.ORIENTATION_HEAD_IS_LEFT:
                    rightBitmap = Utils.rotate(rightBitmap, 270);
                    break;
                case ScreenSwitchUtils.ORIENTATION_HEAD_IS_RIGHT:
                    rightBitmap = Utils.rotate(rightBitmap, 90);
                    break;
            }
        }else{
            //前置摄像头
            rightBitmap = Utils.rotate(b, 270);//摆正位置
            //根据重力感应 更正旋转
            switch (orientationState){
                case ScreenSwitchUtils.ORIENTATION_HEAD_IS_UP:
                    break;
                case ScreenSwitchUtils.ORIENTATION_HEAD_IS_DOWN:
                    rightBitmap = Utils.rotate(rightBitmap, 180);
                    break;
                case ScreenSwitchUtils.ORIENTATION_HEAD_IS_LEFT:
                    rightBitmap = Utils.rotate(rightBitmap, 90);
                    break;
                case ScreenSwitchUtils.ORIENTATION_HEAD_IS_RIGHT:
                    rightBitmap = Utils.rotate(rightBitmap, 270);
                    break;
            }
        }




        //## Bitmap rightBitmap = Utils.rotate(b, 90);

       /* if (!mScreenSwitchInstance.isPortrait()){
            rightBitmap=Utils.rotate(rightBitmap,270);
        }*/

       //### ???Utils.compress(rightBitmap, 2 * 1024 * 1024);

        // 偏移量
        int moveX = mPreview.moveX * 2;
        int previewW = mPreview.mPreviewSize.width;
        int previewH = mPreview.mPreviewSize.height;
        int pictureW = mPreview.mPictureSize.width;
        int pictureH = mPreview.mPictureSize.height;
        int viewW = mPreview.getWidth();
        int viewH = mPreview.getHeight();

        rightBitmap = Utils.scale(rightBitmap, previewW, 1F * previewW * pictureW / pictureH);
        // moveX = 0;

        int cropWidth = (int) (1F * viewHeight / mScreenWidth * (rightBitmap.getWidth() - moveX));

        int statusBarHeight = Utils.getStatusBarHeight(MainActivity.this);
        int cropX = (int) (rightBitmap.getWidth() / 2 - cropWidth / 2 + 1F * statusBarHeight * pictureW / pictureH / 2);
        int cropY = rightBitmap.getHeight() / 2 - cropWidth / 2 - Utils.dip2px(MainActivity.this, 59)
                + statusBarHeight / 2;
        Log.d("", "mPreview.moveY : " + mPreview.moveY);
        if (rightBitmap.getWidth() < cropWidth + cropX) {
            cropX = rightBitmap.getWidth() - cropWidth;
        }
        if (rightBitmap.getHeight() < cropWidth + cropY) {
            cropY = rightBitmap.getHeight() - cropY;
        }
        // Log.d("CameraSurfaceView", "CameraSurfaceView viewWidth   : " + centerWindowView.getWidth());
        Log.d("CameraSurfaceView", "CameraSurfaceView bitmapWidth : " + rightBitmap.getWidth() / 2);

        final Bitmap bmp = Bitmap.createBitmap(rightBitmap, cropX, cropY, cropWidth, cropWidth);

        handler.post(new Runnable() {
            @Override
            public void run() {
                //####2016年4月20日20:02:15 preview_iv.setImageBitmap(bmp);
                preview_iv.setImageBitmap(rightBitmap);
               // preview_iv.setScaleType(ScaleType.FIT_XY);
            }
        });

        // Bitmap bmp = Utils.getCroppedImage(b, centerWindowView);
        File file = Utils.getDiskCacheDir(this, "bitmap");
        if (!file.exists()) {
            file.mkdirs();
        }
        File f = new File(file, "picture.jpg");
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f));
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (b != null && !b.isRecycled()) {
                b.recycle();
                b = null;
            }
            /*if (bmp != null && !bmp.isRecycled()) {
                bmp.recycle();
                bmp = null;
            }*/
        }
    }

    /**
     * 获取从相册中选择的图片的据对路径
     *
     * @param uri
     * @return
     */
    private String getUrl(Uri uri) {
        if (uri == null) {
            return null;
        }

        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor actualimagecursor = managedQuery(uri, proj, null, null, null);
        int actual_image_column_index = actualimagecursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        actualimagecursor.moveToFirst();
        String img_path = actualimagecursor.getString(actual_image_column_index);
        return TextUtils.isEmpty(img_path) ? null : img_path;
    }




    /**
     * 重力感应导致横竖屏切换的条件下：获取当前屏幕旋转角度，要是锁定竖屏就一直返回0了。。。
     *
     * @param activity
     * @return 0表示是竖屏; 90表示是左横屏; 180表示是反向竖屏; 270表示是右横屏
     */
    public static int getDisplayRotation(Activity activity) {
        if(activity == null)
        {return 0;}

        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }
    public String saveImageFilePath(){
        String imageFilePath = Environment.getExternalStorageDirectory().getPath();
        SimpleDateFormat sdf =   new SimpleDateFormat("yyyy_MM_dd" );
        imageFilePath = imageFilePath+ File.separator+"TongueImage";

        IfNotExistMkdir(imageFilePath);

        String subPath=File.separator+sdf.format(new Date());
        imageFilePath +=subPath;

        IfNotExistMkdir(imageFilePath);

        SimpleDateFormat hms =   new SimpleDateFormat("HHMMSS");
        String FileName=hms.format(new Date());
        return imageFilePath=File.separator+imageFilePath+File.separator+FileName+".jpg";
    }
    private void IfNotExistMkdir(String filePath)
    {
        File file = new File(filePath);
        if (!file.exists()) {
            try{
                file.mkdir();
            }catch(Exception e)
            {
                //no do
            }


        }
    }
    public void saveToSDCard(byte[] data) throws IOException {
       // Log.d(TAG, "saveToSDCard");
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss"); // 格式化时间
        String filename = format.format(date) + ".jpg";
        File fileFolder = new File(Environment.getExternalStorageDirectory() + "/ansen/");// Environment.getRootDirectory()
        if (!fileFolder.exists()) {
            fileFolder.mkdir();
        }
        File jpgFile = new File(fileFolder, filename);
        FileOutputStream outputStream = new FileOutputStream(jpgFile); // 文件输出流
        outputStream.write(data);
        outputStream.close();
        mCamera.startPreview(); // 拍完照后，重新开始预览
        if (false) {
            Bitmap b = Utils.byteToBitmap(data);
            // 获取手机屏幕的宽高
            WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            int windowWidth = windowManager.getDefaultDisplay().getWidth();
            int windowHight = windowManager.getDefaultDisplay().getHeight();
            Bitmap bitmap = Bitmap.createBitmap(b, 0, 0, windowWidth, windowHight);
            // 图片压缩
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();

        }
    }


}


