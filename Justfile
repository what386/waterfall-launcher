default:
    just --list

fmt:
    ktfmt --kotlinlang-style .

lint:
    ktfmt --kotlinlang-style --dry-run . | diff - /dev/null
    detekt --all-rules

test:
    ./gradlew test

prepare version:
    lash run scripts/release/prepare.lash {{version}}

promote:
    just lint
    just test
    lash run scripts/release/promote.lash

publish version:
    lash run scripts/release/publish.lash {{version}}
    git switch dev

build-debug:
    ./gradlew :app:assembleDebug

build-release:
    ./gradlew :app:assembleRelease
    "$ANDROID_SDK_ROOT/build-tools/35.0.0/apksigner" sign --ks "$HOME/.android/debug.keystore" --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out app/build/outputs/apk/release/app-release.apk app/build/outputs/apk/release/app-release-unsigned.apk

dist:
    mkdir -p dist
    cp app/build/outputs/apk/release/app-release.apk dist/waterfall-launcher.apk

uninstall:
    adb uninstall org.example.launchertest

install-debug:
    adb install -r app/build/outputs/apk/debug/app-debug.apk

install-release:
    adb install -r app/build/outputs/apk/release/app-release.apk

start-app:
    adb shell monkey -p org.example.launchertest -c android.intent.category.LAUNCHER 1

run-debug:
    just build-debug
    just install-debug
    just start-app

run-release:
    just build-release
    just install-release
    just start-app

refresh-release:
    just build-release
    just install-release

gen-release:
    just build-release
    just dist
