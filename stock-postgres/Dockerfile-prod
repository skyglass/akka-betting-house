FROM debezium/postgres:15

ENV TZ=Europe/Amsterdam
ENV PGTZ=Europe/Amsterdam
ENV POSTGRES_PASSWORD=dbpass
ENV POSTGRES_USER=postgres
ENV CUSTOMER_DB_USER=customerdbuser
ENV CUSTOMER_DB_PASSWORD=dbpass
ENV CUSTOMER_DB_NAME=customerdb
ENV ORDER_DB_USER=orderdbuser
ENV ORDER_DB_PASSWORD=dbpass
ENV ORDER_DB_NAME=orderdb
ENV INVENTORY_DB_USER=inventorydbuser
ENV INVENTORY_DB_PASSWORD=dbpass
ENV INVENTORY_DB_NAME=inventorydb

RUN DEBIAN_FRONTEND="noninteractive" apt-get -y install tzdata
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY scripts/ /docker-entrypoint-initdb.d/
