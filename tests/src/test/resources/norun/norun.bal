import ballerina/io;

function main(string... args) {
    io:println("started");
}

function example(json jsonInput) returns json {
    return jsonInput;
}
