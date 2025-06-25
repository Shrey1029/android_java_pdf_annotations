package com.technikh.java_pdf_annotations.presentation;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.technikh.java_pdf_annotations.R;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotation;

import java.io.File;
import java.io.InputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private MainViewModel viewModel;

    private final ActivityResultLauncher<String> getContent = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                if (uris != null) {
                    if (uris.size() != 5) {
                        Toast.makeText(this, "Please select exactly 5 images", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    viewModel.processImagesAndGeneratePdf(uris);
                }
            }
    );

    private final ActivityResultLauncher<String> getPdfFile = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    extractAnnotationsFromPdf(uri);
                }
            }
    );

    private EditText etAnnotationText;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch switchToggleableAnnotations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PDFBoxResourceLoader.init(getApplicationContext());

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        etAnnotationText = findViewById(R.id.et_annotation_text);
        switchToggleableAnnotations = findViewById(R.id.switch_toggleable_annotations);

        setupObservers();
        setupClickListeners();

        handleIncomingPdfIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIncomingPdfIntent(intent);
    }

    private void handleIncomingPdfIntent(Intent intent) {
        String action = intent.getAction();
        Uri data = null;

        if (Intent.ACTION_VIEW.equals(action)) {
            data = intent.getData();
        } else if (Intent.ACTION_SEND.equals(action)) {
            data = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        }

        if (data != null && "application/pdf".equals(intent.getType())) {
            extractAnnotationsFromPdf(data);
        }
    }

    private void setupClickListeners() {
        switchToggleableAnnotations.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setUseToggleableAnnotations(isChecked);
            Toast.makeText(this,
                    isChecked ? "Using toggleable annotations" : "Using standard annotations",
                    Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btn_generate_pdf).setOnClickListener(v -> {
            String annotationText = etAnnotationText.getText().toString().trim();
            if (annotationText.isEmpty()) {
                Toast.makeText(this, "Please enter annotation text", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.setAnnotationText(annotationText);
            getContent.launch("image/*");
        });

        findViewById(R.id.btn_select_pdf).setOnClickListener(v -> {
            getPdfFile.launch("application/pdf");
        });
    }

    private void setupObservers() {
        viewModel.getIsPdfGenerated().observe(this, isGenerated -> {
            if (isGenerated) {
                openPdfFile(new File(viewModel.getLastGeneratedPdfPath()));
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openPdfFile(File pdfFile) {
        try {
            Uri pdfUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".provider",
                    pdfFile);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(intent, "Open PDF with..."));
        } catch (Exception e) {
            Toast.makeText(this, "Error opening PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void extractAnnotationsFromPdf(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             PDDocument document = PDDocument.load(inputStream)) {

            LinearLayout layout = findViewById(R.id.layout_annotations);
            layout.removeAllViews();

            int pageIndex = 1;
            for (PDPage page : document.getPages()) {
                List<PDAnnotation> annotations = page.getAnnotations();
                for (PDAnnotation annotation : annotations) {
                    String subType = annotation.getSubtype();
                    if ("Text".equalsIgnoreCase(subType) || "FreeText".equalsIgnoreCase(subType)) {
                        String content = annotation.getContents();
                        String title = annotation.getCOSObject().getString("T");
                        String modifiedDate = annotation.getModifiedDate();

                        if (content != null && !content.trim().isEmpty()) {
                            LinearLayout cardLayout = new LinearLayout(this);
                            cardLayout.setOrientation(LinearLayout.VERTICAL);
                            cardLayout.setPadding(24, 16, 24, 16);
                            cardLayout.setBackgroundColor(0xFFF1F1F1);

                            TextView tv = new TextView(this);
                            StringBuilder display = new StringBuilder();
                            display.append("Page ").append(pageIndex).append("\n");
                            if (title != null) display.append("📝 ").append(title).append("\n");
                            display.append(content).append("\n");
                            if (modifiedDate != null)
                                display.append("📅 ").append(modifiedDate.replace("D:", "")).append("\n");
                            tv.setText(display.toString());
                            tv.setTextSize(15f);
                            tv.setPadding(0, 0, 0, 8);

                            // Buttons layout
                            LinearLayout buttonLayout = new LinearLayout(this);
                            buttonLayout.setOrientation(LinearLayout.HORIZONTAL);

                            // Copy button
                            Button btnCopy = new Button(this);
                            btnCopy.setText("Copy");
                            btnCopy.setTextSize(14f);
                            btnCopy.setOnClickListener(view -> {
                                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText("annotation", content);
                                clipboard.setPrimaryClip(clip);
                                Toast.makeText(this, "Copied to clipboard!", Toast.LENGTH_SHORT).show();
                            });

                            // Share button
                            Button btnShare = new Button(this);
                            btnShare.setText("Share");
                            btnShare.setTextSize(14f);
                            btnShare.setOnClickListener(view -> {
                                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                shareIntent.setType("text/plain");
                                shareIntent.putExtra(Intent.EXTRA_TEXT, content);
                                startActivity(Intent.createChooser(shareIntent, "Share annotation via"));
                            });

                            // Add buttons to button layout
                            buttonLayout.addView(btnCopy);
                            buttonLayout.addView(btnShare);

                            // Add views to card
                            cardLayout.addView(tv);
                            cardLayout.addView(buttonLayout);
                            layout.addView(cardLayout);
                        }
                    }
                }
                pageIndex++;
            }

            if (layout.getChildCount() == 0) {
                Toast.makeText(this, "No visible annotations found.", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
