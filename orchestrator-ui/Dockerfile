FROM nginx:stable-alpine
COPY WebContent /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --timeout=3s CMD wget -qO - http://localhost:80 || exit 1
CMD /bin/sh -c "envsubst '\$ORCHESTRATOR_URL' < /usr/share/nginx/html/js/main.js.template > /usr/share/nginx/html/js/main.js && nginx -g 'daemon off;'"
