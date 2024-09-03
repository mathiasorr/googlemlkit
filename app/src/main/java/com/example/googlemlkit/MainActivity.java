package com.example.googlemlkit;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView txtResults;
    private Bitmap mSelectedImage;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CHOOSE_IMAGE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.image_view);
        txtResults = findViewById(R.id.txtresults);

        Button btGallery = findViewById(R.id.btGallery);
        Button btCamera = findViewById(R.id.btCamera);
        Button btText = findViewById(R.id.btText);
        Button btFace = findViewById(R.id.btFace);
        Button btLabel = findViewById(R.id.btLabel);

        btLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanBarcode();
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
    }

    public void abrirGaleria(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CHOOSE_IMAGE);
    }

    public void abrirCamera(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                mSelectedImage = (Bitmap) data.getExtras().get("data");
            } else if (requestCode == REQUEST_CHOOSE_IMAGE) {
                try {
                    mSelectedImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            imageView.setImageBitmap(mSelectedImage);
        }
    }

    public void OCRfx(View view) {
        if (mSelectedImage == null) {
            txtResults.setText("Por favor, seleccione una imagen primero.");
            return;
        }
        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text visionText) {
                        txtResults.setText(visionText.getText());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        txtResults.setText("Error: " + e.getMessage());
                    }
                });
    }

    public void Rostrosfx(View view) {
        if (mSelectedImage == null) {
            txtResults.setText("Por favor, seleccione una imagen primero.");
            return;
        }
        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .build();
        FaceDetector detector = FaceDetection.getClient(options);
        detector.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        txtResults.setText("Detectados " + faces.size() + " rostros");
                        drawFaceRectangles(faces);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        txtResults.setText("Error: " + e.getMessage());
                    }
                });
    }

    private void drawFaceRectangles(List<Face> faces) {
        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap().copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);

        for (Face face : faces) {
            canvas.drawRect(face.getBoundingBox(), paint);
        }

        imageView.setImageBitmap(bitmap);
    }

    public void scanBarcode() {
        if (mSelectedImage == null) {
            txtResults.setText("Por favor, seleccione una imagen primero.");
            return;
        }
        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);
        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_QR_CODE,
                                Barcode.FORMAT_AZTEC,
                                Barcode.FORMAT_ALL_FORMATS)
                        .build();
        BarcodeScanner scanner = BarcodeScanning.getClient(options);
        scanner.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                    @Override
                    public void onSuccess(List<Barcode> barcodes) {
                        if (barcodes.isEmpty()) {
                            txtResults.setText("No se detectaron códigos de barras o QR.");
                        } else {
                            StringBuilder resultText = new StringBuilder();
                            for (Barcode barcode : barcodes) {
                                String rawValue = barcode.getRawValue();
                                resultText.append("Valor: ").append(rawValue).append("\n");
                                int valueType = barcode.getValueType();
                                switch (valueType) {
                                    case Barcode.TYPE_URL:
                                        resultText.append("Tipo: URL\n");
                                        break;
                                    case Barcode.TYPE_PRODUCT:
                                        resultText.append("Tipo: Producto\n");
                                        break;

                                    default:
                                        resultText.append("Tipo: Otro\n");
                                }
                                resultText.append("\n");
                            }
                            txtResults.setText(resultText.toString());
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        txtResults.setText("Error en el escaneo: " + e.getMessage());
                    }
                });
    }
}