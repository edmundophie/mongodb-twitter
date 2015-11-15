# mongodb-twitter
Simple usage of MongoDB driver on Java for CRUD operation

## Requirements
 - JDK >= 1.7
 - [Maven](https://maven.apache.org/download.cgi) 
 - MongoDB

## How to Build
1. Resolve maven dependency  

	 ```
	 $ mvn dependency:copy-dependencies
	 ```
2. Build `jar` using maven `mvn`  

	 ```
	 $ mvn package
	 ```

## How to Run	 

Run `Twitter` from the generated `jar` in `target` folder  

	 $ java -cp target/dependency/*:target/mongodb-twitter-1.0.jar com.edmundophie.mongodb.Twitter <host> <port>
	 
*Note that parameter `host` and `port` are optional. If any of them are not provided then they will be set to each own default value*

Default value:
- `host`: `127.0.0.1`
- `port`: `27017`

## Commands
List of command available in the application:
- `register <username> <password>`
- `follow <follower_username> <followed_username>`
- `addtweet <username> <tweet>`
- `viewtweet <username>`
- `timeline <username>`
- `exit`

## Query Implementation
*TBD*

## Team Member
- Edmund Ophie/ 13512095
- Kevin/ 13512097
