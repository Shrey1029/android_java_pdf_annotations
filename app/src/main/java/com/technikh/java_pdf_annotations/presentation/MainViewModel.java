package com.technikh.java_pdf_annotations.presentation;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.technikh.java_pdf_annotations.domain.PdfGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for PDF annotation operations
 */
public class MainViewModel extends AndroidViewModel {
    private static final String TAG = "MainViewModel";
    private final PdfGenerator pdfGenerator;
    private final ExecutorService executorService;
    private final MutableLiveData<Boolean> isPdfGenerated = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private String annotationText = "made in India";
    private File lastGeneratedPdf;
    private boolean useToggleableAnnotations = true;

    public MainViewModel(Application application) {
        super(application);
        pdfGenerator = new PdfGenerator(application);
        executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Sets the text to use for annotations
     */
    public void setAnnotationText(String text) {
        this.annotationText = text != null ? text : "made in India";
    }

    /**
     * Controls whether to use toggleable annotations
     */
    public void setUseToggleableAnnotations(boolean useToggleableAnnotations) {
        this.useToggleableAnnotations = useToggleableAnnotations;
    }

    /**
     * Returns whether toggleable annotations are enabled
     */
    public boolean isUsingToggleableAnnotations() {
        return useToggleableAnnotations;
    }

    /**
     * Returns the path to the last generated PDF
     */
    public String getLastGeneratedPdfPath() {
        return lastGeneratedPdf != null ? lastGeneratedPdf.getAbsolutePath() : "";
    }

    /**
     * LiveData indicating if a PDF has been generated
     */
    public LiveData<Boolean> getIsPdfGenerated() {
        return isPdfGenerated;
    }

    /**
     * LiveData for error messages
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Processes selected images and generates a PDF with annotations
     */
    public void processImagesAndGeneratePdf(List<Uri> imageUris) {
        if (imageUris == null || imageUris.isEmpty()) {
            errorMessage.postValue("No images provided");
            return;
        }

        executorService.execute(() -> {
            try {
                List<String> processedImagePaths = new ArrayList<>();

                for (Uri imageUri : imageUris) {
                    if (imageUri != null) {
                        String imagePath = processImage(imageUri);
                        if (imagePath != null) {
                            processedImagePaths.add(imagePath);
                        }
                    }
                }

                if (processedImagePaths.size() == 5) {
                    generatePdf(processedImagePaths);
                } else {
                    errorMessage.postValue("Error: Expected 5 images but processed " + processedImagePaths.size());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing images", e);
                errorMessage.postValue("Error: " + e.getMessage());
            }
        });
    }

    /**
     * Processes an image URI and saves it to a temporary file
     */
    private String processImage(Uri imageUri) {
        InputStream in = null;
        OutputStream out = null;
        File imageFile;

        try {
            String fileName = "image_" + System.currentTimeMillis() + ".jpg";
            imageFile = new File(getApplication().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), fileName);

            in = getApplication().getContentResolver().openInputStream(imageUri);
            if (in == null) {
                Log.e(TAG, "Failed to open input stream for URI: " + imageUri);
                return null;
            }

            out = new FileOutputStream(imageFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            return imageFile.getAbsolutePath();

        } catch (IOException e) {
            Log.e(TAG, "Error processing image: " + imageUri, e);
            return null;
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing streams", e);
            }
        }
    }

    /**
     * Generates a PDF with the processed images and annotations
     */
    private void generatePdf(List<String> imagePaths) throws IOException {
        File pdfDir = new File(getApplication().getExternalFilesDir(null), "pdfs");
        if (!pdfDir.exists() && !pdfDir.mkdirs()) {
            throw new IOException("Failed to create PDF directory");
        }

        lastGeneratedPdf = new File(pdfDir, "annotated_images_" + System.currentTimeMillis() + ".pdf");

        // appropriate method based on the toggle setting
        if (useToggleableAnnotations) {
            pdfGenerator.createPdfWithToggleableAnnotations(imagePaths, lastGeneratedPdf, annotationText);
        } else {
            pdfGenerator.createPdfWithAnnotations(imagePaths, lastGeneratedPdf, annotationText);
        }

        isPdfGenerated.postValue(true);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}