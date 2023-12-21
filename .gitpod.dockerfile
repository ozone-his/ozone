FROM gitpod/workspace-full

RUN sudo apt-get update \
 && sudo apt-get install -y \
    gettext-base \
 && sudo rm -rf /var/lib/apt/lists/*