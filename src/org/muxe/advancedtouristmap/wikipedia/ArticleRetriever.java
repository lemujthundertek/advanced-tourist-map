package org.muxe.advancedtouristmap.wikipedia;

import java.util.ArrayList;

import org.mapsforge.android.maps.GeoPoint;

public interface ArticleRetriever {
	public ArrayList<WikiArticleInterface> getArticles(GeoPoint geoPoint, int radius,
			int limit, int offset);
}
