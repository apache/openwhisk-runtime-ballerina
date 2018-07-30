<!--
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
-->

# Apache OpenWhisk Runtime for Ballerina
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Build Status](https://travis-ci.com/apache/incubator-openwhisk-runtime-ballerina.svg?branch=master)](https://travis-ci.com/apache/incubator-openwhisk-runtime-ballerina)

This repository contains the [Ballerina](https://ballerinalang.org) runtime for the Apache OpenWhisk serverless platform.

### Prerequisites

The following prerequisites are needed to try this out:

- [Ballerina](https://ballerina.io/downloads/) >= 0.975.0

### Creating a Ballerina function

Create a file `hello.bal` for your Ballerina function with the following code:

```ballerina
import ballerina/io;
function main(string... args) {
  io:println("started");
}
function run(json jsonInput) returns json {
  io:println(jsonInput);
  json output = { "response": "hello-world"};
  return output;
}
```

The Ballerina file should include:
 - `main(string... args)` and
 - `run(json jsonInput)`.

The first is necessary to compile the function but does not execute when you
invoke the action.

### Compiling your function

Run the [Ballerina](https://ballerina.io/downloads) compiler to
build your function.
```bash
ballerina build hello.bal
```

This generates an executable `hello.balx`. You will use this binary to create
the OpenWhisk action.

### Creating and invoking your Ballerina action

Use the OpenWhisk [`wsk` CLI](https://github.com/apache/incubator-openwhisk/blob/master/docs/cli.md)
to create your Ballerina action.

```bash
wsk action create hello hello.balx --docker openwhisk/action-ballerina-v0.975
```

Now you're ready to invoke the action:

```bash
wsk action invoke hello --result
```
```json
{
  "response": "hello-world"
}
```

You can learn more about working with OpenWhisk Actions [here](https://github.com/apache/incubator-openwhisk/blob/master/docs/actions.md).

### Developing the Ballerina runtime for OpenWhisk

To build the Ballerina runtime, you need an OpenWhisk snapshot release which
you can install as follows:
```bash
pushd $OPENWHISK_HOME
./gradlew install
podd $OPENWHISK_HOME
```
where `$OPENWHISK_HOME` is an environment variable that points to your
OpenWhisk directory.

The Ballerina runtime is built with the Gradle wrapper `gradlew`.
```bash
./gradlew distDocker
```

You can also use `gradlew` to run all the unit tests.
```bash
./gradlew :tests:test
```

Or to run a specific test.
```bash
./gradlew :tests:test --tests *ActionContainerTests*
```

This project can be imported into [IntelliJ](https://www.jetbrains.com/idea/)
for development and testing. Import the project as a Gradle project, and make
sure your working directory is the root directory for this repository.
