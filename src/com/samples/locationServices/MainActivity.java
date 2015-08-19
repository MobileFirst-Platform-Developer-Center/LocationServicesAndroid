/* 
* Copyright 2015 IBM Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.samples.locationServices;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

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

public class MainActivity extends Activity {
	
	LinearLayout linearLayout;
	TextView longitude;
	TextView latitude;
	TextView timestamp;
	
	WLDevice wlDevice;
	
	AtomicBoolean stopClicked;
	private Button acquireButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		linearLayout = new LinearLayout(this);
		linearLayout.setOrientation(LinearLayout.VERTICAL);

		
		ScrollView view = new ScrollView(this);
		view.addView(linearLayout);		
		setContentView(view);
		
		
		TextView title = new TextView(this);
		title.setText("MobileFirst Location Services");
		
		TextView spacer = new TextView(this);
		spacer.setText("------------------\n");		
		
		linearLayout.addView(title);
		linearLayout.addView(spacer);
		
		TextView explanation = new TextView(this);
		explanation.setText(
				"This is a sample application.\n" + 
				"Your position will appear below.\n" +
				"Errors will be displayed in alerts\n." +
				"After 3 seconds, if you haven't moved more than 50 meters, an alert will be displayed and event will be sent to the server.\n" +
				"Once you have moved more than 200 meters, an alert will be displayed and event will be sent to the server.");
		
		linearLayout.addView(explanation);

		stopClicked = new AtomicBoolean(true);
		
		acquireButton = new Button(this);
		acquireButton.setText("Start Acquisition");
		if(android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1){
	        acquireButton.setOnClickListener(new View.OnClickListener() {
	                @Override
	                public void onClick(View v) {
	                    getLocation();
	                }
	            });
	        }
	        else{
	            acquireButton.setOnClickListener(new View.OnClickListener() {
	                @Override
	                public void onClick(View v) {
	                    if (!(WLClient.getInstance().getContext().checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
	                            == PackageManager.PERMISSION_GRANTED))
	                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, GET_LOCATION_PERMISSION);
	                    else {
	                        getLocation();
	                    }
	                }
	            });
	        }
		linearLayout.addView(acquireButton);

		
		longitude = new TextView(this);
		longitude.setText("Longitude: ");
		
		latitude = new TextView(this);
		latitude.setText("Latitude: ");
		
		timestamp = new TextView(this);
		timestamp.setText("Timestamp: ");

		linearLayout.addView(longitude);
		linearLayout.addView(latitude);
		linearLayout.addView(timestamp);
		
		WLClient.createInstance(getApplicationContext());
		
		wlDevice = WLClient.getInstance().getWLDevice();
		WLClient.getInstance().connect(new WLResponseListener() {
			
			@Override
			public void onSuccess(WLResponse arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onFailure(WLFailResponse arg0) {
				displayAlert(arg0.getErrorMsg());
			}
		});
	}

	public void getLocation(){
	        if (stopClicked.compareAndSet(true, false)) {
	            acquireLocation();
	            startAsyncTask();
	            acquireButton.setText("Stop Acquisition");
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
		acquireButton.setText("Start Acquisition");
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
				longitude.setText("Longitude: " + pos.getCoordinate().getLongitude());
				latitude.setText("Latitude: " + pos.getCoordinate().getLatitude());
				timestamp.setText("Timstamp: " + new Date(pos.getTimestamp()));
			}
		}));
	}

	private void displayAlert(final String message) {
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				AlertDialog.Builder builder = new Builder(MainActivity.this);
				
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
