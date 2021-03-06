/*
 * Copyright 2011 mapsforge.org
 *
 *	This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.muxe.advancedtouristmap.poi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.poi.PoiCategory;
import org.mapsforge.poi.PointOfInterest;
import org.muxe.advancedtouristmap.AdvancedTouristMap;
import org.muxe.advancedtouristmap.BaseActivity;
import org.muxe.advancedtouristmap.R;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class PoiBrowserActivity extends BaseActivity {

	private static final String IMAGEKEY = "image";
	private static final String NAMEKEY = "name";
	private static final String INFOKEY = "info";
	private static final String TAG = PoiBrowserActivity.class.getSimpleName();
	private static final String SAVESTATECATEGORY = "saved_current_category";
	private static final int MAX_POIS = 15;

	private double latitude;
	private double longitude;
	String currentCategory;
	ArrayList<PoiOrCategory> poisAndCategories;
	private ListView poiListView;
	private SimpleAdapter poiListAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.advancedMapViewer.setViewWithHelp(this, R.layout.activity_poi_browser);
		this.poiListView = (ListView) findViewById(R.id.poi_browser_poi_list);

		this.currentCategory = "Root";

		Intent startingIntent = getIntent();
		this.latitude = startingIntent.getDoubleExtra("lat", 0.0);
		this.longitude = startingIntent.getDoubleExtra("lon", 0.0);
	}

	@Override
	protected void onResume() {
		super.onResume();
		browseToCurrentCategory();
	}

	void browseToCurrentCategory() {
		// list to hold categories and pois currently displayed
		this.poisAndCategories = new ArrayList<PoiOrCategory>();
		// get the current category by it's name
		PoiCategory currCategory = this.advancedMapViewer.getPerstManager().getCategory(
				this.currentCategory);
		if (currCategory == null) {
			return;
		}
		// this list will hold all the information to fill our adapter
		List<HashMap<String, Object>> fillPois = new ArrayList<HashMap<String, Object>>();
		ArrayList<PoiCategory> childCategories = new ArrayList<PoiCategory>(
				this.advancedMapViewer.getPerstManager().directChildren(this.currentCategory));

		// check if category has parent and set it as first element, to browse up tree
		if (currCategory.getParent() != null) {
			childCategories.add(0, currCategory.getParent());
		}
		for (PoiCategory category : childCategories) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			if (currCategory.getParent() == category) {
				map.put(IMAGEKEY, R.drawable.ic_menu_back);
				map.put(NAMEKEY, "..");
			} else {
				map.put(IMAGEKEY, R.drawable.ic_menu_archive);
				map.put(NAMEKEY, category.getTitle());
			}
			fillPois.add(map);
			this.poisAndCategories.add(new PoiOrCategory(category));
		}

		Iterator<PointOfInterest> iterator = this.advancedMapViewer.getPerstManager()
				.neighborIterator(new GeoCoordinate(this.latitude, this.longitude),
						this.currentCategory);
		for (int i = 0; i < MAX_POIS && iterator.hasNext(); i++) {
			PointOfInterest poi = iterator.next();
			HashMap<String, Object> map = new HashMap<String, Object>();
			// calculate the distance for display purposes
			int distance = (int) GeoCoordinate.sphericalDistance(this.longitude, this.latitude,
					poi.getLongitude(), poi.getLatitude());
			String description;
			if (poi.getName() != null) {
				// display name and category
				description = poi.getName() + " (" + poi.getCategory().getTitle() + ")";
			} else {
				// poi has no name, so just dosplay the category (e.g. Telephones)
				description = poi.getCategory().getTitle();
			}
			map.put(IMAGEKEY, R.drawable.ic_menu_myplaces);
			map.put(NAMEKEY, description);
			map.put(INFOKEY, distance + " m");
			fillPois.add(map);
			this.poisAndCategories.add(new PoiOrCategory(poi));
		}

		String[] from = new String[] { IMAGEKEY, NAMEKEY, INFOKEY };
		int[] to = new int[] { R.id.poi_row_image, R.id.poi_row_name, R.id.poi_row_distance };

		this.poiListAdapter = new SimpleAdapter(this, fillPois, R.layout.poi_row, from, to);
		this.poiListView.setAdapter(this.poiListAdapter);
		this.poiListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				PoiOrCategory poiOrCat = PoiBrowserActivity.this.poisAndCategories
						.get(position);
				if (poiOrCat.isPoiCategory()) {
					PoiBrowserActivity.this.currentCategory = poiOrCat.getPoiCategory()
							.getTitle();
					PoiBrowserActivity.this.browseToCurrentCategory();
				} else {
					// startActivity(new Intent(PoiBrowserActivity.this, PositionInfo.class)
					// .putExtra("LATITUDE", poiOrCat.getPoi().getLatitude()).putExtra(
					// "LONGITUDE", poiOrCat.getPoi().getLongitude()));
					PoiBrowserActivity.this.advancedMapViewer.getCurrentPois().clear();
					PoiBrowserActivity.this.advancedMapViewer.getCurrentPois().add(
							poiOrCat.getPoi());
					startActivity(new Intent(PoiBrowserActivity.this, AdvancedTouristMap.class)
							.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("CENTER_POI", true));
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.poi_browser_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_show_pois_map:
				this.advancedMapViewer.getCurrentPois().clear();
				for (PoiOrCategory poiOrCat : this.poisAndCategories) {
					if (poiOrCat.isPoi()) {
						this.advancedMapViewer.getCurrentPois().add(poiOrCat.getPoi());
					}
				}
				startActivity(new Intent(PoiBrowserActivity.this, AdvancedTouristMap.class)
						.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				return true;
			default:
				return false;
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		String savedCategory = savedInstanceState.getString(SAVESTATECATEGORY);
		if (savedCategory != null) {
			this.currentCategory = savedCategory;
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString(SAVESTATECATEGORY, this.currentCategory);
		super.onSaveInstanceState(outState);
	}
}
