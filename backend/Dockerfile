FROM java:8
EXPOSE 8080

ENV PGHOST="ups"
ENV PGUSER="ups"
ENV PGDATABASE="ups"
ENV PGPORT="ups"
ENV PGPASSWORD="ups"

ADD target/app.jar /app.jar

CMD java -cp /app.jar clojure.main -m app.core
