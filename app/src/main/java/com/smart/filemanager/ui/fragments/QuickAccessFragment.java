package com.smart.filemanager.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.smart.filemanager.R;
import com.smart.filemanager.filehandling.FileItem;

public class QuickAccessFragment extends Fragment {

    public interface OnCategoryClickListener {
        void onCategorySelected(FileItem.FileType type, String categoryName);
    }

    private OnCategoryClickListener listener;

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quick_access, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.card_images).setOnClickListener(v ->
                notifyCategory(FileItem.FileType.IMAGE, "Images"));
        view.findViewById(R.id.card_videos).setOnClickListener(v ->
                notifyCategory(FileItem.FileType.VIDEO, "Videos"));
        view.findViewById(R.id.card_audio).setOnClickListener(v ->
                notifyCategory(FileItem.FileType.AUDIO, "Audio"));
        view.findViewById(R.id.card_documents).setOnClickListener(v ->
                notifyCategory(FileItem.FileType.DOCUMENT, "Documents"));
        view.findViewById(R.id.card_archives).setOnClickListener(v ->
                notifyCategory(FileItem.FileType.ARCHIVE, "Archives"));
        view.findViewById(R.id.card_apks).setOnClickListener(v ->
                notifyCategory(FileItem.FileType.APK, "APK Files"));
    }

    private void notifyCategory(FileItem.FileType type, String name) {
        if (listener != null) listener.onCategorySelected(type, name);
    }
}
