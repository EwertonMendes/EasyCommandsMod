package br.tblack.plugin.config;

import com.google.gson.Gson;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.function.Supplier;

public final class JsonConfigStore<T> {

    private final Gson gson;
    private final Path directoryPath;
    private final Path filePath;
    private final Type dataType;
    private final Supplier<T> defaultValueSupplier;

    public JsonConfigStore(Gson gson, String directory, String fileName, Type dataType, Supplier<T> defaultValueSupplier) {
        if (gson == null || directory == null || fileName == null || dataType == null || defaultValueSupplier == null) {
            throw new IllegalArgumentException("JsonConfigStore parameters must not be null");
        }

        this.gson = gson;
        this.directoryPath = Path.of(directory);
        this.filePath = this.directoryPath.resolve(fileName);
        this.dataType = dataType;
        this.defaultValueSupplier = defaultValueSupplier;
    }

    public Path getFilePath() {
        return filePath;
    }

    public T loadOrCreate() {
        ensureDirectoryExists();

        if (!Files.exists(filePath)) {
            T defaultValue = defaultValueSupplier.get();
            save(defaultValue);
            return defaultValue;
        }

        try (Reader reader = Files.newBufferedReader(filePath)) {
            T loaded = gson.fromJson(reader, dataType);
            if (loaded != null) return loaded;
        } catch (Exception ignored) {
        }

        T fallbackValue = defaultValueSupplier.get();
        save(fallbackValue);
        return fallbackValue;
    }

    public void save(T value) {
        ensureDirectoryExists();

        String tempFileName = filePath.getFileName().toString() + ".tmp";
        Path tempFilePath = directoryPath.resolve(tempFileName);

        try (Writer writer = Files.newBufferedWriter(
                tempFilePath,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        )) {
            gson.toJson(value, dataType, writer);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        try {
            Files.move(tempFilePath, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            try {
                Files.move(tempFilePath, filePath, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception moveFallbackException) {
                moveFallbackException.printStackTrace();
            }
        }
    }

    private void ensureDirectoryExists() {
        try {
            Files.createDirectories(directoryPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
