Installation
============

Currently, this project is built as an RPM package for RHEL8 and later. The RPM will install the binaries to `/opt/dans.knaw.nl/dd-dataverse-ingest` and the
configuration files to `/etc/opt/dans.knaw.nl/dd-dataverse-ingest`.

Building from source
--------------------

Prerequisites:

* Java 17 or higher
* Maven 3.3.3 or higher
* RPM

Steps:

    git clone https://github.com/DANS-KNAW/dd-dataverse-ingest.git
    cd dd-dataverse-ingest 
    mvn clean install
