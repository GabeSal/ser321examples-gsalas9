# Assignment 4 Activity 1
## Description
The initial Performer code only has one function for adding strings to the list in the starter.

The updated version includes the following additional functionality in the `Performer` class:
- `display`: Returns the list of strings as a whole.
- `search`: Searches for a specific string in the list and returns the index.
- `reverse`: Reverses the string located at the provided index.
- `quit`: Terminates the session with a goodbye message.

This activity includes three different server implementations:
- `Server` – single-threaded, handles one client at a time.
- `ThreadedServer` – handles unlimited concurrent clients using threads.
- `ThreadPoolServer` – handles a bounded number of clients using a fixed thread pool.

## Protocol

### Requests
General Request Format:
```
{ 
   "operation": <String: "add", "display", "search", "reverse", "quit">, 
   "data": data to send, see below
}
```

 - data <Depends on the operation>
   - add <String>: New string to be added
   - display <None>: No data
   - search <String>: String to search in list
   - reverse <int>: index of word to reverse
   - quit <None>: No data

Example:

Add request:
```
{ 
   "operation": "add", 
   "data": "New data"
}
```

Reverse request, wanting to reverse the string that is saved under index 3 in the list (list index starts at 0)
```
{ 
   "operation": "reverse", 
   "data": 3
}
```


### Responses
General Success Response: 
```
{
   "operation": <String: "add", "display", "search", "reverse", "quit">, 
   "data": <thing to return> 
}
```

Fields:
 - operation <String>: Echoes original operation selected from request.
 - data <Depends on the operation>: The result returned by the server.
   - add <String>: Returns the new list 
   - display <String>: Returns the current list
   - search <int>: Return index of the String to search, -1 if not found
   - reverse <String>: Reversed string from the index from the request
   - quit <String>: Some goodbye message
 
General Error Response: 
```
{
   "type": "error", 
   "message"": <error string> 
}
```
Some messages to use:
- "index not found"  // for "reverse"
- "list empty" // if display is called on empty list
- "wrong data" // if the request got wrong data
- "Connection rejected: Max clients reached. Try again later." // for `ThreadPoolServer` overload

Additionally, an informational message may be sent on successful connection:
```
{
   "type": "info",
   "message": "Connection accepted. Welcome!"
}
```

## How to run the program

### Terminal
Base Code, please use the following commands:

#### Single-threaded Server
```
gradle runServer -Pport=9099 -q --console=plain
```

#### Multi-threaded Server (unbounded)
```
gradle runThreadedServer -Pport=9099 -q --console=plain
```

#### Bounded Thread Pool Server
```
gradle runThreadPoolServer -Pport=9099 -PmaxClients=3 -q --console=plain
```

#### Client
```   
gradle runClient -Phost=localhost -Pport=9099 -q --console=plain
```

If using the ThreadPoolServer, the number of allowed concurrent clients is specified via the `-PmaxClients` flag. If the server is full, any additional client will be rejected with a JSON error message.