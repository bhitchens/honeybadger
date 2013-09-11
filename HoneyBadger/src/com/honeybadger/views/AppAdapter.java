package com.honeybadger.views;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Version: 1.3
 * Date of last modification: 14 JUNE 2012
 * Source Info: n/a
 * 
 * Edit 1.3: Created
 *--------------------------------------------------------------------------------------------------------------------------------
 */

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.honeybadger.R;
import com.honeybadger.api.SharedMethods;
import com.honeybadger.api.SharedMethods.AppInfo;
import com.honeybadger.api.databases.DBApps;
import com.honeybadger.api.databases.DBContentProvider;

public class AppAdapter extends ArrayAdapter<AppInfo>
{
	Context context;
	int layoutResourceId;
	ArrayList<AppInfo> data = null;

	/**
	 * Constructor for AppAdapter
	 * 
	 * @param context Passed in context.
	 * @param layoutResourceId Passed in integer for ID of the layout.
	 * @param data Passed in ArrayList of AppInfo
	 */
	public AppAdapter(Context context, int layoutResourceId, ArrayList<AppInfo> data)
	{
		super(context, layoutResourceId, data);
		this.layoutResourceId = layoutResourceId;
		this.context = context;
		this.data = data;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View row = convertView;
		AppHolder holder = null;

		if (row == null)
		{
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);

			holder = new AppHolder();

			holder.boxW = (CheckBox) row.findViewById(R.id.block_wifi);
			holder.boxC = (CheckBox) row.findViewById(R.id.block_cell);
			holder.imgIcon = (ImageView) row.findViewById(R.id.imgIcon);
			holder.txtTitle = (TextView) row.findViewById(R.id.txtTitle);

			row.setTag(holder);
		}
		else
		{
			holder = (AppHolder) row.getTag();
		}

		final AppInfo app = data.get(position);

		//dba = new DBApps(context);		
		
		holder.boxW.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{

			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				//dba.open();
				Boolean prevBlock = SharedMethods.checkBlockW(context, app.uid);//dba.checkBlockW(app.uid);
				if (isChecked && !prevBlock)
				{
					//dba.changeStatus(app.uid, "block", "default");
					context.getContentResolver().update(DBContentProvider.CONTENT_URI_APPS, null, Integer.toString(app.uid), new String[] {"block", "default"});
				}
				else if (!isChecked && prevBlock)
				{
					//dba.changeStatus(app.uid, "allow", "default");
					context.getContentResolver().update(DBContentProvider.CONTENT_URI_APPS, null, Integer.toString(app.uid), new String[] {"allow", "default"});
				}
				//dba.close();
			}
		});
		
		holder.boxC.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{

			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				//dba.open();
				Boolean prevBlock = SharedMethods.checkBlockC(context, app.uid);//dba.checkBlockC(app.uid);
				//SharedMethods.checkBlockC(context, app.uid);
				if (isChecked && !prevBlock)
				{
					//dba.changeStatus(app.uid, "default", "block");
					context.getContentResolver().update(DBContentProvider.CONTENT_URI_APPS, null, Integer.toString(app.uid), new String[] {"default", "block"});
				}
				else if (!isChecked && prevBlock)
				{
					//dba.changeStatus(app.uid, "default", "allow");
					context.getContentResolver().update(DBContentProvider.CONTENT_URI_APPS, null, Integer.toString(app.uid), new String[] {"default", "allow"});
				}
				//dba.close();
			}
		});
		
		//dba.open();
		final CheckBox boxW = holder.boxW;
		boxW.setChecked(SharedMethods.checkBlockW(context, app.uid));//dba.checkBlockW(app.uid));
		//dba.open();
		final CheckBox boxC = holder.boxC;
		boxC.setChecked(SharedMethods.checkBlockC(context, app.uid));//dba.checkBlockC(app.uid));
		//dba.close();
		holder.txtTitle.setText(app.appname + " (" + Integer.toString(app.uid) + ")");
		holder.imgIcon.setImageDrawable(app.icon);

		return row;
	}

	static class AppHolder
	{
		CheckBox boxW;
		CheckBox boxC;
		ImageView imgIcon;
		TextView txtTitle;
	}

}
