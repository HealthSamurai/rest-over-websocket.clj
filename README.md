# REST over WebSocket

Demo - https://row-ui.health-samurai.io


## The main point

Websockets nowadays are used as consumables - you have one when you need it and flush away just after.

Often there are couple of them running simultaneously, in addition for vanila HTTP you sure have.

But there is a story to hear - we can have just one. And that's the one we actually desire - WS format of information is simple data you send - the shit you care.

Build a routing around it and you get a substitution for our vanila HTTP communication mech.


It's comfy, it's light.

WS is a line of communication, communicate the shit out of it!


# What the app does

We supply a sample app implementing those principes for you to get going straight away!

It's a chat app, implementing reactive flow of data (yet remember websockets, eh?): the whole client & server.

Routing around a websocket line is the only communication mech it uses, the only it needs.



## Using

In order to up and run you need three things alive:
- frontend
Serves a client, interface for your interactions
- Postgres
Being used for keeping app's state: Users, Rooms, Messages
- backend
Provides reactive interface

### frontend

`lein do clean, figwheel`

### Postgres

`rest-over-websocket.clj$ docker-compose up`

### backend

`lein do clean, repl`

`app.core=> (def srv (start {:db {:dbtype "postgresql"
                                  :connection-uri "jdbc:postgresql://localhost:5444/postgres?stringtype=unspecified&user=postgres&password=secret"}
                                  :ev {:uri "jdbc:postgresql://localhost:5444/postgres"
                                  :user "postgres"
                                  :password "secret"
                                  :slot "test_slot"
                                  :decoder "wal2json"}}))`

Connect to server by ws://host/$conn


1. Serialization? json? edn? transit? or all of them?
2. Request - Response by ring convetion 
