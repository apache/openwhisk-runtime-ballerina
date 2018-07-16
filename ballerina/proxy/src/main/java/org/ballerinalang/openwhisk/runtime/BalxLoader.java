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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.ballerinalang.util.codegen.ProgramFile;
import org.ballerinalang.util.debugger.Debugger;
import org.wso2.msf4j.Request;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.List;

/**
 * Loader Utils for proxy handlers.
 */
public class BalxLoader {

    /**
     * Writes the received encoded input stream to function.balx file.
     *
     * @param encoded Base64 Encoded Input Stream
     * @return Path to function.balx
     * @throws IOException
     */
    public static Path saveBase64EncodedFile(InputStream encoded) throws IOException {
        InputStream decoded = null;
        try {
            Base64.Decoder decoder = Base64.getDecoder();

            decoded = decoder.wrap(encoded);

            File destinationFile = File.createTempFile(Constants.FUNCTION_FILE_NAME, Constants.FUNCTION_EXTENSION);
            destinationFile.deleteOnExit();
            Path destinationPath = destinationFile.toPath();

            Files.copy(decoded, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            return destinationPath;
        } finally {
            if (decoded != null) {
                decoded.close();
            }
        }
    }

    /**
     * Initializes the program file debugger and Global Memory area
     *
     * @param programFile Program File object to initialize
     * @return Initialized program file with debugger and Global Memory
     */

    public static ProgramFile initProgramFile(ProgramFile programFile) {
        Debugger debugger = new Debugger(programFile);
        programFile.setDebugger(debugger);

        if (debugger.isDebugEnabled()) {
            debugger.init();
            debugger.waitTillDebuggeeResponds();
        }

        programFile.initializeGlobalMemArea();
        return programFile;
    }

    /**
     * Parsing the received request object to JSON.
     *
     * @param request Request object received in http call
     * @return JSON formatted request object
     */
    public static JsonObject requestToJson(Request request) {
        JsonParser parser = new JsonParser();
        List<ByteBuffer> byteBuffers = request.getFullMessageBody();
        StringBuilder req = new StringBuilder();
        for (ByteBuffer buffer : byteBuffers) {
            req.append(Charset.forName(StandardCharsets.UTF_8.name()).decode(buffer).toString());
        }
        return parser.parse(req.toString()).getAsJsonObject();
    }
}
