package com.balloon.printer.printpdf;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SortOrder;
import com.google.android.gms.drive.query.SortableField;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String SEND_MESSAGE_SERVLET = "http://135.23.64.27:8080/TestOAuthServer/servlet/PushMessageServlet";
    private GoogleApiClient mGoogleApiClient;
    private ResultsAdapter mResultsAdapter;
    private ListView mResultsListView;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final String TAG = "BaseDriveActivity";
    /**
     * Request code for auto Google Play Services error resolution.
     */
    protected static final int REQUEST_CODE_RESOLUTION = 1;

    /**
     * Next available request code.
     */
    protected static final int NEXT_AVAILABLE_REQUEST_CODE = 2;

    /**
     * Google API client.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_listfiles);
        mResultsListView = (ListView) findViewById(R.id.listViewResults);
        mResultsAdapter = new ResultsAdapter(this);
        mResultsListView.setAdapter(mResultsAdapter);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addScope(Drive.SCOPE_APPFOLDER) // required for App Folder sample
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        SortOrder sortOrder = new SortOrder.Builder()
                .addSortAscending(SortableField.TITLE)
                .build();
        Query query = new Query.Builder()
                .setSortOrder(sortOrder)
                .build();
        Drive.DriveApi.query(mGoogleApiClient, query)
                .setResultCallback(metadataCallback);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (!connectionResult.hasResolution()) {
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(this, connectionResult.getErrorCode(), 0).show();
            return;
        }
        try {
            connectionResult.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "permission allowed");
                    return;

                } else {

                    Log.e(TAG, "permission denied");
                }
                return;
            }
        }
    }

    final private ResultCallback<DriveApi.MetadataBufferResult> metadataCallback =
            new ResultCallback<DriveApi.MetadataBufferResult>() {
                @Override
                public void onResult(final DriveApi.MetadataBufferResult result) {
                    if (!result.getStatus().isSuccess()) {

                        return;
                    }
                    mResultsAdapter.clear();
                    mResultsAdapter.append(result.getMetadataBuffer());

                    mResultsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        String[] buttons = {"Print", "Open", "Download"};

                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                            builder.setTitle("Choose an option");
                            builder.setItems(buttons, new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String filename = result.getMetadataBuffer().get(position).getTitle();
                                    DriveId driveID = result.getMetadataBuffer().get(position).getDriveId();
                                    String fileId = driveID.encodeToString();
                                    Log.i(TAG, filename);
                                    Log.i(TAG, fileId);
                                    switch (which) {
                                        case 0:
                                            Toast.makeText(MainActivity.this, "Print",
                                                    Toast.LENGTH_LONG).show();

                                            new SendFilename2Server().execute(filename, fileId);
                                            break;
                                        case 1:
                                            Toast.makeText(MainActivity.this, "Open",
                                                    Toast.LENGTH_LONG).show();
                                            break;
                                        case 2:

                                            new DownloadFromGD().execute(filename, fileId);

                                    }
                                }
                            });
                            AlertDialog alertDialog = builder.create();
                            alertDialog.show();
                        }
//                        @Override
//                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                            String filename = result.getMetadataBuffer().get(position).getTitle();
//                            DriveId driveID = result.getMetadataBuffer().get(position).getDriveId();
//                            String fileId = driveID.encodeToString();
//                            Log.i(TAG, filename);
//                            Log.i(TAG, fileId);
////                            new sendFilename2Server().execute(filename);
//                            new sendFilename2Server().execute(filename,fileId);
//                        }
                    });
                }
            };

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    private class SendFilename2Server extends AsyncTask<String, Void, String> {
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                String filename1 = params[0];
                String driveId1 = params[1];
                URL url = new URL(SEND_MESSAGE_SERVLET);
                HttpURLConnection httpConn = null;

                httpConn = (HttpURLConnection) url.openConnection();

                httpConn.setUseCaches(false);
                httpConn.setDoOutput(true);
                httpConn.setRequestMethod("POST");
                httpConn.setDoInput(true);
                httpConn.setRequestProperty("fileName", filename1);
                httpConn.setRequestProperty("driveId", driveId1);

                OutputStream outputStream = httpConn.getOutputStream();
                outputStream.close();

                int responseCode = httpConn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
                    String response = reader.readLine();
                    String result = "Server's response: " + response;
                    Log.i("response", result);

                } else {
                    System.out.println("Server returned non-OK code: " + responseCode);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private class DownloadFromGD extends AsyncTask<String, Void, Void> {
        public String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/";

        @Override
        protected Void doInBackground(String... params) {
            final String fileName = params[0];
            final String driveId = params[1];
            DriveFile driveFile = DriveId.decodeFromString(driveId).asDriveFile();
//                       depreciated
//                      DriveFile file = Drive.DriveApi.getFile(mGoogleApiClient,
//                                DriveId.decodeFromString(resourceId));
            driveFile.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null).setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(DriveApi.DriveContentsResult result) {
                    try {
                        if (!result.getStatus().isSuccess()) {
                            // Handle error
                            return;
                        }

                        DriveContents contents = result.getDriveContents();
                        InputStream inputStream = contents.getInputStream();
                        verifyStoragePermissions(MainActivity.this);
                        File file = new File(filePath + fileName);
                        FileOutputStream fileOut = null;
                        fileOut = new FileOutputStream(file);

                        byte[] buffer = new byte[1024];
                        int len = -1;
                        while ((len = inputStream.read(buffer)) != -1) {
                            fileOut.write(buffer, 0, len);
                        }

                        inputStream.close();
                        fileOut.flush();
                        fileOut.close();

                        Log.i(TAG, "printed " + fileName);

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Toast.makeText(MainActivity.this, "File has been downloaded to sdcard/Download/",
                    Toast.LENGTH_LONG).show();
        }
    }
}

