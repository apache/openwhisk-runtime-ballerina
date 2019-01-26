import ballerina/io;

public function main(json jsonInput) returns json {
    string delimiter = <string> jsonInput.delimiter;
    string str = delimiter + " ☃ " + delimiter;
    io:println(str);
    jsonInput.winter = str;
    return jsonInput;
}
