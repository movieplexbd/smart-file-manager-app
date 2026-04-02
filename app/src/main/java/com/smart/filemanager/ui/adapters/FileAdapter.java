package com.smart.filemanager.ui.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.smart.filemanager.R;
import com.smart.filemanager.filehandling.FileItem;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FileAdapter extends ListAdapter<FileItem, RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_LIST = 0;
    private static final int VIEW_TYPE_GRID = 1;

    public interface OnFileActionListener {
        void onFileClick(FileItem item);
        void onFileLongClick(FileItem item);
        void onFavoriteClick(FileItem item);
        void onSelectionChanged(int count, List<FileItem> selectedItems);
    }

    private OnFileActionListener listener;
    private boolean isGridMode = false;
    private boolean isMultiSelectMode = false;
    private final Set<String> selectedPaths = new HashSet<>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());

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
                        && a.isFavorite() == b.isFavorite();
            }
        });
    }

    public void setOnFileActionListener(OnFileActionListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return isGridMode ? VIEW_TYPE_GRID : VIEW_TYPE_LIST;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_GRID) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_file_grid, parent, false);
            return new GridViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_file, parent, false);
            return new ListViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        FileItem item = getItem(position);
        boolean isSelected = selectedPaths.contains(item.getPath());
        if (holder instanceof ListViewHolder) {
            ((ListViewHolder) holder).bind(item, isSelected);
        } else if (holder instanceof GridViewHolder) {
            ((GridViewHolder) holder).bind(item, isSelected);
        }
    }

    private void handleClick(FileItem item) {
        if (isMultiSelectMode) {
            toggleSelection(item);
        } else {
            if (listener != null) listener.onFileClick(item);
        }
    }

    private void handleLongClick(FileItem item) {
        if (!isMultiSelectMode) {
            isMultiSelectMode = true;
            toggleSelection(item);
        } else {
            if (listener != null) listener.onFileLongClick(item);
        }
    }

    private void toggleSelection(FileItem item) {
        String path = item.getPath();
        if (selectedPaths.contains(path)) {
            selectedPaths.remove(path);
        } else {
            selectedPaths.add(path);
        }
        if (selectedPaths.isEmpty()) {
            isMultiSelectMode = false;
        }
        notifyDataSetChanged();
        if (listener != null) {
            listener.onSelectionChanged(selectedPaths.size(), getSelectedItems());
        }
    }

    public void setGridMode(boolean gridMode) {
        if (this.isGridMode != gridMode) {
            this.isGridMode = gridMode;
            notifyDataSetChanged();
        }
    }

    public boolean isGridMode() { return isGridMode; }
    public boolean isInMultiSelectMode() { return isMultiSelectMode; }

    public void exitMultiSelectMode() {
        isMultiSelectMode = false;
        selectedPaths.clear();
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionChanged(0, new ArrayList<>());
    }

    public List<FileItem> getSelectedItems() {
        List<FileItem> selected = new ArrayList<>();
        for (FileItem item : getCurrentList()) {
            if (selectedPaths.contains(item.getPath())) {
                selected.add(item);
            }
        }
        return selected;
    }

    public void selectAll() {
        isMultiSelectMode = true;
        for (FileItem item : getCurrentList()) {
            selectedPaths.add(item.getPath());
        }
        notifyDataSetChanged();
        if (listener != null) {
            listener.onSelectionChanged(selectedPaths.size(), getSelectedItems());
        }
    }

    private int getTypeColor(Context ctx, FileItem.FileType type) {
        switch (type) {
            case FOLDER: return ContextCompat.getColor(ctx, R.color.color_folder);
            case IMAGE: return ContextCompat.getColor(ctx, R.color.color_image);
            case VIDEO: return ContextCompat.getColor(ctx, R.color.color_video);
            case AUDIO: return ContextCompat.getColor(ctx, R.color.color_audio);
            case PDF: return ContextCompat.getColor(ctx, R.color.color_pdf);
            case DOCUMENT: return ContextCompat.getColor(ctx, R.color.color_document);
            case ARCHIVE: return ContextCompat.getColor(ctx, R.color.color_archive);
            case APK: return ContextCompat.getColor(ctx, R.color.color_apk);
            case TEXT: return ContextCompat.getColor(ctx, R.color.color_text);
            default: return ContextCompat.getColor(ctx, R.color.color_unknown);
        }
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

    private void applyIconStyle(Context ctx, FrameLayout container, ImageView icon, FileItem item) {
        int typeColor = getTypeColor(ctx, item.getFileType());
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.argb(30, Color.red(typeColor), Color.green(typeColor), Color.blue(typeColor)));
        container.setBackground(circle);
        icon.setImageResource(getIconRes(item.getFileType()));
        icon.setColorFilter(typeColor, android.graphics.PorterDuff.Mode.SRC_IN);
    }

    class ListViewHolder extends RecyclerView.ViewHolder {
        final FrameLayout iconContainer;
        final ImageView ivIcon, ivFavorite, ivCheck;
        final TextView tvName, tvInfo;
        final View selectionOverlay;

        ListViewHolder(@NonNull View itemView) {
            super(itemView);
            iconContainer = itemView.findViewById(R.id.icon_container);
            ivIcon = itemView.findViewById(R.id.iv_file_icon);
            ivFavorite = itemView.findViewById(R.id.iv_favorite);
            ivCheck = itemView.findViewById(R.id.iv_check);
            tvName = itemView.findViewById(R.id.tv_file_name);
            tvInfo = itemView.findViewById(R.id.tv_file_info);
            selectionOverlay = itemView.findViewById(R.id.selection_overlay);
        }

        void bind(FileItem item, boolean isSelected) {
            Context ctx = itemView.getContext();
            tvName.setText(item.getName());

            if (item.isDirectory()) {
                File[] children = item.getFile().listFiles();
                int count = children != null ? children.length : 0;
                tvInfo.setText(count + " item" + (count != 1 ? "s" : ""));
            } else {
                tvInfo.setText(item.getFormattedSize() + "  •  " + sdf.format(item.getLastModifiedDate()));
            }

            applyIconStyle(ctx, iconContainer, ivIcon, item);

            ivFavorite.setImageResource(item.isFavorite()
                    ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_outline);

            selectionOverlay.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            ivCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> handleClick(item));
            itemView.setOnLongClickListener(v -> { handleLongClick(item); return true; });
            ivFavorite.setOnClickListener(v -> {
                if (!isMultiSelectMode && listener != null) listener.onFavoriteClick(item);
            });
        }
    }

    class GridViewHolder extends RecyclerView.ViewHolder {
        final FrameLayout iconContainer;
        final ImageView ivIcon, ivCheck;
        final TextView tvName, tvInfo;
        final View selectionOverlay;

        GridViewHolder(@NonNull View itemView) {
            super(itemView);
            iconContainer = itemView.findViewById(R.id.icon_container);
            ivIcon = itemView.findViewById(R.id.iv_file_icon);
            ivCheck = itemView.findViewById(R.id.iv_check);
            tvName = itemView.findViewById(R.id.tv_file_name);
            tvInfo = itemView.findViewById(R.id.tv_file_info);
            selectionOverlay = itemView.findViewById(R.id.selection_overlay);
        }

        void bind(FileItem item, boolean isSelected) {
            Context ctx = itemView.getContext();
            tvName.setText(item.getName());

            if (item.isDirectory()) {
                File[] children = item.getFile().listFiles();
                int count = children != null ? children.length : 0;
                tvInfo.setText(count + " items");
            } else {
                tvInfo.setText(item.getFormattedSize());
            }

            applyIconStyle(ctx, iconContainer, ivIcon, item);

            selectionOverlay.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            ivCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> handleClick(item));
            itemView.setOnLongClickListener(v -> { handleLongClick(item); return true; });
        }
    }
}
