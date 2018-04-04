/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.sdk;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService.SdkStatus;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ThreadTracker;
import java.time.Clock;
import java.util.TimerTask;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

/** Tests for {@link ManagedCloudSdkUpdater}. */
public class ManagedCloudSdkUpdaterTest {
  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Mock private ManagedCloudSdkService mockSdkService;
  @TestService @Mock private CloudSdkServiceManager mockSdkServiceManager;
  @Mock private Clock mockClock;

  @Spy private ManagedCloudSdkUpdater managedCloudSdkUpdater;

  @Before
  public void setUp() {
    // add timer thread to one not to be checked for 'leaks'
    ThreadTracker.longRunningThreadCreated(
        ApplicationManager.getApplication(), ManagedCloudSdkUpdater.SDK_UPDATER_THREAD_NAME);

    when(mockSdkServiceManager.getCloudSdkService()).thenReturn(mockSdkService);

    doReturn(mockClock).when(managedCloudSdkUpdater).getClock();

    // directly execute scheduled tasks.
    doAnswer(
            invocationOnMock -> {
              ((TimerTask) invocationOnMock.getArgument(0)).run();
              return null;
            })
        .when(managedCloudSdkUpdater)
        .schedule(any(), anyLong(), anyLong());
  }

  @Test
  public void update_scheduledNow_ifLastUpdate_elapsed() {
    CloudSdkServiceUserSettings.getInstance().setLastAutomaticUpdateTimestamp(1);
    when(mockClock.millis()).thenReturn(ManagedCloudSdkUpdater.SDK_UPDATE_INTERVAL + 2);

    managedCloudSdkUpdater.activate();

    verify(managedCloudSdkUpdater)
        .schedule(any(), eq(0L), eq(ManagedCloudSdkUpdater.SDK_UPDATE_INTERVAL));
  }

  @Test
  public void update_called_when_sdkStatusReady() {
    when(mockSdkService.getStatus()).thenReturn(SdkStatus.READY);

    managedCloudSdkUpdater.activate();

    // managed SDK is UI thread only,
    ApplicationManager.getApplication().invokeAndWait(() -> verify(mockSdkService).update());
  }

  @Test
  public void update_notCalled_when_sdkStatus_notReady() {
    when(mockSdkService.getStatus()).thenReturn(SdkStatus.INSTALLING);

    managedCloudSdkUpdater.activate();

    // managed SDK is UI thread only,
    ApplicationManager.getApplication()
        .invokeAndWait(() -> verify(mockSdkService, never()).update());
  }
}
