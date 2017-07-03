FROM nginx:stable-alpine
COPY WebContent /usr/share/nginx/html
CMD /bin/sh -c "envsubst '\$ORCHESTRATOR_URL' < /usr/share/nginx/html/js/main.js.template > /usr/share/nginx/html/js/main.js && nginx -g 'daemon off;'"