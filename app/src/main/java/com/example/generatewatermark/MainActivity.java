package com.example.generatewatermark;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private ImageView selectedImageView;
    private Bitmap selectedBitmap;

    private final ActivityResultLauncher<Intent> selectImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    try {
                        selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                        selectedImageView.setImageBitmap(selectedBitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private final ActivityResultLauncher<Intent> captureImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    selectedBitmap = (Bitmap) extras.get("data");
                    selectedImageView.setImageBitmap(selectedBitmap);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectedImageView = findViewById(R.id.selectedImageView);

        Button selectImageButton = findViewById(R.id.selectImageButton);
        selectImageButton.setOnClickListener(v -> openGallery());

        Button captureImageButton = findViewById(R.id.captureImageButton);
        captureImageButton.setOnClickListener(v -> requestCameraPermission());

        Button addWatermarkButton = findViewById(R.id.addWatermarkButton);
        addWatermarkButton.setOnClickListener(v -> addWatermarkToImage());

        Button saveImageButton = findViewById(R.id.saveImageButton);
        saveImageButton.setOnClickListener(v -> saveImageToGallery());
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        selectImageLauncher.launch(galleryIntent);
    }

    private void requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            } else {
                captureImage();
            }
        } else {
            captureImage();
        }
    }

    private void captureImage() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        captureImageLauncher.launch(cameraIntent);
    }

    private void addWatermarkToImage() {
        if (selectedBitmap != null) {
            Bitmap watermarkBitmap = loadWatermarkBitmap();
            if (watermarkBitmap != null) {
                Bitmap watermarkedBitmap = addImageWatermark(selectedBitmap, watermarkBitmap);
                selectedBitmap = watermarkedBitmap;
                selectedImageView.setImageBitmap(watermarkedBitmap);
            } else {
                Toast.makeText(this, "Failed to load watermark image.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please select or capture an image first.", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap addImageWatermark(Bitmap original, Bitmap watermark) {
        Bitmap result = Bitmap.createBitmap(original.getWidth(), original.getHeight(), original.getConfig());
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(original, 0, 0, null);

        // Scale the watermark to fit the original image
        int watermarkWidth = original.getWidth() / 4;
        int watermarkHeight = (watermark.getHeight() * watermarkWidth) / watermark.getWidth();
        Bitmap scaledWatermark = Bitmap.createScaledBitmap(watermark, watermarkWidth, watermarkHeight, true);

        int left = 0; // Left margin (adjust as needed)
        int top = original.getHeight() - watermarkHeight - 20;

        canvas.drawBitmap(scaledWatermark, left, top, null);
        return result;
    }

    private Bitmap loadWatermarkBitmap() {
        try {
            InputStream inputStream = getAssets().open("watermark.png");
            return BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveImageToGallery() {
        if (selectedBitmap != null) {
            File folder = new File(getExternalFilesDir(null), "WatermarkedImages");
            if (!folder.exists()) {
                folder.mkdirs();
            }

            File file = new File(folder, "watermarked_image_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream out = new FileOutputStream(file)) {
                selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), "Watermarked image");
                Toast.makeText(this, "Image saved successfully!", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No image to save. Please add a watermark first.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureImage();
            } else {
                Toast.makeText(this, "Camera permission is required to use this feature.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
