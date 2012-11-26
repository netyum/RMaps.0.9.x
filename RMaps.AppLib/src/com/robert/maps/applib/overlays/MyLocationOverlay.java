package com.robert.maps.applib.overlays;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.util.TypeConverter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.preference.PreferenceManager;

import com.robert.maps.applib.R;
import com.robert.maps.applib.utils.Ut;
import com.robert.maps.applib.view.TileView;
import com.robert.maps.applib.view.TileView.OpenStreetMapViewProjection;
import com.robert.maps.applib.view.TileViewOverlay;

/**
 *
 * @author Nicolas Gramlich
 *
 */
public class MyLocationOverlay extends TileViewOverlay {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	protected final Paint mPaint = new Paint();

	protected Bitmap PERSON_ICON2 = null;
	private Bitmap mArrow = null;

	private Context mCtx;
	protected GeoPoint mLocation;
	private float mAccuracy;
	private int mPrefAccuracy;
	private float mBearing;
	private float mSpeed;
	private int METER_IN_PIXEL = 156412;
	private Paint mPaintAccurasyFill;
	private Paint mPaintAccurasyBorder;
	private Paint mPaintLineToGPS, mPaintMapLabelText, mPaintMapLabelBack;
	private boolean mNeedCrosshair;
	private final Paint mPaintCross = new Paint();
	private final static int mCrossSize = 7;
	private Location mLoc;

	private boolean mLineToGPS;
	private int mUnits;

	// ===========================================================
	// Constructors
	// ===========================================================

	public MyLocationOverlay(final Context ctx){
		mCtx = ctx.getApplicationContext();

		mPaintAccurasyFill = new Paint();
		mPaintAccurasyFill.setAntiAlias(true);
		mPaintAccurasyFill.setStrokeWidth(2);
		mPaintAccurasyFill.setStyle(Paint.Style.FILL);
		mPaintAccurasyFill.setColor(0x4490B8D8);

		mPaintAccurasyBorder = new Paint(mPaintAccurasyFill);
		mPaintAccurasyBorder.setStyle(Paint.Style.STROKE);
		mPaintAccurasyBorder.setColor(0xFF90B8D8);
		
		mPaintLineToGPS = new Paint(mPaintAccurasyFill);
		mPaintLineToGPS.setColor(ctx.getResources().getColor(R.color.line_to_gps));
		mPaintMapLabelText = new Paint(mPaintAccurasyFill);
		mPaintMapLabelText.setColor(ctx.getResources().getColor(R.color.map_label_text));
		mPaintMapLabelBack = new Paint(mPaintAccurasyFill);
		mPaintMapLabelBack.setColor(ctx.getResources().getColor(R.color.map_label_back));
		
		mPaintCross.setAntiAlias(true);

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
		mPrefAccuracy = Integer.parseInt(pref.getString("pref_accuracy", "1").replace("\"", ""));
		mNeedCrosshair = pref.getBoolean("pref_crosshair", true);
		mLineToGPS = true;
		mUnits = Integer.parseInt(pref.getString("pref_units", "0"));
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	private boolean getPersonIcon(){
		if(PERSON_ICON2 == null)
			try {
				this.PERSON_ICON2 = BitmapFactory.decodeResource(mCtx.getResources(), R.drawable.person);
			} catch (Exception e) {
				PERSON_ICON2 = null;
			} catch (OutOfMemoryError e) {
				PERSON_ICON2 = null;
			}

		return PERSON_ICON2 == null ? false : true;
	}

	private boolean getArrowIcon(){
		if(mArrow == null)
			try {
				this.mArrow = BitmapFactory.decodeResource(mCtx.getResources(), R.drawable.arrow);
			} catch (Exception e) {
				mArrow = null;
			} catch (OutOfMemoryError e) {
				mArrow = null;
			}

		return mArrow == null ? false : true;
	}
	
	public GeoPoint getLastGeoPoint() {
		return mLocation;
	}
	
	public Location getLastLocation() {
		return mLoc;
	}

	public void setLocation(final Location loc){
		this.mLocation = TypeConverter.locationToGeoPoint(loc);
		this.mAccuracy = loc.getAccuracy();
		this.mBearing = loc.getBearing();
		this.mSpeed = loc.getSpeed();
		mLoc = loc;
	}

	public void setLocation(final GeoPoint geopoint){
		this.mLocation = geopoint;
		this.mAccuracy = 0;
		this.mBearing = 0;
		this.mSpeed = 0;
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected void onDrawFinished(Canvas c, TileView osmv) {
		return;
	}

	@Override
	public void onDraw(final Canvas c, final TileView osmv) {
		if(this.mLocation != null){
			final OpenStreetMapViewProjection pj = osmv.getProjection();
			final Point screenCoords = new Point();
			pj.toPixels(this.mLocation, screenCoords);
			

			if (mPrefAccuracy != 0
					&& mSpeed <= 0.278
					&& ((mAccuracy > 0 && mPrefAccuracy == 1) || (mPrefAccuracy > 1 && mAccuracy >= mPrefAccuracy))) {
				int PixelRadius = (int) (osmv.mTouchScale * mAccuracy / ((float)METER_IN_PIXEL / (1 << osmv.getZoomLevel())));
				c.drawCircle(screenCoords.x, screenCoords.y, PixelRadius, mPaintAccurasyFill);
				c.drawCircle(screenCoords.x, screenCoords.y, PixelRadius, mPaintAccurasyBorder);
			}
			
			if(mLineToGPS) {
				c.drawLine(screenCoords.x, screenCoords.y, osmv.getWidth() / 2, osmv.getHeight() / 2, mPaintLineToGPS);
				final GeoPoint geo = pj.fromPixels(osmv.getWidth() / 2, osmv.getHeight() / 2);
				final float dist = this.mLocation.distanceTo(geo);
				final String lbl = Ut.formatDistance(mCtx, dist, mUnits); 
				final Rect r = new Rect();
				mPaintMapLabelText.getTextBounds(lbl, 0, lbl.length()-1, r);
				final Drawable d = mCtx.getResources().getDrawable(R.drawable.rect);
				final int pad = mCtx.getResources().getDimensionPixelSize(R.dimen.label_map_padding);
				d.setBounds(r.left, r.top, r.right+pad, r.bottom+pad);
				c.save();
				c.translate(osmv.getWidth() / 2, osmv.getHeight() / 2);
				d.draw(c);
				c.restore();
				c.drawText(lbl, osmv.getWidth() / 2, osmv.getHeight() / 2, mPaintMapLabelText);
			}

			c.save();
			if (mSpeed <= 0.278) {
				c.rotate(osmv.getBearing(), screenCoords.x, screenCoords.y);
				if(getPersonIcon()){
					c.drawBitmap(PERSON_ICON2, screenCoords.x - (int)(PERSON_ICON2.getWidth()/2), screenCoords.y - (int)(PERSON_ICON2.getHeight() / 2), mPaint);
				};
			} else {
				if(getArrowIcon()){
					c.rotate(mBearing, screenCoords.x, screenCoords.y);
					c.drawBitmap(mArrow, screenCoords.x - (int)(mArrow.getWidth()/2), screenCoords.y - (int)(mArrow.getHeight() / 2), mPaint);
				}
			}
			c.restore();

		}
		
		if(mNeedCrosshair){
			final int x = osmv.getWidth() / 2;
			final int y = osmv.getHeight() / 2;
			c.drawLine(x - mCrossSize, y, x + mCrossSize, y, this.mPaintCross);
			c.drawLine(x, y - mCrossSize, x, y + mCrossSize, this.mPaintCross);
		}
	}

}
