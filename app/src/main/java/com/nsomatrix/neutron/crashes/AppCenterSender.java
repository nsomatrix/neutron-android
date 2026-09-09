/*
 * Copyright 2018 Nikita Shakarun
 * Copyright 2026 nsomatrix
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nsomatrix.neutron.crashes;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.acra.ReportField;
import org.acra.data.CrashReportData;
import org.acra.sender.ReportSender;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.nsomatrix.neutron.config.Config;
import com.nsomatrix.neutron.util.Constants;

public class AppCenterSender implements ReportSender {
	private static final String TAG = "NeutronCrashReporter";

	@Override
	public void send(@NonNull Context context, @NonNull final CrashReportData report) {
		File emuDir = new File(Config.getEmulatorDir());
		if (!emuDir.exists()) {
			emuDir.mkdirs();
		}
		String logFile = Config.getEmulatorDir() + "/crash.txt";
		try (FileOutputStream fos = new FileOutputStream(logFile)) {
			String logcat = report.getString(ReportField.LOGCAT);
			if (logcat != null) {
				fos.write(logcat.getBytes());
			}
			String stack = report.getString(ReportField.STACK_TRACE);
			if (stack != null) {
				fos.write("\n====================Error==================\n".getBytes());
				fos.write(stack.getBytes());
			}
			JSONObject o = (JSONObject) report.get(ReportField.CUSTOM_DATA.name());
			if (o != null) {
				Object od = o.opt(Constants.KEY_APPCENTER_ATTACHMENT);
				if (od != null) {
					String customData = (String) od;
					fos.write("\n==========application=info=============\n".getBytes());
					fos.write(customData.getBytes());
				}
			}
			fos.flush();
			Log.i(TAG, "Crash report successfully saved to " + logFile);
			new Handler(Looper.getMainLooper()).post(() ->
					Toast.makeText(context, "Crash report saved to:\n" + logFile, Toast.LENGTH_LONG).show());
		} catch (IOException e) {
			Log.e(TAG, "Failed to write crash log", e);
		}
	}
}
