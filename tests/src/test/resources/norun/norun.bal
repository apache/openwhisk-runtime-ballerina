import ballerina/io;

function main(string... args) {
    io:println("started");
}

function example(json jsonInput) returns json {
    json result = {"string":"hello"};
    return result;
}
