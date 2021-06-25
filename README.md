# dremio-udf-example
An example of a user defined function deployed in Dremio 15.x

# Background

Dremio user defined functions (UDF) allow users to extend the list of functions that can be included in their SQL queries. These example shows how an row oriented function and an aggregate function can be implemented within the Dremio query engine.

NOTE: UDFs are not officially supported by Dremio, meaning you cannot raise a case with Dremio Support and expect them to help you with it. Assistance can only be made for a UDF in the form of billable Professional Services work.

### Row Oriented Functions

Some user defined functions may operate on values contained in a single result row, like this:

     SELECT concat_udf(first_name, last_name) AS name, hire_date, email
     FROM employees 
     WHERE hire_date > TO_DATE('2015-01-01', 'yyyy-MM-dd');

|name|hire_date|email|
|:--|:--|:--|
|Alexis Bull|1997-02-20|`ABULL@email.com`|
|Julia Dellinger|1998-06-24|`JDELLING@email.com`|
|Anthony Cabrio|1999-02-07|`ACABRIO@email.com`|
|Kelly Chung|1997-06-14|`KCHUNG@email.com`|
|Jennifer Dilly|1997-08-13|`JDILLY@email.com`|
|Timothy Gates|1998-07-11|`TGATES@email.com`|
|Randall Perkins|1999-12-19|`RPERKINS@email.com`|
|Sarah Bell|1996-02-04|`SBELL@email.com`|

### Aggreation Functions

Other user defined functions may operate on values across multiple result rows, like this:

     SELECT string_agg_udf(country_code, ',') AS countries
     FROM employees; 

|countries|
|:--|
|UK,FR,CN,US,GE,BY|

# These Examples

### UDF: concat_udf()

The concat_udf() example user defined function (UDF) is a single row oriented UDF that illustrates how to operate on multiple column values within each row. It implements a string concatenating function that takes two arguments and merges them into one output string. This example also shows how log entries can be written (from within the UDF) to the Dremio server log file via STDOUT and STDERR.
This example UDF is implemented in the source file:

     src/main/java/com/dremio/example_udfs/ConcatUDF.java

### UDF: string_agg_udf()

The string_agg_udf() example user defined function (UDF) is a aggregation function that operates on values from multiple rows, combinging the values into a single result row. This example implements a string_agg() like function that combines column values from a set of rows. It also shows how to use the Java System.getPropreties() method to read properties defined in the Dremio configuration file. Specifically the /opt/dremio/conf/dremio-env file. This example UDF is implemented in the source file:

     src/main/java/com/dremio/example_udfs/StringAggUDF.java

