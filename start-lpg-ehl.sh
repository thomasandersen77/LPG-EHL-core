java -Xms512m -Xmx512m \
  -XX:+UseG1GC -XX:MaxGCPauseMillis=100 \
  -Dspring.config.additional-location=file:release/config/ \
  -Dlogging.config=file:release/config/logback-spring.xml \
  -jar release/lpg-ehl-webapp.jar \
  --spring.profiles.active=field