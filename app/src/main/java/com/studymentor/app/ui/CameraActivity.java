package com.studymentor.app.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.studymentor.app.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 * UC2.5 — CameraActivity.
 *
 * Hosts a CameraX live preview with shutter / flash / flip / gallery controls.
 * On capture (or gallery pick) routes to ScanPreviewActivity with the image Uri.
 *
 * Result contract: this Activity intentionally has no Activity result — it just
 * forwards to ScanPreviewActivity, which routes to ChatActivity on send.
 *
 * Notes:
 *   - CAMERA permission requested at runtime via ActivityResultContracts.
 *   - Photo Picker (PickVisualMedia) is used for gallery — no READ_MEDIA_IMAGES
 *     permission required on Android 13+ when using the Photo Picker.
 */
public class CameraActivity extends AppCompatActivity {

    /** Optional extra — passes through to ChatActivity so we can return to the right place. */
    public static final String EXTRA_SOURCE = "extra_source"; // "home" | "chat"

    private PreviewView preview;
    private View permEmpty;
    private View processingOverlay;
    private MaterialButton btnFlash;

    private ImageCapture imageCapture;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private int flashMode = ImageCapture.FLASH_MODE_OFF;

    /** Runtime CAMERA permission. */
    private final ActivityResultLauncher<String> cameraPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startCamera();
                else showPermEmpty(true);
            });

    /** Photo Picker — no READ_MEDIA_IMAGES needed on Android 13+. */
    private final ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) openScanPreview(uri);
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen on while scanning — students often spend a few seconds aiming
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_camera);

        preview = findViewById(R.id.camera_preview);
        permEmpty = findViewById(R.id.perm_empty);
        processingOverlay = findViewById(R.id.processing_overlay);
        btnFlash = findViewById(R.id.btn_flash);

        findViewById(R.id.btn_close).setOnClickListener(v -> finish());
        findViewById(R.id.btn_shutter).setOnClickListener(v -> takePhoto());
        findViewById(R.id.btn_gallery).setOnClickListener(v -> openGallery());
        findViewById(R.id.btn_flip).setOnClickListener(v -> flipCamera());
        btnFlash.setOnClickListener(v -> cycleFlash());

        findViewById(R.id.btn_grant_perm).setOnClickListener(v ->
                cameraPermLauncher.launch(Manifest.permission.CAMERA));
        findViewById(R.id.btn_open_settings).setOnClickListener(v -> openAppSettings());

        if (hasCameraPermission()) {
            showPermEmpty(false);
            startCamera();
        } else {
            cameraPermLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void showPermEmpty(boolean show) {
        permEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void openAppSettings() {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(i);
    }

    /** Bind CameraX preview + image capture use cases. */
    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);
        final Executor mainExec = ContextCompat.getMainExecutor(this);

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                bindCameraUseCases(provider);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, R.string.camera_error_capture, Toast.LENGTH_SHORT).show();
            }
        }, mainExec);
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider provider) {
        provider.unbindAll();

        Preview previewUseCase = new Preview.Builder().build();
        previewUseCase.setSurfaceProvider(preview.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(flashMode)
                .build();

        CameraSelector selector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        try {
            provider.bindToLifecycle(this, selector, previewUseCase, imageCapture);
        } catch (Exception e) {
            Toast.makeText(this, R.string.camera_error_capture, Toast.LENGTH_SHORT).show();
        }
    }

    /** Snap a photo to cache and route to ScanPreviewActivity. */
    private void takePhoto() {
        if (imageCapture == null) return;

        processingOverlay.setVisibility(View.VISIBLE);

        File outFile = newCacheFile();
        ImageCapture.OutputFileOptions opts =
                new ImageCapture.OutputFileOptions.Builder(outFile).build();

        imageCapture.takePicture(opts, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults results) {
                        processingOverlay.setVisibility(View.GONE);
                        Uri uri = results.getSavedUri();
                        if (uri == null) {
                            uri = FileProvider.getUriForFile(
                                    CameraActivity.this,
                                    getPackageName() + ".fileprovider",
                                    outFile);
                        }
                        openScanPreview(uri);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exc) {
                        processingOverlay.setVisibility(View.GONE);
                        Toast.makeText(CameraActivity.this,
                                R.string.camera_error_capture, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private File newCacheFile() {
        File dir = getExternalCacheDir() != null ? getExternalCacheDir() : getCacheDir();
        String name = "scan_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(new Date()) + ".jpg";
        return new File(dir, name);
    }

    private void openGallery() {
        galleryLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void flipCamera() {
        lensFacing = (lensFacing == CameraSelector.LENS_FACING_BACK)
                ? CameraSelector.LENS_FACING_FRONT
                : CameraSelector.LENS_FACING_BACK;
        startCamera();
    }

    private void cycleFlash() {
        if (flashMode == ImageCapture.FLASH_MODE_OFF) {
            flashMode = ImageCapture.FLASH_MODE_AUTO;
            btnFlash.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_flash));
            int amber = ContextCompat.getColor(this, R.color.brand_primary);
            btnFlash.setBackgroundTintList(ColorStateList.valueOf(
                    (0xCC << 24) | (amber & 0x00FFFFFF)));
            btnFlash.setIconTint(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.dark_header)));
        } else if (flashMode == ImageCapture.FLASH_MODE_AUTO) {
            flashMode = ImageCapture.FLASH_MODE_ON;
        } else {
            flashMode = ImageCapture.FLASH_MODE_OFF;
            btnFlash.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_flash_off));
            btnFlash.setBackgroundTintList(ColorStateList.valueOf(0x80000000)); // camera overlay, no token
            btnFlash.setIconTint(ColorStateList.valueOf(Color.WHITE));
        }
        if (imageCapture != null) imageCapture.setFlashMode(flashMode);
    }

    private void openScanPreview(@NonNull Uri uri) {
        Intent i = new Intent(this, ScanPreviewActivity.class);
        i.putExtra(ScanPreviewActivity.EXTRA_IMAGE_URI, uri.toString());
        i.putExtra(ScanPreviewActivity.EXTRA_SOURCE, getIntent().getStringExtra(EXTRA_SOURCE));
        startActivity(i);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
