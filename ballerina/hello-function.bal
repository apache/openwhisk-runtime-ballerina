import ballerina/io;

function main(string... args) {
   io:println("started");
}

function run(json jsonInput) returns json {
   io:println(jsonInput);
   json output = { "response": "hello-world"};
   return output;
}
