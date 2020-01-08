# TCP-UDP-Server-Client
Example of Multi threaded Server and Client chat application. Written in both UDP and TCP in java. To run the application please chmod the .sh files and runs the server, then the client. Multiple instances of the client can simultaneously connect with the same server.

Commands for Client:
1. message (user) (message)
2. broadcast (message)
3. whoelse
4. whoelsesince (time) (who has logged in since past [time] seconds)
5. block (user)
6. unblock (user)
7. logout
  
Commands for P2P messaging:
1. startprivate (user)
2. private (user) (message)
3. stopprivate (user)
  
Note:
- Bugs do exist for p2p messaging. If multiple peers message the same individual, message status is not reflected for both parties.
