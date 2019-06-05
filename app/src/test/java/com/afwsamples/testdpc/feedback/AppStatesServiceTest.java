/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.afwsamples.testdpc.feedback;

import static android.util.Log.ERROR;
import static android.util.Log.INFO;
import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build.VERSION_CODES;
import androidx.enterprise.feedback.KeyedAppState;
import androidx.enterprise.feedback.ReceivedKeyedAppState;
import androidx.preference.PreferenceManager;
import com.afwsamples.testdpc.R;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

@RunWith(RobolectricTestRunner.class)
@Config(minSdk=VERSION_CODES.N) // Feedback channel is supported from N onwards
public class AppStatesServiceTest {

  private static final String REQUEST_SYNC_LOG_TEXT = "SYNC REQUESTED";

  private static final ReceivedKeyedAppState STATE1 =
      ReceivedKeyedAppState.builder()
          .setPackageName("test.package")
          .setTimestamp(123L)
          .setSeverity(KeyedAppState.SEVERITY_INFO)
          .setKey("key1")
          .setMessage("message1")
          .setData("data1")
          .build();

  private static final ReceivedKeyedAppState STATE1_DIFFERENT_MESSAGE =
      ReceivedKeyedAppState.builder()
          .setPackageName("test.package")
          .setTimestamp(123L)
          .setSeverity(KeyedAppState.SEVERITY_INFO)
          .setKey("key1")
          .setMessage("different message1")
          .setData("data1")
          .build();

  private static final ReceivedKeyedAppState STATE2 =
      ReceivedKeyedAppState.builder()
          .setPackageName("test.package")
          .setTimestamp(123L)
          .setSeverity(KeyedAppState.SEVERITY_ERROR)
          .setKey("key2")
          .setMessage("message2")
          .setData("data2")
          .build();


  private static final ReceivedKeyedAppState INFO_STATE = STATE1;
  private static final ReceivedKeyedAppState ERROR_STATE = STATE2;

  private final Context context = RuntimeEnvironment.application;
  private final SharedPreferences preferences =
      PreferenceManager.getDefaultSharedPreferences(context);
  private final NotificationManager notificationManager =
    context.getSystemService(NotificationManager.class);
  private final AppStatesService service =
    Robolectric.buildService(AppStatesService.class).get();

  @Test
  public void onReceive_shouldNotNotify_noNotification() {
    setNotificationPreference(false);

    service.onReceive(Arrays.asList(STATE1), /* requestSync= */ false);

    assertThat(shadowOf(notificationManager).getActiveNotifications()).isEmpty();
  }

  @Test
  public void onReceive_shouldNotNotify_noLogs() {
    setNotificationPreference(false);

    service.onReceive(Arrays.asList(STATE1), /* requestSync= */ false);

    assertThat(ShadowLog.getLogsForTag(AppStatesService.TAG)).isEmpty();
  }

  @Test
  public void onReceive_shouldNotify_logContainsRequiredInformation() {
    setNotificationPreference(true);

    service.onReceive(Arrays.asList(STATE1), /* requestSync= */ false);

    assertThatLogContainsRequiredInformation(
        ShadowLog.getLogsForTag(AppStatesService.TAG).get(0), STATE1);
  }

  private void assertThatLogContainsRequiredInformation(
      ShadowLog.LogItem logItem, ReceivedKeyedAppState state) {
    assertThat(logItem.msg).contains(Long.toString(state.timestamp()));
    assertThat(logItem.msg).contains(state.packageName());
    assertThat(logItem.msg).contains(state.key());
    assertThat(logItem.msg).contains(state.data());
    assertThat(logItem.msg).contains(state.message());
  }

  @Test
  public void onReceive_infoLog_shouldNotify_logIsInfoLevel() {
    setNotificationPreference(true);

    service.onReceive(Arrays.asList(INFO_STATE), /* requestSync= */ false);

    assertThat(ShadowLog.getLogsForTag(AppStatesService.TAG).get(0).type).isEqualTo(INFO);
  }

  @Test
  public void onReceive_errorLog_shouldNotify_logIsErrorLevel() {
    setNotificationPreference(true);

    service.onReceive(Arrays.asList(ERROR_STATE), /* requestSync= */ false);

    assertThat(ShadowLog.getLogsForTag(AppStatesService.TAG).get(0).type).isEqualTo(ERROR);
  }

  @Test
  public void onReceive_shouldNotify_noRequestSync_logDoesNotContainRequestSync() {
    setNotificationPreference(true);

    service.onReceive(Arrays.asList(STATE1), /* requestSync= */ false);

    ShadowLog.LogItem logItem = ShadowLog.getLogsForTag(AppStatesService.TAG).get(0);
    assertThat(logItem.msg).doesNotContain(REQUEST_SYNC_LOG_TEXT);
  }

