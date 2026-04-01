package com.smart.filemanager.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.smart.filemanager.R;
import com.smart.filemanager.filehandling.FileItem;
import com.smart.filemanager.filehandling.FileManager;
import com.smart.filemanager.logic.FileViewModel;
import com.smart.filemanager.ui.adapters.FileAdapter;
import com.smart.filemanager.ui.fragments.StorageAnalyticsFragment;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION = 1001;

    private FileViewModel viewModel;
    private FileAdapter adapter;
    private RecyclerView recyclerView;
    private View progressBar;
    private View emptyView;
    private EditText searchBar;
    private BottomNavigationView bottomNav;
    private FloatingActionButton fabSort;

    private boolean isSearching = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(FileViewModel.class);

        initViews();
        setupRecyclerView();
        setupBottomNav();
        setupSearch();
        observeViewModel();

        requestPermissions();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        emptyView = findViewById(R.id.empty_view);
        searchBar = findViewById(R.id.search_bar);
        bottomNav = findViewById(R.id.bottom_navigation);
        fabSort = findViewById(R.id.fab_sort);

        fabSort.setOnClickListener(v -> showSortDialog());
    }

    private void setupRecyclerView() {
        adapter = new FileAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);

        adapter.setOnFileClickListener(new FileAdapter.OnFileClickListener() {
            @Override
            public void onFileClick(FileItem item) {
                if (item.isDirectory()) {
                    viewModel.navigateTo(item.getFile());
                } else {
                    openFile(item);
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
        });
    }

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            isSearching = false;
            searchBar.setText("");
            searchBar.setVisibility(View.GONE);

            if (id == R.id.nav_files) {
                viewModel.loadRoot();
                fabSort.setVisibility(View.VISIBLE);
                return true;
            } else if (id == R.id.nav_recents) {
                viewModel.loadRecents();
                fabSort.setVisibility(View.GONE);
                return true;
            } else if (id == R.id.nav_favorites) {
                viewModel.loadFavorites();
                fabSort.setVisibility(View.GONE);
                return true;
            } else if (id == R.id.nav_storage) {
                fabSort.setVisibility(View.GONE);
                showStorageAnalytics();
                return true;
            } else if (id == R.id.nav_search) {
                searchBar.setVisibility(View.VISIBLE);
                searchBar.requestFocus();
                fabSort.setVisibility(View.GONE);
                return true;
            }
            return false;
        });
    }

    private void setupSearch() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 1) {
                    viewModel.search(s.toString());
                    isSearching = true;
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void observeViewModel() {
        viewModel.getFileList().observe(this, files -> {
            if (!isSearching) {
                adapter.submitList(files);
                emptyView.setVisibility(files.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getSearchResults().observe(this, results -> {
            if (isSearching) {
                adapter.submitList(results);
                emptyView.setVisibility(results.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getRecentFiles().observe(this, files -> {
            if (bottomNav.getSelectedItemId() == R.id.nav_recents) {
                adapter.submitList(files);
                emptyView.setVisibility(files.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getFavoriteFiles().observe(this, files -> {
            if (bottomNav.getSelectedItemId() == R.id.nav_favorites) {
                adapter.submitList(files);
                emptyView.setVisibility(files.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getIsLoading().observe(this, loading ->
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        viewModel.getCurrentDirectory().observe(this, dir -> {
            if (dir != null) {
                getSupportActionBar();
                setTitle(dir.getName().isEmpty() ? "Storage" : dir.getName());
            }
        });

        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openFile(FileItem item) {
        Intent intent = new Intent(this, FileDetailActivity.class);
        intent.putExtra(FileDetailActivity.EXTRA_FILE_PATH, item.getPath());
        startActivity(intent);
        viewModel.addToRecents(item.getPath());
    }

    private void showFileOptionsDialog(FileItem item) {
        String[] options = {"Rename", "Copy", "Move", "Delete",
                item.isFavorite() ? "Remove Favorite" : "Add to Favorites"};

        new AlertDialog.Builder(this)
                .setTitle(item.getName())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: showRenameDialog(item); break;
                        case 1: showCopyMoveDialog(item, false); break;
                        case 2: showCopyMoveDialog(item, true); break;
                        case 3: confirmDelete(item); break;
                        case 4: viewModel.toggleFavorite(item); break;
                    }
                })
                .show();
    }

    private void showRenameDialog(FileItem item) {
        EditText input = new EditText(this);
        input.setText(item.getName());
        input.selectAll();

        new AlertDialog.Builder(this)
                .setTitle("Rename")
                .setView(input)
                .setPositiveButton("Rename", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) viewModel.renameFile(item, name);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCopyMoveDialog(FileItem item, boolean isMove) {
        EditText input = new EditText(this);
        input.setHint("Destination path");
        input.setText(Environment.getExternalStorageDirectory().getAbsolutePath());

        new AlertDialog.Builder(this)
                .setTitle(isMove ? "Move to" : "Copy to")
                .setView(input)
                .setPositiveButton(isMove ? "Move" : "Copy", (d, w) -> {
                    String path = input.getText().toString().trim();
                    File dest = new File(path);
                    if (dest.exists() && dest.isDirectory()) {
                        if (isMove) viewModel.moveFile(item, dest);
                        else viewModel.copyFile(item, dest);
                    } else {
                        Toast.makeText(this, "Invalid destination", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDelete(FileItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete")
                .setMessage("Delete \"" + item.getName() + "\"?")
                .setPositiveButton("Delete", (d, w) -> viewModel.deleteFile(item))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSortDialog() {
        String[] options = {"Name", "Size", "Date"};
        new AlertDialog.Builder(this)
                .setTitle("Sort by")
                .setItems(options, (d, w) -> {
                    FileManager.SortOrder order = FileManager.SortOrder.values()[w];
                    viewModel.setSortOrder(order);
                })
                .show();
    }

    private void showStorageAnalytics() {
        viewModel.loadStorageStats();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new StorageAnalyticsFragment())
                .addToBackStack(null)
                .commit();
        recyclerView.setVisibility(View.GONE);
        findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        if (!viewModel.navigateBack()) {
            super.onBackPressed();
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                new AlertDialog.Builder(this)
                        .setTitle("Permission Required")
                        .setMessage("SmartFileManager needs full storage access to browse and manage files.")
                        .setPositiveButton("Grant Access", (d, w) -> {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                    Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                viewModel.loadRoot();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_PERMISSION);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            if (viewModel.getCurrentDirectory().getValue() == null) {
                viewModel.loadRoot();
            }
        }
    }
}
