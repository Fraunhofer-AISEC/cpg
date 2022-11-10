#!/bin/bash

GRADLE_PROPERTIES_FILE=gradle.properties

function getProperty {
    local PROP_KEY=$1
    local PROP_VALUE=`cat $GRADLE_PROPERTIES_FILE | grep "$PROP_KEY" | grep -o -E "true|false"`
    case $PROP_VALUE in
      "true" ) echo "enabled";;
      "false") echo "disabled";;
    esac
}

function setProperty {
  local PROP_KEY=$1
  local PROP_VALUE=$2
  sed -i '' "s/\(${PROP_KEY}[[:space:]]*=[[:space:]]*\).*\$/\1${PROP_VALUE}/" $GRADLE_PROPERTIES_FILE
}

function ask() {
  local _ask_answer
  while true; do
    read -p "$1 [Y/n/a] " _ask_answer
    case $_ask_answer in
      y|yes|"" ) echo "true" ; break; ;;
      n|no     ) echo "false" ; break; ;;
      a|abort  ) exit 1; ;;
    esac
  done
}

echo "Hi, this is a short script to configure the inclusion of experimental language frontends."
echo "This script only changes the 'gradle.properties' file."
echo "When you run into build problems after enabling a frontend please make sure that you have installed all the"
echo "necessary dependencies. There is a reason why they are not enabled by default."
echo "You can always rerun this script in order to disable experimental frontends again."
echo ""

answerGo=$(ask "Do you want to enable the Go frontend? (currently $(getProperty "enableGoFrontend"))")
setProperty "enableGoFrontend" $answerGo
answerPython=$(ask "Do you want to enable the Python frontend? (currently $(getProperty "enablePythonFrontend"))")
setProperty "enablePythonFrontend" $answerPython
answerLLVM=$(ask "Do you want to enable the LLVM frontend? (currently $(getProperty "enableLLVMFrontend"))")
setProperty "enableLLVMFrontend" $answerLLVM
answerTypescript=$(ask "Do you want to enable the TypeScript frontend? (currently $(getProperty "enableTypeScriptFrontend"))")
setProperty "enableTypeScriptFrontend" $answerTypescript
