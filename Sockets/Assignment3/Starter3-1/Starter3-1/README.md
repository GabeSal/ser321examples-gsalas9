##### Author: Instructor team SE, ASU Polytechnic, CIDSE, SE


##### Purpose
This program shows a very simple client server implementation. The server
has 3 services, echo, add, addmany already implemented. Basic error handling on the server side
is implemented. Client does not have error handling and only has hard coded
calls to the server in the starter code.

* Please run `gradle runServer` and `gradle runClient` together.
* Program runs on localhost (hardcoded)
* Port is hard coded


##### Testing
You have two test classes given. 

`gradle localTest` runs the tests in *Testing.java*, which tests things locally. Meaning no server needs to be started at all. It just creates a SocketServer intance and runs tests on the methods directly. 


`gradle serverTest` runs the tests in *ServerTest.java*, which actually connects to a server. You need to make sure that the server is starter, runs on localhost and is on port 8888 `gradle runServer` beofre `gradle serverTest`.


## Protocol: ##


### Echo: ###
This one is already implemented as example and is tested in the ServerTest class. 
Request: 

    {
        "type" : "echo",    \\ type of request
        "data" : <String>   \\ String to be echoed 
    }

General response:

    {
        "type" : "echo",        \\ echoes the initial response
        "ok" : <bool>,          \\ true or false depending on request
        "echo" : <String>,      \\ echoed String if ok true
        "message" : <String>,   \\ error message if ok false
    }

Success response:

    {
        "type" : "echo",
        "ok" : true,
        "echo" : <String>       \\ the echoed string
    }

Error response:

    {
        "type" : "echo",
        "ok" : false,
        "message" : <String>    \\ what went wrong - also see general error response messages
    }

### Add: ### 
This one is already implemented as example and is tested in the ServerTest class. 
Request:

    {
        "type" : "add",
        "num1" : <String>,  \\ first number -- String needs to be an int number e.g. "3"
        "num2" : <String>   \\ second number -- String needs to be an int number e.g. "4" 
    }

General response

    {
        "type" : "add",         \\ echoes the initial request
        "ok" : <bool>,          \\true or false depending on request
        "result" : <int>,       \\ result if ok true
        "message" : <String>,   \\ error message if ok false
    }

Success response:

    {
        "type" : "add",
        "ok" : true,
        "result" : <int> -- the result of add
    }

Error response:

    {
        "type" : "add",
        "ok" : false,
        "message" : <String> \\ error message about what went wrong
    }

### AddMany: ###
Another request, this one does not just get two numbers but gets an array of numbers.

Request:

    {
        "type" : "addmany",
        "nums" : [<String>], -- json array of ints but given as Strings, e.g. ["1", "2"]
    }

General response

    {
        "type" : "addmany", -- echoes the initial request
        "ok" : <bool>, -- true or false depending on request
        "result" : <int>,  -- result if ok true
        "message" : <String>,  -- error message if ok false
    }

Success response:

    {
        "type" : "addmany",
        "ok" : true,
        "result" : <int> -- the result of adding
    }

Error response:

    {
        "type" : "addmany",
        "ok" : false,
        "message" : <String> - error message about what went wrong
    }

### Temp Convert: ###
Converts a temperature value from one unit to another (Celsius, Fahrenheit, Kelvin). The service only allows values that are warmer than -20°C and cooler than 400°C (or their equivalent in other units).

Request:

    {
        "type": "tempconvert",
        "value": <double>,         // Temperature value to convert
        "fromUnit": "<String>",    // "C", "F", or "K"
        "toUnit": "<String>"       // "C", "F", or "K"
    }

General response:

    {
        "type": "tempconvert",
        "ok": <bool>,              // true if successful, false otherwise
        "result": <double>,        // Converted temperature value if ok is true
        "message": <string>        // Error message if ok is false
    }

Success response:

    {
        "type": "tempconvert",
        "ok": true,
        "result": <number>         // Converted temperature value
    }

Error response:
General error responses for missing fields, wrong types, or unsupported types are handled as described in the general protocol section.

    {
        "type": "tempconvert",
        "ok": false,
        "message": "value out of bounds"      // Error message
    }


### Jokes: ###
This service allows users to add new "dad" jokes to the server, request a random joke, and rate jokes. Each server will develop its own unique collection as more jokes are added.
Your server should start with some jokes (hardcoded or loaded from a file).

Request to add a new joke:
The joke should be added to the list, receive a unique jokeID, and start with a rating of 5.

    {
        "type": "dadjoke",
        "action": "add",
        "joke": "<String>"      // The dad joke to add
    }

Success response:

    {
        "type": "dadjoke",
        "ok": true,
        "message": "Joke added, this is joke number <id>!"
    }

Error response:

    {
        "type" : "dadjoke",
        "ok" : false,
        "message" : <String>    \\ Likely only needed for general errors
    }


Request to get a joke:

    {
        "type": "dadjoke",
        "action": "get"
    }

Success response:

    {
        "type": "dadjoke",
        "ok": true,
        "jokeID": <int>,         // Unique id for this joke
        "joke": "<String>",      // A random dad joke from the server
        "rating": <double>       // Current rating (0-5)
    }

Error response:

    {
        "type" : "dadjoke",
        "ok" : false,
        "message" : "No jokes on server" // hopefully never happens
    }


Request to rate a joke:
This should then change the rating of the joke, to keep it simple (not accurate) just use (oldrating+newrating)/2. It would be much better to of course keep track of how many ratings and make a real average but lets keep it very simple. 

    {
        "type" : "dadjoke",
        "action": "rate"
        "jokeID": <int>         // Unique id this joke has on the server
        "rating" : <String>     // Client's answer to the question
    }

Success response:

    {
        "type" : "dadjoke",
        "ok" : true,
        "jokeID": <int>         // Unique id this joke has on the server
        "joke": "<String>"      // The server that belongs to that id
        "rating": <double>      // The new rating
    }

Error response:

    {
        "type" : "dadjoke",
        "ok" : false,
        "message" : "Rating is out of bounds" // if provided rating is not between 0-5
    }



### General error responses: ###
These are used for all requests.

If the request leads to two errors, e.g. one field missing and one has incorrect data type go from top to bottom on this list. So in the example you would return the first error message that a field is missing. 

If an error you come up with is neither listed here and neither in the protocol description return an appropriate message you see fit. 

Error response: When a required field "key" is not in request

    {
        "ok" : false
        "message" : "Field <key> does not exist in request" 
    }

Error response: When a required field "key" is not of correct "data type"

    {
        "ok" : false
        "message" : "Field <key> needs to be of type: <type>"
    }

Error response: When the "type" of the request is not supported, so an unsupported request

    {
        "ok" : false
        "message" : "Type <type> is not supported."
    }


Error response: When the server does not receive a correct JSON

    {
        "ok" : false
        "message" : "req not JSON"
    }