MAMEHub
=======

MAMEHub: A free gaming platform that brings people from different ends of the earth together to experience both arcade and console games online

Imagine walking into the biggest arcade you've ever seen in your life.  There are thousands of arcade cabinets with all of your favorite past-time games, and even rooms full of consoles, including super nintendo, genesis, and even playstation.  Just like a real arcade, you can call your friends and invite them to come and play games, but in this arcade your friends don't have to meet you in the arcade, they don't even have to be on the same continent.

MAMEHub is the world's biggest arcade & living room in one: a massive library of games sitting on a scalable, peer-to-peer platform that can bring people from different ends of the earth together to experience both arcade and console games online.

When it comes to code, MAMEHub is two things:  A fork of MAME & MESS that supports Netplay (coming to github soon), and a client/server interface that allows players from around the word to meet and play games (server coming soon).

Building the MAMEHub Client
---------------------------
The MAMEHub Client uses [Maven](http://maven.apache.org/).  To build the client, go to the Client/ directory and run this command in the client directory:

    mvn package

Maven will automatically download the dependencies and build MAMEHub.  Then to run your client, run this command:

    java -jar target/MAMEHubClient-0.0.1-SNAPSHOT.jar
