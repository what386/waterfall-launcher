default:
    just --list

fmt:
    ktfmt --kotlinlang-style .

lint:
    ktfmt --kotlinlang-style --dry-run . | diff - /dev/null
    detekt --all-rules

test:
    ./gradlew test

run *args:
    ./gradlew run --args="{{args}}"

prepare version:
    lash run scripts/release/prepare.lash {{version}}

promote:
    just lint
    just test
    lash run scripts/release/promote.lash

publish version:
    lash run scripts/release/publish.lash {{version}}
    git switch dev
