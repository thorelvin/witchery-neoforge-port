import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public final class OriginalWitcheryCaptureAgent {
    private static final long ACTION_TIMEOUT_MILLIS = 120_000L;

    private OriginalWitcheryCaptureAgent() {
    }

    public static void premain(String planPath, Instrumentation instrumentation) {
        start(planPath, instrumentation);
    }

    public static void agentmain(String planPath, Instrumentation instrumentation) {
        start(planPath, instrumentation);
    }

    private static void start(String planPath, Instrumentation instrumentation) {
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

    private static void log(String message) {
        System.out.println("[Witchery original capture] " + message);
    }
}
