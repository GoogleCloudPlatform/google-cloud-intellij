![Google Cloud Platform Logo](https://cloud.google.com/_static/images/gcp-logo.png)
# Google Cloud Tools for IntelliJ plugin

|  | Build Status | 
| :--- | :---: |
| Ubuntu | ![Build Status](https://storage.googleapis.com/cloud-tools-for-java-kokoro-build-badges/intellij-ubuntu-master-orb.png) |
| Windows | ![Build Status](https://storage.googleapis.com/cloud-tools-for-java-kokoro-build-badges/intellij-windows-master-orb.png) |
| MacOS | ![Build Status](https://storage.googleapis.com/cloud-tools-for-java-kokoro-build-badges/intellij-macos-master-orb.png) |
| Latest EAP Snapshot| ![Build Status](https://storage.googleapis.com/cloud-tools-for-java-kokoro-build-badges/intellij-ubuntu-master-eap-orb.png) |

The plugin integrates the [Google Cloud Platform](https://cloud.google.com/)
into the IntelliJ IDEA UI. Currently this includes:

* [Google Cloud Source Repositories](https://cloud.google.com/tools/cloud-repositories/) 
  Fully-featured private Git repositories hosted on Google Cloud Platform.
* The [Google Cloud Debugger](https://cloud.google.com/tools/cloud-debugger/) 
  The Cloud Debugger can inspect the state of a Java application running on 
  [Google App Engine](https://cloud.google.com/appengine/)
  at any code location without stopping the application.
* [Google App Engine](https://cloud.google.com/appengine/docs/) deployment via the Cloud SDK.
* [Google Cloud Java Client Libraries](https://github.com/GoogleCloudPlatform/google-cloud-java/) 
  Add Java client libraries to your project and enable Google Cloud APIs.

For detailed user documentation go to our documentation
 [website](https://cloud.google.com/tools/intellij/docs/).

## Supported Platforms

* IntelliJ IDEA Community Edition 2016.3 or later
* IntelliJ IDEA Ultimate Edition 2016.3 or later

## Installation

You can find our plugin in the Jetbrains plugin repository by going to IntelliJ -> Settings -> Browse Repositories, and search for 'Google Cloud Tools'. 

### Pre-releases 

The pre-release binaries are being deployed to the Jetbrains plugin repository on an alpha
channel. To install them please perform the following steps:

1. Install the Google Cloud Tools plugin
    1. Use the same steps as step 1 but use the following URL `https://plugins.jetbrains.com/plugins/alpha/8079`
    1. When installing look for the 'Google Cloud Tools' plugin.

You can also grab the latest nightly build of the plugin by following the same steps as above but 
replacing 'alpha' with 'nightly' in the URLs.

If you wish to build this plugin from source, please see the
[contributor instructions](https://github.com/GoogleCloudPlatform/google-cloud-intellij/blob/master/CONTRIBUTING.md).

## FAQ


**None yet**
