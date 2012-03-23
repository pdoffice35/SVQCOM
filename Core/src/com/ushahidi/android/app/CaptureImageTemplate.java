package com.ushahidi.android.app;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class CaptureImageTemplate extends ArrayAdapter<String> 
{
	private LayoutInflater mInflater;
	
	private ArrayList<String> mIcons;

	private int mViewResourceId;
	
	/*
	 * - Class constructor.
	 */
	public CaptureImageTemplate(Context ctx, int viewResourceId, ArrayList<String> imagePath) 
	{
		super(ctx, viewResourceId, imagePath);
		mInflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mIcons = imagePath;
		mViewResourceId = viewResourceId;
	}
		

	/*
	 * - 
	 */
	@Override
	public long getItemId(int position) {
		return 0;
	}

	/*
	 * - 
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) 
	{
		Bitmap selectedImage = null;
		
		convertView = mInflater.inflate(mViewResourceId, null);		
		
		ImageView imageView = (ImageView) convertView.findViewById(R.id.DashBoardICON);
		
		try 
		{
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = 8; 
			options.inTempStorage = new byte[16*1024];			
			selectedImage = BitmapFactory.decodeFile(mIcons.get(position), options);// new BitmapDrawable(mIcons.get(position));
			Log.d("Image", "Imgae Width" + options.outWidth + " Image Height " + options.outHeight);
			if(selectedImage != null)
			{
				imageView.setImageBitmap(selectedImage);
				LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(100, 100);
				imageView.setLayoutParams(layoutParams);		
				imageView.setScaleType(ImageView.ScaleType.FIT_XY);
			}
		} 
		catch (Exception e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			/*if(selectedImage != null)
				selectedImage.recycle();
			selectedImage = null;*/
		}
		
		
		/*Uri uri = Uri.fromFile(new File(mIcons.get(position)));		
		imageView.setImageURI((uri));*/
		return convertView;
	}
}
