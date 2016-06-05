/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.cloud.tools.intellij.appengine.cloud;

import com.google.cloud.tools.app.impl.cloudsdk.internal.process.ProcessExitListener;
import com.google.cloud.tools.app.impl.cloudsdk.internal.process.ProcessOutputLineListener;
import com.google.cloud.tools.app.impl.cloudsdk.internal.process.ProcessStartListener;
import com.google.cloud.tools.app.impl.cloudsdk.internal.sdk.CloudSdk;
import com.google.cloud.tools.intellij.appengine.cloud.CloudSdkAppEngineHelper.Environment;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.impl.CancellableRunnable;
import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance.DeploymentOperationCallback;
import com.intellij.remoteServer.runtime.log.LoggingHandler;

import java.io.File;
import java.io.IOException;

/**
 * Provides basic Gcloud based App Engine functionality for our Cloud Tools plugin.
 */
public interface AppEngineHelper {

  /**
   * The project within the context of this helper.
   */
  Project getProject();

  /**
   * The path to the gcloud command on the local file system.
   */
  File getGcloudCommandPath();

  /**
   * The App Engine environment.
   */
  Environment getEnvironment();

  /**
   * The default app.yaml to use.
   */
  File defaultAppYaml();

  /**
   * The default Dockerfile we suggest for custom flexible deployments.
   *
   * @param deploymentArtifactType depending on the artifact type we provide a different default
   *                               Dockerfile
   * @return A {@link java.io.File} path to the default Dockerfile
   */
  File defaultDockerfile(AppEngineFlexDeploymentArtifactType deploymentArtifactType);

  /**
   * Creates a {@link Runnable} that will execute the tasks necessary for deployment to an App
   * Engine environment.
   *
   * @param loggingHandler logging messages will be output to this
   * @param artifactToDeploy the {@link File} path to the Java artifact to be deployed
   * @param deploymentConfiguration the configuration specifying the deployment
   * @param callback a callback for handling completions of the operation
   * @return the runnable that will perform the deployment operation
   */
  CancellableRunnable createDeployRunner(
      LoggingHandler loggingHandler,
      File artifactToDeploy,
      AppEngineDeploymentConfiguration deploymentConfiguration,
      DeploymentOperationCallback callback);

  /**
   * Creates a temporary staging directory on the local filesystem.
   *
   * @param loggingHandler logging messages will be output to this
   * @return the file representing the staging directory
   * @throws IOException if the staging fails
   */
  File createStagingDirectory(LoggingHandler loggingHandler) throws IOException;

  /**
   * Creates an {@link CloudSdk} object that is used in execution of various App Engine actions.
   *
   * @param loggingHandler logging messages will be output to this
   * @param startListener the "callback" listener used for fetching the running process.
   * @param logListener the output listener for handling "normal" operation log messages
   * @param outputListener the output listener for handling the output messages of the operation
   * @param exitListener the listener for handling the completeion of the operation
   * @return the {@link CloudSdk} object used in executing the operation
   */
  CloudSdk createSdk(
      LoggingHandler loggingHandler,
      ProcessStartListener startListener,
      ProcessOutputLineListener logListener,
      ProcessOutputLineListener outputListener,
      ProcessExitListener exitListener);

  /**
   * Locally stages user credentials to support various App Engine actions.
   */
  void stageCredentials(String googleUsername);

  /**
   * Delets the locally staged credentials, if they exist.
   */
  void deleteCredentials();
}
