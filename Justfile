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

uninstall:
    adb uninstall org.example.launchertest

install:
    adb install -r app/build/outputs/apk/debug/app-debug.apk

start:
    adb shell monkey -p org.example.launchertest -c android.intent.category.LAUNCHER 1

build-debug:
    ./gradlew :app:assembleDebug

update:
    just uninstall
    just install

refresh:
    just uninstall
    just install
    just start

run:
    just build-debug
    just update
    just start
