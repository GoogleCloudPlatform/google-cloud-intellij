/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService.SdkStatus;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.log.TestInMemoryLogger;
import com.google.cloud.tools.intellij.util.ThreadUtil;
import com.google.cloud.tools.managedcloudsdk.ManagedCloudSdk;
import com.google.cloud.tools.managedcloudsdk.UnsupportedOsException;
import com.google.cloud.tools.managedcloudsdk.command.CommandExecutionException;
import com.google.cloud.tools.managedcloudsdk.components.SdkComponent;
import com.google.cloud.tools.managedcloudsdk.components.SdkComponentInstaller;
import com.google.cloud.tools.managedcloudsdk.install.SdkInstaller;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;

/** Tests for {@link ManagedCloudSdkService} */
public class ManagedCloudSdkServiceTest {

  @Rule public CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  private final Path MOCK_SDK_PATH = Paths.get("/tools/gcloud");

  @Spy private ManagedCloudSdkService sdkService;

  @Mock private ManagedCloudSdk mockManagedCloudSdk;

  @Mock private CloudSdkService.SdkStatusUpdateListener mockStatusUpdateListener;

  @Before
  public void setUp() throws UnsupportedOsException {
    doReturn(mockManagedCloudSdk).when(sdkService).createManagedSdk();
    // TODO(ivanporty) remove once test logging system is done via CloudToolsRule
    sdkService.setLogger(new TestInMemoryLogger());
    // make sure everything in test is done synchronously
    ExecutorService directExecutorService = MoreExecutors.newDirectExecutorService();
    ThreadUtil.getInstance().setBackgroundExecutorService(directExecutorService);
    // run UI updates synchronously
    doAnswer(
            invocation -> {
              ((Runnable) invocation.getArgument(0)).run();
              return null;
            })
        .when(sdkService)
        .invokeOnApplicationUIThread(any());
  }

  @Test
  public void initial_service_notActivated_status_notAvailable() {
    assertThat(sdkService.getStatus()).isEqualTo(SdkStatus.NOT_AVAILABLE);
  }

  @Test
  public void initial_service_notActivated_path_isNull() {
    assertThat((Object) sdkService.getSdkHomePath()).isNull();
  }

  @Test
  public void activate_service_sdkInstalled_status_ready() {
    makeMockSdkInstalled(MOCK_SDK_PATH);

    sdkService.activate();

    assertThat(sdkService.getStatus()).isEqualTo(SdkStatus.READY);
  }

  @Test
  public void activate_service_sdkInstalled_sdkPath_valid() {
    makeMockSdkInstalled(MOCK_SDK_PATH);

    sdkService.activate();

    assertThat((Object) sdkService.getSdkHomePath()).isEqualTo(MOCK_SDK_PATH);
  }

  @Test
  public void install_isSupported() {
    makeMockSdkInstalled(MOCK_SDK_PATH);

    assertThat(sdkService.install()).isTrue();
  }

  @Test
  public void removeListener_does_remove() {
    sdkService.addStatusUpdateListener(mockStatusUpdateListener);
    sdkService.removeStatusUpdateListener(mockStatusUpdateListener);
    emulateMockSdkInstallationProcess(MOCK_SDK_PATH);

    verifyNoMoreInteractions(mockStatusUpdateListener);
  }

  @Test
  public void successful_install_returnsValidSdkPath() {
    emulateMockSdkInstallationProcess(MOCK_SDK_PATH);
    sdkService.install();

    assertThat((Object) sdkService.getSdkHomePath()).isEqualTo(MOCK_SDK_PATH);
  }

  @Test
  public void successful_install_changesSdkStatus_inProgress() {
    sdkService.addStatusUpdateListener(mockStatusUpdateListener);

    emulateMockSdkInstallationProcess(MOCK_SDK_PATH);
    sdkService.install();

    ArgumentCaptor<SdkStatus> statusCaptor = ArgumentCaptor.forClass(SdkStatus.class);
    verify(mockStatusUpdateListener, times(2)).onSdkStatusChange(any(), statusCaptor.capture());

    assertThat(statusCaptor.getAllValues())
        .isEqualTo(Arrays.asList(SdkStatus.INSTALLING, SdkStatus.READY));
  }

