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

package com.google.cloud.tools.intellij.stackdriver.debugger.actions;

import com.google.cloud.tools.intellij.CloudToolsRunConfigurationAction;
import com.google.cloud.tools.intellij.stackdriver.debugger.CloudDebugConfigType;
import com.google.cloud.tools.intellij.stackdriver.debugger.StackdriverDebuggerBundle;
import com.google.cloud.tools.intellij.stackdriver.debugger.StackdriverDebuggerIcons;

/** Creates a shortcut to the Stackdriver debugger configuration in the tools menu. */
public class CloudDebuggerToolsMenuAction extends CloudToolsRunConfigurationAction {

  public CloudDebuggerToolsMenuAction() {
    super(
        CloudDebugConfigType.getInstance(),
        StackdriverDebuggerBundle.message("appengine.tools.menu.debug.text"),
        StackdriverDebuggerBundle.message("appengine.tools.menu.debug.description"),
        StackdriverDebuggerIcons.STACKDRIVER_DEBUGGER);
  }
}