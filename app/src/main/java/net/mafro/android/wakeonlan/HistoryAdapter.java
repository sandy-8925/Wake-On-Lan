/*
Copyright (C) 2008-2014 Matt Black
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.
* Neither the name of the author nor the names of its contributors may be used
  to endorse or promote products derived from this software without specific
  prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package net.mafro.android.wakeonlan;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.DataSetObserver;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListAdapter;
import android.widget.TextView;

import net.mafro.android.widget.StarButton;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


/**
 *	@desc	Custom adapter to aid in UI binding
 */
public class HistoryAdapter implements OnCheckedChangeListener, ListAdapter {

	private static final String TAG = "HistoryAdapter";

	private ContentResolver content;
	private boolean showStars;

	@NonNull
	private List<HistoryIt> historyItems = Collections.emptyList();

	HistoryAdapter(Context context, boolean showStars)
	{
		this.content = context.getContentResolver();
		this.showStars = showStars;
	}

	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
	{
		// extract record's _ID from tag
		int id = (Integer) buttonView.getTag();

		if(isChecked) {
			setIsStarred(id, 1);
		}else{
			setIsStarred(id, 0);
		}
	}

	private void setIsStarred(int id, int value) {
		// update history setting is_starred to value
		ContentValues values = new ContentValues(1);
		values.put(History.Items.IS_STARRED, value);

		Uri itemUri = Uri.withAppendedPath(History.Items.CONTENT_URI, Integer.toString(id));
		this.content.update(itemUri, values, null, null);
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public boolean isEnabled(int position) {
		return true;
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
	}

	@Override
	public int getCount() {
		return historyItems.size();
	}

	@Override
	public Object getItem(int position) {
		return historyItems.get(position);
	}

	@Override
	public long getItemId(int position) {
		return historyItems.get(position).getId();
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		if(convertView != null) view = convertView;
		else {
			view = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_row, parent, false);
		}
		TextView vtitle = view.findViewById(R.id.history_row_title);
		TextView vmac = view.findViewById(R.id.history_row_mac);
		TextView vip = view.findViewById(R.id.history_row_ip);
		TextView vport = view.findViewById(R.id.history_row_port);
		StarButton star = view.findViewById(R.id.history_row_star);

		final HistoryIt item = (HistoryIt) getItem(position);

		// bind the cursor data to the form items
		vtitle.setText(item.getTitle());
		vmac.setText(item.getMac());
		vip.setText(item.getIp());
		vport.setText(Integer.toString(item.getPort()));

		if(this.showStars) {
			// remove click handler to prevent recursive calls
			star.setOnCheckedChangeListener(null);

			// change the star state if different
			boolean starred = (item.getStarred() != 0);	// non-zero == true
			star.setChecked(starred);
			star.render();

			// add event listener to star button
			star.setOnCheckedChangeListener(this);

			// save our record _ID in the star's tag
			star.setTag(item.getId());

		}else{
			// disable the star button
			star.setClickable(false);
			star.noRender = true;
			star.render();
		}
		return view;
	}

	@Override
	public int getItemViewType(int position) {
		return 0;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return historyItems.isEmpty();
	}

	@Nullable
	@Override
	public CharSequence[] getAutofillOptions() {
		return new CharSequence[0];
	}

	public void setHistoryItems(@Nullable List<HistoryIt> historyItems) {
		this.historyItems = historyItems!=null ? historyItems : Collections.<HistoryIt>emptyList();
	}
}
