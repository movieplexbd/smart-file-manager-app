package com.smart.filemanager.logic;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.smart.filemanager.filehandling.FileItem;
import com.smart.filemanager.filehandling.FileManager;
import com.smart.filemanager.filehandling.ZipManager;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileViewModel extends AndroidViewModel {

    private final FileManager fileManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<List<FileItem>> fileList = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<File> currentDirectory = new MutableLiveData<>();
    private final MutableLiveData<List<FileItem>> searchResults = new MutableLiveData<>();
    private final MutableLiveData<List<FileItem>> recentFiles = new MutableLiveData<>();
    private final MutableLiveData<List<FileItem>> favoriteFiles = new MutableLiveData<>();
    private final MutableLiveData<long[]> storageStats = new MutableLiveData<>();
    private final MutableLiveData<List<FileItem>> categoryFiles = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();

    private final Deque<File> backStack = new ArrayDeque<>();
    private boolean showHidden = false;
    private FileManager.SortOrder sortOrder = FileManager.SortOrder.NAME;
    private boolean sortDescending = false;

    public FileViewModel(@NonNull Application application) {
        super(application);
        fileManager = new FileManager(application);
    }

    public LiveData<List<FileItem>> getFileList() { return fileList; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<File> getCurrentDirectory() { return currentDirectory; }
    public LiveData<List<FileItem>> getSearchResults() { return searchResults; }
    public LiveData<List<FileItem>> getRecentFiles() { return recentFiles; }
    public LiveData<List<FileItem>> getFavoriteFiles() { return favoriteFiles; }
    public LiveData<long[]> getStorageStats() { return storageStats; }
    public LiveData<List<FileItem>> getCategoryFiles() { return categoryFiles; }
    public LiveData<String> getSuccessMessage() { return successMessage; }

    public void loadRoot() {
        navigateTo(fileManager.getRootDirectory());
    }

    public void navigateTo(File directory) {
        File current = currentDirectory.getValue();
        if (current != null && !current.equals(directory)) {
            backStack.push(current);
        }
        currentDirectory.setValue(directory);
        loadFiles(directory);
    }

    public boolean navigateBack() {
        if (backStack.isEmpty()) return false;
        File parent = backStack.pop();
        currentDirectory.setValue(parent);
        loadFiles(parent);
        return true;
    }

    private void loadFiles(File directory) {
        isLoading.setValue(true);
        executor.execute(() -> {
            List<FileItem> items = fileManager.listFiles(directory, showHidden, sortOrder, sortDescending);
            fileList.postValue(items);
            isLoading.postValue(false);
        });
    }

    public void search(String query) {
        File root = fileManager.getRootDirectory();
        isLoading.setValue(true);
        executor.execute(() -> {
            List<FileItem> results = fileManager.searchFiles(root, query, showHidden);
            searchResults.postValue(results);
            isLoading.postValue(false);
        });
    }

    public void loadByCategory(FileItem.FileType type) {
        File root = fileManager.getRootDirectory();
        isLoading.setValue(true);
        executor.execute(() -> {
            List<FileItem> results = fileManager.getFilesByCategory(root, type);
            categoryFiles.postValue(results);
            isLoading.postValue(false);
        });
    }

    public void createFolder(String name) {
        File dir = currentDirectory.getValue();
        if (dir == null) return;
        executor.execute(() -> {
            boolean ok = fileManager.createFolder(dir, name);
            if (ok) {
                reload();
                successMessage.postValue("Folder \"" + name + "\" created");
            } else {
                errorMessage.postValue("Could not create folder");
            }
        });
    }

    public void compressFile(FileItem item) {
        executor.execute(() -> {
            File source = item.getFile();
            File dest = new File(source.getParent(), source.getName() + ".zip");
            try {
                ZipManager.compress(source, dest);
                reload();
                successMessage.postValue("Compressed: " + dest.getName());
            } catch (Exception e) {
                errorMessage.postValue("Compression failed: " + e.getMessage());
            }
        });
    }

    public void extractZip(FileItem item) {
        executor.execute(() -> {
            File zip = item.getFile();
            String baseName = zip.getName().toLowerCase().replace(".zip", "");
            File destDir = new File(zip.getParent(), baseName + "_extracted");
            try {
                ZipManager.extract(zip, destDir);
                reload();
                successMessage.postValue("Extracted to: " + destDir.getName());
            } catch (Exception e) {
                errorMessage.postValue("Extraction failed: " + e.getMessage());
            }
        });
    }

    public void deleteFile(FileItem item) {
        executor.execute(() -> {
            boolean success = fileManager.deleteFile(item.getFile());
            if (success) reload();
        });
    }

    public void renameFile(FileItem item, String newName) {
        executor.execute(() -> {
            boolean success = fileManager.renameFile(item.getFile(), newName);
            if (success) reload();
        });
    }

    public void copyFile(FileItem item, File destDir) {
        executor.execute(() -> {
            try {
                fileManager.copyFile(item.getFile(), destDir);
                reload();
            } catch (Exception e) {
                errorMessage.postValue("Copy failed: " + e.getMessage());
            }
        });
    }

    public void moveFile(FileItem item, File destDir) {
        executor.execute(() -> {
            try {
                fileManager.moveFile(item.getFile(), destDir);
                reload();
            } catch (Exception e) {
                errorMessage.postValue("Move failed: " + e.getMessage());
            }
        });
    }

    public void toggleFavorite(FileItem item) {
        fileManager.toggleFavorite(item.getPath());
        reload();
        loadFavorites();
    }

    public void loadRecents() {
        executor.execute(() -> recentFiles.postValue(fileManager.getRecentFiles()));
    }

    public void loadFavorites() {
        executor.execute(() -> favoriteFiles.postValue(fileManager.getFavoriteFiles()));
    }

    public void loadStorageStats() {
        executor.execute(() -> storageStats.postValue(fileManager.getStorageStats()));
    }

    public void setShowHidden(boolean showHidden) {
        this.showHidden = showHidden;
        reload();
    }

    public void setSortOrder(FileManager.SortOrder order) {
        this.sortOrder = order;
        reload();
    }

    public void setSortDescending(boolean descending) {
        this.sortDescending = descending;
        reload();
    }

    public boolean isShowHidden() { return showHidden; }
    public boolean isSortDescending() { return sortDescending; }

    private void reload() {
        File dir = currentDirectory.getValue();
        if (dir != null) loadFiles(dir);
    }

    public boolean canGoBack() {
        return !backStack.isEmpty();
    }

    public void addToRecents(String path) {
        fileManager.addToRecents(path);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
