import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

public final class OriginalWitcheryCaptureAgent {
    private static final long ACTION_TIMEOUT_MILLIS = 120_000L;
    private static final ConcurrentLinkedQueue<FutureTask<Void>> SERVER_ACTIONS =
            new ConcurrentLinkedQueue<>();
    private static Path runLog;
    private static boolean serverBridgeInstalled;

    private OriginalWitcheryCaptureAgent() {
    }

    public static void premain(String planPath, Instrumentation instrumentation) {
        start(planPath, instrumentation);
    }

    public static void agentmain(String planPath, Instrumentation instrumentation) {
        start(planPath, instrumentation);
    }

    private static void start(String planPath, Instrumentation instrumentation) {
        runLog = Paths.get(planPath + ".run.log");
        try {
            Files.deleteIfExists(runLog);
        } catch (Exception exception) {
            System.err.println("[Witchery original capture] Could not reset run log " + runLog);
        }
        Thread worker = new Thread(() -> runPlan(Paths.get(planPath), instrumentation),
                "witchery-original-capture-agent");
        worker.setDaemon(true);
        worker.start();
    }

    private static void runPlan(Path planPath, Instrumentation instrumentation) {
        try {
            Class<?> minecraftClass = waitForLoadedClass(instrumentation,
                    "net.minecraft.client.Minecraft", "bao");
            ClassLoader gameLoader = minecraftClass.getClassLoader();
            Object minecraft = waitForMinecraftInstance(minecraftClass);
            Method schedule = findMethod(minecraftClass, new Class<?>[]{Runnable.class},
                    "func_152344_a", "a");
            List<String> lines = Files.readAllLines(planPath, StandardCharsets.UTF_8);
            Path outputRoot = null;

            log("Running plan " + planPath);
            for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
                String raw = lines.get(lineNumber).trim();
                if (raw.isEmpty() || raw.startsWith("#")) {
                    continue;
                }

                int separator = raw.indexOf(' ');
                String directive = separator < 0 ? raw : raw.substring(0, separator);
                String argument = separator < 0 ? "" : raw.substring(separator + 1).trim();
                log("Line " + (lineNumber + 1) + ": " + directive);

                switch (directive) {
                    case "OUTPUT":
                        outputRoot = Paths.get(argument);
                        Files.createDirectories(outputRoot);
                        break;
                    case "LOAD":
                        String[] world = argument.split("\\|", 2);
                        if (world.length != 2) {
                            throw new IllegalArgumentException("LOAD requires folder|display-name");
                        }
                        invokeOnClient(schedule, minecraft, () -> invokeWorldLoad(minecraftClass, minecraft,
                                gameLoader, world[0], world[1]));
                        break;
                    case "WAIT_WORLD":
                        waitForWorld(minecraftClass, minecraft, Long.parseLong(argument));
                        break;
                    case "WAIT":
                        Thread.sleep(Long.parseLong(argument));
                        break;
                    case "WAIT_ENTITY":
                        String[] waitEntity = argument.split("\\s+");
                        if (waitEntity.length != 2) {
                            throw new IllegalArgumentException("WAIT_ENTITY requires class timeout-ms");
                        }
                        waitForEntity(schedule, minecraftClass, minecraft, waitEntity[0],
                                Long.parseLong(waitEntity[1]));
                        break;
                    case "NO_PAUSE":
                        invokeOnClient(schedule, minecraft,
                                () -> setGameSetting(minecraftClass, minecraft, "w", false));
                        break;
                    case "HIDE_GUI":
                        boolean hideGui = Boolean.parseBoolean(argument);
                        invokeOnClient(schedule, minecraft,
                                () -> setGameSetting(minecraftClass, minecraft, "av", hideGui));
                        break;
                    case "GAMMA":
                        float gamma = Float.parseFloat(argument);
                        invokeOnClient(schedule, minecraft,
                                () -> setGameSettingFloat(minecraftClass, minecraft, "aG", gamma));
                        break;
                    case "FOV":
                        float fov = Float.parseFloat(argument);
                        invokeOnClient(schedule, minecraft,
                                () -> setGameSettingFloat(minecraftClass, minecraft, "aF", fov));
                        break;
                    case "THIRD_PERSON":
                        int thirdPerson = Integer.parseInt(argument);
                        invokeOnClient(schedule, minecraft,
                                () -> setGameSetting(minecraftClass, minecraft, "aw", thirdPerson));
                        break;
                    case "LOOK":
                        String[] look = argument.split("\\s+");
                        if (look.length != 2) {
                            throw new IllegalArgumentException("LOOK requires yaw pitch");
                        }
                        float yaw = Float.parseFloat(look[0]);
                        float pitch = Float.parseFloat(look[1]);
                        invokeOnClient(schedule, minecraft,
                                () -> setPlayerLook(minecraftClass, minecraft, yaw, pitch));
                        break;
                    case "CLEAR_SCREEN":
                        invokeOnClient(schedule, minecraft,
                                () -> clearScreen(minecraftClass, minecraft, gameLoader));
                        break;
                    case "COMMAND":
                        invokeOnClient(schedule, minecraft,
                                () -> sendCommand(minecraftClass, minecraft, argument));
                        break;
                    case "WAIT_DIMENSION":
                        waitForDimension(minecraftClass, minecraft, Integer.parseInt(argument));
                        break;
                    case "SPIRIT_ENTER":
                        invokeOnServer(minecraftClass, minecraft,
                                () -> enterSpiritWorld(minecraftClass, minecraft, gameLoader, argument));
                        break;
                    case "SPIRIT_STATE":
                        invokeOnServer(minecraftClass, minecraft,
                                () -> setSpiritState(minecraftClass, minecraft, gameLoader, argument));
                        break;
                    case "SPIRIT_STUDIO":
                        invokeOnServer(minecraftClass, minecraft,
                                () -> buildSpiritStudio(minecraftClass, minecraft, gameLoader, argument));
                        break;
                    case "SPIRIT_PRESENTATION":
                        invokeOnServer(minecraftClass, minecraft,
                                () -> setupSpiritPresentation(minecraftClass, minecraft, gameLoader, argument));
                        invokeOnClient(schedule, minecraft,
                                () -> setupSpiritPresentationClient(minecraftClass, minecraft, gameLoader, argument));
                        break;
                    case "SPIRIT_WAKE":
                        invokeOnServer(minecraftClass, minecraft,
                                () -> wakeFromSpiritWorld(minecraftClass, minecraft, gameLoader));
                        break;
                    case "REPORT_SPIRIT":
                        invokeOnClient(schedule, minecraft,
                                () -> reportSpiritState(minecraftClass, minecraft));
                        break;
                    case "POSE_ENTITY":
                        invokeOnClient(schedule, minecraft,
                                () -> poseEntity(minecraftClass, minecraft, argument));
                        break;
                    case "CLEAR_ENTITIES":
                        invokeOnClient(schedule, minecraft,
                                () -> clearEntities(minecraftClass, minecraft, argument));
                        break;
                    case "REPORT":
                        invokeOnClient(schedule, minecraft,
                                () -> reportScene(minecraftClass, minecraft));
                        break;
                    case "CAPTURE":
                        if (outputRoot == null) {
                            throw new IllegalStateException("OUTPUT must appear before CAPTURE");
                        }
                        Path captureRoot = outputRoot;
                        invokeOnClient(schedule, minecraft,
                                () -> capture(minecraftClass, minecraft, gameLoader, captureRoot, argument));
                        break;
                    case "POSE_CAPTURE":
                        if (outputRoot == null) {
                            throw new IllegalStateException("OUTPUT must appear before POSE_CAPTURE");
                        }
                        String[] poseCapture = argument.split("\\s+");
                        if (poseCapture.length != 7) {
                            throw new IllegalArgumentException(
                                    "POSE_CAPTURE requires class x y z yaw pitch file");
                        }
                        String poseArgument = String.join(" ", java.util.Arrays.copyOf(poseCapture, 6));
                        String captureFile = poseCapture[6];
                        Path poseCaptureRoot = outputRoot;
                        invokeOnClient(schedule, minecraft, () -> {
                            poseEntity(minecraftClass, minecraft, poseArgument);
                            reportScene(minecraftClass, minecraft);
                            capture(minecraftClass, minecraft, gameLoader, poseCaptureRoot, captureFile);
                        });
                        break;
                    case "STABILIZE_CAPTURE":
                        if (outputRoot == null) {
                            throw new IllegalStateException("OUTPUT must appear before STABILIZE_CAPTURE");
                        }
                        String[] stableCapture = argument.split("\\s+");
                        if (stableCapture.length != 7) {
                            throw new IllegalArgumentException(
                                    "STABILIZE_CAPTURE requires class x y z yaw pitch file");
                        }
                        String stablePose = String.join(" ", java.util.Arrays.copyOf(stableCapture, 6));
                        String stableFile = stableCapture[6];
                        Path stableRoot = outputRoot;
                        for (int stableFrame = 0; stableFrame < 8; stableFrame++) {
                            invokeOnClient(schedule, minecraft,
                                    () -> poseEntity(minecraftClass, minecraft, stablePose));
                            Thread.sleep(5L);
                        }
                        invokeOnClient(schedule, minecraft, () -> {
                            poseEntity(minecraftClass, minecraft, stablePose);
                            reportScene(minecraftClass, minecraft);
                            capture(minecraftClass, minecraft, gameLoader, stableRoot, stableFile);
                        });
                        break;
                    case "STOP":
                        invokeOnClient(schedule, minecraft, () -> shutdown(minecraftClass, minecraft));
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown directive: " + directive);
                }
            }
            log("Plan complete");
        } catch (Throwable throwable) {
            System.err.println("[Witchery original capture] Plan failed");
            throwable.printStackTrace(System.err);
            log("Plan failed: " + throwable);
            for (StackTraceElement frame : throwable.getStackTrace()) {
                log("  at " + frame);
            }
        }
    }

    private static Class<?> waitForLoadedClass(Instrumentation instrumentation, String... names)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + ACTION_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            for (Class<?> loaded : instrumentation.getAllLoadedClasses()) {
                for (String name : names) {
                    if (name.equals(loaded.getName())) {
                        return loaded;
                    }
                }
            }
            Thread.sleep(100L);
        }
        throw new IllegalStateException("Minecraft class was not loaded: " + String.join(", ", names));
    }

    private static Object waitForMinecraftInstance(Class<?> minecraftClass) throws Exception {
        Method getter = findMethod(minecraftClass, new Class<?>[0], "func_71410_x", "B");
        long deadline = System.currentTimeMillis() + ACTION_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            Object minecraft = getter.invoke(null);
            if (minecraft != null) {
                return minecraft;
            }
            Thread.sleep(100L);
        }
        throw new IllegalStateException("Minecraft singleton was not initialized");
    }

    private static void invokeWorldLoad(Class<?> minecraftClass, Object minecraft, ClassLoader loader,
                                        String folder, String displayName) {
        try {
            Class<?> worldSettings = loadClass(loader, "net.minecraft.world.WorldSettings", "ahj");
            Method load = findMethod(minecraftClass,
                    new Class<?>[]{String.class, String.class, worldSettings}, "func_71371_a", "a");
            load.invoke(minecraft, folder, displayName, null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not load original world", exception);
        }
    }

    private static void waitForWorld(Class<?> minecraftClass, Object minecraft, long timeoutMillis)
            throws Exception {
        Field player = field(minecraftClass, "field_71439_g", "h");
        Field level = field(minecraftClass, "field_71441_e", "f");
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (player.get(minecraft) != null && level.get(minecraft) != null) {
                log("World and player are ready");
                return;
            }
            Thread.sleep(100L);
        }
        throw new IllegalStateException("Original world did not become ready within " + timeoutMillis + " ms");
    }

    private static void waitForEntity(Method schedule, Class<?> minecraftClass, Object minecraft,
                                      String className, long timeoutMillis) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            AtomicBoolean found = new AtomicBoolean();
            invokeOnClient(schedule, minecraft,
                    () -> found.set(hasLoadedEntity(minecraftClass, minecraft, className)));
            if (found.get()) {
                log("Entity ready: " + className);
                return;
            }
            Thread.sleep(10L);
        }
        throw new IllegalStateException("Entity did not become ready: " + className);
    }

    private static boolean hasLoadedEntity(Class<?> minecraftClass, Object minecraft, String className) {
        try {
            Object level = field(minecraftClass, "field_71441_e", "f").get(minecraft);
            Object loaded = field(level.getClass(), "field_72996_f", "e").get(level);
            if (loaded instanceof Iterable<?>) {
                for (Object entity : (Iterable<?>) loaded) {
                    Class<?> type = entity.getClass();
                    if (className.equals(type.getName()) || className.equals(type.getSimpleName())) {
                        return true;
                    }
                }
            }
            return false;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not inspect original entities", exception);
        }
    }

    private static void setGameSetting(Class<?> minecraftClass, Object minecraft, String fieldName, Object value) {
        try {
            Object settings = field(minecraftClass, "field_71474_y", "u").get(minecraft);
            Field setting;
            if ("w".equals(fieldName)) {
                setting = field(settings.getClass(), "field_82881_y", "w");
            } else if ("av".equals(fieldName)) {
                setting = field(settings.getClass(), "field_74319_N", "av");
            } else if ("aw".equals(fieldName)) {
                setting = field(settings.getClass(), "field_74320_O", "aw");
            } else {
                setting = field(settings.getClass(), fieldName);
            }
            if (value instanceof Boolean) {
                setting.setBoolean(settings, (Boolean) value);
            } else if (value instanceof Integer) {
                setting.setInt(settings, (Integer) value);
            } else {
                throw new IllegalArgumentException("Unsupported setting value " + value);
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not set original game setting " + fieldName, exception);
        }
    }

    private static void clearScreen(Class<?> minecraftClass, Object minecraft, ClassLoader loader) {
        try {
            Class<?> screenClass = loadClass(loader, "net.minecraft.client.gui.GuiScreen", "bdw");
            Method setScreen = findMethod(minecraftClass, new Class<?>[]{screenClass},
                    "func_147108_a", "a");
            setScreen.invoke(minecraft, new Object[]{null});
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not clear original GUI screen", exception);
        }
    }

    private static void setGameSettingFloat(Class<?> minecraftClass, Object minecraft,
                                            String fieldName, float value) {
        try {
            Object settings = field(minecraftClass, "field_71474_y", "u").get(minecraft);
            Field setting;
            if ("aG".equals(fieldName)) {
                setting = field(settings.getClass(), "field_74333_Y", "aG");
            } else if ("aF".equals(fieldName)) {
                setting = field(settings.getClass(), "field_74334_X", "aF");
            } else {
                setting = field(settings.getClass(), fieldName);
            }
            setting.setFloat(settings, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not set original float game setting " + fieldName, exception);
        }
    }

    private static void setPlayerLook(Class<?> minecraftClass, Object minecraft, float yaw, float pitch) {
        try {
            Object player = field(minecraftClass, "field_71439_g", "h").get(minecraft);
            if (player == null) {
                throw new IllegalStateException("Cannot set look direction without a player");
            }
            field(player.getClass(), "field_70177_z", "y").setFloat(player, yaw);
            field(player.getClass(), "field_70125_A", "z").setFloat(player, pitch);
            field(player.getClass(), "field_70126_B", "A").setFloat(player, yaw);
            field(player.getClass(), "field_70127_C", "B").setFloat(player, pitch);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not set original player look direction", exception);
        }
    }

    private static void sendCommand(Class<?> minecraftClass, Object minecraft, String command) {
        try {
            Object player = field(minecraftClass, "field_71439_g", "h").get(minecraft);
            if (player == null) {
                throw new IllegalStateException("Cannot send command without a player");
            }
            Method sendChat = findMethod(player.getClass(), new Class<?>[]{String.class},
                    "func_71165_d", "a");
            sendChat.invoke(player, command);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not send original command " + command, exception);
        }
    }

    private static void waitForDimension(Class<?> minecraftClass, Object minecraft, int dimension)
            throws Exception {
        long deadline = System.currentTimeMillis() + ACTION_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            Object player = field(minecraftClass, "field_71439_g", "h").get(minecraft);
            if (player != null
                    && field(player.getClass(), "field_71093_bK", "ap").getInt(player) == dimension) {
                log("Player reached dimension " + dimension);
                return;
            }
            Thread.sleep(100L);
        }
        throw new IllegalStateException("Player did not reach dimension " + dimension);
    }

    private static void enterSpiritWorld(Class<?> minecraftClass, Object minecraft,
                                         ClassLoader loader, String mode) {
        try {
            Object serverPlayer = serverPlayer(minecraftClass, minecraft);
            Class<?> provider = loadClass(loader,
                    "com.emoniph.witchery.dimension.WorldProviderDreamWorld");
            Method enter = methodByNameAndCount(provider, "sendPlayerToSpiritWorld", 2);
            boolean nightmare = !"dream".equalsIgnoreCase(mode);
            enter.invoke(null, serverPlayer, nightmare ? 1.0D : 0.0D);
            if ("demonic".equalsIgnoreCase(mode)) {
                setSpiritState(minecraftClass, minecraft, loader, "demonic");
            }
            log("Requested original Spirit World entry: " + mode);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not enter the original Spirit World", exception);
        }
    }

    private static void setSpiritState(Class<?> minecraftClass, Object minecraft,
                                       ClassLoader loader, String mode) {
        try {
            int level = "demonic".equalsIgnoreCase(mode) ? 2
                    : "nightmare".equalsIgnoreCase(mode) ? 1 : 0;
            boolean nightmare = level > 0;
            boolean demonic = level > 1;
            Class<?> provider = loadClass(loader,
                    "com.emoniph.witchery.dimension.WorldProviderDreamWorld");
            Object serverPlayer = serverPlayer(minecraftClass, minecraft);
            Method setter = methodByNameAndFirstArgument(provider,
                    "setPlayerHasNightmare", 3, serverPlayer);
            setter.invoke(null, serverPlayer, nightmare, demonic);
            setProviderNightmare(serverPlayer, level);

            Object clientPlayer = field(minecraftClass, "field_71439_g", "h").get(minecraft);
            if (clientPlayer != null) {
                setter.invoke(null, clientPlayer, nightmare, demonic);
                setProviderNightmare(clientPlayer, level);
            }
            log("Original Spirit World state set to " + mode + " (" + level + ")");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not set the original Spirit World state", exception);
        }
    }

    private static void wakeFromSpiritWorld(Class<?> minecraftClass, Object minecraft,
                                            ClassLoader loader) {
        try {
            Object serverPlayer = serverPlayer(minecraftClass, minecraft);
            Class<?> provider = loadClass(loader,
                    "com.emoniph.witchery.dimension.WorldProviderDreamWorld");
            methodByNameAndCount(provider, "returnPlayerToOverworld", 1)
                    .invoke(null, serverPlayer);
            log("Requested original Spirit World wake-up");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not wake from the original Spirit World", exception);
        }
    }

    private static void buildSpiritStudio(Class<?> minecraftClass, Object minecraft,
                                          ClassLoader loader, String argument) {
        String[] values = argument.split("\\s+");
        if (values.length != 3) {
            throw new IllegalArgumentException("SPIRIT_STUDIO requires center-x floor-y center-z");
        }
        int centerX = Integer.parseInt(values[0]);
        int floorY = Integer.parseInt(values[1]);
        int centerZ = Integer.parseInt(values[2]);
        try {
            Object serverPlayer = serverPlayer(minecraftClass, minecraft);
            Object world = field(serverPlayer.getClass(), "field_70170_p", "o").get(serverPlayer);
            Class<?> blockClass = loadClass(loader, "net.minecraft.block.Block", "aji");
            Class<?> blocksClass = loadClass(loader, "net.minecraft.init.Blocks", "ajn");
            Object grass = field(blocksClass, "field_150349_c", "c").get(null);
            Object stone = field(blocksClass, "field_150348_b", "b").get(null);
            Object water = field(blocksClass, "field_150355_j", "j").get(null);
            Method setBlock = findMethod(world.getClass(),
                    new Class<?>[]{int.class, int.class, int.class, blockClass, int.class, int.class},
                    "func_147465_d", "d");

            for (int x = centerX - 32; x <= centerX + 32; x++) {
                for (int z = centerZ - 32; z <= centerZ + 32; z++) {
                    setBlock.invoke(world, x, floorY, z, grass, 0, 2);
                }
            }
            for (int x = centerX - 5; x <= centerX + 5; x++) {
                for (int z = centerZ + 8; z <= centerZ + 14; z++) {
                    setBlock.invoke(world, x, floorY, z, water, 0, 2);
                }
            }
            for (int y = floorY + 1; y <= floorY + 8; y++) {
                setBlock.invoke(world, centerX - 8, y, centerZ + 16, stone, 0, 2);
                if (y <= floorY + 4) {
                    setBlock.invoke(world, centerX + 8, y, centerZ + 16, stone, 0, 2);
                }
            }
            log("Built original Spirit World comparison studio at "
                    + centerX + " " + floorY + " " + centerZ);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not build the original Spirit World studio", exception);
        }
    }

    private static void setupSpiritPresentation(Class<?> minecraftClass, Object minecraft,
                                                ClassLoader loader, String captureCase) {
        final int centerX = 4096;
        final int floorY = 120;
        final int centerZ = 4096;
        try {
            Object player = serverPlayer(minecraftClass, minecraft);
            Object world = field(player.getClass(), "field_70170_p", "o").get(player);
            clearPresentationEntities(world);
            buildPresentationFloor(world, loader, centerX, floorY, centerZ);
            setOriginalGhost(loader, player, false);
            clearAndEquipPresentationInventory(loader, player, false);

            double playerX = centerX + 0.5D;
            double playerY = floorY + 2.0D;
            double playerZ = centerZ - 5.5D;
            if ("body".equalsIgnoreCase(captureCase)) {
                playerZ = centerZ - 2.5D;
                spawnPresentationEntity(loader, world, "com.emoniph.witchery.entity.EntityCorpse",
                        centerX + 0.5D, floorY + 1.0D, centerZ + 3.0D, player);
            } else if ("manifestation".equalsIgnoreCase(captureCase)) {
                playerY = floorY + 1.0D;
                playerZ = centerZ + 1.0D;
                setOriginalGhost(loader, player, true);
                clearAndEquipPresentationInventory(loader, player, true);
            } else if ("spirit".equalsIgnoreCase(captureCase)) {
                playerZ = centerZ + 0.5D;
            } else if ("nightmare".equalsIgnoreCase(captureCase)
                    || "demonic-nightmare".equalsIgnoreCase(captureCase)) {
                playerZ = centerZ - 3.5D;
            } else if ("portal".equalsIgnoreCase(captureCase)) {
                buildPresentationPortal(world, loader, centerX, floorY, centerZ + 3);
            } else {
                throw new IllegalArgumentException("Unknown Spirit presentation case " + captureCase);
            }

            Method place = findMethod(player.getClass(),
                    new Class<?>[]{double.class, double.class, double.class, float.class, float.class},
                    "func_70012_b", "b");
            place.invoke(player, playerX, playerY, playerZ, 0.0F,
                    "body".equalsIgnoreCase(captureCase) ? 13.0F
                            : "nightmare".equalsIgnoreCase(captureCase)
                            || "demonic-nightmare".equalsIgnoreCase(captureCase) ? 5.0F : 0.0F);
            log("Prepared original Spirit presentation case " + captureCase);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not prepare original Spirit presentation " + captureCase,
                    exception);
        }
    }

    private static void setupSpiritPresentationClient(Class<?> minecraftClass, Object minecraft,
                                                      ClassLoader loader, String captureCase) {
        try {
            Object player = field(minecraftClass, "field_71439_g", "h").get(minecraft);
            if (player != null) {
                boolean manifestation = "manifestation".equalsIgnoreCase(captureCase);
                setOriginalGhost(loader, player, manifestation);
                clearAndEquipPresentationInventory(loader, player, manifestation);
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not sync original Spirit presentation " + captureCase,
                    exception);
        }
    }

    private static void clearPresentationEntities(Object world) throws ReflectiveOperationException {
        Object loaded = field(world.getClass(), "field_72996_f", "e").get(world);
        if (!(loaded instanceof Iterable<?>)) {
            return;
        }
        List<Object> removals = new ArrayList<>();
        for (Object entity : (Iterable<?>) loaded) {
            String name = entity.getClass().getName();
            if (name.equals("com.emoniph.witchery.entity.EntityCorpse")
                    || name.equals("com.emoniph.witchery.entity.EntitySpirit")
                    || name.equals("com.emoniph.witchery.entity.EntityNightmare")
                    || name.equals("net.minecraft.entity.item.EntityItem")) {
                removals.add(entity);
            }
        }
        for (Object entity : removals) {
            findMethod(entity.getClass(), new Class<?>[0], "func_70106_y", "B").invoke(entity);
        }
    }

    private static void buildPresentationFloor(Object world, ClassLoader loader,
                                               int centerX, int floorY, int centerZ)
            throws ReflectiveOperationException {
        Class<?> blockClass = loadClass(loader, "net.minecraft.block.Block", "aji");
        Class<?> blocksClass = loadClass(loader, "net.minecraft.init.Blocks", "ajn");
        Object grass = field(blocksClass, "field_150349_c", "c").get(null);
        Object air = field(blocksClass, "field_150350_a", "a").get(null);
        Method setBlock = findMethod(world.getClass(),
                new Class<?>[]{int.class, int.class, int.class, blockClass, int.class, int.class},
                "func_147465_d", "d");
        for (int x = centerX - 12; x <= centerX + 12; x++) {
            for (int z = centerZ - 12; z <= centerZ + 12; z++) {
                setBlock.invoke(world, x, floorY, z, grass, 0, 2);
                for (int y = floorY + 1; y <= floorY + 8; y++) {
                    setBlock.invoke(world, x, y, z, air, 0, 2);
                }
            }
        }
    }

    private static void buildPresentationPortal(Object world, ClassLoader loader,
                                                int centerX, int floorY, int portalZ)
            throws ReflectiveOperationException {
        Class<?> blockClass = loadClass(loader, "net.minecraft.block.Block", "aji");
        Class<?> blocksClass = loadClass(loader, "net.minecraft.init.Blocks", "ajn");
        Object snow = field(blocksClass, "field_150433_aE", "aE").get(null);
        Class<?> witcheryClass = loadClass(loader, "com.emoniph.witchery.Witchery");
        Object witcheryBlocks = field(witcheryClass, "Blocks").get(null);
        Object portal = field(witcheryBlocks.getClass(), "SPIRIT_PORTAL").get(witcheryBlocks);
        Method setBlock = findMethod(world.getClass(),
                new Class<?>[]{int.class, int.class, int.class, blockClass, int.class, int.class},
                "func_147465_d", "d");
        for (int width = -1; width <= 2; width++) {
            for (int height = -1; height <= 2; height++) {
                boolean corner = (width == -1 || width == 2) && (height == -1 || height == 2);
                boolean frame = width == -1 || width == 2 || height == -1 || height == 2;
                if (frame && !corner) {
                    setBlock.invoke(world, centerX + width, floorY + 1 + height, portalZ, snow, 0, 2);
                }
            }
        }
        for (int width = 0; width < 2; width++) {
            for (int height = 0; height < 2; height++) {
                setBlock.invoke(world, centerX + width, floorY + 1 + height, portalZ, portal, 0, 2);
            }
        }
    }

    private static void spawnPresentationEntity(ClassLoader loader, Object world, String className,
                                                double x, double y, double z, Object player)
            throws ReflectiveOperationException {
        Class<?> entityClass = loader.loadClass(className);
        Object entity = null;
        for (java.lang.reflect.Constructor<?> constructor : entityClass.getConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            if (parameters.length == 1 && parameters[0].isInstance(world)) {
                entity = constructor.newInstance(world);
                break;
            }
        }
        if (entity == null) {
            throw new IllegalStateException("No world constructor for " + className);
        }
        if (className.endsWith("EntityCorpse")) {
            String name = (String) findMethod(player.getClass(), new Class<?>[0],
                    "func_70005_c_", "b_").invoke(player);
            findMethod(entityClass, new Class<?>[]{String.class}, "setOwner").invoke(entity, name);
            findMethod(entityClass, new Class<?>[]{String.class}, "func_94058_c", "b").invoke(entity, name);
        }
        findMethod(entityClass,
                new Class<?>[]{double.class, double.class, double.class, float.class, float.class},
                "func_70012_b", "b").invoke(entity, x, y, z, 180.0F, 0.0F);
        Class<?> baseEntity = loadClass(loader, "net.minecraft.entity.Entity", "sa");
        Method spawn = findMethod(world.getClass(), new Class<?>[]{baseEntity}, "func_72838_d", "d");
        spawn.invoke(world, entity);
    }

    private static void setOriginalGhost(ClassLoader loader, Object player, boolean ghost)
            throws ReflectiveOperationException {
        Class<?> infusion = loader.loadClass("com.emoniph.witchery.infusion.Infusion");
        Object nbt = methodByNameAndFirstArgument(infusion, "getNBT", 1, player).invoke(null, player);
        Class<?> provider = loader.loadClass("com.emoniph.witchery.dimension.WorldProviderDreamWorld");
        Method setter = methodByNameAndFirstArgument(provider, "setPlayerIsGhost", 2, nbt);
        setter.invoke(null, nbt, ghost);
    }

    private static void clearAndEquipPresentationInventory(ClassLoader loader, Object player, boolean equip)
            throws ReflectiveOperationException {
        Object inventory = field(player.getClass(), "field_71071_by", "bm").get(player);
        Object[] main = (Object[]) field(inventory.getClass(), "field_70462_a", "a").get(inventory);
        Object[] armor = (Object[]) field(inventory.getClass(), "field_70460_b", "b").get(inventory);
        java.util.Arrays.fill(main, null);
        java.util.Arrays.fill(armor, null);
        if (!equip) {
            return;
        }
        Class<?> registry = loader.loadClass("cpw.mods.fml.common.registry.GameRegistry");
        Method findItem = registry.getMethod("findItem", String.class, String.class);
        Class<?> itemClass = loadClass(loader, "net.minecraft.item.Item", "adb");
        Class<?> stackClass = loadClass(loader, "net.minecraft.item.ItemStack", "add");
        java.lang.reflect.Constructor<?> stack = stackClass.getConstructor(itemClass);
        main[0] = stack.newInstance(findItem.invoke(null, "minecraft", "diamond_sword"));
        armor[0] = stack.newInstance(findItem.invoke(null, "minecraft", "diamond_boots"));
        armor[1] = stack.newInstance(findItem.invoke(null, "minecraft", "diamond_leggings"));
        armor[2] = stack.newInstance(findItem.invoke(null, "minecraft", "diamond_chestplate"));
        armor[3] = stack.newInstance(findItem.invoke(null, "minecraft", "diamond_helmet"));
        field(inventory.getClass(), "field_70461_c", "c").setInt(inventory, 0);
    }

    private static void reportSpiritState(Class<?> minecraftClass, Object minecraft) {
        try {
            Object player = field(minecraftClass, "field_71439_g", "h").get(minecraft);
            Object world = field(minecraftClass, "field_71441_e", "f").get(minecraft);
            Object provider = field(world.getClass(), "field_73011_w", "t").get(world);
            int dimension = field(player.getClass(), "field_71093_bK", "ap").getInt(player);
            int nightmare = field(provider.getClass(), "nightmare").getInt(provider);
            long worldTime = ((Number) findMethod(world.getClass(), new Class<?>[0],
                    "func_72820_D", "J").invoke(world)).longValue();
            log("Spirit report dimension=" + dimension + " nightmare=" + nightmare
                    + " worldTime=" + worldTime + " provider=" + provider.getClass().getName());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not report original Spirit World state", exception);
        }
    }

    private static Object serverPlayer(Class<?> minecraftClass, Object minecraft)
            throws ReflectiveOperationException {
        Object clientPlayer = field(minecraftClass, "field_71439_g", "h").get(minecraft);
        if (clientPlayer == null) {
            throw new IllegalStateException("Cannot locate the server player without a client player");
        }
        String name = (String) findMethod(clientPlayer.getClass(), new Class<?>[0],
                "func_70005_c_", "b_").invoke(clientPlayer);
        Object server = field(minecraftClass, "field_71437_Z", "X").get(minecraft);
        if (server == null) {
            throw new IllegalStateException("The original client is not running an integrated server");
        }
        Object playerList = findMethod(server.getClass(), new Class<?>[0],
                "func_71203_ab", "ah").invoke(server);
        Object player = findMethod(playerList.getClass(), new Class<?>[]{String.class},
                "func_152612_a", "a").invoke(playerList, name);
        if (player == null) {
            throw new IllegalStateException("Integrated-server player not found: " + name);
        }
        return player;
    }

    private static void setProviderNightmare(Object player, int level)
            throws ReflectiveOperationException {
        Object world = field(player.getClass(), "field_70170_p", "o").get(player);
        Object provider = field(world.getClass(), "field_73011_w", "t").get(world);
        try {
            field(provider.getClass(), "nightmare").setInt(provider, level);
        } catch (NoSuchFieldException ignored) {
            // Entry/exit can briefly expose another dimension's provider.
        }
    }

    private static Method methodByNameAndCount(Class<?> type, String name, int parameterCount)
            throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (name.equals(method.getName()) && method.getParameterCount() == parameterCount) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException(type.getName() + "." + name + "/" + parameterCount);
    }

    private static Method methodByNameAndFirstArgument(Class<?> type, String name,
                                                        int parameterCount, Object argument)
            throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (name.equals(method.getName()) && parameters.length == parameterCount
                    && parameters[0].isInstance(argument)) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException(type.getName() + "." + name
                + "/" + parameterCount + " for " + argument.getClass().getName());
    }

    private static void poseEntity(Class<?> minecraftClass, Object minecraft, String argument) {
        String[] values = argument.split("\\s+");
        if (values.length != 6) {
            throw new IllegalArgumentException("POSE_ENTITY requires class x y z yaw pitch");
        }
        String className = values[0];
        double x = Double.parseDouble(values[1]);
        double y = Double.parseDouble(values[2]);
        double z = Double.parseDouble(values[3]);
        float yaw = Float.parseFloat(values[4]);
        float pitch = Float.parseFloat(values[5]);
        try {
            Object level = field(minecraftClass, "field_71441_e", "f").get(minecraft);
            Object loaded = field(level.getClass(), "field_72996_f", "e").get(level);
            Object nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            if (loaded instanceof Iterable<?>) {
                for (Object entity : (Iterable<?>) loaded) {
                    Class<?> type = entity.getClass();
                    if (!className.equals(type.getName()) && !className.equals(type.getSimpleName())) {
                        continue;
                    }
                    double dx = coordinate(entity, "field_70165_t", "s") - x;
                    double dy = coordinate(entity, "field_70163_u", "t") - y;
                    double dz = coordinate(entity, "field_70161_v", "u") - z;
                    double distance = dx * dx + dy * dy + dz * dz;
                    if (distance < nearestDistance) {
                        nearest = entity;
                        nearestDistance = distance;
                    }
                }
            }
            if (nearest == null) {
                throw new IllegalStateException("No loaded entity matched " + className);
            }

            Method place = findMethod(nearest.getClass(),
                    new Class<?>[]{double.class, double.class, double.class, float.class, float.class},
                    "func_70012_b", "b");
            place.invoke(nearest, x, y, z, yaw, pitch);
            setFloatIfPresent(nearest, yaw, "field_70177_z", "y");
            setFloatIfPresent(nearest, yaw, "field_70126_B", "A");
            setFloatIfPresent(nearest, pitch, "field_70125_A", "z");
            setFloatIfPresent(nearest, pitch, "field_70127_C", "B");
            setFloatIfPresent(nearest, yaw, "field_70761_aq", "aM");
            setFloatIfPresent(nearest, yaw, "field_70760_ar", "aN");
            setFloatIfPresent(nearest, yaw, "field_70759_as", "aO");
            setFloatIfPresent(nearest, yaw, "field_70758_at", "aP");
            setFloatIfPresent(nearest, 0.0F, "field_70754_ba", "aG");
            setFloatIfPresent(nearest, 0.0F, "field_70721_aZ", "aF");
            setFloatIfPresent(nearest, 0.0F, "field_70722_aY", "aE");
            setIntIfPresent(nearest, 0, "field_70173_aa", "aa");
            log("Posed " + className + " at (" + x + ", " + y + ", " + z + ") yaw=" + yaw);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not pose original entity " + className, exception);
        }
    }

    private static void clearEntities(Class<?> minecraftClass, Object minecraft, String className) {
        try {
            Object level = field(minecraftClass, "field_71441_e", "f").get(minecraft);
            Object loaded = field(level.getClass(), "field_72996_f", "e").get(level);
            int cleared = 0;
            if (loaded instanceof Iterable<?>) {
                List<Object> matching = new ArrayList<>();
                for (Object entity : (Iterable<?>) loaded) {
                    Class<?> type = entity.getClass();
                    if (className.equals(type.getName()) || className.equals(type.getSimpleName())) {
                        matching.add(entity);
                    }
                }
                for (Object entity : matching) {
                    Method setDead = findMethod(entity.getClass(), new Class<?>[0], "func_70106_y", "B");
                    setDead.invoke(entity);
                    cleared++;
                }
            }
            log("Cleared " + cleared + " loaded " + className + " entities");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not clear original entities " + className, exception);
        }
    }

    private static void setFloatIfPresent(Object target, float value, String... names)
            throws IllegalAccessException {
        try {
            field(target.getClass(), names).setFloat(target, value);
        } catch (NoSuchFieldException ignored) {
            // Base Entity instances do not expose every living-animation field.
        }
    }

    private static void setIntIfPresent(Object target, int value, String... names)
            throws IllegalAccessException {
        try {
            field(target.getClass(), names).setInt(target, value);
        } catch (NoSuchFieldException ignored) {
            // Base Entity mappings can differ between development and obfuscated runtimes.
        }
    }

    private static void reportScene(Class<?> minecraftClass, Object minecraft) {
        try {
            Object player = field(minecraftClass, "field_71439_g", "h").get(minecraft);
            Object level = field(minecraftClass, "field_71441_e", "f").get(minecraft);
            logEntity("player", player);
            Object loaded = field(level.getClass(), "field_72996_f", "e").get(level);
            if (loaded instanceof Iterable<?>) {
                for (Object entity : (Iterable<?>) loaded) {
                    if (entity == player) {
                        continue;
                    }
                    double dx = coordinate(entity, "field_70165_t", "s")
                            - coordinate(player, "field_70165_t", "s");
                    double dy = coordinate(entity, "field_70163_u", "t")
                            - coordinate(player, "field_70163_u", "t");
                    double dz = coordinate(entity, "field_70161_v", "u")
                            - coordinate(player, "field_70161_v", "u");
                    if (dx * dx + dy * dy + dz * dz <= 400.0D) {
                        logEntity("nearby", entity);
                    }
                }
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not report original capture scene", exception);
        }
    }

    private static void logEntity(String label, Object entity) throws ReflectiveOperationException {
        String livingLook = optionalFloat(entity, "field_70761_aq", "aM") == null ? ""
                : " body=(" + optionalFloat(entity, "field_70761_aq", "aM")
                + ", " + optionalFloat(entity, "field_70760_ar", "aN") + ")"
                + " head=(" + optionalFloat(entity, "field_70759_as", "aO")
                + ", " + optionalFloat(entity, "field_70758_at", "aP") + ")";
        log(label + " " + entity.getClass().getName()
                + " pos=(" + coordinate(entity, "field_70165_t", "s")
                + ", " + coordinate(entity, "field_70163_u", "t")
                + ", " + coordinate(entity, "field_70161_v", "u") + ")"
                + " look=(" + field(entity.getClass(), "field_70177_z", "y").getFloat(entity)
                + ", " + field(entity.getClass(), "field_70125_A", "z").getFloat(entity) + ")"
                + livingLook
                + " size=(" + field(entity.getClass(), "field_70130_N", "M").getFloat(entity)
                + ", " + field(entity.getClass(), "field_70131_O", "N").getFloat(entity) + ")");
    }

    private static Float optionalFloat(Object target, String... names) throws IllegalAccessException {
        try {
            return field(target.getClass(), names).getFloat(target);
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }

    private static double coordinate(Object entity, String srgName, String obfuscatedName)
            throws ReflectiveOperationException {
        return field(entity.getClass(), srgName, obfuscatedName).getDouble(entity);
    }

    private static void capture(Class<?> minecraftClass, Object minecraft, ClassLoader loader,
                                Path outputRoot, String fileName) {
        try {
            File gameDirectory = (File) field(minecraftClass, "field_71412_D", "w").get(minecraft);
            int width = field(minecraftClass, "field_71443_c", "d").getInt(minecraft);
            int height = field(minecraftClass, "field_71440_d", "e").getInt(minecraft);
            Object frameBuffer = field(minecraftClass, "field_147124_at", "au").get(minecraft);
            Class<?> frameBufferClass = loadClass(loader, "net.minecraft.client.shader.Framebuffer", "bmg");
            Class<?> screenshotHelper = loadClass(loader, "net.minecraft.util.ScreenShotHelper", "bbp");
            Method save = findMethod(screenshotHelper,
                    new Class<?>[]{File.class, int.class, int.class, frameBufferClass},
                    "func_148260_a", "a");

            Path screenshotDirectory = gameDirectory.toPath().resolve("screenshots");
            Files.createDirectories(screenshotDirectory);
            long startedAt = System.currentTimeMillis() - 1_000L;
            save.invoke(null, gameDirectory, width, height, frameBuffer);
            Path generated = waitForScreenshot(screenshotDirectory, startedAt);
            Path destination = outputRoot.resolve(fileName);
            Files.createDirectories(destination.getParent());
            Files.move(generated, destination, StandardCopyOption.REPLACE_EXISTING);
            log("Captured " + destination);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not capture original framebuffer", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not store original framebuffer", exception);
        }
    }

    private static Path waitForScreenshot(Path directory, long startedAt) throws Exception {
        long deadline = System.currentTimeMillis() + 10_000L;
        while (System.currentTimeMillis() < deadline) {
            Path newest = null;
            long newestTime = Long.MIN_VALUE;
            try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.png")) {
                for (Path file : files) {
                    long modified = Files.getLastModifiedTime(file).toMillis();
                    if (modified >= startedAt && modified > newestTime) {
                        newest = file;
                        newestTime = modified;
                    }
                }
            }
            if (newest != null) {
                return newest;
            }
            Thread.sleep(50L);
        }
        throw new IllegalStateException("Minecraft did not write a screenshot");
    }

    private static void shutdown(Class<?> minecraftClass, Object minecraft) {
        try {
            Method shutdown = findMethod(minecraftClass, new Class<?>[0], "func_71400_g", "k");
            shutdown.invoke(minecraft);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not stop original client", exception);
        }
    }

    private static void invokeOnClient(Method schedule, Object minecraft, Runnable action) throws Exception {
        Object result = schedule.invoke(minecraft, action);
        if (!(result instanceof Future<?>)) {
            throw new IllegalStateException("Minecraft scheduler did not return a Future");
        }
        ((Future<?>) result).get(ACTION_TIMEOUT_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private static void invokeOnServer(Class<?> minecraftClass, Object minecraft, Runnable action)
            throws Exception {
        Object server = field(minecraftClass, "field_71437_Z", "X").get(minecraft);
        if (server == null) {
            throw new IllegalStateException("The original client is not running an integrated server");
        }
        installServerTickBridge(server);
        FutureTask<Void> task = new FutureTask<>(() -> {
            action.run();
            return null;
        });
        SERVER_ACTIONS.add(task);
        task.get(ACTION_TIMEOUT_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    @SuppressWarnings("unchecked")
    private static synchronized void installServerTickBridge(Object server) throws Exception {
        if (serverBridgeInstalled) {
            return;
        }
        ClassLoader loader = server.getClass().getClassLoader();
        Class<?> tickable = loadClass(loader,
                "net.minecraft.server.gui.IUpdatePlayerListBox", "lr");
        Object bridge = Proxy.newProxyInstance(loader, new Class<?>[]{tickable},
                (proxy, method, arguments) -> {
                    if (method.getParameterCount() == 0 && method.getReturnType() == Void.TYPE) {
                        FutureTask<Void> task;
                        while ((task = SERVER_ACTIONS.poll()) != null) {
                            task.run();
                        }
                        return null;
                    }
                    if ("toString".equals(method.getName())) {
                        return "WitcheryOriginalCaptureServerBridge";
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return arguments != null && arguments.length == 1 && proxy == arguments[0];
                    }
                    return null;
                });
        Object tickables = field(server.getClass(), "field_71322_p", "n").get(server);
        if (!(tickables instanceof List<?>)) {
            throw new IllegalStateException("Minecraft server tickable registry is not a List");
        }
        ((List<Object>) tickables).add(bridge);
        serverBridgeInstalled = true;
        log("Installed integrated-server tick bridge");
    }

    private static Field field(Class<?> type, String... names) throws NoSuchFieldException {
        for (String name : names) {
            Class<?> current = type;
            while (current != null) {
                try {
                    Field field = current.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ignored) {
                    current = current.getSuperclass();
                }
            }
        }
        throw new NoSuchFieldException(type.getName() + " fields " + String.join(", ", names));
    }

    private static Method findMethod(Class<?> type, Class<?>[] parameters, String... names)
            throws NoSuchMethodException {
        for (String name : names) {
            Class<?> current = type;
            while (current != null) {
                try {
                    Method method = current.getDeclaredMethod(name, parameters);
                    method.setAccessible(true);
                    return method;
                } catch (NoSuchMethodException ignored) {
                    current = current.getSuperclass();
                }
            }
        }
        throw new NoSuchMethodException(type.getName() + " methods " + String.join(", ", names));
    }

    private static Class<?> loadClass(ClassLoader loader, String... names) throws ClassNotFoundException {
        for (String name : names) {
            try {
                return Class.forName(name, true, loader);
            } catch (ClassNotFoundException ignored) {
                // Try the other runtime namespace.
            }
        }
        throw new ClassNotFoundException(String.join(", ", names));
    }

    private static synchronized void log(String message) {
        System.out.println("[Witchery original capture] " + message);
        if (runLog == null) {
            return;
        }
        try {
            Files.write(runLog,
                    ("[Witchery original capture] " + message + System.lineSeparator())
                            .getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception exception) {
            System.err.println("[Witchery original capture] Could not write run log: " + exception);
        }
    }
}
