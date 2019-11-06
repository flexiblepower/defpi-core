FROM node:alpine AS builder
COPY WebContent /usr/local/orchestrator-ui
RUN cd /usr/local/orchestrator-ui && \
    yarn install

FROM nginx:stable-alpine
COPY --from=builder /usr/local/orchestrator-ui /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --timeout=3s CMD wget -qO - http://localhost:80 || exit 1
CMD /bin/sh -c "envsubst '\$ORCHESTRATOR_URL' < /main.js.template > /usr/share/nginx/html/js/main.js && nginx -g 'daemon off;'"
COPY main.js.template /
