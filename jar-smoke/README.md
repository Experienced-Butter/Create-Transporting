# Jar smoke test: loads the built mod jar from run/mods instead of the Gradle source set, so the
# packaging (mods.toml, mixin config, assets, classes) is verified the way users load it.

cd jar-smoke
mkdir -p run/mods
cp ../build/libs/create_village_transit-*.jar run/mods/
echo "eula=true" > run/eula.txt
../gradlew -p . runServer --no-daemon
