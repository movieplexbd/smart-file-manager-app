package com.smart.filemanager.filehandling;

import java.io.File;
import java.util.Date;

public class FileItem {
    private final File file;
    private boolean isSelected;
    private boolean isFavorite;

    public FileItem(File file) {
        this.file = file;
        this.isSelected = false;
        this.isFavorite = false;
    }

    public File getFile() { return file; }

    public String getName() { return file.getName(); }

    public String getPath() { return file.getAbsolutePath(); }

    public boolean isDirectory() { return file.isDirectory(); }

    public long getSize() { return file.isDirectory() ? 0 : file.length(); }

    public long getLastModified() { return file.lastModified(); }

    public Date getLastModifiedDate() { return new Date(file.lastModified()); }

    public boolean isHidden() { return file.isHidden() || file.getName().startsWith("."); }

    public boolean isSelected() { return isSelected; }

    public void setSelected(boolean selected) { this.isSelected = selected; }

    public boolean isFavorite() { return isFavorite; }

    public void setFavorite(boolean favorite) { this.isFavorite = favorite; }

    public FileType getFileType() {
        if (isDirectory()) return FileType.FOLDER;
        String name = getName().toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
                || name.endsWith(".gif") || name.endsWith(".bmp") || name.endsWith(".webp")) {
            return FileType.IMAGE;
        } else if (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi")
                || name.endsWith(".mov") || name.endsWith(".3gp")) {
            return FileType.VIDEO;
        } else if (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".aac")
                || name.endsWith(".flac") || name.endsWith(".ogg")) {
            return FileType.AUDIO;
        } else if (name.endsWith(".pdf")) {
            return FileType.PDF;
        } else if (name.endsWith(".doc") || name.endsWith(".docx") || name.endsWith(".xls")
                || name.endsWith(".xlsx") || name.endsWith(".ppt") || name.endsWith(".pptx")) {
            return FileType.DOCUMENT;
        } else if (name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z")
                || name.endsWith(".tar") || name.endsWith(".gz")) {
            return FileType.ARCHIVE;
        } else if (name.endsWith(".apk")) {
            return FileType.APK;
        } else if (name.endsWith(".txt") || name.endsWith(".log") || name.endsWith(".xml")
                || name.endsWith(".json") || name.endsWith(".md")) {
            return FileType.TEXT;
        }
        return FileType.UNKNOWN;
    }

    public String getFormattedSize() {
        long size = getSize();
        if (size < 1024) return size + " B";
        else if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        else if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        else return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    public enum FileType {
        FOLDER, IMAGE, VIDEO, AUDIO, PDF, DOCUMENT, ARCHIVE, APK, TEXT, UNKNOWN
    }
}
