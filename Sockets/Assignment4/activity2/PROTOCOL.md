## Protocol Elaboration

*Note:* **CL == client, SV == server**

### Logging in
1) CL Request
Client sends over their name
```
RequestType: NAME
Required Fields: name
```
2. SV Response
Server responds with a greeting message
```
ResponseType: WELCOME
Require Fields: message -- this is just a greeting message to the client, you can write any greeting you like
```

### View Leader Board (from main menu)
CL Request
Clients wants the leaderboard
```
RequestType: LEADERBOARD
Required Fields: *none*
```
SV Response
Server responds with a repeated field of all past players
```
ResponseType: LEADERBOARD
Required Fields: leaderboard (repeated field) including everyone on the leaderboard
```
### Play Game (from main menu)
Client wants a game to be started/joined
CL Request
```
RequestType: START
Required Fields: *none*
```
SV Response
Server responds with "phrase" represents the current phrase (hiddenPhrase from the Game), while "task" just lets the user know what to do "Guess a letter". The task and the phrase should then of course be shown to the user
```
ResponseType: TASK
Required Fields: phrase, task
```
CL Request
While in game the client sends a guess to the server, the server expects one letter
```
RequestType: GUESS
Required Fields: guess
```
SV Response
If game is not yet won the server will reply with a phrase and task again
```
	ResponseType: TASK
	Required Fields: phrase, task, eval
```
	eval will either be true/false
	- if false then the phrase will be the same as the prvious one (no letter turned), if true letters will be turned

OR if game is won the current finished phrase will be sent and a message that the game has been won. message field has some winning comment "You won, congratulations."
```
	ResponseType: WON
	Required Fields: phrase, message
```

OR if game is lost
```
	ResponseType: LOST
	Required Fields: phrase, message
```
phrase -- is the actual answer
message = "You lost and got 0 points"


### Quit Game 
Client wants to quit the game 
CL Request
```
RequestType: QUIT
Required Fields: *none*
```
SV Response
```
ResponseTupe: BYE
Required Fields: message = "Good bye <name> thanks for playing"
```
### Errors
*These can be generated by any malformed or unexpected request, e.g. the client sends over many letters instead of just one. The client*
*is responsible for keeping track of state to continue.*

SV Response
```
ResponseType: ERROR
RequiredFields: message (description of error -- see below), errorCode
```
Some error codes with messages to use
1 - required field missing 
2 - request not supported
3 - request got wrong type // the given value does not have the correct type -- should not really happen since proto is typed
4 - invalid guess, guess needs to be one letter
0 - any other error you might want to come up with, then include a good error message in the "message field"

*NOTE: The client should display the error message that is sent by the server so the client can display it to the player.*