  @Test
  public void install_thenException_changesSdkStatus_inProgress() throws Exception {
    sdkService.addStatusUpdateListener(mockStatusUpdateListener);
    emulateMockSdkInstallationProcess(MOCK_SDK_PATH);
    SdkInstaller mockInstaller = mockManagedCloudSdk.newInstaller();
    when(mockInstaller.install(any())).thenThrow(new IOException("IO Error"));
    sdkService.install();

    ArgumentCaptor<SdkStatus> statusCaptor = ArgumentCaptor.forClass(SdkStatus.class);
    verify(mockStatusUpdateListener, times(2)).onSdkStatusChange(any(), statusCaptor.capture());

    assertThat(statusCaptor.getAllValues())
        .isEqualTo(Arrays.asList(SdkStatus.INSTALLING, SdkStatus.NOT_AVAILABLE));
  }

  @Test
  public void install_appEngineException_changesSdkStatus_inProgress() throws Exception {
    sdkService.addStatusUpdateListener(mockStatusUpdateListener);
    emulateMockSdkInstallationProcess(MOCK_SDK_PATH);
    SdkComponentInstaller mockComponentInstaller = mockManagedCloudSdk.newComponentInstaller();
    doThrow(new CommandExecutionException(new UnsupportedOperationException()))
        .when(mockComponentInstaller)
        .installComponent(any(), any());
    sdkService.install();

    ArgumentCaptor<SdkStatus> statusCaptor = ArgumentCaptor.forClass(SdkStatus.class);
    verify(mockStatusUpdateListener, times(2)).onSdkStatusChange(any(), statusCaptor.capture());

    assertThat(statusCaptor.getAllValues())
        .isEqualTo(Arrays.asList(SdkStatus.INSTALLING, SdkStatus.NOT_AVAILABLE));
  }

  @Test
  public void interruptedInstall_status_notAvailable() throws Exception {
    emulateMockSdkInstallationProcess(MOCK_SDK_PATH);
    SdkInstaller sdkInstaller = mockManagedCloudSdk.newInstaller();
    when(sdkInstaller.install(any())).thenThrow(new InterruptedException());
    when(mockManagedCloudSdk.newInstaller()).thenReturn(sdkInstaller);

    sdkService.install();

    assertThat(sdkService.getStatus()).isEqualTo(SdkStatus.NOT_AVAILABLE);
  }

  /** Mocks managed SDK as if installed and having App Engine Component. */
  private void makeMockSdkInstalled(Path mockSdkPath) {
    try {
      when(mockManagedCloudSdk.isInstalled()).thenReturn(true);
      when(mockManagedCloudSdk.hasComponent(SdkComponent.APP_ENGINE_JAVA)).thenReturn(true);
      when(mockManagedCloudSdk.getSdkHome()).thenReturn(mockSdkPath);
    } catch (Exception ex) {
      // shouldn't happen in the tests.
      throw new AssertionError(ex);
    }
  }

  /** Mocks successful installation process with all steps included (SDK, App Engine Java). */
  private void emulateMockSdkInstallationProcess(Path mockSdkPath) {
    try {
      when(mockManagedCloudSdk.isInstalled()).thenReturn(false);
      SdkInstaller mockInstaller = mock(SdkInstaller.class);
      when(mockManagedCloudSdk.newInstaller()).thenReturn(mockInstaller);
      when(mockInstaller.install(any())).thenReturn(mockSdkPath);

      when(mockManagedCloudSdk.hasComponent(SdkComponent.APP_ENGINE_JAVA)).thenReturn(false);
      SdkComponentInstaller mockComponentInstaller = mock(SdkComponentInstaller.class);
      when(mockManagedCloudSdk.newComponentInstaller()).thenReturn(mockComponentInstaller);

      when(mockManagedCloudSdk.getSdkHome()).thenReturn(mockSdkPath);
    } catch (Exception ex) {
      // shouldn't happen in the tests.
      throw new AssertionError(ex);
    }
  }
}
