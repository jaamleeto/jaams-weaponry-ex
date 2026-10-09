package net.jaams.weaponry_ex.pack;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class GeneratedPack implements PackResources {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final int PACK_FORMAT = 15;

    private final String id;
    private final PackType type;
    private final Set<String> namespaces = new LinkedHashSet<>();
    private final Component description;
    private final Map<ResourceLocation, byte[]> resources = new HashMap<>();

    public GeneratedPack(String id, PackType type, String descriptionKey, String... namespaces) {
        this.id = id;
        this.type = type;
        for (String namespace : namespaces) {
            this.namespaces.add(namespace);
        }
        this.description = Component.translatable(descriptionKey);
    }

    public GeneratedPack allowNamespace(String namespace) {
        if (namespace != null && !namespace.isBlank()) {
            namespaces.add(namespace);
        }
        return this;
    }

    public GeneratedPack put(ResourceLocation location, String content) {
        return put(location, content.getBytes(StandardCharsets.UTF_8));
    }

    public GeneratedPack put(ResourceLocation location, byte[] content) {
        if (namespaces.contains(location.getNamespace())) {
            resources.put(location, content);
        } else {
            LOGGER.warn("GeneratedPack '{}': resource {} is not in any of its namespaces {}",
                    id, location, namespaces);
        }
        return this;
    }

    public int size() {
        return resources.size();
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... levels) {
        return null;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType packType, ResourceLocation location) {
        if (packType != type) {
            return null;
        }
        byte[] data = resources.get(location);
        if (data == null) {
            return null;
        }
        return () -> new ByteArrayInputStream(data);
    }

    @Override
    public void listResources(PackType packType, String namespace, String path,
            ResourceOutput output) {
        if (packType != type || !namespaces.contains(namespace)) {
            return;
        }

        String prefix = path.isEmpty() || path.endsWith("/") ? path : path + "/";
        for (Map.Entry<ResourceLocation, byte[]> entry : resources.entrySet()) {
            ResourceLocation loc = entry.getKey();
            if (loc.getNamespace().equals(namespace) && loc.getPath().startsWith(prefix)) {
                byte[] data = entry.getValue();
                output.accept(loc, () -> new ByteArrayInputStream(data));
            }
        }
    }

    @Override
    public Set<String> getNamespaces(PackType packType) {
        return packType == type ? namespaces : Set.of();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) throws IOException {
        if (serializer == PackMetadataSection.TYPE) {
            return (T) new PackMetadataSection(description, PACK_FORMAT);
        }
        return null;
    }

    @Override
    public String packId() {
        return id;
    }

    @Override
    public void close() {
    }
}
