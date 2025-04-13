package io.openiap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.Timer;
import java.util.TimerTask;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;

import io.openiap.ColTimeseriesWrapper.TimeUnit;

@SuppressWarnings("unused")
public class clienttestcli {
    private static volatile boolean gotwatchevent = false;
    private static volatile int queuemessagecount = 0;
    private static volatile int exchangemessagecount = 0;
    private static Timer queuetimer;
    private static Timer exctimer;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entity {
        public String _type;
        public String _id;
        public String name;
        public String java;
    }

    public static void RunAll() {
        System.out.println("CLI initializing...");
        String libpath = NativeLoader.loadLibrary("openiap");

        Client client = new Client(libpath);
        try {
            client.enableTracing("openiap=trace", "");
            client.enableTracing("openiap=info", "");
            client.start();
            client.connect("");

            client.onClientEventAsync(
                    (event) -> {
                        client.info("Event: " + event.event + " Reason: " + event.reason);
                    });
            User user = client.getUser();
            if (user != null) {
                client.info("User ID: " + user.id);
                client.info("User Name: " + user.name);
                client.info("User Username: " + user.username);
                client.info("User Email: " + user.email);
                client.info("User Roles Pointer: " + user.roles);
                var roles = user.getRoleList();
                for (int i = 0; i < roles.size(); i++) {
                    client.info("Role[" + i + "]: " + roles.get(i));
                }
            } else {
                client.info("No user found.");
            }
            var jwt = client.signin(
                    new SigninParameters.Builder()
                            .username(System.getenv("testusername"))
                            .password(System.getenv("testpassword"))
                            .validateonly(true)
                            .build());
            client.info("Signin: " + jwt);

            user = client.getUser();
            if (user != null) {
                client.info("User ID: " + user.id);
                client.info("User Name: " + user.name);
                client.info("User Username: " + user.username);
                client.info("User Email: " + user.email);
                client.info("User Roles Pointer: " + user.roles);
                var roles = user.getRoleList();
                for (int i = 0; i < roles.size(); i++) {
                    client.info("Role[" + i + "]: " + roles.get(i));
                }
            } else {
                client.info("No user found.");
            }

            QueryParameters queryParams = new QueryParameters.Builder()
                    .collectionname("entities")
                    .query("{\"_type\":\"test\"}")
                    .top(10)
                    .build();

            List<Entity> results = client.query(new TypeReference<List<Entity>>() {
            }.getType(), queryParams);
            if (results != null) {
                for (Entity item : results) {
                    client.info("Item: " + item._type + " " + item._id + " " + item.name);
                }
            }

            // Example of querying and getting the raw JSON string
            // queryParams.query = "{}";
            queryParams.query = "{\"_type\":\"test\"}";
            String jsonResult = client.query(String.class, queryParams);
            client.info("Raw JSON Result: " + jsonResult);

            AggregateParameters aggregateParams = new AggregateParameters.Builder()
                    .collectionname("entities")
                    .aggregates("[{\"$match\": {\"_type\": \"test\"}}, {\"$limit\": 10}]")
                    .build();

            String aggregateJsonResult = client.aggregate(String.class, aggregateParams);
            client.info("Raw JSON Aggregate Result: " + aggregateJsonResult);
            List<Entity> aggregate = client.aggregate(new TypeReference<List<Entity>>() {
            }.getType(), aggregateParams);
            if (aggregate != null) {
                for (Entity item : aggregate) {
                    client.info("Item: " + item._type + " " + item._id + " " + item.name);
                }
            }

            CreateCollection createColParams = new CreateCollection.Builder("testjavacollection")
                    .build();
            boolean Colcreated = client.createCollection(createColParams);
            if (Colcreated) {
                client.info("Collection created successfully!");
            } else {
                client.error("Failed to create collection!");
            }
            List<Index> indexes = client.getIndexes("testjavacollection");
            if (indexes != null) {
                for (Index index : indexes) {
                    client.info(" Index Name: " + index.name);
                    client.info(" Index Key: " + index.key.toString());
                    client.info(" Index Unique: " + index.unique);
                    client.info(" Index Sparse: " + index.sparse);
                    client.info(" Index Background: " + index.background);
                    client.info(" Index ExpireAfterSeconds: " + index.expireAfterSeconds);
                    if (index.name.equals("_type_1")) {
                        client.dropIndex("testjavacollection", index.name);
                    }
                }
            }
            client.createIndex(
                    new CreateIndexParameters.Builder()
                            .collectionname("testjavacollection")
                            .index("{\"_type\":1}")
                            .build());
            client.dropCollection("testjavacollection");

            CreateCollection createExpColParams = new CreateCollection.Builder("testjavaexpcollection")
                    .expire(60)
                    .build();
            boolean ExpColcreated = client.createCollection(createExpColParams);
            if (ExpColcreated) {
                client.info("Collection created successfully!");
            } else {
                client.error("Failed to create collection!");
            }
            // client.dropCollection("testjavaexpcollection");

            ColTimeseriesWrapper timeseries = new ColTimeseriesWrapper(TimeUnit.MINUTES,
                    "ts");
            CreateCollection createTSColParams = new CreateCollection.Builder("testjavatscollection")
                    .timeseries(timeseries)
                    .build();
            boolean TSColcreated = client.createCollection(createTSColParams);
            if (TSColcreated) {
                client.info("Collection created successfully!");
            } else {
                client.error("Failed to create collection!");
            }
            // client.dropCollection("testjavatscollection");
            ColTimeseriesWrapper timeseries2 = new ColTimeseriesWrapper(TimeUnit.MINUTES,
                    "ts", "metadata");
            CreateCollection createTSColParams2 = new CreateCollection.Builder("testjavats2collection")
                    .timeseries(timeseries2)
                    .build();
            boolean TSColcreated2 = client.createCollection(createTSColParams2);
            if (TSColcreated2) {
                client.info("Collection created successfully!");
            } else {
                client.error("Failed to create collection!");
            }
            // client.dropCollection("testjavats2collection");

            var str_collections = client.listCollections(false);
            client.info("Collections: " + str_collections);
            List<Collection> collections = client.listCollections(
                    new TypeReference<List<Collection>>() {
                    }.getType(),
                    false);

            // Print collection details
            for (Collection collection : collections) {
                client.info("Collection name: " + collection.name);
                client.info("Type: " + collection.type);
                if (collection.info != null && collection.idIndex != null) {
                    client.info("UUID: " + collection.info.uuid + " ReadOnly: " +
                            collection.info.readOnly + " _id index: " + collection.idIndex.name);
                } else if (collection.info != null) {
                    client.info("UUID: " + collection.info.uuid + " ReadOnly: " +
                            collection.info.readOnly);
                } else if (collection.idIndex != null) {
                    client.info("_id index: " + collection.idIndex.name);
                }
                client.info("---");
            }

            InsertOneParameters insertOneParams = new InsertOneParameters.Builder()
                    .collectionname("entities")
                    .item("{\"_type\":\"test\", \"name\":\"test01\"}")
                    .build();

            String insertOneResult = client.insertOne(insertOneParams);
            client.info("InsertOne Result (JSON): " + insertOneResult);

            InsertOneParameters insertOneParams2 = new InsertOneParameters.Builder()
                    .collectionname("entities")
                    .item("{\"_type\":\"test\", \"name\":\"test02\"}")
                    .build();

            Entity insertedEntity = client.insertOne(Entity.class, insertOneParams2);
            client.info("InsertOne Result (Entity): " + insertedEntity.name +
                    "id: " + insertedEntity._id);

            insertedEntity._id = null;
            InsertOneParameters insertOneParams3 = new InsertOneParameters.Builder()
                    .collectionname("entities")
                    .itemFromObject(insertedEntity)
                    .build();

            Entity insertedEntity3 = client.insertOne(Entity.class, insertOneParams3);
            client.info("InsertOne Result (Entity): " + insertedEntity3.name +
                    "id: " + insertedEntity3._id);

            UpdateOneParameters updateOneParams = new UpdateOneParameters.Builder()
                    .collectionname("entities")
                    .item("{\"_id\":\"" + insertedEntity3._id + "\",\"name\":\"test02-updated\"}")
                    .build();

            String updateOneResult = client.updateOne(updateOneParams);
            client.info("UpdateOne Result (JSON): " + updateOneResult);

            insertedEntity3.name = "test02-updated-again";
            UpdateOneParameters updateOneParams2 = new UpdateOneParameters.Builder()
                    .collectionname("entities")
                    .itemFromObject(insertedEntity3)
                    .build();

            Entity updatedEntity = client.updateOne(Entity.class, updateOneParams2);
            client.info("UpdateOne Result (Entity): " + updatedEntity.name + " id: " + updatedEntity._id);

            InsertOrUpdateOneParameters insertOrUpdateOneParams = new InsertOrUpdateOneParameters.Builder()
                    .collectionname("entities")
                    .uniqeness("name")
                    .item("{\"_type\":\"test\", \"name\":\"test01-uniqene\", \"now\":\"" +
                            System.currentTimeMillis() + "\"}")
                    .build();

            String insertOrUpdateOneResult = client.insertOrUpdateOne(insertOrUpdateOneParams);
            client.info("InsertOrUpdateOne Result (JSON): " +
                    insertOrUpdateOneResult);

            InsertOrUpdateOneParameters insertOrUpdateOneParams2 = new InsertOrUpdateOneParameters.Builder()
                    .collectionname("entities")
                    .uniqeness("name")
                    .item("{\"_type\":\"test\", \"name\":\"test01-uniqene\", \"now\":\"" +
                            System.currentTimeMillis() + "\"}")
                    .build();

            updatedEntity = client.insertOrUpdateOne(Entity.class,
                    insertOrUpdateOneParams2);
            client.info("InsertOrUpdateOne Result (Entity): " + updatedEntity.name
                    + " id: " + updatedEntity._id);

            List<Object> entities = new ArrayList<>();
            entities.add(new Entity() {
                {
                    name = "insertmany1";
                    _type = "test";
                    java = "many";
                }
            });
            entities.add(new Entity() {
                {
                    name = "insertmany2";
                    _type = "test";
                    java = "many";
                }
            });

            InsertManyParameters insertManyParams = new InsertManyParameters.Builder()
                    .collectionname("entities")
                    .itemsFromObjects(entities)
                    .build();

            String insertManyResult = client.insertMany(insertManyParams);
            client.info("InsertMany Result (JSON): " + insertManyResult);

            String jsonItems = "[{\"_type\":\"test\", \"java\":\"many\", \"name\":\"insertmany3\"}, {\"_type\":\"test\", \"java\":\"many\", \"name\":\"insertmany4\"}]";
            InsertManyParameters insertManyParams2 = new InsertManyParameters.Builder()
                    .collectionname("entities")
                    .items(jsonItems)
                    .build();

            List<Entity> insertedEntities = client.insertMany(new TypeReference<List<Entity>>() {
            }.getType(), insertManyParams2);
            client.info("InsertMany Result (Entity List):");
            for (Entity entity : insertedEntities) {
                client.info(" " + entity.name + " id: " + entity._id);
                client.deleteOne(
                        new DeleteOneParameters.Builder()
                                .collectionname("entities")
                                .id(entity._id)
                                .build());
            }

            var deletecount = client.deleteMany(
                    new DeleteManyParameters.Builder()
                            .collectionname("entities")
                            .query("{\"java\":\"many\"}")
                            .build(),
                    null // or an array of ids
            );
            if (deletecount == 0) {
                client.info("No entities deleted.");
            } else {
                client.info("Deleted " + deletecount + " entities.");
            }

            gotwatchevent = false;
            Client.WatchEventCallback eventCallback = new Client.WatchEventCallback() {
                @Override
                public void onEvent(WatchEvent event) {
                    client.info("Received watch event:");
                    client.info(" Operation: " + event.operation);
                    client.info(" Document: " + event.document);
                    gotwatchevent = true;
                }
            };

            WatchParameters watchParams = new WatchParameters.Builder()
                    .collectionname("entities")
                    .build();

            String watchId = client.watchAsync(watchParams, eventCallback);
            client.info("Watch started with id: " + watchId);

            client.insertOne(Entity.class,
                    new InsertOneParameters.Builder()
                            .collectionname("entities")
                            .itemFromObject(new Entity() {
                                {
                                    name = "watchtest";
                                    _type = "test";
                                    java = "many";
                                }
                            })
                            .build());
            do {
                Thread.sleep(1000);
            } while (gotwatchevent == false);

            watchId = client.watchAsync(
                    new WatchParameters.Builder()
                            .collectionname("entities")
                            .build(),
                    (result) -> {
                        client.info("Watch2 result: " + result.operation + " on " + result.id
                                + " " + result.document);
                    });
            client.info("Watch2 started with id: " + watchId);
            client.unwatch(watchId);

            InsertOneParameters insertOneParams4 = new InsertOneParameters.Builder()
                    .collectionname("entities")
                    .itemFromObject(new Entity() {
                        {
                            name = "watchtest";
                            _type = "test";
                            java = "many";
                        }
                    })
                    .build();

            Entity insertedEntity4 = client.insertOne(Entity.class, insertOneParams4);
            client.info("InsertOne Result (Entity): " + insertedEntity4.name + "id: " + insertedEntity4._id);

            var id = client.upload(
                    new UploadParameters.Builder()
                            .filepath("testfile.csv")
                            .filename("testfile.csv")
                            .metadata("{\"_type\":\"test\"}")
                            .collectionname("fs.files")
                            .build());
            client.info("testfile.csv uploaded as " + id);
            var filename = client.download(
                    new DownloadParameters.Builder()
                            .collectionname("fs.files")
                            .filename("train.csv")
                            .id(id)
                            .build());
            client.info(id + " downloaded as " + filename);

            var count = client.count(
                    new CountParameters.Builder()
                            .collectionname("entities")
                            .query("{\"_type\":\"test\"}")
                            .build());
            client.info("Count: " + count);

            var distinct = client.distinct(
                    new DistinctParameters.Builder()
                            .collectionname("entities")
                            .field("_type")
                            .build());
            client.info("Distinct: " + distinct);

            var queuename = client.registerQueueAsync(
                    new RegisterQueueParameters.Builder()
                            .queuename("test2queue")
                            .build(),
                    (eventPtr) -> {
                        queuemessagecount++;
                        return "";
                    });
            client.info("Wait for message sent to queue " + queuename);

            queuetimer = new Timer(true);
            queuetimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        client.queueMessage(
                                new QueueMessageParameters.Builder()
                                        .queuename("test2queue")
                                        .striptoken(true)
                                        .message("{\"find\":\"me\"}")
                                        .build());
                    } catch (Exception e) {
                        // Silently cancel timer if client is disconnected
                        if (e.getMessage().contains("Not connected")) {
                            queuetimer.cancel();
                        } else {
                            e.printStackTrace();
                        }
                    }
                }
            }, 0, 3000);

            do {
                Thread.sleep(1000);
            } while (queuemessagecount < 3);
            client.info("Quere message received");
            client.unregisterQueue(queuename);

            var excqueuename = client.registerExchangeAsync(
                    new RegisterExchangeParameters.Builder()
                            .exchangename("test2exchange")
                            .algorithm("fanout")
                            .addqueue(true)
                            .build(),
                    (result) -> {
                        client.info("Exchange result: " + result.data + " on " + result.queuename);
                        exchangemessagecount++;
                    });
            client.info("Wait for message sent to exchange queue " + excqueuename);

            exctimer = new Timer(true);
            exctimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        client.queueMessage(
                                new QueueMessageParameters.Builder()
                                        .exchangename("test2exchange")
                                        .striptoken(true)
                                        .message("{\"find\":\"me\"}")
                                        .build());

                    } catch (Exception e) {
                        // Silently cancel timer if client is disconnected
                        if (e.getMessage().contains("Not connected")) {
                            exctimer.cancel();
                        } else {
                            e.printStackTrace();
                        }
                    }
                }
            }, 0, 3000);

            do {
                Thread.sleep(1000);
            } while (exchangemessagecount < 3);
            client.info("Exchange message received");
            client.unregisterQueue(excqueuename);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (queuetimer != null) {
                queuetimer.cancel();
            }
            if (exctimer != null) {
                exctimer.cancel();
            }
            client.disconnect();
            client.close(); 
            client.info("CLI executed successfully!");
        }
    }
}
