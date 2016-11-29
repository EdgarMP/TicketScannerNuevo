package com.edgar.ticketscannernuevo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Path;
import android.media.Image;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.edgar.ticketscannernuevo.MainActivity.MEDIA_TYPE_IMAGE;

public class MergeActivity extends Activity {

    private static String TAG = MergeActivity.class.getSimpleName();
    private int promocionId = 100;
    private int mNumeroDeFotos = 0;
    private File mMediaStorageDir = null;
    private ImageView mTicketCompleto;
    private ImageView mIntenta;
    private ImageView mSnapCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merge);

        mTicketCompleto = (ImageView)findViewById(R.id.ticketCompleto);
        mIntenta = (ImageView)findViewById(R.id.intenta);
        mSnapCheck = (ImageView)findViewById(R.id.snapCheck);


        Intent intent = getIntent();
        mNumeroDeFotos = intent.getIntExtra(MainActivity.NUMERO_DE_FOTOS, 0);

        Log.wtf(TAG, "numero de fotos: "+mNumeroDeFotos);

        mMediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "TicketScanner" + File.separator + promocionId + File.separator);

        mergeBitmap();

        mSnapCheck.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                  enviarImagen();
              }
        });

        mIntenta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteAllFiles();
                Intent i = new Intent(MergeActivity.this, MainActivity.class);
                startActivity(i);
            }
        });
    }

    public void mergeBitmap(){


        //File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "TicketScanner" + File.separator + promocionId + File.separator);
        mMediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "TicketScanner" + File.separator + promocionId + File.separator);
        File[] listOfFiles = mMediaStorageDir.listFiles();
        Log.wtf(TAG, "Numero de archivos: "+listOfFiles.length);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inDither = true;
        //options.inSampleSize = 4;
        if(listOfFiles.length == 1) {
            Toast.makeText(this, "Procesando "+listOfFiles.length+ "Imagen", Toast.LENGTH_SHORT).show();

            File mediaFile0 = new File(mMediaStorageDir.getPath() + File.separator + "IMG_" + 1 + ".jpg");
            Bitmap bitmap0 = BitmapFactory.decodeFile(mediaFile0.getPath(), options);
            mTicketCompleto.setImageBitmap(bitmap0);

        }else if(listOfFiles.length == 2){

            Toast.makeText(this, "Procesando "+listOfFiles.length+ "Imagenes", Toast.LENGTH_SHORT).show();

            File mediaFile0 = new File(mMediaStorageDir.getPath() + File.separator + "IMG_" + 1 + ".jpg");
            File mediaFile1 = new File(mMediaStorageDir.getPath() + File.separator + "IMG_" + 2 + ".jpg");
            Bitmap bitmap0 = BitmapFactory.decodeFile(mediaFile0.getPath(), options);
            Bitmap bitmap1 = BitmapFactory.decodeFile(mediaFile1.getPath(), options);


            Bitmap ticketCompleto = mergeBitmap(bitmap0, bitmap1);

            File croppedBitmapFile = getOutputMediaFile(MEDIA_TYPE_IMAGE, true);
            try {
                FileOutputStream fos = new FileOutputStream(croppedBitmapFile);
                ticketCompleto.compress(Bitmap.CompressFormat.JPEG, 50, fos);
                //fos.write(data.get);
                fos.flush();
                fos.close();

            }catch (IOException e){

            }

            mTicketCompleto.setImageBitmap(ticketCompleto);

        }else if(listOfFiles.length > 2){

            Toast.makeText(this, "Procesando "+listOfFiles.length+ "Imagenes", Toast.LENGTH_SHORT).show();

            File mediaFile1 = new File(mMediaStorageDir.getPath() + File.separator + "IMG_" + 1 + ".jpg");
            File mediaFile2 = new File(mMediaStorageDir.getPath() + File.separator + "IMG_" + 2 + ".jpg");
            Bitmap bitmap1 = BitmapFactory.decodeFile(mediaFile1.getPath(), options);
            Bitmap bitmap2 = BitmapFactory.decodeFile(mediaFile2.getPath(), options);

            Bitmap ticketCompleto = mergeBitmap(bitmap1, bitmap2);
            File croppedBitmapFile = getOutputMediaFile(MEDIA_TYPE_IMAGE, false);
            try {
                FileOutputStream fos = new FileOutputStream(croppedBitmapFile);
                ticketCompleto.compress(Bitmap.CompressFormat.JPEG, 50, fos);
                //fos.write(data.get);
                fos.flush();
                fos.close();

            }catch (IOException e){
                Log.wtf(TAG, "Ya se cagó el merge");
            }

            mTicketCompleto.setImageBitmap(ticketCompleto);

            Log.wtf(TAG, "ANTES DEL FOR");
            for (int i = 3; i <= listOfFiles.length; i++) {
                Log.wtf(TAG, "LA I: "+i);

                File mediaFile0 = new File(mMediaStorageDir.getPath() + File.separator + "IMG_" + 0 + ".jpg");
                File mediaFile = new File(mMediaStorageDir.getPath() + File.separator + "IMG_" + i + ".jpg");
                Bitmap bitmap0 = BitmapFactory.decodeFile(mediaFile0.getPath(), options);
                Bitmap bitmap = BitmapFactory.decodeFile(mediaFile.getPath(), options);

                Bitmap continueBitmap = mergeBitmap(bitmap0, bitmap);

                File continueFile = null;
                if(i == listOfFiles.length){
                    Log.wtf(TAG, "YA SE ACABO");
                    continueFile = getOutputMediaFile(MEDIA_TYPE_IMAGE, true);
                }else{
                    continueFile = getOutputMediaFile(MEDIA_TYPE_IMAGE, false);
                }

                try {
                    FileOutputStream fos = new FileOutputStream(continueFile);
                    continueBitmap.compress(Bitmap.CompressFormat.JPEG, 50, fos);
                    //fos.write(data.get);
                    fos.flush();
                    fos.close();

                }catch (IOException e){

                }

                mTicketCompleto.setImageBitmap(continueBitmap);
            }
        }

        //enviarImagen();
        //deleteAllFiles();
    }

    public Bitmap mergeBitmap(Bitmap bitmap1, Bitmap bitmap2) {
        Bitmap mergedBitmap = null;

        int w, h = 0;

        h = bitmap1.getHeight() + bitmap2.getHeight();
        if (bitmap1.getWidth() > bitmap2.getWidth()) {
            w = bitmap1.getWidth();
        } else {
            w = bitmap2.getWidth();
        }

        mergedBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);

        Canvas canvas = new Canvas(mergedBitmap);

        canvas.drawBitmap(bitmap1, 0f, 0f, null);
        canvas.drawBitmap(bitmap2, 0f, bitmap1.getHeight(), null);


        return mergedBitmap;
    }

    private void enviarImagen(){
        File[] listOfFiles = mMediaStorageDir.listFiles();
        Log.wtf(TAG, "Numero de archivos: "+listOfFiles.length);


        for(int i=1; i<=listOfFiles.length; i++){
            Log.wtf(TAG, "El For: "+i);
            File mediaFile = new File(mMediaStorageDir.getPath() + File.separator + "IMG_" + i + ".jpg");
            final String mediaFileName = "IMG_"+i+".jpg";
            byte[] bytesToSend = null;
            try {
                InputStream is = new FileInputStream(mediaFile);
                bytesToSend = IOUtils.toByteArray(is);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //String url = "http://201.151.40.24:8080/Ribeits/api/ribeits/image/imagenAppProveedor";
            String url = "http://201.151.40.24:8080/Ribeits/image/imagenAppProveedor";
            final byte[] finalBytesToSend = bytesToSend;
            VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, url, new Response.Listener<NetworkResponse>() {
                @Override
                public void onResponse(NetworkResponse response) {
                    String resultResponse = new String(response.data);
                    try {
                        JSONObject result = new JSONObject(resultResponse);
                        int idAccion = result.getInt("idAccion");
                        String nombreImagen = result.getString("nombreImagen");
                        String fecha = result.getString("fecha");
                        double tamanoKb = result.getDouble("kb");

                        Toast.makeText(MergeActivity.this, "idAccion: "+idAccion + " nombreImagen: "+nombreImagen + " fecha: "+ fecha + " tamaño: "+tamanoKb+ "KB", Toast.LENGTH_LONG).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    NetworkResponse networkResponse = error.networkResponse;
                    String errorMessage = "Unknown error";
                    Log.wtf(TAG, errorMessage.toString());
                    if (networkResponse == null) {
                        if (error.getClass().equals(TimeoutError.class)) {
                            errorMessage = "Request timeout";
                        } else if (error.getClass().equals(NoConnectionError.class)) {
                            errorMessage = "Failed to connect server";
                        }
                    } else {
                        String result = new String(networkResponse.data);
                        try {
                            JSONObject response = new JSONObject(result);
                            Log.wtf(TAG, response.toString());

                            if (networkResponse.statusCode == 404) {
                                errorMessage = "Resource not found";
                            } else if (networkResponse.statusCode == 401) {
                                errorMessage = " Please login again";
                            } else if (networkResponse.statusCode == 400) {
                                errorMessage = " Check your inputs";
                            } else if (networkResponse.statusCode == 500) {
                                errorMessage = " Something is getting wrong";
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    Log.wtf(TAG, errorMessage);
                    error.printStackTrace();
                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
//                    params.put("api_token", "gh659gjhvdyudo973823tt9gvjf7i6ric75r76");
//                    params.put("name", mNameInput.getText().toString());
//                    params.put("location", mLocationInput.getText().toString());
//                    params.put("about", mAvatarInput.getText().toString());
                    params.put("file", "ejemplo.jpg");
                    return params;
                }

                @Override
                protected Map<String, DataPart> getByteData() {
                    Map<String, DataPart> params = new HashMap<>();
                    // file name could found file base or direct access from real path
                    // for now just get bitmap data from ImageView
//                    Log.wtf(TAG, "finalBytesToSend" +finalBytesToSend.length);
                    params.put("file", new DataPart(mediaFileName, finalBytesToSend, "image/jpeg"));
                    //params.put("cover", new DataPart("file_cover.jpg", AppHelper.getFileDataFromDrawable(getBaseContext(), mCoverImage.getDrawable()), "image/jpeg"));

                    return params;
                }
            };

            VolleySingleton.getInstance(getBaseContext()).addToRequestQueue(multipartRequest);
        }
    }
    /**
     * Create a File for saving an image or video
     */
    private File getOutputMediaFile(int type, boolean croppedImage) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        int promocionId = 100;
        //mMediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "TicketScanner" + File.separator + promocionId + File.separator);
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.
        Log.wtf(TAG, mMediaStorageDir.getAbsolutePath());
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
            if(croppedImage){
                mediaFile = new File(mMediaStorageDir.getPath() + File.separator + "IMG_ticketCompleto.jpg");
            }else{
                mediaFile = new File(mMediaStorageDir.getPath() + File.separator + "IMG_0.jpg");
            }

        } else {
            return null;
        }

        return mediaFile;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public void deleteAllFiles(){
        mMediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "TicketScanner" + File.separator + promocionId + File.separator);
        File[] listOfFiles = mMediaStorageDir.listFiles();
        for (File fInDir : listOfFiles) {
            fInDir.delete();
        }
    }
}
