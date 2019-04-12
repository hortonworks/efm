#!/bin/sh
#
# (c) 2018-2019 Cloudera, Inc. All rights reserved.
#
# This is a adaptation of the launch.script from the Spring Boot project.
#   https://github.com/spring-projects/spring-boot
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#
# Usage:
#
#   myapp.sh {start|stop|restart|status|print-env|run}"
#   myapp.sh {start|stop|restart|status|print-env|run}"
#
# Install (init.d service):
#
#   ln -s /path/to/this/file/myapp.sh /etc/init.d/myapp
#
# Environment Variables (all are optional and will be discovered/defaulted if not set externally)
#
#   DEBUG                  if set to anything, it will enable debug output
#   APP_HOME               the home directory of the app. defaults to the parent directory of the directory containing this file
#   APP_NAME               the name of the app, used as part of the default name of other files
#   APP_BIN_DIR            the directory of app's executable files. the directory containing this file
#   APP_LIB_DIR            the directory of app's library files. defaults to ${APP_HOME}/lib
#   APP_CONF_DIR           the directory of app's config files. defaults to ${APP_HOME}/conf
#   APP_LOG_DIR            the directory of app's log output files. defaults to ${APP_HOME}/logs
#   APP_RUN_DIR            the directory of app's runtime files (i.e., PID file). defaults to ${APP_HOME}/run
#   APP_BIN_FILE           the location of this file. defaults to ${APP_HOME}/bin/${APP_NAME}.sh
#   APP_CONF_FILE          the location of the app startup config file. defaults to ${APP_CONF_DIR}/${APP_NAME}.conf
#   APP_PROPS_FILE         the location of the app properties file. defaults to ${APP_CONF_DIR}/${APP_NAME}.properties
#   APP_JAR_FILE           the location of the runnable jar. defaults to ${APP_LIB_DIR}/${APP_NAME}.jar
#   APP_CLASSPATH          the Java classpath to use for running the app. defaults to ${APP_LIB_DIR}:${APP_CONF_DIR}
#   STOP_WAIT_TIME         the time to wait for the app to stop before timing out, defaults to 15 seconds
#   USE_START_STOP_DAEMON  if, when using service commands, the platform start-stop-daemon should be used
#   JAVA_OPTS              options to pass as arguments to the JVM
#   RUN_ARGS               arguments to pass to the main method of the application
#

[ -n "$DEBUG" ] && set -x

### Initialize variables that cannot be provided by a .conf file ###

# Initialize binFile and appBinDir based on $0; detect init.d symlink in process
WORKING_DIR="$(pwd)"
[ -n "$APP_BIN_FILE" ] && binFile="$APP_BIN_FILE"

# Follow symlinks to find the real jar and detect init.d script
cd "$(dirname "$0")" || exit 1
[ -z "$binFile" ] && binFile=$(pwd)/$(basename "$0")
while [ -L "$binFile" ]; do
  configfile="${binFile%.*}.conf"
  # shellcheck source=/dev/null
  [ -r "$configfile" ] && . "$configfile"
  binFile=$(readlink "$binFile")
  cd "$(dirname "$binFile")" || exit 1
  binFile=$(pwd)/$(basename "$binFile")
done
[ -n "$APP_BIN_DIR" ] && appBinDir="$APP_BIN_DIR"
[ -z "$appBinDir" ] && appBinDir="$( (cd "$(dirname "$binFile")" && pwd -P) )"

# Initialize appHome directory location, defaulting to $appBinDir/..
[ -n "$APP_HOME" ] && appHome="$APP_HOME"
[ -z "$appHome" ] && appHome="$( (cd "$appBinDir" && cd .. && pwd -P) )"

cd "$WORKING_DIR" || exit 1

# Initialize appName
[ -n "$APP_NAME" ] && appName="$APP_NAME"
[ -z "$appName" ] && appName="$(basename "${binFile%.*}")"

# Initialize appConfDir location, defaulting to ${APP_HOME}/conf
[ -n "$APP_CONF_DIR" ] && appConfDir="$APP_CONF_DIR"
[ -z "$appConfDir" ] && appConfDir="$appHome/conf"

