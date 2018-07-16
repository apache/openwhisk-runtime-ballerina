import ballerina/io;
import ballerina/system;

function main(string... args) {
    io:println("started");
}

function run(json jsonInput) returns json {
    json output = {};
    output.api_host = system:getEnv("__OW_API_HOST");
    output.api_key = system:getEnv("__OW_API_KEY");
    output.namespace = system:getEnv("__OW_NAMESPACE");
    output.action_name = system:getEnv("__OW_ACTION_NAME");
    output.activation_id = system:getEnv("__OW_ACTIVATION_ID");
    output.deadline = system:getEnv("__OW_DEADLINE");
    return output;
}
