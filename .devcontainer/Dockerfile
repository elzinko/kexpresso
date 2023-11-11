FROM gradle:jdk19

# Or your actual UID, GID on Linux
ARG USERNAME=vscode
ARG USER_UID=1001
ARG USER_GID=$USER_UID

ENV SDKMAN_DIR /usr/local/sdkman
ENV KOTLIN_PATH ${SDKMAN_DIR}/candidates/kotlin/current/

RUN set -o errexit -o nounset \
    && echo "Adding vscode user and group" \
    && groupadd --system --gid 1001 vscode \
    && useradd --system --gid vscode --uid 1001 --shell /bin/bash --create-home vscode \
    && mkdir /home/vscode/.vscode \
    && chown --recursive vscode:vscode /home/vscode \
    \
    && echo "Symlinking root Gradle cache to gradle Gradle cache" \
    && ln -s /home/vscode/.vscode /root/.vscode

RUN set -x \
    && apt-get update \
    && apt-get install -y unzip zip --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*

RUN curl -s get.sdkman.io | bash

RUN set -x \
    && echo "sdkman_auto_answer=true" > $SDKMAN_DIR/etc/config \
    && echo "sdkman_auto_selfupdate=false" >> $SDKMAN_DIR/etc/config \
    && echo "sdkman_insecure_ssl=false" >> $SDKMAN_DIR/etc/config \
    && chmod a+x "$SDKMAN_DIR/bin/sdkman-init.sh" \
    && echo "source $SDKMAN_DIR/bin/sdkman-init.sh" >> /home/vscode/.bashrc \
    && echo "alias kts='java -jar $KOTLIN_PATH/lib/kotlin-compiler.jar -script'" >> /home/vscode/.bashrc

RUN ["/bin/bash", "-c", "source $SDKMAN_DIR/bin/sdkman-init.sh && sdk install kotlin"]