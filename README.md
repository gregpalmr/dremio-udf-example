# dremio-udf-example
An example of a user defined function deployed in Dremio 15.x

# Background

Dremio user defined functions (UDF) allow users to extend the list of functions that can be included in their SQL queries. This example shows how an aggregate function can be implemented within the Dremio query engine.

Some user defined functions may operate on values contained in a single result row, like this:

     SELECT my_string_concat_udf(first_name, last_name) AS name, hire_date, email
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

Other user defined functions, like this example, may operate on values from multiple result rows, like this:

     SELECT my_string_agg_udf(country_code, ',') AS countries
     FROM employees; 

|countries|
|:--|
|UK,FR,CN,US,GE,BY|

NOTE: UDFs are not officially supported by Dremio, meaning you cannot raise a case with Dremio Support and expect them to help you with it. Assistance can only be made for a UDF in the form of billable Professional Services work.

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

Upon a successful build the UDF's jar file will be placed in the targets directory:

    $ ls ./target/udf-string-agg-1.0.0.jar

### Step 3. Copy the UDF's jar file to the Dremio Coordinator and Executor nodes

If you are running Dremio as a "stand-alone" installation (on one or more servers or cloud instances), use scp or your favorate file copying utility to copy the udf-string-agg-1.0.0.jar file to every Dremio Coordinator and Executor node. Place the jar file in the Dremio 3rd party jars directory, which is usually found here:

     $ ls /opt/dremio/jars/3rdparty/

If you are running Dremio on a Kubernetes cluster, then you must copy the udf-string-agg-1.0.0.jar file to the Docker container and save the container as a new image. Then upload that new image to your private container repository. The following commands may be used.

     $ docker run dremio/dremio-oss

     From a different command shell, copy the file to the Docker container:

     $ docker cp ./target/udf-string-agg-1.0.0.jar <CONTAINER_ID>:/opt/dremio/jars/3rdparty/

     Save the modified docker image to a new image:

     $ docker commit <CONTAINER_ID> <new image name>:<tag>

     Upload the modified docker image to your private container registry:
     $ docker push <hub-user>/<repo-name>:<tag>

Then modify the Dremio helm chart values.yaml file to use the new iamge tag. Like this:

     image: my-hub-user/dremio-oss
     imageTag: 15.3_UDFs

### Step 4. Restart all Dremio nodes

If you are running Dremio as a "stand-alone" installation (on 1 or more servers or cloud instances), then use the sysetmctl command to restart all of the Dremio Coordinator nodes and Executor nodes. Use this command on each node:

     $ systemctl restart dremio

If you are running Dremio on a Kubernetes cluster, then use the helm chart to restart all the Dremio pods. Use a command like this:

     $ helm upgrade my-dremio-cluster dremio_v2 -f values.dremio.yaml --namespace dremio


### Step 5. Test the UDF

After restarting the Dremio cluster, run a test SQL command to see the results of the string_agg UDF. Like this:

     SQL> SELECT string_agg(name, ',') FROM sys.options

|options|
|:--|
|acceleration.orphan.cleanup_in_milliseconds,accelerator.enable.subhour.policies,accelerator.enable_agg_join,accelerator.enable_default_raw,accelerator.enable_multijoin,accelerator.matching.filter_threshold,accelerator.matching.timeout_seconds,accelerator.matching.tracing,accelerator.raw.remove_project,accelerator.simplified_match,accelerator.system.limit,accelerator.system.verbose.logging,auth.personal-access-token.max_lifetime_days,auth.personal-access-tokens.enabled,catalog.refresh.permissions.canedit.enabled,client.max_metadata_count,client.tools.powerbi,client.tools.qlik,client.tools.tableau,client.use_legacy_catalog_name,coordinator.alive_queries.limit,coordinator.heap.monitoring.enable,coordinator.heap.monitoring.thresh.percentage,coordinator.metadata.refreshes.concurrency,coordinator.reconcile.queries.enable,coordinator.reconcile.queries.frequency.secs,dac.download.from_jobs_store,dac.download.records_limit,dac.format.preview.batch_size,dac.format.preview.fast_preview,dac.format.preview.max_ms,dac.format.preview.min_records,dac.format.preview.target,dac.search.last_reindex,dac.search.refresh,debug.results.max.age_in_milliseconds,debug.test_memory_limit,dremio.coordinator.enable_version_check,dremio.coordinator.rest.run_query.async,dremio.deltalake.enabled,dremio.exec.operator_batch_bytes,dremio.exec.spill.check.interval,dremio.exec.spill.healthcheck.enable,dremio.exec.spill.limit.bytes,dremio.exec.spill.limit.percentage,dremio.exec.spill.sweep.interval,dremio.exec.spill.sweep.threshold,dremio.exec.storage.file.partition.column.label,dremio.exec.testing.controls,dremio.execution.v2,dremio.iceberg.enabled,dremio.iceberg.min_max.enabled,dremio.materialization.cache.enabled,dremio.profile.debug_columns,dremio.sliced,dremio.sliced.debug_max_runtime,dremio.sliced.enable_monitor,dremio.sliced.migration_multiple,dremio.sliced.min_runtime,dremio.sliced.num_threads,dremio.sliced.spindown_multiple,dremio.sliced.use_cpu_pinning,dremio.sliced.warn_max_runtime,dremio.spill.warn_max_runtime,dremio.store.dfs.max_files,dremio.store.dfs.max_splits,dremio.store.file.dir-field-enabled,dremio.store.file.file-field-enabled,dremio.store.file.file-field-label|

---

Direct comments or questions to greg@dremio.com
