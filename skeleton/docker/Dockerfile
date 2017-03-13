FROM java:alpine

ADD packaged-jar-with-dependencies.jar /



LABEL org.flexiblepower.serviceName="PV Panel Simulation" org.flexiblepower.interfaces="[{\"name\":\"Widget\",\"cardinality\":1,\"autoConnect\":true,\"subscribeHash\":\"d711d463073b4333c021e363a590f5b3dbf0a140da4189d71ed3b81d7b87b5c1\",\"publishHash\":\"6b068b7fac431c719d6600f31667da6e41bc96db61d6919e4c7251b0a15cd445\"},{\"name\":\"Efi\",\"cardinality\":1,\"autoConnect\":true,\"subscribeHash\":\"375d1aa10c119dfe9f602ab99ec1442f9cf8032fcaa7269da998b52415b494f8\",\"publishHash\":\"99707099c8667d42044cbaf4f92dc96c334aa442f6831a8cddedb6a655e8bfca\"}]" org.flexiblepower.mappings="[]" org.flexiblepower.ports="[]"

ENTRYPOINT ["java", "-jar", "/packaged-jar-with-dependencies.jar"]