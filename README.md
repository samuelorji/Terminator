# Terminator 

This project is hugely based off the awesome Alvin Alexander's project [AkkazonEkko](https://github.com/alvinj/AkkazonEkko), 
but this project was built using [Akka-Typed](https://doc.akka.io/docs/akka/current/typed/index.html) and I took some of his suggestions by avoiding using Future.Await amongst others :) 

It's a CLI app that can respond to the some commands and perform some operations using Typed Actors

### USAGE 

Run the project with 

*`sbt run`* 

#### Commands List

When the application is started, type in *`commands`* to see the full list of commands the app accepts as well as arguments,
but here is a summary

| commands  | args          | Description  |
| --------- |:-------------:| -----:|
| *hello*   | [No argument]    | returns a random greeting for each greeting received      |
| *google*  | [search query]      |   opens your search query in a new *chrome* tab |
| *open*    | [application name]      |    opens the application specified or an app that contains that name | 
| *weather*  | no args / [lat lon]  | Fetches weather details for geographic coordinates supplied or defaults to Copenhagen |
| *todo*    | [add] / [rm] / [list] / [clear]|    adds, deletes, lists and also clears todos held in memory | 



**This application currently doesn't support the windows platform and has only been tested on Ubuntu and MAC OS X**



>There is another version of this application that can be used to fetch personal information like Emails, Twitter Feeds, but I'm not comfortable 
releasing such an app as it can be misused. So I just stuck with one that doesn't require any personal credentials


## Future Work 

* Support Windows

* Add Voice recognition to complement the CLI as suggested by Alvin

* Add more features to the App


Feel free to fork this project and enhance it even better :)
