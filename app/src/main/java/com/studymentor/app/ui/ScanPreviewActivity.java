package com.studymentor.app.ui;

import com.studymentor.app.databinding.ActivityScanPreviewBinding;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.studymentor.app.R;
import com.studymentor.app.ocr.OcrService;

/** On-device OCR preview. Images are never uploaded to an AI provider. */
public class ScanPreviewActivity extends AppCompatActivity {
    private ActivityScanPreviewBinding binding;
    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    public static final String EXTRA_SOURCE = "extra_source";

    private TextInputEditText input;
    private OcrService ocrService;
    private boolean destroyed;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScanPreviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        input = binding.inputRecognized;
        String rawUri = getIntent().getStringExtra(EXTRA_IMAGE_URI);
        if (rawUri == null || rawUri.trim().isEmpty()) {
            Toast.makeText(this, R.string.scan_error_no_image, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Uri imageUri = Uri.parse(rawUri);
        Glide.with(this).load(imageUri).into((ImageView) binding.imgCaptured);

        ((MaterialToolbar) binding.toolbar)
                .setNavigationOnClickListener(v -> finish());
        binding.textConfidence.setVisibility(View.GONE);
        binding.btnCrop.setVisibility(View.GONE);
        binding.chipsSuggest.setVisibility(View.GONE);
        ((TextView) binding.textHint).setText(R.string.scan_review_gemini);

        binding.btnCopy.setOnClickListener(v -> copyText());
        binding.btnRetake.setOnClickListener(v -> {
            Intent intent = new Intent(this, CameraActivity.class);
            intent.putExtra(CameraActivity.EXTRA_SOURCE,
                    getIntent().getStringExtra(EXTRA_SOURCE));
            startActivity(intent);
            finish();
        });
        binding.btnSend.setOnClickListener(v -> sendToChat());

        input.setText(R.string.scan_recognizing);
        input.setEnabled(false);
        ocrService = new OcrService(this);
        ocrService.recognize(imageUri, new OcrService.Callback() {
            @Override
            public void onSuccess(String text) {
                if (destroyed) return;
                input.setEnabled(true);
                input.setText(text);
                input.setSelection(text.length());
            }

            @Override
            public void onError(String message, Throwable error) {
                if (destroyed) return;
                input.setEnabled(true);
                input.setText("");
                input.setHint(message);
                Toast.makeText(ScanPreviewActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void copyText() {
        String text = String.valueOf(input.getText()).trim();
        if (text.isEmpty()) return;
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Recognized question", text));
        Toast.makeText(this, R.string.scan_copied, Toast.LENGTH_SHORT).show();
    }

    private void sendToChat() {
        String text = String.valueOf(input.getText()).trim();
        if (text.isEmpty()) {
            input.setError(getString(R.string.scan_error_empty));
            return;
        }
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_PROMPT, text);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        if (ocrService != null) ocrService.close();
        super.onDestroy();
    }
}
