package com.smart.filemanager.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.smart.filemanager.R;
import com.smart.filemanager.filehandling.FileItem;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class FileAdapter extends ListAdapter<FileItem, FileAdapter.FileViewHolder> {

    public interface OnFileClickListener {
        void onFileClick(FileItem item);
        void onFileLongClick(FileItem item);
        void onFavoriteClick(FileItem item);
    }

    private OnFileClickListener listener;
    private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public FileAdapter() {
        super(new DiffUtil.ItemCallback<FileItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull FileItem a, @NonNull FileItem b) {
                return a.getPath().equals(b.getPath());
            }
            @Override
            public boolean areContentsTheSame(@NonNull FileItem a, @NonNull FileItem b) {
                return a.getPath().equals(b.getPath())
                        && a.getLastModified() == b.getLastModified()
                        && a.isFavorite() == b.isFavorite()
                        && a.isSelected() == b.isSelected();
            }
        });
    }

    public void setOnFileClickListener(OnFileClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileItem item = getItem(position);
        holder.bind(item);
    }

    class FileViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivIcon;
        private final ImageView ivFavorite;
        private final TextView tvName;
        private final TextView tvInfo;
        private final View selectionOverlay;

        FileViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_file_icon);
            ivFavorite = itemView.findViewById(R.id.iv_favorite);
            tvName = itemView.findViewById(R.id.tv_file_name);
            tvInfo = itemView.findViewById(R.id.tv_file_info);
            selectionOverlay = itemView.findViewById(R.id.selection_overlay);
        }

        void bind(FileItem item) {
            tvName.setText(item.getName());

            String info;
            if (item.isDirectory()) {
                info = "Folder";
            } else {
                info = item.getFormattedSize() + "  •  " + sdf.format(item.getLastModifiedDate());
            }
            tvInfo.setText(info);

            ivIcon.setImageResource(getIconRes(item.getFileType()));
            ivFavorite.setImageResource(item.isFavorite()
                    ? R.drawable.ic_favorite_filled
                    : R.drawable.ic_favorite_outline);

            selectionOverlay.setVisibility(item.isSelected() ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onFileClick(item);
            });
            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onFileLongClick(item);
                return true;
            });
            ivFavorite.setOnClickListener(v -> {
                if (listener != null) listener.onFavoriteClick(item);
            });
        }

        private int getIconRes(FileItem.FileType type) {
            switch (type) {
                case FOLDER: return R.drawable.ic_folder;
                case IMAGE: return R.drawable.ic_image;
                case VIDEO: return R.drawable.ic_video;
                case AUDIO: return R.drawable.ic_audio;
                case PDF: return R.drawable.ic_pdf;
                case DOCUMENT: return R.drawable.ic_document;
                case ARCHIVE: return R.drawable.ic_archive;
                case APK: return R.drawable.ic_apk;
                case TEXT: return R.drawable.ic_text;
                default: return R.drawable.ic_file;
            }
        }
    }
}
