FROM debezium/postgres:15

# Set environment variables
ENV TZ=Europe/Amsterdam
ENV PGTZ=Europe/Amsterdam
ENV POSTGRES_PASSWORD=dbpass
ENV POSTGRES_USER=postgres

RUN DEBIAN_FRONTEND="noninteractive" apt-get -y install tzdata
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY scripts/ /docker-entrypoint-initdb.d/
