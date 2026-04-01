package com.smart.filemanager.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.smart.filemanager.R;
import com.smart.filemanager.filehandling.FileItem;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class FileDetailActivity extends AppCompatActivity {

    public static final String EXTRA_FILE_PATH = "extra_file_path";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String path = getIntent().getStringExtra(EXTRA_FILE_PATH);
        if (path == null) { finish(); return; }

        File file = new File(path);
        FileItem item = new FileItem(file);

        setTitle(item.getName());

        TextView tvName = findViewById(R.id.tv_detail_name);
        TextView tvPath = findViewById(R.id.tv_detail_path);
        TextView tvSize = findViewById(R.id.tv_detail_size);
        TextView tvDate = findViewById(R.id.tv_detail_date);
        TextView tvType = findViewById(R.id.tv_detail_type);
        MaterialButton btnOpen = findViewById(R.id.btn_open);
        MaterialButton btnShare = findViewById(R.id.btn_share);

        tvName.setText(item.getName());
        tvPath.setText(item.getPath());
        tvSize.setText(item.getFormattedSize());
        tvType.setText(item.getFileType().name());

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy  HH:mm", Locale.getDefault());
        tvDate.setText(sdf.format(item.getLastModifiedDate()));

        btnOpen.setOnClickListener(v -> openFile(file, item));
        btnShare.setOnClickListener(v -> shareFile(file));
    }

    private void openFile(File file, FileItem item) {
        if (item.getFileType() == FileItem.FileType.APK) {
            installApk(file);
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, getMimeType(item));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Open with"));
        } catch (Exception e) {
            Toast.makeText(this, "No app to open this file", Toast.LENGTH_SHORT).show();
        }
    }

    private void installApk(File file) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    private void shareFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setType("*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share via"));
        } catch (Exception e) {
            Toast.makeText(this, "Could not share file", Toast.LENGTH_SHORT).show();
        }
    }

    private String getMimeType(FileItem item) {
        switch (item.getFileType()) {
            case IMAGE: return "image/*";
            case VIDEO: return "video/*";
            case AUDIO: return "audio/*";
            case PDF: return "application/pdf";
            case DOCUMENT: return "application/msword";
            case TEXT: return "text/plain";
            default: return "*/*";
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
