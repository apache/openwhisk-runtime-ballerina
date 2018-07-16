import ballerina/io;
import ballerina/log;

function main(string... args) {
    io:println("started");
}

function run(json jsonInput) returns json {
    io:println("hello stdout");
    log:printError("hello stderr");
    return jsonInput;
}
