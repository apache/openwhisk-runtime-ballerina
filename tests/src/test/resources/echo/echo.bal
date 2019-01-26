import ballerina/io;
import ballerina/log;

public function main(json jsonInput) returns json {
    io:println("hello stdout");
    log:printError("hello stderr");
    return jsonInput;
}
