#!/usr/bin/env bash

if [[ -t 1 ]]; then
    _R="\033[0m" _BOLD="\033[1m"
    _RED="\033[31m" _GREEN="\033[32m" _YELLOW="\033[33m" _CYAN="\033[36m"
else
    _R="" _BOLD="" _RED="" _GREEN="" _YELLOW="" _CYAN=""
fi

log_inf() { echo -e "${_CYAN}[INF]${_R} $*"; }
log_ok()  { echo -e "${_GREEN}[OK ]${_R} $*"; }
log_wrn() { echo -e "${_YELLOW}[WRN]${_R} $*"; }
log_err() { echo -e "${_RED}[ERR]${_R} $*" >&2; }
die()     { log_err "$*"; exit 1; }
