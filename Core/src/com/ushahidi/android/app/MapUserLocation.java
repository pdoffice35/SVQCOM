package com.ushahidi.android.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ZoomButtonsController;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public abstract class MapUserLocation extends MapActivity implements
		LocationListener {

	public static final String PREFS_NAME = "UshahidiService";

	private static final String TAG = "Ushahidi/MapUserLocation";

	protected static final int ONE_MINUTE = 60 * 1000;

	protected static final int FIVE_MINUTES = 5 * ONE_MINUTE;

	protected static final int ACCURACY_THRESHOLD = 30; // in meters

	protected int gpsTimeout = 0;

	// private CountDownTimer countDownTimer;

	private AsyncTimer countDownTimer;

	private AsyncTimerTask timerTask;

	protected boolean didFindLocation;

	protected MapView mapView;

	protected ZoomButtonsController mapZoomButtonsController;

	protected MapController mapController;

	protected LocationManager locationManager;

	protected UpdatableMarker updatableMarker;

	protected Location currrentLocation;

	/*
	 * Subclasses must implement a method which updates any relevant interface
	 * elements when the location changes. e.g. TextViews displaying the
	 * location.
	 */
	protected abstract void locationChanged(double latitude, double longitude,
			boolean doReverseGeocode);

	/*
	 * protected abstract void locationLatLonChanged(double latitude, double
	 * longitude);
	 */
	/* Override this to set a custom marker */
	protected UpdatableMarker createUpdatableMarker(Drawable marker,
			GeoPoint point) {
		return new MapMarker(marker, point);
	}

	protected void setDeviceLocation() {

		Log.d(TAG, "setDeviceLocation");

		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		// Location lastNetLocation = null;
		// Location lastGpsLocation = null;

		/*
		 * boolean netAvailable =
		 * locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		 * boolean gpsAvailable =
		 * locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		 * 
		 * if (!netAvailable && !gpsAvailable) { AlertDialog.Builder builder =
		 * new AlertDialog.Builder(this);
		 * builder.setTitle(getString(R.string.location_disabled))
		 * .setMessage(getString(R.string.location_reenable))
		 * .setPositiveButton(android.R.string.yes, new
		 * DialogInterface.OnClickListener() { public void
		 * onClick(DialogInterface dialog, int id) { startActivity(new
		 * Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)); }
		 * }) .setNegativeButton(android.R.string.no, new
		 * DialogInterface.OnClickListener() { public void
		 * onClick(DialogInterface dialog, int id) { dialog.cancel(); } })
		 * .create() .show(); }
		 */
		/*
		 * if (netAvailable) { lastNetLocation =
		 * locationManager.getLastKnownLocation
		 * (LocationManager.NETWORK_PROVIDER); } if (gpsAvailable) {
		 * lastGpsLocation =
		 * locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER); }
		 * setBestLocation(lastNetLocation, lastGpsLocation); // If chosen
		 * location is more than a minute old, start querying network/GPS if
		 * (currrentLocation == null || (new Date()).getTime() -
		 * currrentLocation.getTime() > ONE_MINUTE) { if (netAvailable) {
		 * locationManager
		 * .requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0,
		 * this); } if (gpsAvailable) {
		 * locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
		 * 0, 0, this); } }
		 */

		useGPSProvider();
	}

	protected void useGPSProvider() {

		Log.d(TAG, "useGPSProvider");

		boolean gpsAvailable = locationManager
				.isProviderEnabled(LocationManager.GPS_PROVIDER);

		Log.d(TAG, "gpsAvailable: " + gpsAvailable);

		if (!gpsAvailable) {
			useNetworkProvider();
			return;
		}

		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
				0, this);
		startTimer();
	}

	protected void useNetworkProvider() {

		Log.d(TAG, "useNetworkProvider");

		boolean netAvailable = locationManager
				.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

		if (!netAvailable) {
			Log.d(TAG, "!netAvailable");
			showLocationDisabledDialog();
			return;
		}

		Log.d(TAG, "requestLocationUpdates(LocationManager.NETWORK_PROVIDER)");
		locationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, 0, 0, this);
	}

	protected void getLastKnownLocation() {
		Log.d(TAG, "getLastKnownLocation");
		Location lastNetLocation = locationManager
				.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		Location lastGPSLocation = locationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		setBestLocation(lastNetLocation, lastGPSLocation);
	}

	/*
	 * private void startTimer() {
	 * 
	 * Log.d(TAG, "startTimer");
	 * 
	 * if(countDownTimer != null) { countDownTimer.cancel(); countDownTimer =
	 * null; }
	 * 
	 * Log.d(TAG, "gpsTimeout: "+gpsTimeout);
	 * 
	 * countDownTimer = new CountDownTimer(gpsTimeout * 1000L, 1000L) {
	 * 
	 * @Override public void onTick(long millisUntilFinished) {
	 * 
	 * Log.d(TAG, "millisUntilFinished: "+millisUntilFinished);
	 * 
	 * if(didFindLocation) { Log.d(TAG, "didFindLocation: "+didFindLocation);
	 * stopLocating(); cancel(); } }
	 * 
	 * @Override public void onFinish() {
	 * 
	 * if(!didFindLocation) { useNetworkProvider(); } } };
	 * countDownTimer.start(); }
	 */

	private void startTimer() {
		Log.d(TAG, "startTimer");

		if (timerTask != null) {
			timerTask.cancel(true);
			timerTask = null;
		}

		timerTask = new AsyncTimerTask();
		timerTask.execute();
	}

	private class AsyncTimerTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
			if (countDownTimer != null) {
				countDownTimer.cancel();
				countDownTimer = null;
			}
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params) {
			Looper.prepare();
			countDownTimer = new AsyncTimer(gpsTimeout * 1000L, 1000L);
			countDownTimer.start();
			return null;
		}

		@Override
		protected void onCancelled() {
			if (countDownTimer != null)
				countDownTimer.cancel();			
			super.onCancelled();
		}
	}

	private class AsyncTimer extends CountDownTimer {

		public AsyncTimer(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);

		}

		@Override
		public void onFinish() {
			if (!didFindLocation) {
				useNetworkProvider();
			}
		}

		@Override
		public void onTick(long millisUntilFinished) {
			Log.d(TAG, "millisUntilFinished: " + millisUntilFinished);

			if (didFindLocation) {
				Log.d(TAG, "didFindLocation: " + didFindLocation);
				stopLocating();
				timerTask.cancel(true);
			}
		}
	}

	protected void showLocationDisabledDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.location_disabled))
				.setMessage(getString(R.string.location_reenable))
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								startActivity(new Intent(
										android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
							}
						})
				.setNegativeButton(android.R.string.no,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						}).create().show();
	}

	public void stopLocating() {
		if (locationManager != null) {
			try {

				Log.d(TAG, "locationManager.removeUpdates");
				locationManager.removeUpdates(this);
			} catch (Exception ex) {
				Log.e(TAG, "stopLocating", ex);
			}
			locationManager = null;
		}
	}

	protected void updateMarker(double latitude, double longitude,
			boolean center) {
		updateMarker(getPoint(latitude, longitude), center);
	}

	protected void updateMarker(GeoPoint point, boolean center) {
		if (updatableMarker == null) {
			Drawable marker = getResources().getDrawable(
					R.drawable.map_marker_green);

			marker.setBounds(0, 0, marker.getIntrinsicWidth(),
					marker.getIntrinsicHeight());
			mapController.setZoom(14);

			updatableMarker = createUpdatableMarker(marker, point);
			mapView.getOverlays().add((Overlay) updatableMarker);
		} else {
			updatableMarker.update(point);
		}
		if (center) {
			mapController.animateTo(point);
		}
	}

	/**
	 * Convert latitude and longitude to a GeoPoint
	 * 
	 * @param latitude
	 *            Latitude
	 * @param longitude
	 *            Longitude
	 * @return GeoPoint
	 */
	protected GeoPoint getPoint(double latitude, double longitude) {
		return (new GeoPoint((int) (latitude * 1E6), (int) (longitude * 1E6)));
	}

	protected void setBestLocation(Location location1, Location location2) {
		if (location1 != null && location2 != null) {
			boolean location1Newer = location1.getTime() - location2.getTime() > FIVE_MINUTES;
			boolean location2Newer = location2.getTime() - location1.getTime() > FIVE_MINUTES;
			boolean location1MoreAccurate = location1.getAccuracy() < location2
					.getAccuracy();
			boolean location2MoreAccurate = location2.getAccuracy() < location1
					.getAccuracy();
			if (location1Newer || location1MoreAccurate) {
				locationChanged(location1.getLatitude(),
						location1.getLongitude(), true);
			} else if (location2Newer || location2MoreAccurate) {
				locationChanged(location2.getLatitude(),
						location2.getLongitude(), true);
			}
		} else if (location1 != null) {
			locationChanged(location1.getLatitude(), location1.getLongitude(),
					true);
		} else if (location2 != null) {
			locationChanged(location2.getLatitude(), location2.getLongitude(),
					true);
		}
	}

	private class MapMarker extends ItemizedOverlay<OverlayItem> implements
			UpdatableMarker {
		private OverlayItem myOverlayItem;
		//private long lastTouchTime = -1;

		public MapMarker(Drawable defaultMarker, GeoPoint point) {
			super(boundCenterBottom(defaultMarker));
			update(point);
		}

		public void update(GeoPoint point) {
			myOverlayItem = new OverlayItem(point, " ", " ");
			populate();
		}

		@Override
		protected OverlayItem createItem(int i) {
			return myOverlayItem;
		}

		@Override
		public int size() {
			return 1;
		}

		@Override
		public boolean onTouchEvent(MotionEvent event, MapView mapView) {
			/*final int action = event.getAction();
			final int x = (int) event.getX();
			final int y = (int) event.getY();
			if (action == MotionEvent.ACTION_DOWN) {
				long thisTime = System.currentTimeMillis();
				if (thisTime - lastTouchTime < 250) {
					lastTouchTime = -1;
					GeoPoint geoPoint = mapView.getProjection().fromPixels(
							(int) event.getX(), (int) event.getY());
					double latitude = geoPoint.getLatitudeE6() / 1E6;
					double longitude = geoPoint.getLongitudeE6() / 1E6;
					Log.i(getClass().getSimpleName(), String.format(
							"%d, %d >> %f, %f", x, y, latitude, longitude));
					locationChanged(latitude, longitude, true);
					stopLocating();
					return true;
				} else {
					lastTouchTime = thisTime;
				}
			}*/
			return super.onTouchEvent(event, mapView);
		}
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	public void onLocationChanged(Location location) {
		if (location != null) {
			locationChanged(location.getLatitude(), location.getLongitude(),
					true);
			if (location.hasAccuracy()
					&& location.getAccuracy() < ACCURACY_THRESHOLD) {
				// accuracy is within ACCURACY_THRESHOLD, de-activate location
				// detection
				stopLocating();
			}
		}
	}

	public void onProviderDisabled(String provider) {
	}

	public void onProviderEnabled(String provider) {
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	protected void onResume() {
		super.onResume();
		SharedPreferences prefs = getSharedPreferences(PREFS_NAME, 0);
		try {
			gpsTimeout = Integer.parseInt(prefs.getString(
					"gps_timeout_preference", "60"));

		} catch (NumberFormatException nfe) {
			Log.e(TAG, nfe.getMessage());
			nfe.printStackTrace();
			gpsTimeout = 60;
		}
		setDeviceLocation();
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopLocating();
	}

	@Override
	protected void onDestroy() {
		super.onPause();
		stopLocating();
	}

	public abstract interface UpdatableMarker {
		public abstract void update(GeoPoint point);
	}

	protected class MapOverlay extends Overlay {
		private boolean isPinch = false;
		private long lastTouchTime = -1;

		@Override
		public boolean onTap(GeoPoint p, MapView map) {
			/*if (isPinch) {
				return false;
			} else {
				Log.i(TAG, "TAP: "+p);
				if (p != null) {
					updateMarker(p, true);
					return true; // We handled the tap
				} else {
					return false; // Null GeoPoint
				}
			}*/
			return false;
		}

		@Override
		public boolean onTouchEvent(MotionEvent event, MapView mapView) {
			int fingers = event.getPointerCount();
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				isPinch = false; // Touch DOWN, don't know if it's a pinch yet
			}
			if (event.getAction() == MotionEvent.ACTION_MOVE && fingers == 2) {
				isPinch = true; // Two fingers, it's a pinch
			}
			
			final int action = event.getAction();
			final int x = (int) event.getX();
			final int y = (int) event.getY();
			if (action == MotionEvent.ACTION_DOWN) {
				long thisTime = System.currentTimeMillis();
				if (thisTime - lastTouchTime < 250) {
					lastTouchTime = -1;
					GeoPoint geoPoint = mapView.getProjection().fromPixels(
							(int) event.getX(), (int) event.getY());
					double latitude = geoPoint.getLatitudeE6() / 1E6;
					double longitude = geoPoint.getLongitudeE6() / 1E6;
					Log.i(getClass().getSimpleName(), String.format(
							"%d, %d >> %f, %f", x, y, latitude, longitude));
					locationChanged(latitude, longitude, true);
					stopLocating();
					return true;
				} else {
					lastTouchTime = thisTime;
				}
			}
			
			return super.onTouchEvent(event, mapView);
		}

	}
}