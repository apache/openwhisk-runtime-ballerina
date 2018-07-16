import ballerina/io;

function main(string... args) {
    io:println("started");
}

function run(json jsonInput) returns json {
    return jsonInput;
}
