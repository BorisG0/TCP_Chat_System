# TCP Chat System

### Client Usage

type your commands in the console of a client

words marked with `[]` have to be replaced by you

#### Log In

`LOGIN [username] [password]`

there are 3 users predefined in the code of the server class:
- Tom - 111
- Peter - 222
- Heinz - 333


#### Messaging

`MSG [recipient] [message]`


#### Get Conversation

`CONV [chat partner]`

## Starting

### Client

clients are started with an id followed by any number of server ports as parameters

use `java Client [client id] [server port 1] [server port 2] ...` to start a client

the default parameters are 0 7777 8888

### Two Server Synchronized Chat System

start first server class without paramaters `java Server`, the default parameters are 7777 8888

start second server with two ports, opposite of the first one, as parameters `java Server 8888 7777`

### Majority Consensus Strategy Servers

servers are started with their own port as the first parameter and the ports of all other servers as the following parameters

example `java ServerMCS [port of server] [other port 1] [other ports 2] ...`

start first server class without parameters `java Server`, the default parameters are 7770 7771 7772

start second server using `java Server 7771 7770 7772`

start third server using `java Server 7772 7770 7771`

and the client with `java ClientMCS 0 7770 7771 7772`