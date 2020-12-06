package com.example.filemanager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;

import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // initialize the app view on creation.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appview1);

    }

    static class TextAdapter extends BaseAdapter {

        private List<String> data = new ArrayList<>();

        private boolean[] selection;

        public void setData(List<String> data) {
            if (data != null) {
                this.data.clear();
                if (data.size() > 0) {
                    this.data.addAll(data);
                }
                notifyDataSetChanged();
            }
        }

        void setSelection(boolean[] selection) {
            if (selection != null) {
                this.selection = new boolean[selection.length];
                for (int i = 0; i < selection.length; i++) {
                    this.selection[i] = selection[i];
                }
                notifyDataSetChanged();
            }
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public String getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        // shows the item and selects item from list
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
                convertView.setTag(new ViewHolder((TextView) convertView.findViewById(R.id.textItem)));
            }
            ViewHolder holder = (ViewHolder) convertView.getTag();
            final String item = getItem(position);
            // cleans up the file path
            holder.info.setText(item.substring(item.lastIndexOf('/') + 1));
            if (selection != null) {
                if (selection[position]) {
                    // selected color background of file
                    holder.info.setBackgroundColor(Color.YELLOW);
                } else {
                    // unselected color background of file
                    holder.info.setBackgroundColor(Color.WHITE);
                }
            }
            return convertView;
        }

        class ViewHolder {
            TextView info;
            ViewHolder(TextView info) {
                this.info = info;
            }
        }
    }

    // checking if the app has granted access to folders
    private static final int REQUEST_PERMISSIONS = 0;
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int PERMISSIONS_COUNT = 2;

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean arePermissionsDenied() {

        int p = 0;
        while (p < PERMISSIONS_COUNT) {
            if (checkSelfPermission(PERMISSIONS[p]) != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
            p++;
        }
        return false;
    }

    private boolean isFIleManagerInitialized = false;
    private boolean[] selection;
    private File[] files;
    private List<String> filesList;
    private int filesFoundCount;

    // method to check permissions on start
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && arePermissionsDenied()) {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
            return;
        }
        if (!isFIleManagerInitialized) {
            final String dlPath = String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
            final String docPath = String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS));
            final String imgPath = String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));
            // file locations from string paths
            File images = new File(imgPath);
            File doc = new File(docPath); // documents not used in android 7
            File dir = new File(dlPath);
            files = dir.listFiles();
            TextView pathOutput = findViewById(R.id.pathOutput);
            // removes path from download folder for better view.
            pathOutput.setText(dlPath.substring(dlPath.lastIndexOf('/') + 1));
            filesFoundCount = files.length;
            ListView listView = findViewById(R.id.listView);
            TextAdapter textAdapter1 = new TextAdapter();
            listView.setAdapter(textAdapter1);
            filesList = new ArrayList<>();

            for (int i = 0; i < filesFoundCount; i++) {
                filesList.add(String.valueOf(files[i].getAbsolutePath()));
            }
            textAdapter1.setData(filesList);
            // user selected file
            selection = new boolean[files.length];

            // to select a file in the app directory.
            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    selection[position] = !selection[position];
                    textAdapter1.setSelection(selection);

                    return false;
                }
            });

            // initialize the b uttons from layout file.
            // buttons are used to perform the tasks to files
            Button b1 = findViewById(R.id.b1);
            Button b2 = findViewById(R.id.b2);
            Button b3 = findViewById(R.id.b3);

////////////////////////////////////////////////////////////////////////////////////////////////////
            // this is where the file is deleted if the user chooses to do so.
            b1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final AlertDialog.Builder deleteDialog = new AlertDialog.Builder(MainActivity.this);
                    deleteDialog.setTitle("Delete");
                    deleteDialog.setMessage("Delete File?");
                    deleteDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // deletes selected files
                            for (int i = 0; i < files.length; i++) {
                                if (selection[i]) {
                                    deleteFile(files[i]);
                                    selection[i] = false;
                                }
                            }
                            //update file list
                            files = dir.listFiles();
                            filesFoundCount = files.length;
                            filesList.clear();
                            for (int i = 0; i < filesFoundCount; i++) {
                                filesList.add(String.valueOf(files[i].getAbsolutePath()));
                            }
                            textAdapter1.setData(filesList);
                        }
                    });
                    deleteDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    deleteDialog.show();
                }
            });
////////////////////////////////////////////////////////////////////////////////////////////////////
            // here is where the user can move the file to pictures
            b2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final AlertDialog.Builder moveDialog = new AlertDialog.Builder(MainActivity.this);
                    moveDialog.setTitle("Move");
                    moveDialog.setMessage("Move to Pictures?");
                    moveDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // cycles through selected files and moves them if selected.
                            for (int i = 0; i < files.length; i++) {
                                if (selection[i]) {
                                    try {
                                        // moves file to the images folder then deletes file
                                        moveFile(files[i], images);
                                        deleteFile(files[i]);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    selection[i] = false;
                                }
                            }
                            //update file list in app
                            files = dir.listFiles();
                            filesFoundCount = files.length;
                            filesList.clear();
                            for (int i = 0; i < filesFoundCount; i++) {
                                filesList.add(String.valueOf(files[i].getAbsolutePath()));
                            }
                            textAdapter1.setData(filesList);
                        }
                    });
                    moveDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    moveDialog.show();
                }
            });
            ////////////////////////////////////////////////////////////////////////////////////////
            // here is where the user can open a file
            b3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final AlertDialog.Builder openDialog = new AlertDialog.Builder(MainActivity.this);
                    openDialog.setTitle("Open");
                    openDialog.setMessage("Open File?");
                    openDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            for (int i = 0; i < files.length; i++) {
                                if (selection[i]) {
                                    openFile(files[i]);
                                    selection[i] = false;
                                }
                            }
                        }
                    });
                    openDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    openDialog.show();
                }
            });
            ////////////////////////////////////////////////////////////////////////////////////////
            isFIleManagerInitialized = true;
        }
    }

    // method to delete a file or a folder
    private void deleteFile(File file) {
        if (file.isDirectory()) {
            // if empty, delete folder
            if (file.list().length == 0) {
                file.delete();
            } else {//if not empty, delete all files inside folder
                String[] files = file.list();
                for (String temp : files) {
                    File fileToDelete = new File(file, temp);
                    deleteFile(fileToDelete);
                }
                if (file.list().length == 0) {
                    file.delete();
                }
            }
        } else {
            file.delete();
        }
    }

    // method to move a file
    private void moveFile(File file, File dir) throws IOException {
        File newFile = new File(dir, file.getName());
        try (FileChannel outputChannel = new FileOutputStream(newFile).getChannel(); FileChannel inputChannel = new FileInputStream(file).getChannel()) {
            inputChannel.transferTo(0, inputChannel.size(), outputChannel);

        }

    }

    // method to open a file
    private void openFile(File file) {
        Uri uri = Uri.fromFile(file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, MimeTypeMap.getSingleton().getExtensionFromMimeType(file.getName()));

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(Intent.createChooser(intent, "Open " + file.getName() + " with ..."));
    }

    // checks permissions to make sure they are granted
    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS && grantResults.length > 0) {
            if (arePermissionsDenied()) {
                // error if permissions not granted. only shows if permissions are denied.
                Toast errorToast = Toast.makeText(MainActivity.this, "Error, please grand permissions access", Toast.LENGTH_SHORT);
                errorToast.show();
            } else {
                onResume();
            }
        }
    }
}
