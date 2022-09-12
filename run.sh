#!/bin/sh

export COMPOSE_FILE_PATH="${PWD}/target/classes/docker/docker-compose.yml"

if [ -z "${M2_HOME}" ]; then
  export MVN_EXEC="mvn"
else
  export MVN_EXEC="${M2_HOME}/bin/mvn"
fi

start() {
    podman volume create alfresco-system-usage-statistics-acs-volume
    podman volume create alfresco-system-usage-statistics-db-volume
    podman volume create alfresco-system-usage-statistics-ass-volume
    podman-compose -f "$COMPOSE_FILE_PATH" up --build -d
}

start_share() {
    podman-compose -f "$COMPOSE_FILE_PATH" up --build -d alfresco-system-usage-statistics-share
}

start_acs() {
    podman-compose -f "$COMPOSE_FILE_PATH" up --build -d alfresco-system-usage-statistics-acs
}

down() {
    if [ -f "$COMPOSE_FILE_PATH" ]; then
        podman-compose -f "$COMPOSE_FILE_PATH" down
    fi
}

purge() {
    podman volume rm -f alfresco-system-usage-statistics-acs-volume
    podman volume rm -f alfresco-system-usage-statistics-db-volume
    podman volume rm -f alfresco-system-usage-statistics-ass-volume
}

build() {
    $MVN_EXEC clean package
}

build_share() {
    podman-compose -f "$COMPOSE_FILE_PATH" kill alfresco-system-usage-statistics-share
    yes | podman-compose -f "$COMPOSE_FILE_PATH" rm -f alfresco-system-usage-statistics-share
    $MVN_EXEC clean package -pl alfresco-system-usage-statistics-share,alfresco-system-usage-statistics-share-docker
}

build_acs() {
    podman-compose -f "$COMPOSE_FILE_PATH" kill alfresco-system-usage-statistics-acs
    yes | podman-compose -f "$COMPOSE_FILE_PATH" rm -f alfresco-system-usage-statistics-acs
    $MVN_EXEC clean package -pl alfresco-system-usage-statistics-integration-tests,alfresco-system-usage-statistics-platform,alfresco-system-usage-statistics-platform-docker
}

tail() {
    podman-compose -f "$COMPOSE_FILE_PATH" logs -f
}

tail_all() {
    podman-compose -f "$COMPOSE_FILE_PATH" logs --tail="all"
}

prepare_test() {
    $MVN_EXEC verify -DskipTests=true -pl alfresco-system-usage-statistics-platform,alfresco-system-usage-statistics-integration-tests,alfresco-system-usage-statistics-platform-docker
}

test() {
    $MVN_EXEC verify -pl alfresco-system-usage-statistics-platform,alfresco-system-usage-statistics-integration-tests
}

case "$1" in
  build_start)
    down
    build
    start
    tail
    ;;
  build_start_it_supported)
    down
    build
    prepare_test
    start
    tail
    ;;
  start)
    start
    tail
    ;;
  stop)
    down
    ;;
  purge)
    down
    purge
    ;;
  tail)
    tail
    ;;
  reload_share)
    build_share
    start_share
    tail
    ;;
  reload_acs)
    build_acs
    start_acs
    tail
    ;;
  build_test)
    down
    build
    prepare_test
    start
    test
    tail_all
    down
    ;;
  test)
    test
    ;;
  *)
    echo "Usage: $0 {build_start|build_start_it_supported|start|stop|purge|tail|reload_share|reload_acs|build_test|test}"
esac