  @Test
  public void onReceive_shouldNotify_requestSync_logContainsRequestSync() {
    setNotificationPreference(true);

    service.onReceive(Arrays.asList(STATE1), /* requestSync= */ true);

    ShadowLog.LogItem logItem = ShadowLog.getLogsForTag(AppStatesService.TAG).get(0);
    assertThat(logItem.msg).contains(REQUEST_SYNC_LOG_TEXT);
  }

  @Test
  public void onReceive_shouldNotify_oneLogPerState() {
    setNotificationPreference(true);

    service.onReceive(Arrays.asList(STATE1, STATE2), /* requestSync= */ false);

    assertThat(ShadowLog.getLogsForTag(AppStatesService.TAG)).hasSize(2);
  }

  @Test
  public void onReceive_shouldNotify_notificationContainsRequiredInformation() {
    setNotificationPreference(true);

    service.onReceive(Arrays.asList(STATE1), /* requestSync= */ false);

    assertThatNotificationContainsRequiredInformation(
        shadowOf(notificationManager).getAllNotifications().get(0),
        STATE1
    );
  }

  private void assertThatNotificationContainsRequiredInformation(
      Notification notification, ReceivedKeyedAppState state) {
    assertThat(shadowOf(notification).getContentTitle().toString()).contains(state.packageName());
    assertThat(shadowOf(notification).getContentTitle().toString()).contains(state.key());
    assertThat(shadowOf(notification).getContentText().toString())
      .contains(Long.toString(state.timestamp()));
    assertThat(shadowOf(notification).getContentText().toString()).contains(state.data());
    assertThat(shadowOf(notification).getContentText().toString()).contains(state.message());
  }

  @Test
  public void onReceive_infoLog_shouldNotify_notificationTitleIncludesInfo() {
    setNotificationPreference(true);

    service.onReceive(Arrays.asList(INFO_STATE), /* requestSync= */ false);

    final Notification notification = shadowOf(notificationManager).getAllNotifications().get(0);
    assertThat(
        shadowOf(notification).getContentTitle().toString()).contains("INFO");
  }

  @Test
  public void onReceive_errorLog_shouldNotify_notificationTitleIncludesError() {
    setNotificationPreference(true);

    service.onReceive(Arrays.asList(ERROR_STATE), /* requestSync= */ false);

    final Notification notification = shadowOf(notificationManager).getAllNotifications().get(0);
    assertThat(
        shadowOf(notification).getContentTitle().toString()).contains("ERROR");
  }

  @Test
  public void onReceive_shouldNotify_noRequestSync_notificationDoesNotContainRequestSync() {
    setNotificationPreference(true);

    service.onReceive(Arrays.asList(STATE1), /* requestSync= */ false);

    final Notification notification = shadowOf(notificationManager).getAllNotifications().get(0);
    assertThat(
        shadowOf(notification).getContentText().toString()).doesNotContain(REQUEST_SYNC_LOG_TEXT);
  }

  @Test
  public void onReceive_shouldNotify_requestSync_notificationContainsRequestSync() {
    setNotificationPreference(true);

    service.onReceive(Arrays.asList(STATE1), /* requestSync= */ true);

    final Notification notification = shadowOf(notificationManager).getAllNotifications().get(0);
    assertThat(shadowOf(notification).getContentText().toString()).contains(REQUEST_SYNC_LOG_TEXT);
  }

  @Test
  public void onReceive_shouldNotify_oneNotificationPerKey() {
    setNotificationPreference(true);

    service.onReceive(Arrays.asList(STATE1, STATE2), /* requestSync= */ false);

    assertThat(shadowOf(notificationManager).getAllNotifications()).hasSize(2);
  }

  @Test
  public void onReceive_multiple_shouldNotify_oneNotificationPerKey() {
    setNotificationPreference(true);
    service.onReceive(Arrays.asList(STATE1), /* requestSync= */ false);

    service.onReceive(Arrays.asList(STATE2), /* requestSync= */ false);

    assertThat(shadowOf(notificationManager).getAllNotifications()).hasSize(2);
  }

  @Test
  public void onReceive_shouldNotify_sameKeyUpdatesNotification() {
    setNotificationPreference(true);
    service.onReceive(Arrays.asList(STATE1), /* requestSync= */ false);

    service.onReceive(Arrays.asList(STATE1_DIFFERENT_MESSAGE), /* requestSync= */ false);

    assertThat(shadowOf(notificationManager).getAllNotifications()).hasSize(1);
  }

  private void setNotificationPreference(boolean shouldNotify) {
    preferences.edit()
        .putBoolean(context.getString(R.string.app_feedback_notifications), shouldNotify)
        .commit();
  }
}