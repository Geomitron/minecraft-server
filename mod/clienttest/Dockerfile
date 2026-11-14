# The in-game checks' machine: Java 25, a virtual screen (Xvfb) with Mesa's software OpenGL, and xdotool for real
# key presses and clicks. Nothing shows on the host's screen.
FROM eclipse-temurin:25-jdk-noble

RUN apt-get update \
	&& apt-get install -y --no-install-recommends \
		xvfb xdotool rsync \
		libgl1 libgl1-mesa-dri libglx-mesa0 libegl1 libegl-mesa0 \
		libx11-6 libxext6 libxcursor1 libxi6 libxrandr2 libxfixes3 libxss1 libxtst6 libxkbcommon0 libxkbcommon-x11-0 \
		libasound2t64 \
	&& rm -rf /var/lib/apt/lists/*

# Minecraft asks for an sRGB-capable framebuffer. Xvfb's GLX has no such visual, so SDL takes EGL, where Mesa has it.
ENV LIBGL_ALWAYS_SOFTWARE=1 \
	LP_NUM_THREADS=2 \
	SDL_VIDEO_FORCE_EGL=1
