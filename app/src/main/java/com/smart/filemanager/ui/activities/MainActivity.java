package com.smart.filemanager.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.smart.filemanager.R;
import com.smart.filemanager.filehandling.FileItem;
import com.smart.filemanager.filehandling.FileManager;
import com.smart.filemanager.logic.FileViewModel;
import com.smart.filemanager.ui.adapters.FileAdapter;
import com.smart.filemanager.ui.fragments.QuickAccessFragment;
import com.smart.filemanager.ui.fragments.StorageAnalyticsFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION = 1001;
    private static final String PREFS = "SmartFileManagerUI";
    private static final String PREF_DARK_MODE = "dark_mode";
    private static final String PREF_GRID_MODE = "grid_mode";
    private static final String PREF_SHOW_HIDDEN = "show_hidden";

    private FileViewModel viewModel;
    private FileAdapter adapter;

    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private FrameLayout fragmentContainer;
    private ProgressBar progressBar;
    private LinearLayout emptyView;
    private LinearLayout searchContainer;
    private EditText searchBar;
    private HorizontalScrollView breadcrumbScroll;
    private LinearLayout breadcrumbContainer;
    private MaterialCardView selectionBar;
    private TextView tvSelectedCount;
    private BottomNavigationView bottomNav;
    private FloatingActionButton fabSort;

    private SharedPreferences prefs;
    private boolean isDarkMode = false;
    private boolean isGridMode = false;
    private boolean isSearchMode = false;
    private boolean showHidden = false;
    private int currentTab = R.id.nav_files;

    // Feature: File Clipboard
    private FileItem clipboardItem = null;
    private boolean isClipboardCut = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        isDarkMode = prefs.getBoolean(PREF_DARK_MODE, false);
        isGridMode = prefs.getBoolean(PREF_GRID_MODE, false);
        showHidden = prefs.getBoolean(PREF_SHOW_HIDDEN, false);
        applyDarkMode(isDarkMode);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(FileViewModel.class);
        viewModel.setShowHidden(showHidden);

        initViews();
        setupToolbar();
        setupBottomNav();
        setupRecyclerView();
        setupSelectionBar();
        setupFab();
        observeViewModel();
        requestStoragePermission();
    }

    // ─── Init ───────────────────────────────────────────────────────────

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        emptyView = findViewById(R.id.empty_view);
        searchContainer = findViewById(R.id.search_container);
        searchBar = findViewById(R.id.search_bar);
        breadcrumbScroll = findViewById(R.id.breadcrumb_scroll);
        breadcrumbContainer = findViewById(R.id.breadcrumb_container);
        selectionBar = findViewById(R.id.selection_bar);
        tvSelectedCount = findViewById(R.id.tv_selected_count);
        bottomNav = findViewById(R.id.bottom_navigation);
        fabSort = findViewById(R.id.fab_sort);
        fragmentContainer = findViewById(R.id.fragment_container);
    }

    // ─── Toolbar ────────────────────────────────────────────────────────

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.app_name);

        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_search) {
                toggleSearch();
            } else if (id == R.id.menu_toggle_view) {
                toggleGridMode();
            } else if (id == R.id.menu_new_folder) {
                showNewFolderDialog();
            } else if (id == R.id.menu_show_hidden) {
                showHidden = !item.isChecked();
                item.setChecked(showHidden);
                prefs.edit().putBoolean(PREF_SHOW_HIDDEN, showHidden).apply();
                viewModel.setShowHidden(showHidden);
            } else if (id == R.id.menu_dark_mode) {
                isDarkMode = !item.isChecked();
                item.setChecked(isDarkMode);
                prefs.edit().putBoolean(PREF_DARK_MODE, isDarkMode).apply();
                applyDarkMode(isDarkMode);
                recreate();
            } else if (id == R.id.menu_select_all) {
                adapter.selectAll();
            }
            return true;
        });

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                if (s.length() >= 1) {
                    viewModel.search(s.toString());
                } else if (currentTab == R.id.nav_files) {
                    File dir = viewModel.getCurrentDirectory().getValue();
                    if (dir != null) viewModel.navigateTo(dir);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ─── Bottom Navigation ──────────────────────────────────────────────

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(navItem -> {
            currentTab = navItem.getItemId();
            exitSearchMode();
            adapter.exitMultiSelectMode();
            selectionBar.setVisibility(View.GONE);

            if (currentTab == R.id.nav_files) {
                navigateToFilesTab();
            } else if (currentTab == R.id.nav_categories) {
                navigateToCategoriesTab();
            } else if (currentTab == R.id.nav_recents) {
                toolbar.setTitle("Recents");
                breadcrumbScroll.setVisibility(View.GONE);
                showRecyclerView();
                viewModel.loadRecents();
                fabSort.setVisibility(View.GONE);
            } else if (currentTab == R.id.nav_favorites) {
                toolbar.setTitle("Favorites");
                breadcrumbScroll.setVisibility(View.GONE);
                showRecyclerView();
                viewModel.loadFavorites();
                fabSort.setVisibility(View.GONE);
            } else if (currentTab == R.id.nav_storage) {
                toolbar.setTitle("Storage");
                breadcrumbScroll.setVisibility(View.GONE);
                fabSort.setVisibility(View.GONE);
                viewModel.loadStorageStats();
                showFragment(new StorageAnalyticsFragment());
            }
            return true;
        });
    }

    private void navigateToFilesTab() {
        toolbar.setTitle(R.string.app_name);
        breadcrumbScroll.setVisibility(View.VISIBLE);
        fabSort.setVisibility(View.VISIBLE);
        showRecyclerView();
        if (viewModel.getCurrentDirectory().getValue() == null) {
            viewModel.loadRoot();
        } else {
            File dir = viewModel.getCurrentDirectory().getValue();
            viewModel.navigateTo(dir);
        }
    }

    private void navigateToCategoriesTab() {
        toolbar.setTitle("Categories");
        breadcrumbScroll.setVisibility(View.GONE);
        fabSort.setVisibility(View.GONE);

        QuickAccessFragment frag = new QuickAccessFragment();
        frag.setOnCategoryClickListener((type, name) -> {
            toolbar.setTitle(name);
            showRecyclerView();
            fabSort.setVisibility(View.VISIBLE);
            viewModel.loadByCategory(type);
        });
        showFragment(frag);
    }

    // ─── RecyclerView ───────────────────────────────────────────────────

    private void setupRecyclerView() {
        adapter = new FileAdapter();
        updateLayoutManager();
        recyclerView.setAdapter(adapter);

        adapter.setOnFileActionListener(new FileAdapter.OnFileActionListener() {
            @Override
            public void onFileClick(FileItem item) {
                if (item.isDirectory()) {
                    if (currentTab == R.id.nav_files || currentTab == R.id.nav_categories) {
                        currentTab = R.id.nav_files;
                        breadcrumbScroll.setVisibility(View.VISIBLE);
                        fabSort.setVisibility(View.VISIBLE);
                        viewModel.navigateTo(item.getFile());
                        viewModel.addToRecents(item.getPath());
                    }
                } else {
                    viewModel.addToRecents(item.getPath());
                    Intent intent = new Intent(MainActivity.this, FileDetailActivity.class);
                    intent.putExtra(FileDetailActivity.EXTRA_FILE_PATH, item.getPath());
                    startActivity(intent);
                }
            }

            @Override
            public void onFileLongClick(FileItem item) {
                showFileOptionsDialog(item);
            }

            @Override
            public void onFavoriteClick(FileItem item) {
                viewModel.toggleFavorite(item);
            }

            @Override
            public void onSelectionChanged(int count, List<FileItem> selectedItems) {
                if (count > 0) {
                    selectionBar.setVisibility(View.VISIBLE);
                    tvSelectedCount.setText(count + " selected");
                } else {
                    selectionBar.setVisibility(View.GONE);
                }
            }
        });
    }

    private void updateLayoutManager() {
        if (isGridMode) {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
        if (adapter != null) adapter.setGridMode(isGridMode);
    }

    private void showRecyclerView() {
        recyclerView.setVisibility(View.VISIBLE);
        fragmentContainer.setVisibility(View.GONE);
    }

    private void showFragment(androidx.fragment.app.Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        fragmentContainer.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
    }

    // ─── Selection Bar ──────────────────────────────────────────────────

    private void setupSelectionBar() {
        findViewById(R.id.btn_share_selected).setOnClickListener(v -> shareSelectedFiles());
        findViewById(R.id.btn_copy_selected).setOnClickListener(v -> showCopyMoveSelected());
        findViewById(R.id.btn_delete_selected).setOnClickListener(v -> deleteSelectedFiles());
    }

    private void setupFab() {
        fabSort.setOnClickListener(v -> showSortDialog());
    }

    // ─── Observe ViewModel ──────────────────────────────────────────────

    private void observeViewModel() {
        viewModel.getFileList().observe(this, files -> {
            if (currentTab == R.id.nav_files && !isSearchMode) {
                updateList(files);
            }
        });

        viewModel.getSearchResults().observe(this, results -> {
            if (isSearchMode) updateList(results);
        });

        viewModel.getRecentFiles().observe(this, files -> {
            if (currentTab == R.id.nav_recents) updateList(files);
        });

        viewModel.getFavoriteFiles().observe(this, files -> {
            if (currentTab == R.id.nav_favorites) updateList(files);
        });

        viewModel.getCategoryFiles().observe(this, files -> {
            if (currentTab == R.id.nav_categories || currentTab == R.id.nav_files) {
                if (recyclerView.getVisibility() == View.VISIBLE) updateList(files);
            }
        });

        viewModel.getIsLoading().observe(this, loading ->
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        viewModel.getCurrentDirectory().observe(this, dir -> {
            if (dir != null && currentTab == R.id.nav_files) {
                String name = dir.getName();
                if (name.isEmpty() || dir.getPath().equals("/storage/emulated/0")) {
                    name = "Storage";
                }
                toolbar.setTitle(name);
                updateBreadcrumbs(dir);
            }
        });

        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getSuccessMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateList(List<FileItem> files) {
        adapter.submitList(files);
        boolean empty = files == null || files.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        fragmentContainer.setVisibility(View.GONE);
    }

    // ─── Feature 1: Dark Mode ───────────────────────────────────────────

    private void applyDarkMode(boolean dark) {
        AppCompatDelegate.setDefaultNightMode(
                dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    // ─── Feature 2: Grid View Toggle ────────────────────────────────────

    private void toggleGridMode() {
        isGridMode = !isGridMode;
        prefs.edit().putBoolean(PREF_GRID_MODE, isGridMode).apply();
        updateLayoutManager();
        Toast.makeText(this, isGridMode ? "Grid view" : "List view", Toast.LENGTH_SHORT).show();
    }

    // ─── Feature 3: Search ──────────────────────────────────────────────

    private void toggleSearch() {
        isSearchMode = !isSearchMode;
        searchContainer.setVisibility(isSearchMode ? View.VISIBLE : View.GONE);
        if (isSearchMode) {
            searchBar.requestFocus();
        } else {
            searchBar.setText("");
            if (currentTab == R.id.nav_files) {
                File dir = viewModel.getCurrentDirectory().getValue();
                if (dir != null) viewModel.navigateTo(dir);
            }
        }
    }

    private void exitSearchMode() {
        if (isSearchMode) {
            isSearchMode = false;
            searchContainer.setVisibility(View.GONE);
            searchBar.setText("");
        }
    }

    // ─── Feature 4: Breadcrumb Navigation ──────────────────────────────

    private void updateBreadcrumbs(File dir) {
        if (dir == null) return;
        breadcrumbContainer.removeAllViews();

        List<File> segments = new ArrayList<>();
        File cur = dir;
        while (cur != null) {
            segments.add(0, cur);
            String path = cur.getPath();
            if (path.equals("/storage/emulated/0") || path.equals("/sdcard") || path.equals("/")) break;
            cur = cur.getParentFile();
        }

        for (int i = 0; i < segments.size(); i++) {
            final File seg = segments.get(i);
            boolean isLast = (i == segments.size() - 1);

            if (i > 0) {
                TextView sep = new TextView(this);
                sep.setText("  ›  ");
                sep.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
                sep.setTextSize(12f);
                breadcrumbContainer.addView(sep);
            }

            TextView tv = new TextView(this);
            String label = (seg.getPath().equals("/storage/emulated/0") ||
                    seg.getPath().equals("/sdcard")) ? "Storage" : seg.getName();
            if (label.isEmpty()) label = "Storage";
            tv.setText(label);
            tv.setTextSize(13f);
            tv.setPadding(4, 0, 4, 0);

            if (isLast) {
                tv.setTextColor(getResources().getColor(R.color.primary, getTheme()));
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                tv.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
                final File segFinal = seg;
                tv.setOnClickListener(v -> viewModel.navigateTo(segFinal));
            }
            breadcrumbContainer.addView(tv);
        }

        breadcrumbScroll.post(() ->
                breadcrumbScroll.fullScroll(HorizontalScrollView.FOCUS_RIGHT));
    }

    // ─── Feature 5: New Folder ──────────────────────────────────────────

    private void showNewFolderDialog() {
        if (currentTab != R.id.nav_files) {
            Toast.makeText(this, "Switch to Files tab to create folder", Toast.LENGTH_SHORT).show();
            return;
        }
        EditText input = new EditText(this);
        input.setHint("Folder name");
        input.setPadding(48, 32, 48, 32);
        new AlertDialog.Builder(this)
                .setTitle("New Folder")
                .setView(input)
                .setPositiveButton("Create", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) viewModel.createFolder(name);
                    else Toast.makeText(this, "Enter folder name", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null).show();
    }

    // ─── Feature 6: Sort (Name/Size/Date/Type + Asc/Desc) ──────────────

    private void showSortDialog() {
        String[] fields = {"Name", "Size", "Date Modified", "Type"};
        new AlertDialog.Builder(this)
                .setTitle("Sort by")
                .setItems(fields, (d, which) -> {
                    FileManager.SortOrder[] orders = FileManager.SortOrder.values();
                    viewModel.setSortOrder(orders[which]);
                    new AlertDialog.Builder(this)
                            .setTitle("Sort Order")
                            .setItems(new String[]{"↑ Ascending", "↓ Descending"}, (d2, w2) ->
                                    viewModel.setSortDescending(w2 == 1))
                            .show();
                }).show();
    }

    // ─── Feature 7: File Options Dialog ─────────────────────────────────

    private void showFileOptionsDialog(FileItem item) {
        List<String> opts = new ArrayList<>();
        opts.add("Open");
        opts.add("Share");                                        // Feature 16: Share
        opts.add("Copy");                                         // Feature 9: Clipboard
        opts.add("Cut");
        if (clipboardItem != null) opts.add("Paste here");       // Feature 9: Paste
        opts.add(item.getFileType() == FileItem.FileType.ARCHIVE
                ? "Extract ZIP" : "Compress to ZIP");            // Feature 2: ZIP
        if (item.getFileType() == FileItem.FileType.TEXT)
            opts.add("Preview Text");                             // Feature 11
        opts.add("Rename");
        opts.add("Delete");
        opts.add("File Info");                                    // Feature 12
        opts.add("Find Duplicates");                              // Feature 15
        opts.add(item.isFavorite() ? "Remove Favorite" : "Add to Favorites");

        String[] arr = opts.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle(item.getName())
                .setItems(arr, (d, w) -> handleFileOption(arr[w], item))
                .show();
    }

    private void handleFileOption(String opt, FileItem item) {
        switch (opt) {
            case "Open": openFile(item); break;
            case "Share": shareFile(item); break;
            case "Copy":
                clipboardItem = item; isClipboardCut = false;
                Toast.makeText(this, "Copied: " + item.getName(), Toast.LENGTH_SHORT).show();
                break;
            case "Cut":
                clipboardItem = item; isClipboardCut = true;
                Toast.makeText(this, "Cut: " + item.getName(), Toast.LENGTH_SHORT).show();
                break;
            case "Paste here": pasteFromClipboard(); break;
            case "Extract ZIP": viewModel.extractZip(item); break;
            case "Compress to ZIP": viewModel.compressFile(item); break;
            case "Preview Text": previewTextFile(item); break;
            case "Rename": showRenameDialog(item); break;
            case "Delete": confirmDelete(item); break;
            case "File Info": showFileInfoDialog(item); break;
            case "Find Duplicates": findDuplicates(item); break;
            case "Add to Favorites": case "Remove Favorite":
                viewModel.toggleFavorite(item); break;
        }
    }

    // ─── Feature 8: Multi-Select Actions ────────────────────────────────

    private void shareSelectedFiles() {
        List<FileItem> selected = adapter.getSelectedItems();
        if (selected.isEmpty()) return;
        if (selected.size() == 1) { shareFile(selected.get(0)); return; }

        ArrayList<Uri> uris = new ArrayList<>();
        for (FileItem fi : selected) {
            try {
                uris.add(FileProvider.getUriForFile(this,
                        getPackageName() + ".provider", fi.getFile()));
            } catch (Exception ignored) {}
        }
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("*/*");
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Share " + selected.size() + " files"));
        adapter.exitMultiSelectMode();
        selectionBar.setVisibility(View.GONE);
    }

    private void showCopyMoveSelected() {
        List<FileItem> selected = adapter.getSelectedItems();
        if (selected.isEmpty()) return;
        new AlertDialog.Builder(this)
                .setTitle(selected.size() + " files")
                .setItems(new String[]{"Batch Rename", "Copy to current folder"}, (d, w) -> {
                    if (w == 0) showBatchRenameDialog(selected);
                    else {
                        File dest = viewModel.getCurrentDirectory().getValue();
                        if (dest != null) for (FileItem fi : selected) viewModel.copyFile(fi, dest);
                    }
                    adapter.exitMultiSelectMode();
                    selectionBar.setVisibility(View.GONE);
                }).show();
    }

    private void deleteSelectedFiles() {
        List<FileItem> selected = adapter.getSelectedItems();
        if (selected.isEmpty()) return;
        new AlertDialog.Builder(this)
                .setTitle("Delete " + selected.size() + " files?")
                .setMessage("This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> {
                    for (FileItem fi : selected) viewModel.deleteFile(fi);
                    Toast.makeText(this, selected.size() + " files deleted", Toast.LENGTH_SHORT).show();
                    adapter.exitMultiSelectMode();
                    selectionBar.setVisibility(View.GONE);
                })
                .setNegativeButton("Cancel", null).show();
    }

    // ─── Feature 9: File Clipboard ──────────────────────────────────────

    private void pasteFromClipboard() {
        if (clipboardItem == null) {
            Toast.makeText(this, "Nothing to paste", Toast.LENGTH_SHORT).show();
            return;
        }
        File dest = viewModel.getCurrentDirectory().getValue();
        if (dest == null) return;
        if (isClipboardCut) {
            viewModel.moveFile(clipboardItem, dest);
            clipboardItem = null;
            Toast.makeText(this, "Moved successfully", Toast.LENGTH_SHORT).show();
        } else {
            viewModel.copyFile(clipboardItem, dest);
            Toast.makeText(this, "Pasted: " + clipboardItem.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    // ─── Feature 10: Batch Rename ────────────────────────────────────────

    private void showBatchRenameDialog(List<FileItem> items) {
        EditText input = new EditText(this);
        input.setHint("Prefix (e.g. photo_)");
        input.setPadding(48, 32, 48, 32);
        new AlertDialog.Builder(this)
                .setTitle("Batch Rename (" + items.size() + " files)")
                .setView(input)
                .setPositiveButton("Rename", (d, w) -> {
                    String prefix = input.getText().toString().trim();
                    if (prefix.isEmpty()) {
                        Toast.makeText(this, "Enter a prefix", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    for (int i = 0; i < items.size(); i++) {
                        FileItem fi = items.get(i);
                        String ext = getExtension(fi.getName());
                        String newName = prefix + (i + 1) + (ext.isEmpty() ? "" : "." + ext);
                        viewModel.renameFile(fi, newName);
                    }
                    Toast.makeText(this, items.size() + " files renamed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null).show();
    }

    // ─── Feature 11: Text Preview ────────────────────────────────────────

    private void previewTextFile(FileItem item) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new FileReader(item.getFile()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null && count < 80) {
                    sb.append(line).append("\n");
                    count++;
                }
                final String text = sb.toString();
                runOnUiThread(() -> new AlertDialog.Builder(this)
                        .setTitle(item.getName())
                        .setMessage(text.isEmpty() ? "(Empty file)" : text)
                        .setPositiveButton("Close", null)
                        .show());
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Cannot preview file", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ─── Feature 12: File Info Dialog ────────────────────────────────────

    private void showFileInfoDialog(FileItem item) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                "MMM dd, yyyy  HH:mm", java.util.Locale.getDefault());
        String info = "Name: " + item.getName() + "\n\n"
                + "Path: " + item.getPath() + "\n\n"
                + "Size: " + item.getFormattedSize() + "\n\n"
                + "Type: " + item.getFileType().name() + "\n\n"
                + "Modified: " + sdf.format(item.getLastModifiedDate()) + "\n\n"
                + (item.isDirectory() ? "Contents: directory" : "");
        new AlertDialog.Builder(this)
                .setTitle("File Info")
                .setMessage(info)
                .setPositiveButton("Open Details", (d, w) -> {
                    Intent i = new Intent(this, FileDetailActivity.class);
                    i.putExtra(FileDetailActivity.EXTRA_FILE_PATH, item.getPath());
                    startActivity(i);
                })
                .setNegativeButton("Close", null).show();
    }

    // ─── Feature 13: Rename ──────────────────────────────────────────────

    private void showRenameDialog(FileItem item) {
        EditText input = new EditText(this);
        input.setText(item.getName());
        input.selectAll();
        input.setPadding(48, 32, 48, 32);
        new AlertDialog.Builder(this)
                .setTitle("Rename")
                .setView(input)
                .setPositiveButton("Rename", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) viewModel.renameFile(item, name);
                })
                .setNegativeButton("Cancel", null).show();
    }

    // ─── Feature 14: Confirm Delete ──────────────────────────────────────

    private void confirmDelete(FileItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete")
                .setMessage("Delete \"" + item.getName() + "\"?\nThis cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> viewModel.deleteFile(item))
                .setNegativeButton("Cancel", null).show();
    }

    // ─── Feature 15: Duplicate Finder ────────────────────────────────────

    private void findDuplicates(FileItem target) {
        List<FileItem> list = adapter.getCurrentList();
        List<String> dupes = new ArrayList<>();
        for (FileItem fi : list) {
            if (!fi.getPath().equals(target.getPath())
                    && fi.getName().equals(target.getName())
                    && fi.getSize() == target.getSize()) {
                dupes.add(fi.getPath());
            }
        }
        if (dupes.isEmpty()) {
            Toast.makeText(this, "No duplicates found in this folder", Toast.LENGTH_SHORT).show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Duplicates of " + target.getName())
                    .setMessage("Found " + dupes.size() + " duplicate(s):\n\n"
                            + TextUtils.join("\n", dupes))
                    .setPositiveButton("OK", null).show();
        }
    }

    // ─── Feature 16: Share Single File ───────────────────────────────────

    private void shareFile(FileItem item) {
        try {
            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider", item.getFile());
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setType("*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share via"));
        } catch (Exception e) {
            Toast.makeText(this, "Could not share file", Toast.LENGTH_SHORT).show();
        }
    }

    // ─── Feature 17: Open File ───────────────────────────────────────────

    private void openFile(FileItem item) {
        try {
            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider", item.getFile());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, getMimeType(item));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Open with"));
        } catch (Exception e) {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show();
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
            case APK: return "application/vnd.android.package-archive";
            default: return "*/*";
        }
    }

    // ─── Utility ─────────────────────────────────────────────────────────

    private String getExtension(String name) {
        int dot = name.lastIndexOf('.');
        return (dot >= 0 && dot < name.length() - 1) ? name.substring(dot + 1) : "";
    }

    // ─── Permissions ─────────────────────────────────────────────────────

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                new AlertDialog.Builder(this)
                        .setTitle("Storage Permission Required")
                        .setMessage("SmartFileManager needs full storage access to browse and manage files.")
                        .setPositiveButton("Grant Access", (d, w) -> {
                            Intent intent = new Intent(
                                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                    Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", null).show();
            } else {
                viewModel.loadRoot();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
            } else {
                viewModel.loadRoot();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            viewModel.loadRoot();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && Environment.isExternalStorageManager()
                && viewModel.getCurrentDirectory().getValue() == null) {
            viewModel.loadRoot();
        }
    }

    @Override
    public void onBackPressed() {
        if (adapter.isInMultiSelectMode()) {
            adapter.exitMultiSelectMode();
            selectionBar.setVisibility(View.GONE);
            return;
        }
        if (isSearchMode) {
            toggleSearch();
            return;
        }
        if (currentTab == R.id.nav_files && viewModel.canGoBack()) {
            viewModel.navigateBack();
            return;
        }
        super.onBackPressed();
    }
}
