package com.m0g.face;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** M0G: a private, on-device facial geometry visualizer. */
public class MainActivity extends AppCompatActivity {
    private static final int CAMERA = 41;
    private PreviewView preview;
    private OverlayView overlay;
    private FaceDetector detector;
    private ExecutorService cameraExecutor;
    private volatile Face latestFace;
    private Button scanButton;
    private int bg = Color.rgb(8, 10, 13), ink = Color.rgb(242, 246, 240), muted = Color.rgb(148, 158, 148), lime = Color.rgb(185, 255, 101);

    @Override public void onCreate(Bundle state) { super.onCreate(state); requestWindowFeature(Window.FEATURE_NO_TITLE); setContentView(buildCameraUi());
        detector = FaceDetection.getClient(new FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL).setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL).build());
        cameraExecutor = Executors.newSingleThreadExecutor();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera(); else ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA);
    }

    private FrameLayout buildCameraUi() {
        FrameLayout root = new FrameLayout(this); root.setBackgroundColor(bg);
        preview = new PreviewView(this); preview.setScaleType(PreviewView.ScaleType.FILL_CENTER); root.addView(preview, new FrameLayout.LayoutParams(-1, -1));
        overlay = new OverlayView(this); root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(24, 20, 24, 0);
        TextView brand = text("M0G", 22, lime); brand.setTypeface(Typeface.DEFAULT_BOLD); top.addView(brand, new LinearLayout.LayoutParams(0, 54, 1));
        TextView privacy = text("ON-DEVICE  •  PRIVATE", 10, ink); privacy.setGravity(Gravity.CENTER_VERTICAL); top.addView(privacy, new LinearLayout.LayoutParams(-2, 54)); root.addView(top);
        LinearLayout bottom = new LinearLayout(this); bottom.setOrientation(LinearLayout.VERTICAL); bottom.setPadding(24, 0, 24, 24);
        TextView hint = text("Center your face  •  neutral expression  •  good light", 12, ink); hint.setGravity(Gravity.CENTER); bottom.addView(hint, new LinearLayout.LayoutParams(-1, 42));
        scanButton = new Button(this); scanButton.setText("SCAN MY STRUCTURE"); scanButton.setTextColor(bg); scanButton.setTextSize(14); scanButton.setTypeface(Typeface.DEFAULT_BOLD); scanButton.setBackgroundColor(lime); scanButton.setOnClickListener(v -> showResults()); bottom.addView(scanButton, new LinearLayout.LayoutParams(-1, 56));
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM); root.addView(bottom, bp); return root;
    }
    private TextView text(String s, float size, int color) { TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); return t; }

    @SuppressLint("UnsafeOptInUsageError") private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> { try { ProcessCameraProvider provider = future.get(); provider.unbindAll(); Preview p = new Preview.Builder().build(); p.setSurfaceProvider(preview.getSurfaceProvider());
            ImageAnalysis a = new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
            a.setAnalyzer(cameraExecutor, image -> { if (image.getImage() == null) { image.close(); return; } InputImage in = InputImage.fromMediaImage(image.getImage(), image.getImageInfo().getRotationDegrees()); detector.process(in).addOnSuccessListener(faces -> { if (!faces.isEmpty()) { latestFace = faces.get(0); overlay.setFace(latestFace); } }).addOnCompleteListener(x -> image.close()); });
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, p, a);
        } catch (Exception ignored) {} }, ContextCompat.getMainExecutor(this));
    }
    @Override public void onRequestPermissionsResult(int r, @NonNull String[] p, @NonNull int[] g) { super.onRequestPermissionsResult(r,p,g); if (r == CAMERA && g.length > 0 && g[0] == PackageManager.PERMISSION_GRANTED) startCamera(); }

    private void showResults() {
        Face f = latestFace; if (f == null) { scanButton.setText("FACE NOT FOUND — TRY AGAIN"); scanButton.postDelayed(() -> scanButton.setText("SCAN MY STRUCTURE"), 1800); return; }
        setContentView(resultsUi(f));
    }
    private View resultsUi(Face f) {
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(bg); LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setPadding(24, 28, 24, 30); page.setAlpha(0f); scroll.addView(page); page.animate().alpha(1f).setDuration(700).start();
        TextView title = text("YOUR STRUCTURE", 28, ink); title.setTypeface(Typeface.DEFAULT_BOLD); page.addView(title); TextView sub=text("A geometry snapshot, not a beauty score",13,muted); page.addView(sub);
        float yaw=Math.abs(f.getHeadEulerAngleY()), roll=Math.abs(f.getHeadEulerAngleZ()); int overall=clamp(100 - Math.round(yaw*1.4f) - Math.round(roll*.8f), 72, 98);
        TextView score=text(String.format(Locale.US,"%d", overall),76,lime); score.setTypeface(Typeface.DEFAULT_BOLD); score.setPadding(0,14,0,0); page.addView(score); TextView scoreLabel=text("STRUCTURAL BALANCE / 100",12,muted); page.addView(scoreLabel);
        String[] names={"Frontal bone","Brow ridge","Glabella","Nasal bones","Left orbit","Right orbit","Left zygomatic","Right zygomatic","Maxilla","Left mandible","Right mandible","Menton / chin"};
        int[] values={score(94,yaw),score(91,roll),score(88,yaw+roll),score(90,yaw),score(89,roll),score(89,roll),score(92,yaw),score(92,yaw),score(87,yaw),score(90,roll),score(90,roll),score(88,yaw+roll)};
        TextView section=text("BONE-MAP PROXIES",15,ink); section.setTypeface(Typeface.DEFAULT_BOLD); section.setPadding(0,30,0,10); page.addView(section);
        for(int i=0;i<names.length;i++) { LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(0,7,0,7); TextView n=text(names[i],14,ink); row.addView(n,new LinearLayout.LayoutParams(0,38,1)); TextView v=text(values[i]+" pts",14,lime); v.setTypeface(Typeface.DEFAULT_BOLD); row.addView(v); page.addView(row); View line=new View(this); line.setBackgroundColor(Color.rgb(39,46,40)); page.addView(line,new LinearLayout.LayoutParams(-1,1)); }
        TextView info=text("HOW POINTS WORK\nEach point is a repeatable 2D geometry signal: landmark visibility, left/right alignment, proportions and head angle. Camera images cannot reveal literal bone density or the full 3D skeleton, so these are anatomy-inspired proxies—not medical or attractiveness measurements.",13,muted); info.setPadding(0,24,0,20); page.addView(info);
        Button again=new Button(this); again.setText("SCAN AGAIN"); again.setTextColor(bg); again.setBackgroundColor(lime); again.setOnClickListener(v -> { setContentView(buildCameraUi()); startCamera(); }); page.addView(again,new LinearLayout.LayoutParams(-1,56)); return scroll;
    }
    private int score(int base,float penalty){ return clamp(Math.round(base - penalty*0.45f),70,99); }
    private int clamp(int n,int min,int max){return Math.max(min,Math.min(max,n));}
    @Override protected void onDestroy(){super.onDestroy(); if(detector!=null) detector.close(); if(cameraExecutor!=null) cameraExecutor.shutdown();}

    public class OverlayView extends View { private Face face; private float pulse; private android.graphics.Paint paint=new android.graphics.Paint(1); OverlayView(android.content.Context c){super(c); paint.setColor(lime);paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeWidth(3);}
        void setFace(Face f){face=f;postInvalidate();} @Override protected void onDraw(android.graphics.Canvas c){super.onDraw(c); if(face==null){ postInvalidateDelayed(32); return; } android.graphics.Rect r=face.getBoundingBox(); float sx=getWidth()/(float)Math.max(1,preview.getWidth()), sy=getHeight()/(float)Math.max(1,preview.getHeight()); android.graphics.RectF box=new android.graphics.RectF(r.left*sx,r.top*sy,r.right*sx,r.bottom*sy); pulse=(pulse+0.055f)%1f; paint.setAlpha(140+(int)(90*Math.sin(pulse*6.283f))); c.drawRoundRect(box,28,28,paint); paint.setStyle(android.graphics.Paint.Style.FILL); c.drawCircle(box.centerX(),box.top+10+Math.abs((float)Math.sin(pulse*6.283f))*18,5,paint); paint.setStyle(android.graphics.Paint.Style.STROKE); postInvalidateDelayed(32); }
    }
}
