package io.openiap;

import java.util.Scanner;
import java.util.List;
import java.io.File;
import java.util.Arrays;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;
import java.util.Random;

public class mainTestCli {
    private static Client client;
    private static volatile boolean running = true;
    private static Scanner scanner;
    private static ExecutorService executor;
    private static Future<?> runningTask;
    private static AtomicBoolean taskRunning = new AtomicBoolean(false);
    
    // Scheduled executor for the observable gauge timers
    private static ScheduledExecutorService scheduledExecutor;
    private static ScheduledFuture<?> f64GaugeTask;
    private static ScheduledFuture<?> u64GaugeTask;
    private static ScheduledFuture<?> i64GaugeTask;

    public static void main(String[] args) {
        System.out.println("CLI initializing...");
        String libpath = NativeLoader.loadLibrary("openiap");
        client = new Client(libpath);
        scanner = new Scanner(System.in);
        executor = Executors.newSingleThreadExecutor();
        scheduledExecutor = Executors.newScheduledThreadPool(3);
        try {
            // client.enableTracing("openiap=trace", "");
            client.enableTracing("openiap=info", "");
            client.start();
            client.connect("");
            client.info("? for help");
            // Simple check to see if we are running inside a container, then run the st_func
            if(System.getenv("oidc_config") != null && System.getenv("oidc_config") != "") {
                handleStartTask();
            }
            while (running) {
                System.out.print("> ");
                String command = scanner.nextLine().trim().toLowerCase();
                handleCommand(command);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (executor != null) {
                executor.shutdownNow();
            }
            if (scheduledExecutor != null) {
                scheduledExecutor.shutdownNow();
            }
            if (client != null) {
                client.disconnect();
            }
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    private static void handleCommand(String command) throws Exception {
        switch (command) {
            case "?":
                showHelp();
                break;
            case "t":
                clienttestcli.RunAll();
                break;
            case "o":
                handleObservable();
                break;
            case "o2":
                handleObservableU64();
                break;
            case "o3":
                handleObservableI64();
                break;
            case "q":
                handleQuery();
                break;
            case "qq":
                handleQueryAll();
                break;
            case "di":
                handleDistinct();
                break;
            case "p":
                handlePopWorkitem();
                break;
            case "p1":
                handlePushWorkitem();
                break;
            case "p2":
                handlePushWorkitem2();
                break;
            case "s":
                handleSignInGuest();
                break;
            case "s2":
                handleSignInTestUser();
                break;
            case "i":
                handleInsertOne();
                break;
            case "im":
                handleInsertMany();
                break;
            case "w":
                handleWatch();
                break;
            case "st":
                handleStartTask();
                break;
            case "st2":
                handleStartTask2();
                break;
            case "pd":
                handleDeleteWorkitem();
                break;
            case "cc":
                handleCustomCommand();
                break;
            case "rpa":
                handleInvokeOpenRPA();
                break;
            case "quit":
                running = false;
                break;
            default:
                client.info("Unknown command. Type ? for help.");
                break;
        }
    }

    private static void showHelp() {
        System.out.println("Available commands:");
        System.out.println("  ?    - Show this help");
        System.out.println("  t    - Run all tests");
        System.out.println("  q    - Query with filter");
        System.out.println("  qq   - Query all");
        System.out.println("  di   - Distinct types");
        System.out.println("  p1   - PushWorkitem");
        System.out.println("  p2   - PushWorkitem second test");
        System.out.println("  p    - Pop/Update workitem state");
        System.out.println("  pd   - Pop/Delete workitem");
        System.out.println("  s    - Sign in as guest");
        System.out.println("  s2   - Sign in as testuser");
        System.out.println("  i    - Insert one");
        System.out.println("  im   - Insert many");
        System.out.println("  w    - Watch collection");
        System.out.println("  st   - Start/stop task (workitem processing)");
        System.out.println("  st2  - Start/stop task (continuous testing)");
        System.out.println("  o    - Toggle f64 observable gauge");
        System.out.println("  o2   - Toggle u64 observable gauge");
        System.out.println("  o3   - Toggle i64 observable gauge");
        System.out.println("  cc   - Get Clients using custom_command");
        System.out.println("  rpa  - Invoke \"Who am I\" on robot \"allan5\"");
        System.out.println("  quit - Exit program");
    }

    private static void handleObservable() {
        try {
            if (f64GaugeTask != null && !f64GaugeTask.isDone()) {
                client.disable_observable_gauge("test_f64");
                f64GaugeTask.cancel(false);
                f64GaugeTask = null;
                client.info("stopped test_f64");
                return;
            }
            
            client.set_f64_observable_gauge("test_f64", 42.7, "test");
            client.info("started test_f64 to 42.7");
            
            Random random = new Random();
            f64GaugeTask = scheduledExecutor.scheduleAtFixedRate(() -> {
                double randomValue = random.nextDouble() * 50;
                client.info("Setting test_f64 to " + randomValue);
                client.set_f64_observable_gauge("test_f64", randomValue, "test");
            }, 30, 30, TimeUnit.SECONDS);
        } catch (Exception e) {
            client.error("Observable gauge error: " + e.getMessage());
        }
    }
    
    private static void handleObservableU64() {
        try {
            if (u64GaugeTask != null && !u64GaugeTask.isDone()) {
                client.disable_observable_gauge("test_u64");
                u64GaugeTask.cancel(false);
                u64GaugeTask = null;
                client.info("stopped test_u64");
                return;
            }
            
            client.set_u64_observable_gauge("test_u64", 42, "test");
            client.info("started test_u64 to 42");
            
            Random random = new Random();
            u64GaugeTask = scheduledExecutor.scheduleAtFixedRate(() -> {
                long randomValue = (long) (random.nextDouble() * 50);
                client.info("Setting test_u64 to " + randomValue);
                client.set_u64_observable_gauge("test_u64", randomValue, "test");
            }, 30, 30, TimeUnit.SECONDS);
        } catch (Exception e) {
            client.error("Observable gauge error: " + e.getMessage());
        }
    }
    
    private static void handleObservableI64() {
        try {
            if (i64GaugeTask != null && !i64GaugeTask.isDone()) {
                client.disable_observable_gauge("test_i64");
                i64GaugeTask.cancel(false);
                i64GaugeTask = null;
                client.info("stopped test_i64");
                return;
            }
            
            client.set_i64_observable_gauge("test_i64", 42, "test");
            client.info("started test_i64 to 42");
            
            Random random = new Random();
            i64GaugeTask = scheduledExecutor.scheduleAtFixedRate(() -> {
                long randomValue = (long) (random.nextDouble() * 50) - 25; // Allow negative values for i64
                client.info("Setting test_i64 to " + randomValue);
                client.set_i64_observable_gauge("test_i64", randomValue, "test");
            }, 30, 30, TimeUnit.SECONDS);
        } catch (Exception e) {
            client.error("Observable gauge error: " + e.getMessage());
        }
    }

    private static void handleQuery() {
        try {
            List<clienttestcli.Entity> results = client.query(new TypeReference<List<clienttestcli.Entity>>() {}.getType(),
                new QueryParameters.Builder()
                    .collectionname("entities")
                    .query("{\"_type\":\"test\"}")
                    .top(10)
                    .build());
            if (results != null) {
                for (clienttestcli.Entity item : results) {
                    client.info("Item: " + item._type + " " + item._id + " " + item.name);
                }
            }
        } catch (Exception e) {
            client.error("Query error: " + e.getMessage());
        }
    }

    private static void handleQueryAll() {
        try {
            String jsonResult = client.query(String.class, 
                new QueryParameters.Builder()
                    .collectionname("entities")
                    .query("{}")
                    .build());
            client.info("Results: " + jsonResult);
        } catch (Exception e) {
            client.error("Query error: " + e.getMessage());
        }
    }

    private static void handleDistinct() {
        try {
            List<String> distinct = client.distinct(
                new DistinctParameters.Builder()
                    .collectionname("entities")
                    .field("_type")
                    .build());
            client.info("Distinct types: " + distinct);
        } catch (Exception e) {
            client.error("Distinct error: " + e.getMessage());
        }
    }

    private static void handlePushWorkitem() {
        try {
            clienttestcli.Entity entity = new clienttestcli.Entity();
            entity.name = "CLI Test";
            entity._type = "test";
            var result = client.pushWorkitem(new PushWorkitem.Builder("q2")
                .name("CLI Test")
                //.payload("{\"_type\":\"test\"}")
                .itemFromObject(entity)
                // .nextrun(System.currentTimeMillis() + 10000)
                .priority(1)
                .build());
            client.info("Pushed workitem: " + result);
        } catch (Exception e) {
            client.error("PushWorkitem error: " + e.getMessage());
        }
    }

    private static void handlePushWorkitem2() {
        try {
            List<String> files = Arrays.asList("testfile.csv"
            // , "/home/allan/Documents/assistant-linux-x86_64.AppImage"
            );
            clienttestcli.Entity entity = new clienttestcli.Entity();
            entity.name = "CLI Test";
            entity._type = "test";

            // Create builder and build workitem
            PushWorkitem.Builder builder = new PushWorkitem.Builder("q2")
                .name(entity.name)
                .itemFromObject(entity)
                .files(files);

            PushWorkitem pushWorkitem = builder.build();
            try {
                // Push the workitem and get back a typed response
                Workitem result = client.pushWorkitem(Workitem.class, pushWorkitem);
                
                client.info("Pushed workitem: " + result.id + " name: " + result.name);
                if (result.files != null) {
                    client.info("Files: " + result.files.size());
                    for (WorkitemFile f : result.files) {
                        client.info("  - " + f.filename + " (id: " + f.id + ")");
                    }
                }
            } finally {
                // Clean up after push is complete
                builder.cleanup();
            }
        } catch (Exception e) {
            client.error("PushWorkitem error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleSignInGuest() {
        try {
            var result = client.signin(new SigninParameters.Builder()
                .username("guest")
                .password("guest")
                .build());
            client.info("Signin result: " + result);
        } catch (Exception e) {
            client.error("Signin error: " + e.getMessage());
        }
    }

    private static void handleSignInTestUser() {
        try {
            var result = client.signin(new SigninParameters.Builder()
                .username(System.getenv("testusername"))
                .password(System.getenv("testpassword"))
                .build());
            client.info("Signin result: " + result);
        } catch (Exception e) {
            client.error("Signin error: " + e.getMessage());
        }
    }

    private static void handleInsertOne() {
        try {
            clienttestcli.Entity entity = new clienttestcli.Entity();
            entity.name = "CLI Test";
            entity._type = "test";
            clienttestcli.Entity result = client.insertOne(clienttestcli.Entity.class,
                new InsertOneParameters.Builder()
                    .collectionname("entities")
                    .itemFromObject(entity)
                    .build());
            client.info("Inserted: " + result._id);
        } catch (Exception e) {
            client.error("Insert error: " + e.getMessage());
        }
    }

    private static void handleInsertMany() {
        try {
            String jsonItems = "[{\"_type\":\"test\", \"name\":\"cli-many-1\"}, {\"_type\":\"test\", \"name\":\"cli-many-2\"}]";
            List<clienttestcli.Entity> results = client.insertMany(new TypeReference<List<clienttestcli.Entity>>() {}.getType(),
                new InsertManyParameters.Builder()
                    .collectionname("entities")
                    .items(jsonItems)
                    .build());
            if (results != null) {
                for (clienttestcli.Entity entity : results) {
                    client.info("Inserted: " + entity._id + " - " + entity.name);
                }
            }
        } catch (Exception e) {
            client.error("Insert many error: " + e.getMessage());
        }
    }

    private static void handleWatch() {
        try {
            String watchId = client.watchAsync(
                new WatchParameters.Builder()
                    .collectionname("entities")
                    .build(),
                (event) -> {
                    client.info("Watch event: " + event.operation + " on " + event.id);
                    client.info("Document: " + event.document);
                });
            client.info("Watch started with ID: " + watchId);
            client.info("(Events will appear as they happen. Start a new operation to trigger events)");
        } catch (Exception e) {
            client.error("Watch error: " + e.getMessage());
        }
    }

    private static void handleStartTask() {
        if (taskRunning.get()) {
            client.info("Stopping running task.");
            if (runningTask != null) {
                runningTask.cancel(true);
            }
            taskRunning.set(false);
            return;
        }
        client.info("Starting task...");
        taskRunning.set(true);
        runningTask = executor.submit(() -> {
            client.info("Task started, begin loop...");
            Runtime runtime = Runtime.getRuntime();
            int x = 0;
            while (taskRunning.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    x++;
                    // Add memory usage logging every 100 iterations
                    if (x % 100 == 0) {
                        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
                        client.info("Memory usage: " + usedMemory + "MB");
                    }
                    
                    PopWorkitem popRequest = new PopWorkitem.Builder("q2").build();
                    Workitem workitem = client.popWorkitem(Workitem.class, popRequest, "downloads");
                    
                    Thread.sleep(1);
                    
                    if (workitem != null) {
                        client.info("Updating " + workitem.id + " " + workitem.name);
                        workitem.state = "successful";
                        UpdateWorkitem.Builder builder = new UpdateWorkitem.Builder(workitem);
                        UpdateWorkitem updateRequest = builder.build();
                        workitem = client.updateWorkitem(Workitem.class, updateRequest);
                    } else {
                        if (x % 500 == 0) {
                            client.info("No new workitem " + new java.util.Date());
                            System.gc();
                        }
                    }
                } catch (Exception e) {
                    client.error("Error in task loop: ");
                    e.printStackTrace(System.out);  // Print full stack trace
                    try {
                        Thread.sleep(5000); // Add delay after error
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            client.info("Task canceled.");
        });
    }

    private static void handleStartTask2() {
        if (taskRunning.get()) {
            client.info("Stopping running task.");
            if (runningTask != null) {
                runningTask.cancel(true);
            }
            taskRunning.set(false);
            return;
        }

        taskRunning.set(true);
        runningTask = executor.submit(() -> {
            client.info("Task started, begin loop...");
            int x = 0;
            while (taskRunning.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    x++;
                    Thread.sleep(1);
                    clienttestcli.RunAll();
                    if (x % 500 == 0) {
                        client.info("No new workitem " + new java.util.Date());
                        System.gc();
                    }
                } catch (Exception e) {
                    client.error("Error: " + e.toString());
                }
            }
            client.info("Task canceled.");
        });
    }

    private static void handlePopWorkitem() {
        try {
            // ensure folder downloads exits
            File downloadsFolder = new File("downloads");
            if (!downloadsFolder.exists()) {
                downloadsFolder.mkdir();
            }
            PopWorkitem popRequest = new PopWorkitem.Builder("q2").build();
            Workitem workitem = client.popWorkitem(Workitem.class, popRequest, "downloads");
            
            if (workitem != null) {
                client.info("Updating workitem: " + workitem.id);
                
                // Update the workitem state
                workitem.state = "successful";
                workitem.name = "Updated by CLI";
                
                // Create update request
                UpdateWorkitem.Builder builder = new UpdateWorkitem.Builder(workitem);
                builder.files(
                    Arrays.asList("/home/allan/Documents/export.csv")
                    // Arrays.asList("/home/allan/Documents/export.csv", "downloads/testfile.csv")
                );
                UpdateWorkitem updateRequest = builder.build();
                
                try {
                    // Send update
                    workitem = client.updateWorkitem(Workitem.class, updateRequest);
                    client.info("Updated workitem state to: " + workitem.state);
                } finally {
                    builder.cleanup();
                }
            } else {
                client.info("No workitem available to update");
            }
        } catch (Exception e) {
            client.error("UpdateWorkitem error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleDeleteWorkitem() {
        try {
            // First pop a workitem to delete
            PopWorkitem popRequest = new PopWorkitem.Builder("q2").build();
            Workitem workitem = client.popWorkitem(Workitem.class, popRequest, "downloads");
            
            if (workitem != null) {
                client.info("Deleting workitem: " + workitem.id);
                
                // Create delete request
                DeleteWorkitem deleteRequest = new DeleteWorkitem.Builder(workitem.id).build();
                
                // Send delete
                boolean success = client.deleteWorkitem(deleteRequest);
                if (success) {
                    client.info("Workitem deleted successfully");
                } else {
                    client.info("Failed to delete workitem");
                }
            } else {
                client.info("No workitem available to delete");
            }
        } catch (Exception e) {
            client.error("DeleteWorkitem error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private static void handleCustomCommand() {
        try {
            String result = client.customCommand(
                new CustomCommandParameters.Builder()
                    .command("getclients")
                    .build(),
                    10
            );
            System.out.println("CustomCommand result: " + result);
            // Optionally, parse JSON and print details if needed
            // ObjectMapper mapper = new ObjectMapper();
            // List<?> clients = mapper.readValue(result, List.class);
            // System.out.println("Client count: " + clients.size());
        } catch (Exception e) {
            System.out.println("CustomCommand error: " + e.getMessage());
        }
    }
    private static void handleInvokeOpenRPA() {
        try {
            String result = client.invokeOpenRPA(
                new InvokeOpenRPAParameters.Builder()
                    .robotid("5ce94386320b9ce0bc2c3d07")
                    .workflowid("5e0b52194f910e30ce9e3e49")
                    .payload("{\"test\":\"test\"}")
                    .rpc(false)
                    .build(),
                    10
            );
            System.out.println("InvokeOpenRPA result: " + result);
        } catch (Exception e) {
            System.out.println("InvokeOpenRPA error: " + e.getMessage());
        }
    }
}

