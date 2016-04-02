package com.balloon.printer.printpdf;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SortOrder;
import com.google.android.gms.drive.query.SortableField;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String SEND_MESSAGE_SERVLET = "http://135.23.64.27:8080/TestOAuthServer/servlet/PushMessageServlet";
    private GoogleApiClient mGoogleApiClient;
    private ResultsAdapter mResultsAdapter;
    private ListView mResultsListView;
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
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            String filename = result.getMetadataBuffer().get(position).getTitle();
                            DriveId driveID = result.getMetadataBuffer().get(position).getDriveId();
                            String fileId = driveID.encodeToString();
                            Log.i(TAG, filename);
                            Log.i(TAG, fileId);
//                            new sendFilename2Server().execute(filename);
                            new sendFilename2Server().execute(filename,fileId);
                        }
                    });
                }
            };

    private class sendFilename2Server extends AsyncTask<String, Void, String> {
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
                    Log.i("response",result);

                } else {
                    System.out.println("Server returned non-OK code: " + responseCode);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
