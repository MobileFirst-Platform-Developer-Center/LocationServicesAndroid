/**
* Copyright 2015 IBM Corp.
*/
package com.samples.locationServices;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONException;
import org.json.JSONObject;

import com.samples.locationServices.R;
import com.worklight.location.api.WLAcquisitionFailureCallbacksConfiguration;
import com.worklight.location.api.WLAcquisitionPolicy;
import com.worklight.location.api.WLDevice;
import com.worklight.location.api.WLDeviceContext;
import com.worklight.location.api.WLLocationServicesConfiguration;
import com.worklight.location.api.WLTriggerCallback;
import com.worklight.location.api.WLTriggersConfiguration;
import com.worklight.location.api.geo.WLCircle;
import com.worklight.location.api.geo.WLGeoAcquisitionPolicy;
import com.worklight.location.api.geo.WLGeoCallback;
import com.worklight.location.api.geo.WLGeoError;
import com.worklight.location.api.geo.WLGeoFailureCallback;
import com.worklight.location.api.geo.WLGeoPosition;
import com.worklight.location.api.geo.triggers.WLGeoDwellInsideTrigger;
import com.worklight.location.api.geo.triggers.WLGeoExitTrigger;
import com.worklight.location.api.geo.triggers.WLGeoPositionChangeTrigger;
import com.worklight.wlclient.api.WLClient;
import com.worklight.wlclient.api.WLFailResponse;
import com.worklight.wlclient.api.WLResponse;
import com.worklight.wlclient.api.WLResponseListener;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainLocation extends Activity {

	private TextView longitude, latitude, timestamp = null;
	private Button acquireBtn;

	private WLDevice wlDevice;

	private AtomicBoolean stopClicked;
	private static final int GET_LOCATION_PERMISSION = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_location);
		getActionBar().setTitle("Location Services");

		stopClicked = new AtomicBoolean(true);


		acquireBtn = (Button) findViewById(R.id.startBtn);
		acquireBtn = (Button) findViewById(R.id.startBtn);
		acquireBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if(android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1){
					getLocation();
			    } else {
                    if (!(WLClient.getInstance().getContext().checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED))
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, GET_LOCATION_PERMISSION);
                    else {
                        getLocation();
                    }
		       }

			}

		});

		WLClient.createInstance(getApplicationContext());

		wlDevice = WLClient.getInstance().getWLDevice();
		WLClient.getInstance().connect(new WLResponseListener() {

			@Override
			public void onSuccess(WLResponse arg0) {
				Log.i("Location Services", "Connected Successfully");
			}

			@Override
			public void onFailure(WLFailResponse arg0) {
				displayAlert("Connection Failure: " + arg0.getErrorMsg());
			}
		});

	}

	public void getLocation(){
        if (stopClicked.compareAndSet(true, false)) {
            acquireLocation();
            startAsyncTask();
            acquireBtn.setText("Stop Acquisition");
        }
        else {
            stopAcquisition("Acquisition stopped");
            stopClicked.set(true);
        }
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
	        switch (requestCode) {
	            case GET_LOCATION_PERMISSION: {
	                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
	                    getLocation();
	                } else { // in case the user deny.
	                    displayAlert("Permission Denied");
	                }
	                return;
	            }
	        }
	}

	private void stopAcquisition(String alertText) {
		WLClient.getInstance().getWLDevice().stopAcquisition();
		stopAsyncTask();
		displayAlert(alertText);
		acquireBtn.setText("Start Acquisition");
	}

	private void startAsyncTask() {
		// to keep running while in the background you should bind to a background or foreground service.
		// since this isn't the point of this sample, we're going to use a hack, but this isn't the recommended way:
		new AsyncTask<Object, Object, Object>() {

			@Override
			protected Object doInBackground(Object... params) {
				synchronized(stopClicked) {
					while (!stopClicked.get())
						try {
							stopClicked.wait(1000);
						}catch(InterruptedException e) {
						}
				}

				return null;
			}

		}.execute();
	}

	@Override
	protected void onDestroy() {
		stopAsyncTask();
		super.onDestroy();
	}

	private void stopAsyncTask() {
		stopClicked.set(true);
		synchronized(stopClicked) {
			stopClicked.notifyAll();
		}
	}

	// acquires a location and then starts on-going acquisition
	// should be a called in a Looper thread, such as the UI thread
	void acquireLocation() {
		// use GPS to get the user's location
		final WLGeoAcquisitionPolicy geoPolicy = WLGeoAcquisitionPolicy.getLiveTrackingProfile();
		geoPolicy.setTimeout(60000); // set timeout to 1 minute
		geoPolicy.setMaximumAge(10000); // allow to use a position that is 10 seconds old

		// get the user's current position
		wlDevice.acquireGeoPosition(
				new WLGeoCallback() {

					@Override
					public void execute(final WLGeoPosition pos) {
						// when we receive the position, we display it and start on-going acquisition
						displayPosition(pos);

						final WLAcquisitionFailureCallbacksConfiguration failureCallbacks = new WLAcquisitionFailureCallbacksConfiguration();
						failureCallbacks.setGeoFailureCallback(new WLGeoFailureCallback() {
							@Override
							public void execute(WLGeoError geoErr) {
								displayAlert(getGeoErrorMessage(geoErr));
							}
						});


						// note: callback's don't occur in a Looper thread, so run startAcquisition on the UI thread
						runOnUiThread(new Runnable() {
							public void run() {
								wlDevice.startAcquisition(new WLLocationServicesConfiguration()
									.setPolicy(new WLAcquisitionPolicy().setGeoPolicy(geoPolicy))
									.setTriggers(getTriggersConfig(pos))
									.setFailureCallbacks(Arrays.asList(failureCallbacks)));
							}
						});
					}
				},
				new WLGeoFailureCallback() {

					@Override
					public void execute(final WLGeoError geoError) {
						runOnUiThread(new Runnable() {
							public void run() {
								stopAcquisition(getGeoErrorMessage(geoError));
							}
						});
					}
				},
				geoPolicy);
	}

	WLTriggersConfiguration getTriggersConfig(WLGeoPosition pos) {
		WLTriggersConfiguration triggers = new WLTriggersConfiguration();
		triggers.getGeoTriggers().put("posChange",
				new WLGeoPositionChangeTrigger()
					.setCallback(new WLTriggerCallback() {
						@Override
						public void execute(WLDeviceContext deviceContext) {
							displayPosition(deviceContext.getGeoPosition());
						}
					}));
		triggers.getGeoTriggers().put("leftArea",
				new WLGeoExitTrigger()
						.setArea(new WLCircle(pos.getCoordinate(), 200))
						.setCallback(new WLTriggerCallback() {
							@Override
							public void execute(WLDeviceContext deviceContext) {
								displayAlert("Left the area");
								JSONObject event = new JSONObject();
								try {
									event.put("event", "exit area");
								} catch (JSONException e) {
									e.printStackTrace();
								}
								WLClient.getInstance().transmitEvent(event, true);
							}
					}));
		triggers.getGeoTriggers().put("dwellArea",
				new WLGeoDwellInsideTrigger()
						.setArea(new WLCircle(pos.getCoordinate(), 50))
						.setDwellingTime(3000)
						.setCallback(new WLTriggerCallback() {

							@Override
							public void execute(WLDeviceContext deviceContext) {
								displayAlert("Still in the vicinity");
								JSONObject event = new JSONObject();
								try {
									event.put("event", "dwell inside area");
								} catch (JSONException e) {
									e.printStackTrace();
								}
								WLClient.getInstance().transmitEvent(event, true);
							}
						}));

		return triggers;
	}

	// display the position to the user
	void displayPosition(final WLGeoPosition pos) {
		runOnUiThread((new Runnable() {
			public void run() {
				longitude = (TextView) findViewById(R.id.longitudeRes);
				longitude.setText("" + pos.getCoordinate().getLongitude());
				latitude = (TextView) findViewById(R.id.latitudeRes);
				latitude.setText("" + pos.getCoordinate().getLatitude());
				timestamp = (TextView) findViewById(R.id.timestampRes);
				timestamp.setText("" + new Date(pos.getTimestamp()));
			}
		}));
	}

	private void displayAlert(final String message) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				AlertDialog.Builder builder = new Builder(MainLocation.this);

				builder.setMessage(message)
					   .setCancelable(false)
					   .setPositiveButton("OK",new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					   });

				builder.show();
			}
		});
	}


	private String getGeoErrorMessage(final WLGeoError geoErr) {
		return "Error acquiring geo (" + geoErr.getErrorCode() + "): " + geoErr.getMessage();
	}


}
