# ---- Colors ----
if tput setaf 1 &>/dev/null; then
	bold=$(tput bold)
	reset=$(tput sgr0)
	red=$(tput setaf 1)
	green=$(tput setaf 2)
	yellow=$(tput setaf 3)
	blue=$(tput setaf 4)
	magenta=$(tput setaf 5)
	cyan=$(tput setaf 6)
	white=$(tput setaf 7)
fi

# ---- Prompt ----
PROMPT_COMMAND='PS1_CMD1=$(git branch --show-current 2>/dev/null)'
PS1='\[\e[91;1m\]\u\[\e[0m\]@\[\e[38;5;26m\]\h\[\e[0m\] \w \[\e[93;3m\]${PS1_CMD1}\[\e[0m\]: '

# ---- Command History Improvements ----
HISTSIZE=50000
HISTFILESIZE=50000
HISTCONTROL=ignoreboth:erasedups
shopt -s histappend

# ---- Auto-Correction for cd ----
shopt -s cdspell
shopt -s checkwinsize

# ---- Aliases ----
alias c="clear"
alias ll="ls -lah"
alias ls="ls --color=auto"
alias la="ls -A"
alias l="ls -CF"
alias gs="git status"
alias gc="git commit"
alias gp="git push"
alias gl="git log --oneline --graph --decorate"
alias gb="git branch"
alias gd="git diff"

# Gradle (use wrapper when available)
alias gw="./gradlew"

# ---- Load local overrides ----
if [ -f ~/.bashrc.local ]; then
	source ~/.bashrc.local
fi

# ---- Go to flaggi code ----
if [ -d /workspaces/flaggi ]; then
	cd /workspaces/flaggi
fi
