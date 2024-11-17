# Assignment 4 Activity 1
## Description
This project is part of Assignment 4 for learning about threads, sockets, and serialization. It involves modifying a simple server to add more functionalities and make it multi-threaded, with different versions to explore unbounded and bounded threading.

The goal is to:
- Convert a single-threaded server to a multithreaded server.
- Implement operations for adding, displaying, and counting strings in a list.
- Understand thread safety and shared state management.

## Implemented Operations
The initial Performer code only had the ability to add strings to a list. This project extends it with the following operations:
- Add: Adds a new string to the list.
- Display: Displays the list of strings.
- Display: Displays the list of strings.
- Quit: Disconnects the client from the server.


## Protocol Details

### Requests
General Request Format:
```
{ 
   "selected": <int: 1=add, 3=display, 4=count,  0=quit>, 
   "data": <thing to send>
}
```
Fields:
 - selected <int>: The operation selected.
 - data <Depends on the operation>:
   - add <String>: Add a new string to the list.
   - display <None>: Display the list of strings
   - count <None>: Count the number of strings in the list.
   - quit <None>: Quit the connection.

### Responses
General Success Response: 
```
{
   "type": <String: "add", "display", "count", "quit">, 
   "data": <thing to return> 
}
```

Fields:
 - type <String>: Echoes original operation selected from request.
 - data <Depends on the operation>: The result returned by the server.
   - Add <String>: Returns the new list 
   - Display <String>: String from list at specified index
   - Count <int>: Number of elements (Strings) in the list
 
General Error Response: 
```
{
   "type": "error", 
   "message"": <error string> 
}
```

## How to run the program
### Terminal
Base Code, please use the following commands:
```
    For Server, run "gradle runTask1 -Pport=9099 -q --console=plain"           // singleton operation
    For Server, run "gradle runTask2 -Pport=9099 -q --console=plain"           // multi-threaded unbounded
    For Server, run "gradle runTask3 -Pport=9099 -Pthreads=4 -q --console=plain" // bounded
    
    For Server, run "gradle runTask1 -q --console=plain"
    For Server, run "gradle runTask2 -q --console=plain"
    For Server, run "gradle runTask3 -q --console=plain" // Defaults to 4 threads
```
```   
    For Client, run "gradle runClient -Phost=localhost -Pport=9099 -q --console=plain"
    For Client, run "gradle runClient -q --console=plain"
```