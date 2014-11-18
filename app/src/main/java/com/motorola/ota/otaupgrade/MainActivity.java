package com.motorola.ota.otaupgrade;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.RecoverySystem;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.security.GeneralSecurityException;


public class MainActivity extends Activity {
    private static final String TAG = "Mesh OTA Upgrade";
    TextView displayMessage = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Inside onCreate");
        setContentView(R.layout.activity_main);

        displayMessage = (TextView) findViewById(R.id.displayMessage);
        updateDisplay("Loading ...");

        // Get the OTA package from Downloads Folder
        File otaFile = getOTAFile();
        if(otaFile == null) {
            updateDisplay(getString(R.string.ota_package_not_found));
            return;
        }

        // Check out the phone build version.
        Log.d(TAG, "Build Version - Incremental = " + Build.VERSION.INCREMENTAL);
        String incrementalVersion = Build.VERSION.INCREMENTAL;
        incrementalVersion = "." + incrementalVersion + ".";

        updateDisplay(getString(R.string.validation_in_progress));
        Log.d(TAG, "Updating display to = " + getString(R.string.validation_in_progress));

        // Check if the phone is already upgraded
        if(otaFile.getName().contains(incrementalVersion)) {
            updateDisplay(getString(R.string.device_already_upgraded));
            Log.d(TAG, getString(R.string.device_already_upgraded));
        } else {
            applyOTA(otaFile);
        }
    }

    private void applyOTA(File otaFile) {
        updateDisplay(getString(R.string.starting_ota));
        Log.d(TAG, getString(R.string.starting_ota));

        Log.d(TAG, "Verifying the package");
        try {
            RecoverySystem.verifyPackage(otaFile, null, null);
            Log.d(TAG, "Package verification completed successfully.");
            RecoverySystem.installPackage(getApplicationContext(), otaFile);
        } catch (IOException e) {
            updateDisplay("IO Error: " + e.getMessage());
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            updateDisplay("Security Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * This method will lookup for the OTA file in /sdcard/Download,
     * copy the contents to Cache directory folder and then returns
     * the file in Cache directory.
     *
     * @return - File reference from Cache directory
     */
    private File getOTAFile() {
        Log.d(TAG, "ENV Var - DIRECTORY_DOWNLOADS = " + Environment.DIRECTORY_DOWNLOADS);
        File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File otaFile = null;
        if(downloadsFolder == null) {
            Log.e(TAG, "Unable to access Downloads folder");
        } else {
            File[] dlFiles = downloadsFolder.listFiles();
            if(dlFiles == null) {
                Log.e(TAG, "No files found under downloads folder");
            } else {
                File sourceFile = null;

                Log.d(TAG, "Number of files in downloads folder = " + dlFiles.length);
                for(File file : dlFiles) {
                    Log.d(TAG, "File Name: " + file.getName());
                    if(file.getName().startsWith("delta-sdcard") && file.getName().contains(Build.PRODUCT)) {
                        sourceFile = file;
                        break;
                    }
                }

                if(sourceFile != null) {
                    updateDisplay("Starting to copy OTA file to : " + Environment.getDownloadCacheDirectory());
                    otaFile = new File(Environment.getDownloadCacheDirectory() + "/" + sourceFile.getName());
                    try {
                        if(!otaFile.createNewFile()) {
                            updateDisplay("OTA file already exists, deleting it.");
                            otaFile.delete();
                            otaFile.createNewFile();
                        }
                        copyFileContent(sourceFile, otaFile);
                        updateDisplay("OTA file copied to /cache/recovery");
                    } catch(Exception e) {
                        Log.e(TAG, "Error when copying OTA file to /cache/recovery", e);
                        updateDisplay("Error when copying OTA file to /cache/recovery : " + e.getMessage());
                        otaFile = null;
                    }

                }
            }
        }

        return otaFile;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "Inside onCreateOptionsMenu");
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "Inside onOptionsItemSelected");
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Utility method to append new status updates to the display
     *
     * @param message - New message to be posted to the UI.
     */
    private void updateDisplay(String message) {
        if(displayMessage == null) {
            return;
        }

        Log.d(TAG, message);
        CharSequence currMsg = displayMessage.getText();
        displayMessage.setText( currMsg + "\n>" + message );
    }

    private void copyFileContent(File source, File target) throws FileNotFoundException, IOException {
        FileChannel input = null, output = null;
        try {
            input = new FileInputStream(source).getChannel();
            updateDisplay("Input channel opened ...");
            output = new FileOutputStream(target).getChannel();
            updateDisplay("Output channel opened ...");
            output.transferFrom(input, 0, input.size());
            updateDisplay("Transfer of file content complete ...");
        } finally {
            if(input != null) input.close();
            if(output != null) output.close();
        }
    }
}
