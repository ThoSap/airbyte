FROM fishtownanalytics/dbt:1.0.0

USER root
WORKDIR /tmp
# Install the OS/Python prerequisites for the Oracle Instant Client
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    libaio-dev \
    libaio1

# Download and install the Oracle Instant Client and the cx_Oracle Python Oracle Database interface
RUN mkdir -p /opt/oracle \
 && wget https://download.oracle.com/otn_software/linux/instantclient/19600/instantclient-basic-linux.x64-19.6.0.0.0dbru.zip \
 && unzip instantclient-basic-linux.x64-19.6.0.0.0dbru.zip -d /opt/oracle \
 && rm -f instantclient-basic-linux.x64-19.6.0.0.0dbru.zip \
 && pip install cx_Oracle

# Set the Oracle environment vars
ENV ORACLE_HOME=/opt/oracle/instantclient_19_6
ENV LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$ORACLE_HOME \
    TNS_ADMIN=/opt/oracle/instantclient_19_6/network/admin

COPY --from=airbyte/base-airbyte-protocol-python:0.1.1 /airbyte /airbyte

# Install SSH Tunneling dependencies
RUN apt-get update && apt-get install -y jq sshpass \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /airbyte
COPY entrypoint.sh .
COPY build/sshtunneling.sh .

WORKDIR /airbyte/normalization_code
COPY normalization ./normalization
COPY setup.py .
COPY dbt-project-template/ ./dbt-template/
COPY dbt-project-template-oracle/* ./dbt-template/

WORKDIR /airbyte/base_python_structs
RUN pip install .

# Install python dependencies
WORKDIR /airbyte/normalization_code
RUN pip install .
# Based on https://github.com/oracle/dbt-oracle/tree/v1.0.0
RUN git clone https://github.com/ThoSap/dbt-oracle \
 && cd dbt-oracle \
 && git checkout fix/incremental_upsert_1.0.3 \
 && pip install . \
 && cd .. \
 && rm -rf dbt-oracle

WORKDIR /airbyte/normalization_code/dbt-template/
# Download external dbt dependencies
RUN dbt deps

WORKDIR /airbyte
ENV AIRBYTE_ENTRYPOINT=/airbyte/entrypoint.sh
ENTRYPOINT ["/airbyte/entrypoint.sh"]

LABEL io.airbyte.name=airbyte/normalization-oracle