[ -n "$APP_CONF_FILE" ] && appConfFile="$APP_CONF_FILE"
[ -z "$appConfFile" ] && [ -r "$appConfDir/$appName.conf" ] && appConfFile="$appConfDir/$appName.conf"
# shellcheck source=/dev/null
[ -r "$appConfFile" ] && . "$appConfFile"
# everything after this point can be overridden in conf file if you do not want to use the defaults/auto-discovered values

### Initialize variables that might be provided by the .conf file ###

# Initialize appLibDir location, defaulting to ${APP_HOME}/lib
[ -n "$APP_LIB_DIR" ] && appLibDir="$APP_LIB_DIR"
[ -z "$appLibDir" ] && appLibDir="$appHome/lib"

# Initialize appExtLibDir location, there is no default value
[ -n "$APP_EXT_LIB_DIR" ] && appExtLibDir="$APP_EXT_LIB_DIR"

# Initialize appLogDir location, defaulting to ${APP_HOME}/logs
[ -n "$APP_LOG_DIR" ] && appLogDir="$APP_LOG_DIR"
[ -z "$appLogDir" ] && appLogDir="$appHome/logs"

# Initialize appRunDir location, defaulting to ${APP_HOME}/run
[ -n "$APP_RUN_DIR" ] && appRunDir="$APP_RUN_DIR"
[ -z "$appRunDir" ] && appRunDir="$appHome/run"

# Initialize appPropsFile location, defaulting to ${APP_CONF_DIR}/${APP_NAME}.properties
[ -n "$APP_PROPS_FILE" ] && appPropsFile="$APP_PROPS_FILE"
[ -z "$appPropsFile" ] && appPropsFile="$appConfDir/$appName.properties"

# Initialize appJarFile location, defaulting to ${APP_LIB_DIR}/${APP_NAME}.jar
[ -n "$APP_JAR_FILE" ] && appJarFile="$APP_JAR_FILE"
[ -z "$appJarFile" ] && appJarFile="$appLibDir/$appName.jar"

# Initialize appClasspath, defaulting to ${APP_CONF_DIR}:${APP_LIB_DIR}
[ -n "$APP_CLASSPATH" ] && appClasspath="$APP_CLASSPATH"
[ -z "$appClasspath" ] && appClasspath="$appConfDir:$appLibDir"
[ -n "$appExtLibDir" ] && appClasspath="$appClasspath:$appExtLibDir"
[ -n "$appClasspath" ] && appLoaderPath=$( printf %s "$appClasspath" | sed 's/:/,/g')

# Initialize stopWaitTime, defaulting to 20 seconds
[ -n "$STOP_WAIT_TIME" ] && stopWaitTime="$STOP_WAIT_TIME"
[ -z "$stopWaitTime" ] && stopWaitTime=20

# Initialize useStartStopDaemon, defaulting to true
[ -n "$USE_START_STOP_DAEMON" ] && useStartStopDaemon="$USE_START_STOP_DAEMON"
[ -z "$useStartStopDaemon" ] && useStartStopDaemon=true


### Utility functions ###

# ANSI Colors
echoRed() { printf '\e[0;31m%s\e[0m\n' "$1"; }
echoGreen() { printf '\e[0;32m%s\e[0m\n' "$1"; }
echoYellow() { printf '\e[0;33m%s\e[0m\n' "$1"; }

checkPermissions() {
  touch "$pid_file" > /dev/null 2>&1 || { echoRed "Operation not permitted (cannot access pid file)"; return 4; }
  touch "$log_file" > /dev/null 2>&1 || { echoRed "Operation not permitted (cannot access log file)"; return 4; }
}

isRunning() {
  ps -p "$1" > /dev/null 2>&1
}

await_file() {
  end=$(date +%s)
  end=$(( end+10 ))
  while [ ! -s "$1" ]
  do
    now=$(date +%s)
    if [ "$now" -ge "$end" ]; then
      break
    fi
    sleep 1
  done
}

### MAIN bootstrap launcher ###

# Determine the script mode and action
action="$1"
shift

# Build the pid and log filenames
pid_file="$appRunDir/$appName.pid"
log_file="$appLogDir/$appName-app.log"

# Determine the user to run as. If the current user is root, run as user is set to the owner of this file
# shellcheck disable=SC2012
[ "$(id -u)" = "0" ] && run_user=$(ls -ld "$binFile" | awk '{print $3}')

