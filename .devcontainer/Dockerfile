FROM docker.io/debian:trixie-slim

ARG SDK_VERSION=36
ARG BUILD_TOOLS_VERSION=36.1.0

# Install development dependencies
RUN apt-get update \
 && DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends \
    build-essential pkg-config musl-tools ca-certificates man-db \
    neovim less git curl wget unzip ripgrep locales file procps net-tools \
    bubblewrap socat \
    openjdk-21-jdk cpu-checker python3 protobuf-compiler \
 && apt-get clean && rm -rf /var/lib/apt/lists/*

# Locale setup
RUN echo 'en_US.UTF-8 UTF-8' > /etc/locale.gen \
 && locale-gen en_US.UTF-8 \
 && update-locale LANG=en_US.UTF-8
ENV LANG=en_US.UTF-8
ENV LC_ALL=en_US.UTF-8

# Create non-root user
RUN groupadd -r dev -g 1000 \
 && useradd -r -g dev -u 1000 -d /home/dev -s /bin/bash dev \
 && mkdir /home/dev && chown dev:dev /home/dev

# Create directories with user ownership
# Note: VS Code Dev Containers will automatically adjust UID/GID via updateRemoteUserUID
RUN mkdir -p /code && chown dev:dev /code
RUN mkdir -p /opt/android && chown dev:dev /opt/android

# Switch to non-root user
USER dev

# Download Android command line tools
RUN mkdir -p /opt/android/cmdline-tools \
 && cd /opt/android/cmdline-tools \
 && wget https://dl.google.com/android/repository/commandlinetools-linux-6609375_latest.zip \
 && unzip commandlinetools-linux-6609375_latest.zip \
 && rm commandlinetools-linux-6609375_latest.zip

# Install Android SDK
RUN yes Y | /opt/android/cmdline-tools/tools/bin/sdkmanager --licenses
RUN /opt/android/cmdline-tools/tools/bin/sdkmanager --install \
    "tools" \
    "platform-tools" \
    "platforms;android-${SDK_VERSION}" \
    "build-tools;${BUILD_TOOLS_VERSION}" \
    "emulator"
ENV ANDROID_SDK_ROOT=/opt/android
ENV PATH="$ANDROID_SDK_ROOT/cmdline-tools/tools/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"

# Create persistent config directory with default ownership
RUN mkdir -p ~/.config-persistent/

# Create Claude Code directories with default ownership
RUN mkdir -p ~/.config/claude/

# Create OpenCode directories with default ownership
RUN mkdir -p ~/.config/opencode/ \
 && mkdir -p ~/.local/share/opencode/

ENTRYPOINT ["/bin/bash"]
