/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ballerinalang.openwhisk.runtime;

/**
 * Constant variables included in
 */
class Constants {
    static final int PROXY_PORT = 8080;
    static final String JSON_VALUE = "value";
    static final String BINARY = "binary";
    static final String CODE = "code";
    static final String IDENTITY = "identity";

    static final String FUNCTION_CALLABLE_NAME = "run";
    static final String FUNCTION_MAIN = "main";
    static final String FUNCTION_FILE_NAME = "function";
    static final String FUNCTION_EXTENSION = ".balx";

    static final String RESPONSE_ERROR = "\"error\"";
    static final String RESPONSE_SUCCESS = "\"success\"";

    static final String INIT_SUCCESS = "\"Function init success\"";

    static final String INIT_ONCE_ERROR = "\"Cannot initialize the action more than once.\"";
    static final String FAILED_TO_LOCATE_BINARY =
            "\"The action failed to generate or locate a binary. See logs for details.\"";
    static final String MISSING_MAIN_ERROR = "\"Missing main/no code to " + "execute.\"";
    static final String FUNCTION_NOT_INITIALIZED = "\"Function not initialized\"";
    static final String INVALID_INPUT_PARAMS = "\"Invalid input parameters for action run\"";
    static final String FUNCTION_RUN_FAILURE = "\"Running Function failed\"";
    static final String DICTIONARY_RETURN_FAILURE = "\"The action did not return a dictionary.\"";
}
