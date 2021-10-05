Publishing to beta in a nutshell: update versionCode in build.gradle, generate the aab file, and create a .txt file under `metadata/en-US/changelogs/XXX.txt`.a

Create a new tag, push to Github.
Then run:
```
fastlane android images
fastlane android beta
```