# Find Java
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  javaexe="$JAVA_HOME/bin/java"
elif type -p java > /dev/null 2>&1; then
  javaexe=$(type -p java)
elif [ -x "/usr/bin/java" ];  then
  javaexe="/usr/bin/java"
else
  echo "Unable to find Java"
  exit 1
fi

# Set the arguments to be passed to java for startup, appending any arguments that were passed to this script
# Arguments assume Spring Boot PropertiesLauncher is being used (-Dloader.path=...)
# This `set -- ...` command sets the content $@. This is a workaround for the lack of array variables in the POSIX shell
# shellcheck disable=SC2086,SC2068
set -- "-Dsun.misc.URLClassPath.disableJarChecking=true" \
       "-Dloader.path=$appLoaderPath" \
       $JAVA_OPTS "-jar" "$appJarFile" \
       "--spring.config.name=$appName" \
       "--spring.config.location=\"classpath:/,classpath:/config/,classpath:/conf/,file:./,file:./config/,file:./conf/,$appConfDir/,$appPropsFile\"" \
       $RUN_ARGS \
       $@

# Action functions
print_env() {
  echoGreen
  echoGreen "The following environment configuration was determined:"
  echoGreen
  echoGreen "APP_NAME=$appName"
  echoGreen "APP_HOME=$appHome"
  echoGreen "APP_BIN_DIR=$appBinDir"
  echoGreen "APP_CONF_DIR=$appConfDir"
  echoGreen "APP_LIB_DIR=$appLibDir"
  echoGreen "APP_LOG_DIR=$appLogDir"
  echoGreen "APP_RUN_DIR=$appRunDir"
  echoGreen "APP_BIN_FILE=$binFile"
  echoGreen "APP_CONF_FILE=$appConfFile"
  echoGreen "APP_PROPS_FILE=$appPropsFile"
  echoGreen "APP_JAR_FILE=$appJarFile"
  echoGreen "APP_CLASSPATH=$appClasspath"
  echoGreen "JAVA_OPTS=$JAVA_OPTS"
  echoGreen "RUN_ARGS=$RUN_ARGS"
  echoGreen "STOP_WAIT_TIME=$stopWaitTime"
  echoGreen "USE_START_STOP_DAEMON=$useStartStopDaemon"
  echoGreen
  echoGreen "java: $javaexe"
  echoGreen "java args: $(printf "%s " "$@")"
  echoGreen
  echoGreen "To enable service script debugging:  export DEBUG=true"
  echoGreen "To disable service script debugging: unset DEBUG"
  echoGreen
  return 0;
}

start() {
  if [ -f "$pid_file" ]; then
    pid=$(cat "$pid_file")
    isRunning "$pid" && { echoYellow "Already running [$pid]"; return 0; }
  fi
  do_start "$@"
}

do_start() {
  cd "$appHome" || { echoRed "Unable to \`cd $appHome\`"; return 1; }
  if [ ! -e "$appRunDir" ]; then
    mkdir -p "$appRunDir" > /dev/null 2>&1
    if [ -n "$run_user" ]; then
      chown "$run_user" "$appRunDir"
    fi
  fi
  if [ ! -e "$appLogDir" ]; then
    mkdir -p "$appLogDir" > /dev/null 2>&1
    if [ -n "$run_user" ]; then
      chown "$run_user" "$appLogDir"
    fi
  fi
  if [ ! -e "$log_file" ]; then
    touch "$log_file" > /dev/null 2>&1
    if [ -n "$run_user" ]; then
      chown "$run_user" "$log_file"
    fi
  fi
  if [ -n "$run_user" ]; then
    checkPermissions || return $?
    if [ ${useStartStopDaemon} = true ] && type start-stop-daemon > /dev/null 2>&1; then
      start-stop-daemon --start --quiet \
        --chuid "$run_user" \
        --name "$appName" \
        --make-pidfile --pidfile "$pid_file" \
        --background --no-close \
        --startas "$javaexe" \
        --chdir "$appHome" \
        -- "$@" \
        >> "$log_file" 2>&1
      await_file "$pid_file"
    else
      su -s /bin/sh -c "$javaexe $(printf "\"%s\" " "$@") >> \"$log_file\" 2>&1 & echo \$!" "$run_user" > "$pid_file"
    fi
    pid=$(cat "$pid_file")
  else
    checkPermissions || return $?
    nohup "$javaexe" "$@" >> "$log_file" 2>&1 &
    pid=$!
    echo "$pid" > "$pid_file"
  fi
  [ -z "$pid" ] && { echoRed "Failed to start"; return 1; }
  echoGreen "Started [$pid]"
}

