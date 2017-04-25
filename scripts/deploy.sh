
sed -i 's/versionCode [0-9]\+/versionCode '"$TRAVIS_BUILD_NUMBER"'/' app/build.gradle

./gradlew publishApkRelease