FROM debian:stable-slim AS builder
WORKDIR /app
RUN apt-get update && apt-get install -y curl git unzip xz-utils zip && rm -rf /var/lib/apt/lists/*
ENV PATH="/usr/local/flutter/bin:/usr/local/flutter/bin/cache/dart-sdk/bin:${PATH}"
RUN git clone https://github.com/flutter/flutter.git -b stable /usr/local/flutter
COPY . /app
RUN flutter pub get
RUN flutter build web --release

FROM nginx:alpine
COPY --from=builder /app/build/web /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf

# Bind dynamically to Railway's assigned $PORT
CMD ["sh", "-c", "sed -i 's/listen 80;/listen '\"${PORT:-80}\"';/g' /etc/nginx/nginx.conf && sed -i 's/listen \\[::\\]:80;/listen \\[::\\]:'\"${PORT:-80}\"';/g' /etc/nginx/nginx.conf && nginx -g 'daemon off;'"]