stop() {
  cd "$appHome" || { echoRed "Unable to \`cd $appHome\`"; return 1; }
  [ -f "$pid_file" ] || { echoYellow "Not running (pidfile not found)"; return 0; }
  pid=$(cat "$pid_file")
  isRunning "$pid" || { echoYellow "Not running (process ${pid}). Removing stale pid file."; rm -f "$pid_file"; return 0; }
  do_stop "$pid" "$pid_file"
  # if did not stop gracefully, attempt to force kill
  isRunning "$pid" && { echoRed "Attempting to force kill process."; return 0; } && do_force_stop "$pid" "$pid_file"
}

do_stop() {
  kill "$1" > /dev/null 2>&1 || { echoRed "Unable to kill process $1"; return 1; }
  for i in $(seq 1 ${stopWaitTime}); do
    isRunning "$1" || { echoGreen "Stopped [$1]"; rm -f "$2"; return 0; }
    [ "$i" -eq $((stopWaitTime/2)) ] && kill "$1" > /dev/null 2>&1
    sleep 1
  done
  echoRed "Unable to kill process $1";
  return 1;
}

force_stop() {
  [ -f "$pid_file" ] || { echoYellow "Not running (pidfile not found)"; return 0; }
  pid=$(cat "$pid_file")
  isRunning "$pid" || { echoYellow "Not running (process ${pid}). Removing stale pid file."; rm -f "$pid_file"; return 0; }
  do_force_stop "$pid" "$pid_file"
}

do_force_stop() {
  kill -9 "$1" > /dev/null 2>&1 || { echoRed "Unable to kill process $1"; return 1; }
  for i in $(seq 1 ${stopWaitTime}); do
    isRunning "$1" || { echoGreen "Stopped [$1]"; rm -f "$2"; return 0; }
    [ "$i" -eq $((stopWaitTime/2)) ] && kill -9 "$1" > /dev/null 2>&1
    sleep 1
  done
  echoRed "Unable to kill process $1";
  return 1;
}

restart() {
  stop && start "$@"
}

force_restart() {
  cd "$appHome" || { echoRed "Unable to \`cd $appHome\`"; return 1; }
  [ -f "$pid_file" ] || { echoRed "Not running (pidfile not found)"; return 7; }
  pid=$(cat "$pid_file")
  rm -f "$pid_file"
  isRunning "$pid" || { echoRed "Not running (process ${pid} not found)"; return 7; }
  do_stop "$pid" "$pid_file"
  do_start "$@"
}

status() {
  cd "$appHome" || { echoRed "Unable to \`cd $appHome\`"; return 1; }
  [ -f "$pid_file" ] || { echoRed "Not running"; return 3; }
  pid=$(cat "$pid_file")
  isRunning "$pid" || { echoRed "Not running (process ${pid} not found)"; return 1; }
  echoGreen "Running [$pid]"
  return 0
}

# shellcheck disable=SC2164
run() {
  print_env "$@"
  user_dir="$(pwd)"
  cd "$appHome" || ( echoRed "Unable to \`cd $appHome\`"; return 1 )
  "$javaexe" "$@"
  result=$?
  cd "$user_dir" > /dev/null 2>&1
  return "$result"
}

# Call the appropriate action function
case "$action" in
start)
  start "$@"; exit $?;;
stop)
  stop "$@"; exit $?;;
restart)
  #restart "$@"; exit $?;;
  force_restart "$@"; exit $?;;
# removed these actions and instead altered 'stop' to attempt force-stop if graceful shutdown fails
#force-stop)
#  force_stop "$@"; exit $?;;
#force-restart)
#  force_restart "$@"; exit $?;;
status)
  status "$@"; exit $?;;
print-env)
  print_env "$@"; exit $?;;
run)
  run "$@"; exit $?;;
*)
  echo "Usage: $(basename "$0") {start|stop|restart|status|print-env|run}"; exit 1;
esac

exit 0
