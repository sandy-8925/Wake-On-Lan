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

import android.app.LocalActivityManager;

import android.os.Bundle;

import android.content.Context;

import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.Menu;

import net.mafro.android.wakeonlan.databinding.ActivityMainBinding;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;


/**
 *	@desc	Base activity, handles all UI events except history ListView clicks
 */
public class WakeOnLanActivity extends AppCompatActivity implements OnTabChangeListener
{
	public static final String TAG = "WakeOnLan";

	public static final int MENU_ITEM_WAKE = Menu.FIRST;
	public static final int MENU_ITEM_DELETE = Menu.FIRST + 1;
	static final String SORT_MODE_PREFS_KEY = "sort_mode";

	private static int _editModeID = 0;
	private static boolean typingMode = false;

	private static boolean isTablet = false;
	private TabHost th;

	private static int sort_mode;

	public static final int CREATED = 0;
	public static final int LAST_USED = 1;
	public static final int USED_COUNT = 2;

	private static final String[] PROJECTION = new String[]
	{
		History.Items._ID,
		History.Items.TITLE,
		History.Items.MAC,
		History.Items.IP,
		History.Items.PORT,
		History.Items.LAST_USED_DATE,
		History.Items.USED_COUNT,
		History.Items.IS_STARRED
	};

	private static Toast notification;
	private ActivityMainBinding binding;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
		binding.wolaViewpager.setAdapter(new TabsAdapter(getSupportFragmentManager(), WakeOnLanActivity.this));
		binding.wolaTablayout.setupWithViewPager(binding.wolaViewpager);
//		doStuff();
	}

	private void doStuff() {
		// configure tabs
		th = findViewById(R.id.tabhost);

		// tabs only exist in phone layouts
		if(th != null) {
			WakeOnLanActivity.isTablet = true;

			LocalActivityManager lam = new LocalActivityManager(this, false);
			th.setup(lam);

			th.addTab(th.newTabSpec("tab_history").setIndicator(getString(R.string.tab_history), getResources().getDrawable(R.drawable.ical)).setContent(R.id.historyview));
			th.addTab(th.newTabSpec("tab_wake").setIndicator(getString(R.string.tab_wake), getResources().getDrawable(R.drawable.wake)).setContent(R.id.wakeview));

			th.setCurrentTab(0);

			// register self as tab changed listener
			th.setOnTabChangedListener(this);
		}else{
			// set the background colour of the titles
			TextView historytitle = findViewById(R.id.historytitle);
			historytitle.setBackgroundColor(0xFF999999);
			TextView waketitle = findViewById(R.id.waketitle);
			waketitle.setBackgroundColor(0xFF999999);
		}
	}

	public void onTabChanged(String tabId)
	{
		if(tabId.equals("tab_wake")) {
			// enter typing mode - no clear of form until exit typing mode
			typingMode = true;

		}else if(tabId.equals("tab_history")) {
			// set form back to defaults, if typing mode has ended (button was clicked)
			if(!typingMode) {
				EditText vtitle = findViewById(R.id.title);
				EditText vmac = findViewById(R.id.mac);
				EditText vip = findViewById(R.id.ip);
				EditText vport = findViewById(R.id.port);

				vtitle.setText(null);
				vmac.setText(null);
				vip.setText(MagicPacket.BROADCAST);
				vport.setText(Integer.toString(MagicPacket.PORT));

				// clear any errors
				vmac.setError(null);

				// reset both our button's text
				Button sendWake = findViewById(R.id.send_wake);
				sendWake.setText(R.string.button_wake);
				Button clearWake = findViewById(R.id.clear_wake);
				clearWake.setText(R.string.button_clear);
			}
		}
	}

	public static String sendPacket(HistoryItem item, Context context)
	{
		return sendPacket(context, item.title, item.mac, item.ip, item.port);
	}

	public static String sendPacket(Context context,String title, String mac, String ip, int port)
	{
		String formattedMac;

		try {
			formattedMac = MagicPacket.send(mac, ip, port);

		}catch(IllegalArgumentException iae) {
			notifyUser(context.getString(R.string.send_failed)+":\n"+iae.getMessage(), context);
			return null;

		}catch(Exception e) {
			notifyUser(context.getString(R.string.send_failed), context);
			return null;
		}

		// display sent message to user
		notifyUser(context.getString(R.string.packet_sent)+" to "+title, context);
		return formattedMac;
	}


	public static void notifyUser(String message, Context context)
	{
		if(notification != null) {
			notification.setText(message);
			notification.show();
		} else {
			notification = Toast.makeText(context, message, Toast.LENGTH_SHORT);
			notification.show();
		}
	}

	private static class TabsAdapter extends FragmentStatePagerAdapter {

        private final Context context;

        TabsAdapter(@NonNull FragmentManager fm, @NonNull Context context) {
			super(fm);
            this.context = context;
        }

		@Override
		public Fragment getItem(int position) {
			TabFragments tabFragmentEnum = TabFragments.values()[position];
			return Fragment.instantiate(context, tabFragmentEnum.getClazz().getName());
		}

		@Override
		public int getCount() {
			return TabFragments.values().length;
		}

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return context.getResources().getString(TabFragments.values()[position].getTitle());
        }
    }
}