### UDF: parse_xml_udf()
The parse_xml_udf() example user defined function (UDF) reads xml formated content from a column and extracts elements from it based on an XPATH search expression (see: https://docs.oracle.com/javase/7/docs/api/javax/xml/xpath/package-summary.html). This UDF returns the extracted results as a simple JSON array.
This example UDF is implemented in the source file:

     src/main/java/com/dremio/example_udfs/ParseXmlUDF.java


# Requirements

To compile this source code, the following are required:

- Java JDK 1.8
- Maven 3.x
- Git
- A working Dremio 15.x or newer cluster for testing

# Usage

### Step 1. Clone this git repo

Clone this git repo with the commands:

     $ git clone https://github.com/gregpalmr/dremio-udf-example

     $ cd dremio-udf-example

### Step 2. Compile the source code

Use the following commands to compile the Java source code:

     $ mvn clean install -DskipTests

Upon a successful build the UDF's jar file will be placed in the target directory:

    $ ls ./target/dremio-udf-examples-15.5.0-*.jar

### Step 3. Copy the UDF's jar file to the Dremio Coordinator and Executor nodes

Copy the newly built JAR file to the Dremio nodes so that references to the UDF functions can be resolved correctly.

Stand-alone Deployment

If you are running Dremio as a "stand-alone" installation (on one or more servers or cloud instances), use scp or your favorate file copying utility to copy the jar file to every Dremio Coordinator and Executor node. Place the jar file in the Dremio 3rd party jars directory, which is usually found here:

     $ ls /opt/dremio/jars/3rdparty/

Kubernetes Deployment

If you are running Dremio on a Kubernetes cluster, then you must copy the jar file to the Docker container and save the container as a new image. Then upload that new image to your private container repository. The following commands may be used.

     $ docker run dremio/dremio-oss

     From a different command shell, copy the file to the Docker container:

     $ docker cp ./target/dremio-udf-examples-15.5.0-*.jar <CONTAINER_ID>:/opt/dremio/jars/3rdparty/

     Save the modified docker image to a new image:

     $ docker commit <CONTAINER_ID> <new image name>:<tag>

     Upload the modified docker image to your private container registry:
     $ docker push <hub-user>/<repo-name>:<tag>

Then modify the Dremio helm chart values.yaml file to use the new iamge tag. Like this:

     image: my-hub-user/dremio-oss
     imageTag: 15.3_UDFs

YARN Deployment

If you are running Dremio executors as YARN containers, then use scp or your favorate file copying utility to copy the jar file to every Dremio Coordinator node. Place the jar file in the Dremio 3rd party jars directory, which is usually found here:

     $ ls /opt/dremio/jars/3rdparty/

Then restart your Dremio engines to redeploy the Dremio Executors as YARN containers. The UDF jar file will be packaged by the Dremio Coordinator node and made available to each YARN container.


### Step 4. Restart all Dremio nodes

If you are running Dremio as a "stand-alone" installation (on 1 or more servers or cloud instances), then use the sysetmctl command to restart all of the Dremio Coordinator nodes and Executor nodes. Use this command on each node:

     $ systemctl restart dremio

If you are running Dremio on a Kubernetes cluster, then use the helm chart to restart all the Dremio pods. Use a command like this:

     $ helm upgrade my-dremio-cluster dremio_v2 -f values.dremio.yaml --namespace dremio

If you are running Dremio as YARN containers, then you must:

     a. Stop the Dremio engine (the YARN containers)
          - Use the Dremio Web UI or REST API

     b. Restart the Coordinator node (usually running on an Hadoop edge node server) 
         - Use the command: systemctl restart dremio

     c. Launch the Dremio engine (the YARN containers)
          - Use the Dremio Web UI or REST API

### Step 5. Test the UDFs

#### UDF: string_add_udf()

After restarting the Dremio cluster, run a test SQL command to see the results of the string_agg_udf() works. Like this:

     SQL> SELECT string_agg_udf(column_name, ',') AS columns 
          FROM (
            SELECT * FROM information_schema.columns
          )

|columns|
|:--|
|CATALOG_NAME,CATALOG_DESCRIPTION,CATALOG_CONNECT,TABLE_CATALOG,TABLE_SCHEMA,TABLE_NAME,COLUMN_NAME,O|

#### UDF: concat_udf()

After restarting the Dremio cluster, run a test SQL command to see the results of the concat_udf() works. Like this:
options

     SQL> SELECT concat_udf(table_name, concat_udf('.', column_name)) AS column_names 
          FROM (
            SELECT * FROM information_schema.columns
          )

|columns_names|
|:--|
|column_names|
|CATALOGS.CATALOG_NAME|
|CATALOGS.CATALOG_DESCRIPTION|
|CATALOGS.CATALOG_CONNECT|
|COLUMNS.TABLE_CATALOG|
|COLUMNS.TABLE_SCHEMA|
|COLUMNS.TABLE_NAME|
|COLUMNS.COLUMN_NAME|
|COLUMNS.ORDINAL_POSITION|
|COLUMNS.COLUMN_DEFAULT|
|COLUMNS.IS_NULLABLE|
|COLUMNS.DATA_TYPE|
|COLUMNS.COLUMN_SIZE|
|COLUMNS.CHARACTER_MAXIMUM_LENGTH|
|COLUMNS.CHARACTER_OCTET_LENGTH|
|COLUMNS.NUMERIC_PRECISION|
|COLUMNS.NUMERIC_PRECISION_RADIX|
|COLUMNS.NUMERIC_SCALE|
|COLUMNS.DATETIME_PRECISION|
|COLUMNS.INTERVAL_TYPE|
|COLUMNS.INTERVAL_PRECISION|
|SCHEMATA.CATALOG_NAME|
|SCHEMATA.SCHEMA_NAME|
|SCHEMATA.SCHEMA_OWNER|
|SCHEMATA.TYPE|
|SCHEMATA.IS_MUTABLE|
|TABLES.TABLE_CATALOG|
|TABLES.TABLE_SCHEMA|
|TABLES.TABLE_NAME|
|TABLES.TABLE_TYPE|
|VIEWS.TABLE_CATALOG|
|VIEWS.TABLE_SCHEMA|
|VIEWS.TABLE_NAME|
|VIEWS.VIEW_DEFINITION|
...

#### UDF: parse_xml_udf()

After restarting the Dremio cluster, run a test SQL command to see the results of the parse_xml_udf() works. Like this:

```
SELECT parse_xml_udf(
'<?xml version="1.0" encoding="UTF-8"?>
<epl>
  <region id="eastern">
    <player id="1">
      <name>Harry Kane</name>
      <position>First Base</position>
    </player>
  </region>
  <region id="western">
    <player id="2">
      <name>Bruno Fernandes</name>
      <position>Third Base</position>
    </player>
    <player id="3">
      <name>Sam Grady</name>
      <position>Picher</position>
    </player>
  </region>
</epl>',
'//epl/region[@id="western"]/player/name/text()') AS player_names 
FROM (VALUES(1));
```
|player_names|
|:--|
|{"results": ["Bruno Fernandes","Sam Grady"]}|

---

Direct comments or questions to greg@dremio.com
