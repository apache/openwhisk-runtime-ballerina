import ballerina/io;

public function main(json jsonInput) returns json {
   json output = { "response": "hello-world" };
   return output;
}
