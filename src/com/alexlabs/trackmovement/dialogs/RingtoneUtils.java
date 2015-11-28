package com.alexlabs.trackmovement.dialogs;

import java.util.ArrayList;
import java.util.List;

import com.alexlabs.trackmovement.App;
import com.alexlabs.trackmovement.Preferences;
import com.alexlabs.trackmovement.R;

public class RingtoneUtils {
	
	private static List<RingtoneEntry> _ringtoneEntryList = new ArrayList<RingtoneEntry>();
	
	static {
		_ringtoneEntryList.add(new RingtoneEntry(App.instance().getResources().getString(R.string.old_clock_ringing_short), R.raw.old_clock_ringing_short)); // Default
		_ringtoneEntryList.add(new RingtoneEntry(App.instance().getResources().getString(R.string.electronic_chime), R.raw.electronic_chime));
	}
	
	public static class RingtoneEntry {
		private String _ringtoneName;
		private int _ringtoneResId;
		
		public RingtoneEntry() {}
		
		public RingtoneEntry(String ringtoneName, int ringtoneResId) {
			_ringtoneName = ringtoneName;
			_ringtoneResId = ringtoneResId;
		}
		
		public int getRingtoneResId() {
			return _ringtoneResId;
		}
		
		public void setRingtoneResId(int ringtoneResId) {
			this._ringtoneResId = ringtoneResId;
		}
		
		public String getRingtoneName() {
			return _ringtoneName;
		}
		
		public void setRingtoneName(String ringtoneName) {
			this._ringtoneName = ringtoneName;
		}
	}
	
	public static String[] getRingtonePreferenceNames() {
		String[] names = new String[_ringtoneEntryList.size()];
		int i = 0;
		for(RingtoneEntry entry : _ringtoneEntryList) {
			names[i++] = entry.getRingtoneName();
		}
		
		return names;
	}
	
	public static int findSavedRingtonePos(Preferences prefs) {
		int i = 0;
		for(RingtoneEntry entry : _ringtoneEntryList) {
			if(entry.getRingtoneResId() == prefs.getRingtoneResId()) break;
			i++;
		}
		
		return i;
	}
	
	public static int gerResId(int resPos){
		return _ringtoneEntryList.get(resPos).getRingtoneResId();
	}
	
	public static String getRingtoneName(int resId) {
		String name = null;
		for(RingtoneEntry entry : _ringtoneEntryList) {
			if(entry.getRingtoneResId() == resId) {
				name = entry.getRingtoneName();
				break;
			}
		}
		
		return name;
	}
}
