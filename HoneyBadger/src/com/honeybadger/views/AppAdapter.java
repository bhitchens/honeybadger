package com.honeybadger.views;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Brad Hitchens
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.honeybadger.R;
import com.honeybadger.views.ShowAppsActivity.App;

public class AppAdapter extends ArrayAdapter<App>
{

	Context context;
	int layoutResourceId;
	ArrayList<App> data = null;

	public AppAdapter(Context context, int layoutResourceId, ArrayList<App> data)
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
			holder.imgIcon = (ImageView) row.findViewById(R.id.imgIcon);
			holder.txtTitle = (TextView) row.findViewById(R.id.txtTitle);

			row.setTag(holder);
		}
		else
		{
			holder = (AppHolder) row.getTag();
		}

		App app = data.get(position);
		holder.txtTitle.setText(app.title + " (" + Integer.toString(app.uid) + ")");
		holder.imgIcon.setImageDrawable(app.icon);

		return row;
	}

	static class AppHolder
	{
		ImageView imgIcon;
		TextView txtTitle;
	}
}
