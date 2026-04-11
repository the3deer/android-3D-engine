package org.the3deer.android.engine.services;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for 3D model loaders.
 * This allows the engine to be decoupled from specific model formats.
 *
 * @author andresoviedo
 */
public final class LoaderRegistry {

    /**
     * Interface for loader factories.
     */
    public interface LoaderFactory {
        LoaderTask create(URI uri, LoadListener listener);
    }

    /**
     * Map of registered loaders by extension or type.
     */
    private static final Map<String, LoaderFactory> loaders = new HashMap<>();

    /**
     * Register a loader factory for a specific extension.
     *
     * @param extension the file extension (e.g., "obj", "gltf")
     * @param factory   the factory to create the loader task
     */
    public static void register(final String extension, final LoaderFactory factory) {
        loaders.put(extension.toLowerCase(), factory);
    }

    /**
     * Get a loader task for the specified type and URI.
     *
     * @param type     the model type or extension
     * @param uri      the URI to the model
     * @param listener the load listener
     * @return the loader task, or null if no loader is registered for the type
     */
    public static LoaderTask get(final String type, final URI uri, final LoadListener listener) {
        final LoaderFactory factory = loaders.get(type.toLowerCase());
        if (factory == null) {
            return null;
        }
        return factory.create(uri, listener);
    }

    private LoaderRegistry() {
        // static utility class
    }
}
