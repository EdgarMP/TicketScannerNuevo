package com.edgar.ticketscannernuevo;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

public class MainActivity extends Activity implements View.OnTouchListener {
    public static String TAG = "CameraActivity";

    public final static int MY_PERMISSIONS_REQUEST_TICKETSCANNER = 1;
    public final static String NUMERO_DE_FOTOS = "com.edgar.ticketscannernuevo.numero_de_fotos";

    private final static double epsilon = 0.17;
    private OrientationEventListener mOrientationEventListener;
    private TextureView mTextureView;
    private Camera mCamera;
    private Camera.CameraInfo mBackCameraInfo;

    public static final int MEDIA_TYPE_IMAGE = 1;
    private File mMediaStorageDir = null;
    private ImageView mImageTicket;
    private static final float FOCUS_AREA_SIZE = 75f;
    private int mContadorFoto = 0;

    private boolean mFocusModeAutoSupported = false;
    private boolean mFocusContinousModeSupported = false;

    private SurfaceTexture mSurface = null;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int promocionId = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Fabric.with(this, new Crashlytics());
            mMediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "TicketScanner" + File.separator + promocionId + File.separator);
            hideStatusBar();

            //releaseCameraAndPreview();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            Log.wtf(TAG, "Ya tiene permisos se pone el listener");
                            setupUI();
                    } else {
                            Log.wtf(TAG, "No tiene permisos, se piden los permisos");
                            requestPermisions();
                    }
            } else {
                    Log.wtf(TAG, "No ocupa permisos, se pone el listener");
                    setupUI();
            }


    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {


                File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE, false);
                Log.wtf(TAG, "pictureTaken");
                    if (pictureFile == null) {
                            Log.wtf(TAG, "Error creating media file, check storage permissions: ");
                            return;
                    }
                    try {
                            FileOutputStream fos = new FileOutputStream(pictureFile);
                            fos.write(data);
                            fos.close();

                    } catch (FileNotFoundException e) {
                            Log.d(TAG, "File not found: " + e.getMessage());
                    } catch (IOException e) {
                            Log.d(TAG, "Error accessing file: " + e.getMessage());
                    }

                    Uri pictureUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
                    CropImage.activity(pictureUri)
                            .setInitialRotation(90)
                            .setGuidelines(CropImageView.Guidelines.ON)
                            .start(MainActivity.this);
            }
    };


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                Log.wtf(TAG, "Result Uri: "+resultUri.toString());
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);

                    /*Imagen que se despliega en el layout*/
                    int fromHere = (int) (bitmap.getHeight() * 0.2);
                    Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, (int) (bitmap.getHeight() * 0.8), bitmap.getWidth(), fromHere);
                    mImageTicket.setImageBitmap(croppedBitmap);

                    /*Guardando la continuación de la imagen que regresó la CropActivity*/
                    mContadorFoto++;
                    File croppedBitmapFile = getOutputMediaFile(MEDIA_TYPE_IMAGE, true);

                    try {
                        FileOutputStream fos = new FileOutputStream(croppedBitmapFile);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                        //fos.write(data.get);
                        fos.flush();
                        fos.close();
                        //mContadorFoto++;
                    } catch (FileNotFoundException e) {
                        Log.d(TAG, "File not found: " + e.getMessage());
                    } catch (IOException e) {
                        Log.d(TAG, "Error accessing file: " + e.getMessage());
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

                // 1. Instantiate an AlertDialog.Builder with its constructor
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AppTheme);

                // 2. Chain together various setter methods to set the dialog characteristics
                builder.setMessage("¿Desea tomar otra foto?")
                        .setTitle("Continuar");

                // Add the buttons
                builder.setPositiveButton("Si", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK button
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog

                        File file = new File(mMediaStorageDir+File.separator+"IMG_temp.jpg");
                        boolean deleted = file.delete();

                        Intent intent = new Intent(MainActivity.this, MergeActivity.class);

                        intent.putExtra(NUMERO_DE_FOTOS, mContadorFoto);
                        startActivity(intent);
                    }
                });

                // Create the AlertDialog
                AlertDialog dialog = builder.create();
                dialog.show();


            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    /**
     * Create a file Uri for saving an image or video
     */
    private Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type, false));
    }

    /**
     * Create a File for saving an image or video
     */
    private File getOutputMediaFile(int type, boolean croppedImage) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.
        // Create the storage directory if it does not exist
        if (!mMediaStorageDir.exists()) {
            if (!mMediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name

        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            if (croppedImage) {
                mediaFile = new File(mMediaStorageDir.getPath() + File.separator + "IMG_" + mContadorFoto + ".jpg");
            } else {
                mediaFile = new File(mMediaStorageDir.getPath() + File.separator + "IMG_temp.jpg");
            }
        } else {
            return null;
        }

        return mediaFile;
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

            Log.wtf(TAG, "onSurfaceTextureAvailable");
            mSurface = surface;
            mSurfaceHeight = height;
            mSurfaceWidth = width;

            openCamera(mSurface, mSurfaceWidth, mSurfaceHeight);


        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                    // Transform you image captured size according to the surface width and height
            Log.d(TAG, "onSurfaceTextureSizeChanged");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.d(TAG, "Stopping preview in SurfaceDestroyed().");
            if (mCamera != null) {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
            return true;
        }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                Log.d(TAG, "onSurfaceTextureUpdated");
            }
    };

    @Override
    protected void onResume() {
        super.onResume();

        if (mOrientationEventListener == null) {
            mOrientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
                private int mOrientation;

                @Override
                public void onOrientationChanged(int orientation) {
                    int lastOrientation = mOrientation;

                    if (orientation >= 315 || orientation < 45) {
                        if (mOrientation != Surface.ROTATION_0) {
                            mOrientation = Surface.ROTATION_0;
                        }
                    } else if (orientation >= 45 && orientation < 135) {
                        if (mOrientation != Surface.ROTATION_90) {
                            mOrientation = Surface.ROTATION_90;
                        }
                    } else if (orientation >= 135 && orientation < 225) {
                        if (mOrientation != Surface.ROTATION_180) {
                            mOrientation = Surface.ROTATION_180;
                        }
                    } else if (mOrientation != Surface.ROTATION_270) {
                        mOrientation = Surface.ROTATION_270;
                    }

                    if (lastOrientation != mOrientation) {
                        Log.wtf(TAG, "rotation!!! lastOrientation:" + lastOrientation + " mOrientation:" + mOrientation + " orientaion:" + orientation);
                    }
                }
            };
        }

        if (mOrientationEventListener.canDetectOrientation()) {
            mOrientationEventListener.enable();
        }
        Log.wtf(TAG, "onResume");
        //releaseCameraAndPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mOrientationEventListener.disable();
        //releaseCameraAndPreview();
        Log.d(TAG, "onPause");
    }

    private void openCamera(SurfaceTexture surface, int surfaceWidth, int surfaceHeight) {
        Log.d("!!!!", "onSurfaceTextureAvailable!!!");
        Pair<Camera.CameraInfo, Integer> backCamera = getBackCamera();
        assert backCamera != null;
        final int backCameraId = backCamera.second;
        mBackCameraInfo = backCamera.first;
        Log.wtf(TAG, "Id Camara: " + backCameraId);


        mCamera = Camera.open(backCameraId);
        cameraDisplayRotation();

        /*Auto focus if supported*/
        Camera.Parameters params = mCamera.getParameters();

        if (params.getSupportedFocusModes().size() > 0) {
                for (int i = 0; i <= params.getSupportedFocusModes().size() - 1; i++) {
                        Log.wtf(TAG, "Supported Focus Mode: " + params.getSupportedFocusModes().get(i).toString());
                }
        }
        if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                Log.wtf(TAG, "El pinche focus Es soportado: FOCUS_MODE_CONTINUOUS_PICTURE");
                mFocusContinousModeSupported = true;
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                Log.wtf(TAG, "El pinche focus Es soportado: FOCUS_MODE_AUTO ");
                mFocusModeAutoSupported = true;
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }

        Camera.Size size = myBestPreviewSize(params.getSupportedPreviewSizes(), surfaceHeight, surfaceWidth);
        Log.wtf(TAG, "Camera Size Elegido: " + size.width + ", " + size.height);

        params.setPreviewSize(size.width, size.height);

        mCamera.setParameters(params);

        try {
            mCamera.setPreviewTexture(surface);
            mCamera.startPreview();
        } catch (IOException ioe) {
            // Something bad happened
            Log.wtf(TAG, "Ocurrió un error");
            //Snackbar snackbar = Snackbar.make(this, "Welcome to AndroidHive", Snackbar.LENGTH_LONG).show();
            Toast.makeText(MainActivity.this, "Ocurrió un error", Toast.LENGTH_LONG).show();
        }
    }

    public void cameraDisplayRotation() {
        final int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
                case Surface.ROTATION_0:
                        degrees = 0;
                        break;
                case Surface.ROTATION_90:
                        degrees = 90;
                        break;
                case Surface.ROTATION_180:
                        degrees = 180;
                        break;
                case Surface.ROTATION_270:
                        degrees = 270;
                        break;
        }

        final int displayOrientation = (mBackCameraInfo.orientation - degrees + 360) % 360;
        Log.wtf(TAG, "Set Display Orientation: "+displayOrientation);
        mCamera.setDisplayOrientation(displayOrientation);
    }

    private Pair<Camera.CameraInfo, Integer> getBackCamera() {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            final int numberOfCameras = Camera.getNumberOfCameras();

            for (int i = 0; i < numberOfCameras; ++i) {
                    Log.wtf(TAG, "Numero de cámaras: " + numberOfCameras);
                    Camera.getCameraInfo(i, cameraInfo);
                    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                            return new Pair<Camera.CameraInfo, Integer>(cameraInfo, i);
                    }
            }
            return null;
    }


    public Camera.Size myBestPreviewSize(List<Camera.Size> sizes, int surfaceWidth, int surfaceHeight) {
            Log.wtf(TAG, "myBestPreviewSize!");
            Camera.Size bestPreviewSize = null;
            Log.wtf(TAG, "Tamaño Sizes: " + sizes.size());
            for (int i = sizes.size() - 1; i >= 0; i--) {
                    Log.wtf(TAG, "I: " + i);
                    Log.wtf(TAG, "Screen Width: " + surfaceWidth + " , Screen Height: " + surfaceHeight);
                    Log.wtf(TAG, "Size Width: " + sizes.get(i).width + " , Size Height: " + sizes.get(i).height);
                    if (sizes.get(i).width == surfaceWidth && sizes.get(i).height == surfaceHeight) {

                            Log.wtf(TAG, "Son iguales!");
                            return sizes.get(i);
                    } else if (sizes.get(i).width <= surfaceWidth && sizes.get(i).height <= surfaceHeight) {
                            if (bestPreviewSize == null) {
                                    bestPreviewSize = sizes.get(i);
                            } else {
                                    int resultArea = bestPreviewSize.width * bestPreviewSize.height;
                                    int newArea = sizes.get(i).width * sizes.get(i).height;

                                    if (newArea > resultArea) {
                                            bestPreviewSize = sizes.get(i);
                                    }
                            }
                    }
            }

            return bestPreviewSize;
    }


    private void releaseCameraAndPreview() {
            //mPreview.setCamera(null);
            if (mCamera != null) {
                    Log.wtf(TAG, "releaseCamera and preview not null");
                    mCamera.stopPreview();

                    mCamera.release();
                    mCamera = null;
            }
    }

    private void hideStatusBar() {
            // If the Android version is lower than Jellybean, use this call to hide
            // the status bar.
            if (Build.VERSION.SDK_INT < 16) {
                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                View decorView = getWindow().getDecorView();
                // Hide both the navigation bar and the status bar.
                // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
                // a general rule, you should design your app to hide the status bar whenever you
                // hide the navigation bar.
                int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN;
                decorView.setSystemUiVisibility(uiOptions);
            }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
            if (mCamera != null) {
                    mCamera.cancelAutoFocus();
                    Rect focusRect = calculateTapArea(motionEvent.getX(), motionEvent.getY(), 1f);

                    Camera.Parameters parameters = mCamera.getParameters();

                    if (mFocusModeAutoSupported) {
                            if (parameters.getMaxNumFocusAreas() > 0) {
                                    List<Camera.Area> mylist = new ArrayList<Camera.Area>();
                                    mylist.add(new Camera.Area(focusRect, 1000));
                                    parameters.setFocusAreas(mylist);
                            }
                    }


                    try {
                            mCamera.cancelAutoFocus();
                            mCamera.setParameters(parameters);
                            mCamera.startPreview();
                            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                                    @Override
                                    public void onAutoFocus(boolean success, Camera camera) {
                                            if (camera.getParameters().getFocusMode() != Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) {
                                                    Camera.Parameters parameters = camera.getParameters();
                                                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                                                    if (parameters.getMaxNumFocusAreas() > 0) {
                                                            parameters.setFocusAreas(null);
                                                    }
//                            camera.setParameters(parameters);
//                            camera.startPreview();
                                            }
                                    }
                            });
                    } catch (Exception e) {
                            e.printStackTrace();
                    }
            }
            return true;
    }

    /**
     * Convert touch position x:y to {@link android.hardware.Camera.Area} position -1000:-1000 to 1000:1000.
     */
    private Rect calculateTapArea(float x, float y, float coefficient) {
            int areaSize = Float.valueOf(FOCUS_AREA_SIZE * coefficient).intValue();

            int left = clamp((int) x - areaSize / 2, 0, mTextureView.getWidth() - areaSize);
            int top = clamp((int) y - areaSize / 2, 0, mTextureView.getHeight() - areaSize);

            RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
            //matrix.mapRect(rectF);

            return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
    }

    private int clamp(int x, int min, int max) {
            if (x > max) {
                    return max;
            }
            if (x < min) {
                    return min;
            }
            return x;
    }

    public void requestPermisions() {

            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_TICKETSCANNER);
            }

    }

    public void setupUI() {
            setContentView(R.layout.activity_main);


            mTextureView = (TextureView) findViewById(R.id.texture);
            mImageTicket = (ImageView) findViewById(R.id.ticketImage);

            assert mTextureView != null;
            mTextureView.setSurfaceTextureListener(textureListener);
            mTextureView.setOnTouchListener(this);

            // Add a listener to the Capture button
            ImageButton captureButton = (ImageButton) findViewById(R.id.btn_takepicture);
            captureButton.setOnClickListener(
                    new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    // get an image from the camera
                                    mCamera.takePicture(null, null, mPicture);
                            }
                    }
            );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_TICKETSCANNER:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Cannot run application because camera service permission have not been granted", Toast.LENGTH_SHORT).show();
                } else {
                    Log.wtf(TAG, "Se acaban de otorgar los permisos, se pone el listener");
                    setupUI();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

} //End Activity

