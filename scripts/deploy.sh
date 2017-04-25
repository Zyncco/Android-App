if [ "$TRAVIS_BRANCH" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ];
then
	sed -i 's/versionCode [0-9]\+/versionCode '"$TRAVIS_BUILD_NUMBER"'/' app/build.gradle
	./gradlew publishApkRelease
fi