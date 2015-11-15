package com.edmundophie.mongodb;

/**
 * Created by edmundophie on 11/15/15.
 */

import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertOneModel;
import org.bson.Document;
import org.bson.types.ObjectId;

import static com.mongodb.client.model.Filters.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Twitter {
    private MongoClient mongoClient;
    private Map<String, MongoCollection> collections;
    private final static String DEFAULT_HOST = "127.0.0.1";
    private final static Integer DEFAULT_PORT = 27017;
    private final static String DBNAME = "13512095_13512097";
    private final static String TABLE_USERS = "users";
    private final static String TABLE_FOLLOWERS = "followers";
    private final static String TABLE_USERLINE = "userline";
    private final static String TABLE_TIMELINE = "timeline";
    private final static String TABLE_TWEETS = "tweets";
    private final static String TABLE_FRIENDS = "friends";

    public static void main(String[] args) throws IOException {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;

        if(args.length==1)
            host = args[0];
        else if(args.length==2)
            port = Integer.parseInt(args[1]);
        else if(args.length>2) {
            System.out.println("Usage: Twitter <host> <port>");
            System.exit(0);
        }

        Twitter twitter = new Twitter();
        twitter.connect(host, port);
        printDirectives();
        processCommand(twitter);

        twitter.close();
    }

    public static void processCommand(Twitter twitter) throws IOException {
        String command = null;
        String unsplittedParams = null;
        do {
            String input = new BufferedReader(new InputStreamReader(System.in)).readLine().trim();

            if(input.isEmpty()){
                printInvalidCommand();
            }
            else {
                String[] parameters = new String[0];
                int i = input.indexOf(" ");

                if (i > -1) {
                    command = input.substring(0, i);
                    unsplittedParams = input.substring(i + 1);
                    parameters = unsplittedParams.split(" ");
                } else
                    command = input;

                if (command.equalsIgnoreCase("REGISTER") && parameters.length == 2) {
                    if(twitter.isUserExist(parameters[0]))
                        System.out.println("* Username already exist!");
                    else {
                        twitter.registerUser(parameters[0], parameters[1]);
                        System.out.println("* " + parameters[0] + " succesfully registered");
                    }
                } else if (command.equalsIgnoreCase("FOLLOW") && parameters.length == 2) {
                    if(!twitter.isUserExist(parameters[0]) || !twitter.isUserExist(parameters[1]))
                        System.out.println("* Failed to execute command!\n* User may have not been registered");
                    else {
                        twitter.followUser(parameters[0], parameters[1]);
                        System.out.println("* " + parameters[0] + " is now following " + parameters[1]);
                    }
                } else if (command.equalsIgnoreCase("ADDTWEET") && parameters.length>=2) {
                    if(!twitter.isUserExist(parameters[0]))
                        System.out.println("* Failed to execute command!\n* User may have not been registered");
                    else {
                        twitter.insertTweet(parameters[0], unsplittedParams.substring(parameters[0].length()));
                        System.out.println("* " + parameters[0] + " tweet has been added");
                    }
                } else if (command.equalsIgnoreCase("VIEWTWEET") && parameters.length==1) {
                    twitter.displayUserline(parameters[0]);
                } else if (command.equalsIgnoreCase("TIMELINE") && parameters.length==1) {
                    twitter.displayTimeline(parameters[0]);
                } else if (command.equalsIgnoreCase("EXIT")) {
                    System.out.println("Exiting...");
                } else
                    printInvalidCommand();
            }
        } while(!command.equalsIgnoreCase("EXIT"));
    }

    public static void printInvalidCommand() {
        System.err.println("* Invalid Command");
    }

    public static void printDirectives() {
        System.out.println("\n*** COMMANDS ***");
        System.out.println("---------****--------");
        System.out.println("* REGISTER <username> <password>");
        System.out.println("* FOLLOW <follower_username> <followed_username>");
        System.out.println("* ADDTWEET <username> <tweet>");
        System.out.println("* VIEWTWEET <username>");
        System.out.println("* TIMELINE <username>");
        System.out.println("* EXIT");
        System.out.println("---------****--------");
        System.out.println("* Type your command...\n");
    }

    public void connect(String dbHost, int dbPort) {
        mongoClient = new MongoClient(dbHost, dbPort);
        MongoDatabase mongoDb = mongoClient.getDatabase(DBNAME);
        collections = new HashMap<String, MongoCollection>();
        collections.put(TABLE_USERS, mongoDb.getCollection(TABLE_USERS));
        collections.put(TABLE_FOLLOWERS, mongoDb.getCollection(TABLE_FOLLOWERS));
        collections.put(TABLE_USERLINE, mongoDb.getCollection(TABLE_USERLINE));
        collections.put(TABLE_TIMELINE, mongoDb.getCollection(TABLE_TIMELINE));
        collections.put(TABLE_TWEETS, mongoDb.getCollection(TABLE_TWEETS));
        collections.put(TABLE_FRIENDS, mongoDb.getCollection(TABLE_FRIENDS));
    }

    public void close() {
        mongoClient.close();
    }

    public void registerUser(String username, String password) {
        Document doc = new Document("username", username).append("password", password);
        collections.get(TABLE_USERS).insertOne(doc);
    }

    public void followUser(String followerUsername, String followedUsername) {
        Document doc = new Document("username", followedUsername)
                .append("follower", followerUsername)
                .append("since", System.currentTimeMillis());
        collections.get(TABLE_FOLLOWERS).insertOne(doc);
    }

    public void insertTweet(String username, String tweet) {
        Document doc = new Document("username", username)
                .append("body", tweet);
        collections.get(TABLE_TWEETS).insertOne(doc);

        ObjectId id = (ObjectId) doc.get("_id");
        long time = System.currentTimeMillis();

        doc = new Document("username", username)
                .append("time", time)
                .append("tweetId", id);
        collections.get(TABLE_USERLINE).insertOne(doc);
        collections.get(TABLE_TIMELINE).insertOne(doc);

        MongoCursor cursor = collections.get(TABLE_FOLLOWERS)
                .find(eq("username",username))
                .iterator();

        if(cursor.hasNext()) {
            List<InsertOneModel> insertOps = new ArrayList<InsertOneModel>();
            while (cursor.hasNext()) {
                Document resDoc = (Document) cursor.next();
                doc = new Document("username", resDoc.getString("follower"))
                        .append("time", time)
                        .append("tweetId", id);
                insertOps.add(new InsertOneModel(doc));
            }
            collections.get(TABLE_TIMELINE).bulkWrite(insertOps);
        }
    }

    public void displayUserline(String username) {
        MongoCursor cursor = collections.get(TABLE_USERLINE).find(eq("username", username)).iterator();
        List<ObjectId> tweetIds = new ArrayList<ObjectId>();
        while(cursor.hasNext()) {
            Document doc = (Document) cursor.next();
            tweetIds.add(doc.getObjectId("tweetId"));
        }

        cursor = collections.get(TABLE_TWEETS).find(in("_id", tweetIds)).iterator();
        printTweet(cursor);
    }

    public void displayTimeline(String username) {
        MongoCursor cursor = collections.get(TABLE_TIMELINE).find(eq("username", username)).iterator();
        List<ObjectId> tweetIds = new ArrayList<ObjectId>();
        while(cursor.hasNext()) {
            Document doc = (Document) cursor.next();
            tweetIds.add(doc.getObjectId("tweetId"));
        }

        cursor = collections.get(TABLE_TWEETS).find(in("_id", tweetIds)).iterator();
        printTweet(cursor);
    }

    public static void printTweet(MongoCursor cursor) {
        while(cursor.hasNext()) {
            Document tweet = (Document) cursor.next();
            System.out.println("@" + tweet.get("username") + ": " + tweet.get("body"));
        }
    }

    public boolean isUserExist(String username) {
        MongoCursor cursor = collections.get(TABLE_USERS).find(eq("username", username)).iterator();
        return cursor.hasNext();
    }
}
