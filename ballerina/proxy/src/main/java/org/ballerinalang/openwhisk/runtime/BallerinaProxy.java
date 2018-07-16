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
import org.ballerinalang.BLangProgramLoader;
import org.ballerinalang.logging.BLogManager;
import org.ballerinalang.model.values.BJSON;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.util.codegen.ProgramFile;
import org.ballerinalang.util.exceptions.BLangRuntimeException;
import org.ballerinalang.util.exceptions.ProgramFileFormatException;
import org.ballerinalang.util.program.BLangFunctions;
import org.wso2.msf4j.Request;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.LogManager;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

/**
 * OpenWhisk Ballerina Runtime Proxy Service
 * Exposes /init and /run resources
 */
@Path("/") public class BallerinaProxy {
    private ProgramFile programFile;
    private String mainFunction = Constants.FUNCTION_CALLABLE_NAME;

    @POST
    @Path("init")
    public Response init(@Context Request request) throws IOException {
        Optional<ProgramFile> optionalValue = Optional.ofNullable(programFile);

        // Check whether init being called before
        if (optionalValue.isPresent()) {
            System.err.println(Constants.INIT_ONCE_ERROR);
            return buildResponse(Response.Status.BAD_GATEWAY, Constants.RESPONSE_ERROR, Constants.INIT_ONCE_ERROR);
        }

        InputStream balxIs = null;
        try {
            ((BLogManager) LogManager.getLogManager()).loadUserProvidedLogConfiguration();
            JsonObject payload = BalxLoader.requestToJson(request);

            if (payload.size() == 0) {
                return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, Constants.RESPONSE_ERROR,
                                     Constants.FAILED_TO_LOCATE_BINARY);

            }

            JsonObject requestElements = payload.getAsJsonObject(Constants.JSON_VALUE);
            Boolean isBinary = requestElements.get(Constants.BINARY).getAsBoolean();
            String main = requestElements.get(Constants.FUNCTION_MAIN).getAsString();

            if (!Constants.FUNCTION_MAIN.equals(main)) {
                mainFunction = main;
            }

            // Check for binary value. .balx should be received with the binary parameter
            if (isBinary) {
                String base64Balx = requestElements.get(Constants.CODE).getAsString();

                balxIs = new ByteArrayInputStream(base64Balx.getBytes(StandardCharsets.UTF_8));

                java.nio.file.Path destinationPath = BalxLoader.saveBase64EncodedFile(balxIs);

                programFile = BLangProgramLoader.read(destinationPath);

                return buildResponse(Response.Status.OK, Constants.RESPONSE_SUCCESS, Constants.INIT_SUCCESS);
            } else {
                return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, Constants.RESPONSE_ERROR,
                                     Constants.FAILED_TO_LOCATE_BINARY);
            }
        } catch (ProgramFileFormatException | BLangRuntimeException e) {
            return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, Constants.RESPONSE_ERROR,
                                 Constants.FAILED_TO_LOCATE_BINARY);
        } catch (IOException e) {
            return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, Constants.RESPONSE_ERROR,
                                 Constants.MISSING_MAIN_ERROR);
        } finally {
            if (balxIs != null) {
                balxIs.close();
            }
        }
    }

    @POST
    @Path("run")
    public Response run(@Context Request request) {
        Optional<ProgramFile> optionalValue = Optional.ofNullable(programFile);
        JsonObject requestElements;
        BValue[] result;

        // Check whether init function has success and the program file is set
        if (!optionalValue.isPresent()) {
            return buildRunResponse(Response.Status.BAD_REQUEST, Constants.RESPONSE_ERROR,
                                    Constants.FUNCTION_NOT_INITIALIZED);
        }

        requestElements = BalxLoader.requestToJson(request);

        if (requestElements.size() == 0 || requestElements.getAsJsonObject(Constants.JSON_VALUE) == null) {
            return buildRunResponse(Response.Status.BAD_REQUEST, Constants.RESPONSE_ERROR,
                                    Constants.INVALID_INPUT_PARAMS);
        }

        //Preparing input parameters
        BValue bJson = new BJSON(requestElements.getAsJsonObject(Constants.JSON_VALUE).toString());
        BValue[] parameters = new BValue[1];
        parameters[0] = bJson;

        //Setting up runtime environment variables
        augmentEnv(requestElements);

        //Invoking the program file
        try {
            programFile = BalxLoader.initProgramFile(programFile);
            result = BLangFunctions
                    .invokeEntrypointCallable(programFile, programFile.getEntryPkgName(), mainFunction, parameters);
        } catch (Exception e) {
            return buildRunResponse(Response.Status.BAD_REQUEST, Constants.RESPONSE_ERROR,
                                    Constants.FUNCTION_RUN_FAILURE);
        }

        //Preparing function response
        StringBuilder response = new StringBuilder();
        for (BValue bValue : result) {
            if ("json".equals(bValue.getType().toString())) {
                response.append(bValue.stringValue());
            } else {
                return buildRunResponse(Response.Status.BAD_REQUEST, Constants.RESPONSE_ERROR,
                                        Constants.DICTIONARY_RETURN_FAILURE);
            }
        }
        return buildResponse(Response.Status.OK, response.toString());
    }

    /**
     * Populating the environment properties by reading the input json object
     *
     * @param requestElements JsonObject with request parameters
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void augmentEnv(JsonObject requestElements) {
        HashMap<String, String> env = new HashMap<>();
        for (String p : new String[] { "api_key", "namespace", "action_name", "activation_id", "deadline" }) {
            try {
                String val = requestElements.getAsJsonPrimitive(p).getAsString();
                env.put(String.format("__OW_%s", p.toUpperCase()), val);
            } catch (Exception ignored) {
            }
        }

        try {
            for (Class cl : Collections.class.getDeclaredClasses()) {
                if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(System.getenv());
                    Map<String, String> map = (Map<String, String>) obj;
                    map.putAll(env);
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Common response build object
     *
     * @param status  Response Status
     * @param type    Response Status
     * @param message Response Message
     * @return Formatted Response
     */
    private Response buildResponse(Response.Status status, String type, String message) {
        String response = responseMsgBuilder(type, message);
        return Response.status(status).header(HttpHeaders.CONTENT_ENCODING, Constants.IDENTITY).entity(response)
                       .build();
    }

    /**
     * Build response for already formatted Strings
     *
     * @param status
     * @param response
     * @return
     */
    private Response buildResponse(Response.Status status, String response) {
        System.out.println("XXX_THE_END_OF_A_WHISK_ACTIVATION_XXX");
        System.err.println("XXX_THE_END_OF_A_WHISK_ACTIVATION_XXX");
        return Response.status(status).header(HttpHeaders.CONTENT_ENCODING, Constants.IDENTITY).entity(response)
                       .build();
    }

    /**
     * Response builder method with WHISK activation end records
     *
     * @param status Response Status
     * @return Formatted Response
     */
    private Response buildRunResponse(Response.Status status, String type, String message) {
        String response = responseMsgBuilder(type, message);
        return buildResponse(status, response);
    }

    /**
     * Build the response Json message
     *
     * @param type    response type error / success
     * @param Message message to be sent as response
     * @return Formatted String {\"<type>\" : \"<message>\"}
     */
    private String responseMsgBuilder(String type, String Message) {
        return "{" + type + ":" + Message + "}";
    }
}
