package com.technikh.java_pdf_annotations.presentation;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.technikh.java_pdf_annotations.R;

import java.io.File;

/**
 * Main activity for PDF annotation application
 */
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
    private EditText etAnnotationText;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch switchToggleableAnnotations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        etAnnotationText = findViewById(R.id.et_annotation_text);
        switchToggleableAnnotations = findViewById(R.id.switch_toggleable_annotations);

        setupObservers();
        setupClickListeners();
    }

    /**
     * Sets up click listeners for UI elements
     */
    private void setupClickListeners() {
        // Toggle switch listener
        switchToggleableAnnotations.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setUseToggleableAnnotations(isChecked);
            String message = isChecked ?
                    "Using toggleable annotations" :
                    "Using standard annotations";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });

        // Generate PDF button
        findViewById(R.id.btn_generate_pdf).setOnClickListener(v -> {
            String annotationText = etAnnotationText.getText().toString().trim();
            if (annotationText.isEmpty()) {
                Toast.makeText(this, "Please enter annotation text", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.setAnnotationText(annotationText);
            getContent.launch("image/*");
        });
    }

    /**
     * Sets up LiveData observers
     */
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

    /**
     * Opens the generated PDF file in a PDF viewer
     */
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
}