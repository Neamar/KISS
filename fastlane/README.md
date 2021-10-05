fastlane documentation
================
Publishing to beta in a nutshell: update versionCode in build.gradle, create a new tag, push to Github.
Generate the aab file, then run:
```
fastlane android images
fastlane android beta
```

----
# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```
xcode-select --install
```

Install _fastlane_ using
```
[sudo] gem install fastlane -NV
```
or alternatively using `brew install fastlane`

# Available Actions
## Android
### android beta
```
fastlane android beta
```
Upload to beta channel
### android prod
```
fastlane android prod
```
Promote beta to production
### android sync
```
fastlane android sync
```
Update current production description
### android images
```
fastlane android images
```
Generate images for all locales

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.
More information about fastlane can be found on [fastlane.tools](https://fastlane.tools).
The documentation of fastlane can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
