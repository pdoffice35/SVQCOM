package com.ushahidi.android.app;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.google.android.maps.MapView;

public class IncidentMapView extends MapView {

	public IncidentMapView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

	}

	public IncidentMapView(Context context, AttributeSet attrs) {
		super(context, attrs);

	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		int action = ev.getAction();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			// Disallow ScrollView to intercept touch events.
			this.getParent().requestDisallowInterceptTouchEvent(true);
			break;

		case MotionEvent.ACTION_UP:
			// Allow ScrollView to intercept touch events.
			this.getParent().requestDisallowInterceptTouchEvent(false);
			break;
		}

		// Handle MapView's touch events.
		super.onTouchEvent(ev);
		return true;
	}
}
