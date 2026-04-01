package com.smart.filemanager.filehandling;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileManager {

    private static final String PREFS_NAME = "SmartFileManager";
    private static final String PREFS_FAVORITES = "favorites";
    private static final String PREFS_RECENTS = "recents";
    private static final int MAX_RECENTS = 50;

    private final SharedPreferences prefs;

    public enum SortOrder { NAME, SIZE, DATE }

    public FileManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public File getRootDirectory() {
        return Environment.getExternalStorageDirectory();
    }

    public List<FileItem> listFiles(File directory, boolean showHidden, SortOrder sortOrder) {
        List<FileItem> items = new ArrayList<>();
        if (directory == null || !directory.exists() || !directory.isDirectory()) return items;

        File[] files = directory.listFiles();
        if (files == null) return items;

        Set<String> favorites = getFavorites();
        for (File file : files) {
            if (!showHidden && (file.isHidden() || file.getName().startsWith("."))) continue;
            FileItem item = new FileItem(file);
            item.setFavorite(favorites.contains(file.getAbsolutePath()));
            items.add(item);
        }

        sort(items, sortOrder);
        return items;
    }

    private void sort(List<FileItem> items, SortOrder order) {
        items.sort((a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;
            switch (order) {
                case SIZE: return Long.compare(a.getSize(), b.getSize());
                case DATE: return Long.compare(b.getLastModified(), a.getLastModified());
                default: return a.getName().compareToIgnoreCase(b.getName());
            }
        });
    }

    public List<FileItem> searchFiles(File root, String query, boolean showHidden) {
        List<FileItem> results = new ArrayList<>();
        if (root == null || query == null || query.isEmpty()) return results;
        searchRecursive(root, query.toLowerCase(), results, showHidden);
        return results;
    }

    private void searchRecursive(File dir, String query, List<FileItem> results, boolean showHidden) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (!showHidden && (file.isHidden() || file.getName().startsWith("."))) continue;
            if (file.getName().toLowerCase().contains(query)) {
                results.add(new FileItem(file));
            }
            if (file.isDirectory()) {
                searchRecursive(file, query, results, showHidden);
            }
        }
    }

    public boolean deleteFile(File file) {
        if (file.isDirectory()) {
            return deleteDirectory(file);
        }
        removeFromRecents(file.getAbsolutePath());
        return file.delete();
    }

    private boolean deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) deleteDirectory(f);
        }
        return dir.delete();
    }

    public boolean renameFile(File file, String newName) {
        File renamed = new File(file.getParent(), newName);
        return file.renameTo(renamed);
    }

    public boolean copyFile(File source, File destDir) throws IOException {
        File dest = new File(destDir, source.getName());
        if (source.isDirectory()) {
            dest.mkdirs();
            File[] files = source.listFiles();
            if (files != null) {
                for (File f : files) copyFile(f, dest);
            }
            return true;
        }
        try (FileInputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(dest);
             FileChannel inCh = in.getChannel();
             FileChannel outCh = out.getChannel()) {
            inCh.transferTo(0, inCh.size(), outCh);
        }
        addToRecents(dest.getAbsolutePath());
        return true;
    }

    public boolean moveFile(File source, File destDir) throws IOException {
        boolean copied = copyFile(source, destDir);
        if (copied) deleteFile(source);
        return copied;
    }

    public void addToRecents(String path) {
        List<String> recents = new ArrayList<>(getRecents());
        recents.remove(path);
        recents.add(0, path);
        if (recents.size() > MAX_RECENTS) recents = recents.subList(0, MAX_RECENTS);
        prefs.edit().putStringSet(PREFS_RECENTS, new HashSet<>(recents)).apply();
    }

    private void removeFromRecents(String path) {
        List<String> recents = new ArrayList<>(getRecents());
        recents.remove(path);
        prefs.edit().putStringSet(PREFS_RECENTS, new HashSet<>(recents)).apply();
    }

    public List<FileItem> getRecentFiles() {
        List<FileItem> items = new ArrayList<>();
        for (String path : getRecents()) {
            File f = new File(path);
            if (f.exists()) items.add(new FileItem(f));
        }
        return items;
    }

    private Set<String> getRecents() {
        return prefs.getStringSet(PREFS_RECENTS, new HashSet<>());
    }

    public void toggleFavorite(String path) {
        Set<String> favs = new HashSet<>(getFavorites());
        if (favs.contains(path)) favs.remove(path);
        else favs.add(path);
        prefs.edit().putStringSet(PREFS_FAVORITES, favs).apply();
    }

    public boolean isFavorite(String path) {
        return getFavorites().contains(path);
    }

    public List<FileItem> getFavoriteFiles() {
        List<FileItem> items = new ArrayList<>();
        for (String path : getFavorites()) {
            File f = new File(path);
            if (f.exists()) items.add(new FileItem(f));
        }
        return items;
    }

    private Set<String> getFavorites() {
        return prefs.getStringSet(PREFS_FAVORITES, new HashSet<>());
    }

    public long[] getStorageStats() {
        File storage = Environment.getExternalStorageDirectory();
        long total = storage.getTotalSpace();
        long free = storage.getFreeSpace();
        long used = total - free;
        return new long[]{total, used, free};
    }
}
