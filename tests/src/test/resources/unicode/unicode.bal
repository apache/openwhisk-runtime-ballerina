import ballerina/io;

function main(string... args) {
    io:println("started");
}

function run(json jsonInput) returns json {
    string delimiter = check <string> jsonInput.delimiter;
    string str = delimiter + " ☃ " + delimiter;
    io:println(str);
    jsonInput.winter = str;
    return jsonInput;
}
