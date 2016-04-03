package com.balloon.printer.printpdf;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.balloon.printer.printpdf.bean.File2Print;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.OpenFileActivityBuilder;

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
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    //constants
    private static final String TAG = "PrintPDF";
    private static final String SEND_MESSAGE_SERVLET = "http://135.23.64.27:8080/TestOAuthServer/servlet/PushMessageServlet";
    private static final int REQUEST_CODE_OPENER = 1;
    private static final int REQUEST_CODE_RESOLUTION = 2;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int STATE_LOG_OUT = 1;
    private static final int STATE_LOG_IN = 2;
    private static final int STATE_FILE_CHOSEN = 3;
    private static String[] MIME_TYPES_FILTER = {"text/plain", "text/html"};
    private static String FILE_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/";
    //global variables
    private GoogleApiClient mGoogleApiClient;
    private String driveIdString;
    File2Print file2Print;
    //widgets
    Button btnSignIn;
    Button btnChooseFile;
    Button btnPrint;
    Button btnDownload;
    Button btnOpen;
    Button btnSignOut;
    TextView txtFileInfo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //build the GoogleApiClient
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addScope(Drive.SCOPE_APPFOLDER) // required for App Folder sample
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }


        //register widgets
        btnSignIn = (Button) findViewById(R.id.btnSignIn);
        btnChooseFile = (Button) findViewById(R.id.btnChooseFile);
        btnDownload = (Button) findViewById(R.id.btnDownload);
        btnOpen = (Button) findViewById(R.id.btnOpen);
        btnPrint = (Button) findViewById(R.id.btnPrint);
        btnSignOut = (Button) findViewById(R.id.btnSignOut);
        txtFileInfo = (TextView) findViewById(R.id.txtFileInfo);
        //set UI
        UpdateUI(STATE_LOG_OUT);

        //add Listeners
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGoogleApiClient.connect();
            }
        });
        btnChooseFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callDriveUI();
            }
        });
        btnPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (file2Print != null)
                    new SendFilename2Server().execute(file2Print);
            }
        });
        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (file2Print != null)
                    new DownloadFromGD().execute(file2Print);
            }
        });
        btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (file2Print != null) {
                    new DownloadFromGD().execute(file2Print);
                    Intent intent = new Intent();
                    intent.setAction(android.content.Intent.ACTION_VIEW);
                    File file = new File(FILE_PATH + file2Print.getFilename());
//                                            while (true) {
//                                                if (file.exists()) {
//                                                    break;
//                                                }
//                                            }
                    intent.setDataAndType(Uri.fromFile(file), file2Print.getMimeType());
                    startActivity(intent);
                }

            }
        });
        btnSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logOut();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mGoogleApiClient.isConnected()&&file2Print==null){
            UpdateUI(STATE_LOG_IN);
        }
        if (file2Print != null&&mGoogleApiClient.isConnected()) {
            UpdateUI(STATE_FILE_CHOSEN);
            txtFileInfo.setText("");
            txtFileInfo.append("The file you choose is : " + file2Print.getFilename());

        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        //update UI
        UpdateUI(STATE_LOG_IN);

    }

    public void callDriveUI() {
        IntentSender intentSender = Drive.DriveApi
                .newOpenFileActivityBuilder()
                .setMimeType(MIME_TYPES_FILTER)
                .build(mGoogleApiClient);
        try {
            startIntentSenderForResult(
                    intentSender, REQUEST_CODE_OPENER, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            Log.w(TAG, "Unable to send intent", e);
        }
    }

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

    public void logOut() {

        mGoogleApiClient.clearDefaultAccountAndReconnect().setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    file2Print=null;
                    UpdateUI(STATE_LOG_OUT);
                }
            }
        });

    }

    public void UpdateUI(int stateCode){
        switch (stateCode){
            case STATE_LOG_OUT:
                btnChooseFile.setVisibility(View.INVISIBLE);
                btnDownload.setVisibility(View.INVISIBLE);
                btnPrint.setVisibility(View.INVISIBLE);
                btnOpen.setVisibility(View.INVISIBLE);
                btnSignOut.setVisibility(View.INVISIBLE);
                btnSignIn.setVisibility(View.VISIBLE);
                txtFileInfo.setVisibility(View.INVISIBLE);
                break;
            case STATE_LOG_IN:
                btnChooseFile.setVisibility(View.VISIBLE);
                btnDownload.setVisibility(View.INVISIBLE);
                btnPrint.setVisibility(View.INVISIBLE);
                btnOpen.setVisibility(View.INVISIBLE);
                btnSignOut.setVisibility(View.VISIBLE);
                btnSignIn.setVisibility(View.INVISIBLE);
                txtFileInfo.setVisibility(View.INVISIBLE);
                break;
            case STATE_FILE_CHOSEN:
                btnChooseFile.setVisibility(View.VISIBLE);
                btnDownload.setVisibility(View.VISIBLE);
                btnPrint.setVisibility(View.VISIBLE);
                btnOpen.setVisibility(View.VISIBLE);
                btnSignOut.setVisibility(View.VISIBLE);
                btnSignIn.setVisibility(View.INVISIBLE);
                txtFileInfo.setVisibility(View.VISIBLE);
        }

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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_OPENER:
                if (resultCode == RESULT_OK) {
                    try {
                        DriveId driveId = data.getParcelableExtra(
                                OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                        driveIdString = driveId.encodeToString();

                        file2Print = new getFileInformation().execute(driveIdString).get();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }
                return;
            case REQUEST_CODE_RESOLUTION:
                if (resultCode == RESULT_OK) {
                    return;
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private class getFileInformation extends AsyncTask<String, Void, File2Print> {

        @Override
        protected File2Print doInBackground(String... params) {
            String driveId = params[0];
            DriveFile driveFile = DriveId.decodeFromString(driveId).asDriveFile();

            //get MetadataResult in order to get Metadata
            DriveResource.MetadataResult metadataResult = driveFile.getMetadata(mGoogleApiClient).await();

            //create a file to print and set information
            File2Print file2Print = new File2Print();
            if (metadataResult != null && metadataResult.getStatus().isSuccess()) {
                Metadata metadata = metadataResult.getMetadata();

                file2Print.setDriveId(driveId);
                file2Print.setFilename(metadata.getTitle());
                file2Print.setMimeType(metadata.getMimeType());
                file2Print.setFileSize(metadata.getFileSize());
            }
            return file2Print;
        }

        @Override
        protected void onPostExecute(File2Print file2Print) {
            super.onPostExecute(file2Print);
        }
    }

    private class SendFilename2Server extends AsyncTask<File2Print, Void, String> {

        @Override
        protected String doInBackground(File2Print... params) {
            try {
                String fileName = params[0].getFilename();
                String driveId = params[0].getDriveId();
                URL url = new URL(SEND_MESSAGE_SERVLET);
                HttpURLConnection httpConn = null;
                httpConn = (HttpURLConnection) url.openConnection();

                httpConn.setUseCaches(false);
                httpConn.setDoOutput(true);
                httpConn.setRequestMethod("POST");
                httpConn.setDoInput(true);
                httpConn.setRequestProperty("fileName", fileName);
                httpConn.setRequestProperty("driveId", driveId);

                OutputStream outputStream = httpConn.getOutputStream();
                outputStream.close();

                int responseCode = httpConn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
                    String response = reader.readLine();
                    String result = "Server's response: " + response;
                    Log.i("response", result);
                    return result;
                } else {
                    System.out.println("Server returned non-OK code: " + responseCode);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Toast.makeText(MainActivity.this, "Sent Information to Server...", Toast.LENGTH_LONG);
            Toast.makeText(MainActivity.this, s, Toast.LENGTH_LONG);
        }
    }

    private class DownloadFromGD extends AsyncTask<File2Print, Void, Void> {


        @Override
        protected Void doInBackground(File2Print... params) {
            final String fileName = params[0].getFilename();
            final String driveId = params[0].getDriveId();
            DriveFile driveFile = DriveId.decodeFromString(driveId).asDriveFile();

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
                        File file = new File(FILE_PATH + fileName);
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
