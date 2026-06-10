package com.studymentor.app.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.studymentor.app.R;
import com.studymentor.app.api.MockOcrService;

/**
 * UC2.5 — ScanPreviewActivity.
 *
 * Shows the captured (or gallery-picked) photo, runs mock OCR on it, and lets
 * the user edit the recognized text before sending it into Chat as a prompt.
 *
 * Extras:
 *   EXTRA_IMAGE_URI — required, the captured/picked image
 *   EXTRA_SOURCE    — optional, "home" or "chat", currently informational only
 *
 * On "Ask Milo": opens ChatActivity with EXTRA_PROMPT prefilled with the
 * cleaned-up recognized text (wrapped with a "Help me solve this problem:" prefix).
 */
public class ScanPreviewActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    public static final String EXTRA_SOURCE = "extra_source";

    private ImageView imgCaptured;
    private TextView textConfidence;
    private TextView textHint;
    private TextInputEditText inputRecognized;
    private MaterialButton btnSend;
    private MaterialButton btnRetake;
    private MaterialButton btnCopy;
    private MaterialButton btnCrop;
    private Chip chipMath, chipStep, chipVi;

    private Uri imageUri;
    private MockOcrService.Result ocrResult;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_preview);

        bindViews();

        String uriStr = getIntent().getStringExtra(EXTRA_IMAGE_URI);
        if (TextUtils.isEmpty(uriStr)) {
            finish();
            return;
        }
        imageUri = Uri.parse(uriStr);

        Glide.with(this)
                .load(imageUri)
                .centerCrop()
                .into(imgCaptured);

        // Disable send while we wait for OCR
        btnSend.setEnabled(false);
        inputRecognized.setHint(R.string.scan_processing);

        runMockOcr();
    }

    private void bindViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        imgCaptured       = findViewById(R.id.img_captured);
        textConfidence    = findViewById(R.id.text_confidence);
        textHint          = findViewById(R.id.text_hint);
        inputRecognized   = findViewById(R.id.input_recognized);
        btnSend           = findViewById(R.id.btn_send);
        btnRetake         = findViewById(R.id.btn_retake);
        btnCopy           = findViewById(R.id.btn_copy);
        btnCrop           = findViewById(R.id.btn_crop);
        chipMath          = findViewById(R.id.chip_math);
        chipStep          = findViewById(R.id.chip_step);
        chipVi            = findViewById(R.id.chip_vi);

        btnSend.setOnClickListener(v -> sendToChat());
        btnRetake.setOnClickListener(v -> retake());
        btnCopy.setOnClickListener(v -> copyRecognized());
        btnCrop.setOnClickListener(v ->
                Toast.makeText(this, R.string.toast_coming_soon, Toast.LENGTH_SHORT).show());
    }

    private void runMockOcr() {
        MockOcrService.recognize(imageUri, new MockOcrService.Listener() {
            @Override
            public void onSuccess(@androidx.annotation.NonNull MockOcrService.Result r) {
                ocrResult = r;
                inputRecognized.setText(r.recognizedText);
                inputRecognized.setHint("");
                textConfidence.setText(getString(R.string.scan_confidence_label, r.confidencePercent));
                textHint.setText(getString(R.string.scan_recognized_hint, r.confidencePercent));
                btnSend.setEnabled(true);

                // Reflect detected meta as chip checked-state
                applySuggestions(r);
            }

            @Override
            public void onError(@androidx.annotation.NonNull Throwable t) {
                inputRecognized.setText("");
                inputRecognized.setHint(R.string.camera_error_capture);
                btnSend.setEnabled(true);
            }
        });
    }

    private void applySuggestions(MockOcrService.Result r) {
        chipMath.setChecked("math".equals(r.detectedSubject));
        chipStep.setChecked("step-by-step".equals(r.suggestedStyle));
        chipVi.setChecked("vi".equals(r.detectedLanguage));

        // Update Math chip label to reflect detected subject when not math
        if (!"math".equals(r.detectedSubject)) {
            int label = "code".equals(r.detectedSubject) ? R.string.subject_code : R.string.subject_science;
            chipMath.setText(label);
            chipMath.setChecked(true);
        } else {
            chipMath.setText(R.string.subject_math);
        }
    }

    private void copyRecognized() {
        String text = String.valueOf(inputRecognized.getText());
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("recognized", text));
        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
    }

    private void retake() {
        Intent i = new Intent(this, CameraActivity.class);
        i.putExtra(CameraActivity.EXTRA_SOURCE, getIntent().getStringExtra(EXTRA_SOURCE));
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    /** Open Chat with the recognized text wrapped as a prompt. */
    private void sendToChat() {
        String body = String.valueOf(inputRecognized.getText()).trim();
        if (body.isEmpty()) {
            Toast.makeText(this, "Add a question first", Toast.LENGTH_SHORT).show();
            return;
        }
        String prompt = getString(R.string.scan_prompt_prefix, body);

        Intent i = new Intent(this, ChatActivity.class);
        i.putExtra(ChatActivity.EXTRA_PROMPT, prompt);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        finish();
    }
}
