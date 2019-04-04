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

FROM adoptopenjdk/openjdk8-openj9:jdk8u202-b08_openj9-0.12.1

RUN rm -rf /var/lib/apt/lists/* && apt-get clean && apt-get update \
	&& apt-get install -y --no-install-recommends locales \
	&& rm -rf /var/lib/apt/lists/* \
	&& locale-gen en_US.UTF-8

ENV LANG="en_US.UTF-8" \
	LANGUAGE="en_US:en" \
	LC_ALL="en_US.UTF-8" \
	VERSION=8 \
	UPDATE=202 \
	BUILD=08

ADD proxy /home/ballerina
RUN cd /home/ballerina \
    && ./gradlew shadowJar \
    && rm -rf /home/ballerina/.gradle /home/ballerina/src /home/ballerina/gradle* /home/ballerina/build.gradle /root/.gradle

EXPOSE 8080

## Use this command to run using jarfile
CMD java -Dballerina.home=/home/ballerina/build -Djava.util.logging.manager=org.ballerinalang.logging.BLogManager -jar /home/ballerina/build/libs/ballerina-all.jar --http.port=8080

## Use this command to debug remotely
#CMD java -Dballerina.home=/home/ballerina/build -Djava.util.logging.manager=org.ballerinalang.logging.BLogManager -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005 -jar /home/ballerina/build/libs/ballerina-all.jar --http.port=8080
