package com.thumbnailfromasset;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {

    private String outFilePath;
    private static MHandler mHandler;
    private ImageView iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv = findViewById(R.id.iv);
        mHandler = new MHandler(this);
        //复制视频到sd卡
        copyAssetVideoToSD();
    }

    private void copyAssetVideoToSD() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String assetsPath = "";
                InputStream in = null;
                OutputStream out = null;
                try {
                    File file;
                    if (android.os.Environment.MEDIA_MOUNTED.equals(android.os.Environment.getExternalStorageState())) {
                        file = getExternalCacheDir();
                    } else {
                        String path = getFilesDir().getPath();
                        file = new File(path);
                        if (!file.exists()) {
                            file.mkdirs();
                        }
                    }
                    final AssetManager assetManager = getAssets();
                    String[] files = null;
                    try {
                        files = assetManager.list("");
                    } catch (IOException e) {
                        Log.e("tag", "Failed to get asset file list.", e);
                    }
                    if(files != null && files.length>0) {
                        for(String f : files) {
                            if(f.contains("mp4")) {
                                assetsPath = f;
                                break;
                            }
                        }
                    }
                    in = assetManager.open(assetsPath);
                    File outFile = new File(file, "aaa.mp4");
                    outFilePath = outFile.getPath();
                    out = new FileOutputStream(outFile);
                    copyFile(in, out);
                    mHandler.sendEmptyMessage(0);
                } catch(IOException e) {
                    Log.e("tag", "Failed to copy asset file: " + assetsPath, e);
                } finally {
                    try {
                        if (in != null) {
                            in.close();
                        }
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                        Log.e("tag", "Failed to copy asset file: " + assetsPath, e);
                    }
                }
            }
        }).start();
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    public static Bitmap createVideoThumbnail(String filePath) {
        Bitmap newBit = null;
        MediaMetadataRetriever mediaMetadataRetriever = null;
        try {
            mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(filePath);
            Bitmap pic = mediaMetadataRetriever.getFrameAtTime();
            mediaMetadataRetriever.release();
            newBit = getWantedBitmap(pic, 720, 400);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        }
        return newBit;
    }

    static Bitmap getWantedBitmap(Bitmap bitmap, int widthWanted, int heightWanted) {
        if (bitmap == null) {
            return null;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float scale;
        int maxSide = width > height ? width : height;
        if (maxSide > (widthWanted * 2 / 3)) {
            scale = Math.min((float) widthWanted / (float) width,
                    (float) heightWanted / (float) height);
        } else {
            scale = 1.0f;
        }

        if (maxSide < heightWanted / 3) {
            Matrix matrix = new Matrix();
            matrix.postScale(2.0f, 2.0f);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix,
                    true);

            width = bitmap.getWidth();
            height = bitmap.getHeight();
        }

        int dx = (int) ((widthWanted / scale - width) / 2);
        int dy = (int) ((heightWanted / scale - height) / 2);

        Bitmap b = Bitmap.createBitmap(widthWanted, heightWanted,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        Paint paint = new Paint();
        paint.setDither(true);
        paint.setFilterBitmap(true);
        canvas.drawColor(0);
        canvas.drawBitmap(bitmap, new Rect(-dx, -dy,
                (int) (widthWanted / scale) - dx, (int) (heightWanted / scale)
                - dy), new Rect(0, 0, widthWanted, heightWanted), paint);
        return b;
    }

    static class MHandler extends Handler {
        private WeakReference<MainActivity> ref;

        MHandler(MainActivity cls){
            ref = new WeakReference<>(cls);
        }

        Activity getRef(){
            return ref != null ? ref.get() : null;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MainActivity activity = (MainActivity) getRef();
            if (activity != null) {
                if (activity.isFinishing()) return;
                if(msg.what==0){
                    //加载缩略图
                    Bitmap bt = createVideoThumbnail(activity.outFilePath);
                    activity.iv.setImageBitmap(bt);
                }
            }
        }
    }
}
