/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.jps;

import com.google.cloud.tools.appengine.api.debug.DefaultGenRepoInfoFileConfiguration;
import com.google.cloud.tools.appengine.api.debug.GenRepoInfoFile;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkGenRepoInfoFile;
import com.google.cloud.tools.appengine.cloudsdk.internal.process.ExitCodeRecorderProcessExitListener;
import com.google.cloud.tools.intellij.jps.model.JpsStackdriverModuleExtension;
import com.google.cloud.tools.intellij.jps.model.impl.JpsStackdriverModuleExtensionImpl;

import com.intellij.openapi.diagnostic.Logger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.ModuleChunk;
import org.jetbrains.jps.builders.DirtyFilesHolder;
import org.jetbrains.jps.builders.java.JavaModuleBuildTargetType;
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor;
import org.jetbrains.jps.incremental.BuilderCategory;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.ModuleBuildTarget;
import org.jetbrains.jps.incremental.ModuleLevelBuilder;
import org.jetbrains.jps.incremental.ProjectBuildException;
import org.jetbrains.jps.model.module.JpsModule;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

/**
 * Generates source context files for a module using the Cloud SDK.
 */
public class GenRepoInfoFileModuleBuilder extends ModuleLevelBuilder {

  private static final Logger LOG =
      Logger.getInstance("#com.google.cloud.tools.intellij.jps.GenRepoInfoFileModuleBuilder");

  public GenRepoInfoFileModuleBuilder() {
    super(BuilderCategory.INITIAL);
  }

  /**
   * Generates source context files for each module in {@param chunk}.
   *
   * <p>Generation is parameterized by the Stackdriver facet associated to the module.
   *
   * <p>Files are added to the same location as the project's compiled .class files.
   */
  @Override
  public ExitCode build(CompileContext context, ModuleChunk chunk,
      DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget> dirtyFilesHolder,
      OutputConsumer outputConsumer) throws ProjectBuildException, IOException {
    for (JpsModule jpsModule : chunk.getModules()) {
      JpsStackdriverModuleExtension extension = jpsModule.getContainer().getChild(
          JpsStackdriverModuleExtensionImpl.ROLE);

      if (!extension.isGenerateSourceContext()) {
        continue;
      }

      if (jpsModule.getSourceRoots().isEmpty()) {
        LOG.warn("Module " + jpsModule.getName() + " contains no source roots. Moving on to the "
            + "next module for source generation.");
        continue;
      }

      if (extension.getModuleSourceDirectory() == null) {
        // Possible to happen on Windows, unlikely in Linux.
        LOG.warn("Invalid module source directory. Check for special characters <>:\"|?*.");
        continue;
      }

      Path sourceDirectory = extension.getModuleSourceDirectory();
      ModuleBuildTarget target =
          new ModuleBuildTarget(jpsModule, JavaModuleBuildTargetType.PRODUCTION);
      Path outputDirectory = target.getOutputDir().toPath();

      if (extension.getCloudSdkPath() == null) {
        LOG.warn("No Cloud SDK path specified. Skipping source context generation.");
        // Cloud SDK path is a singleton for a project, so if it doesn't exist on the first module,
        // we can return right away.
        return ExitCode.NOTHING_DONE;
      }

      ExitCodeRecorderProcessExitListener exitListener = new ExitCodeRecorderProcessExitListener();
      CloudSdk sdk = new CloudSdk.Builder()
          .sdkPath(extension.getCloudSdkPath())
          .exitListener(exitListener)
          .build();

      GenRepoInfoFile genAction = new CloudSdkGenRepoInfoFile(sdk);
      DefaultGenRepoInfoFileConfiguration configuration = new DefaultGenRepoInfoFileConfiguration();
      configuration.setSourceDirectory(sourceDirectory.toFile());
      configuration.setOutputDirectory(outputDirectory.toFile());
      genAction.generate(configuration);

      if (exitListener.getMostRecentExitCode() != 0 && !extension.isIgnoreErrors()) {
        LOG.warn("gen-repo-info-file command returned with status code "
            + exitListener.getMostRecentExitCode());
        return ExitCode.ABORT;
      }

      outputConsumer.registerOutputFile(
          target,
          outputDirectory.resolve("source-context.json").toFile(),
          Collections.<String>emptyList());
      outputConsumer.registerOutputFile(
          target,
          outputDirectory.resolve("source-contexts.json").toFile(),
          Collections.<String>emptyList());
    }
    return ExitCode.OK;
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return "Stackdriver source context generator";
  }
}
