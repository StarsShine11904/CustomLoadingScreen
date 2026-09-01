package alexiil.mc.mod.load.json;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import alexiil.mc.mod.load.CLSLog;
import alexiil.mc.mod.load.ClsManager;
import alexiil.mc.mod.load.json.JsonVariable.JsonConstant;
import alexiil.mc.mod.load.json.serial.ConfigDeserialiser;
import alexiil.mc.mod.load.json.serial.FactoryDeserialiser;
import alexiil.mc.mod.load.json.serial.ImageDeserialiser;
import alexiil.mc.mod.load.json.serial.InstructionDeserialiser;
import alexiil.mc.mod.load.json.serial.RenderingPartDeserialiser;
import alexiil.mc.mod.load.json.serial.VariableArrayDeserialiser;

import buildcraft.lib.expression.api.InvalidExpressionException;

public class ConfigManager {
    public enum EType {
        FACTORY(JsonFactory.class, "factory"),
        ACTION(JsonAction.class, "action"),
        RENDERING_PART(JsonRenderingPart.class, "imagemeta"),
        IMAGE(JsonRender.class, "image"),
        INSTRUCTION(JsonInsn.class, "instruction"),
        CONFIG(JsonConfig.class, "config");

        public final Class<? extends JsonConfigurable<?, ?>> clazz;
        public final String resourceBase;

        public static EType valueOf(Class<? extends JsonConfigurable<?, ?>> configurable) {
            return types.get(configurable);
        }

        <T extends JsonConfigurable<T, ?>> EType(Class<T> clazz, String resourceBase) {
            this.clazz = clazz;
            this.resourceBase = resourceBase;
            types.put(clazz, this);
        }

        public boolean hasDefault() {
            return this == EType.RENDERING_PART;
        }

        public JsonConfigurable<?, ?> getNotFound(String location) throws InvalidExpressionException {
            if (this == EType.RENDERING_PART) {
                JsonRender ji = getAsImage(location);
                if (ji != null) {
                    JsonRenderingPart jrp = new JsonRenderingPart(ji, new JsonInsn[0], "true");
                    jrp.setSource(
                        ("{#-'image':'" + location + "'#}").replace('\'', '"').replace('#', '\n').replace('-', '\t')
                    );
                    jrp.setLocation(ji.resourceLocation);
                    return jrp;
                }
            }
            return null;
        }
    }

    public static final Gson GSON_ADAPTORS;
    public static final Gson GSON_DEFAULT;

    private static final Map<Class<? extends JsonConfigurable<?, ?>>, EType> types = Maps.newHashMap();
    private static final Map<Identifier, String> cache = Maps.newHashMap(), failedCache = Maps.newHashMap();

    static {
        GSON_ADAPTORS = new GsonBuilder()
            .registerTypeAdapter(JsonConfig.class, ConfigDeserialiser.INSTANCE)
            .registerTypeAdapter(JsonRenderingPart.class, RenderingPartDeserialiser.INSTANCE)
            .registerTypeAdapter(JsonRender.class, ImageDeserialiser.INSTANCE)
            .registerTypeAdapter(JsonInsn.class, InstructionDeserialiser.INSTANCE)
            .registerTypeAdapter(JsonVariable[].class, VariableArrayDeserialiser.VARIABLES)
            .registerTypeAdapter(JsonConstant[].class, VariableArrayDeserialiser.CONSTANTS)
            .registerTypeAdapter(JsonFactory.class, FactoryDeserialiser.INSTANCE)
            .registerTypeAdapter(Area.class, Area.DESERIALISER)
            .create();
        GSON_DEFAULT = new GsonBuilder().setPrettyPrinting().create();
    }

    private static String getFirst(Identifier identifier, boolean firstAttempt, boolean hasDefaut) {
        if (identifier == null) {
            throw new NullPointerException("Identifier provided shouldn't have been null!");
        }
        if ("config".equals(identifier.getNamespace())) {
            File file = new File("config/customloadingscreen", identifier.getPath());
            try (FileInputStream fis = new FileInputStream(file)) {
                return IOUtils.toString(fis, StandardCharsets.UTF_8);
            } catch (IOException e) {
                if (firstAttempt) {
                    String real = file.toString();
                    try {
                        real = file.getCanonicalPath();
                    } catch (IOException ignored) {}
                    if (!hasDefaut) {
                        CLSLog.warn("Tried to get the resource but failed! (" + real + ") because " + e.getClass());
                    }
                }
                return null;
            }
        }
        try (Resource res = ClsManager.getResource(identifier)) {
            try (InputStream stream = res.asStream()) {
                return IOUtils.toString(stream, StandardCharsets.UTF_8);
            } catch (IOException e) {
                CLSLog.warn("Tried to access \"" + identifier + "\", but an IO exception occurred!", e);
                return null;
            }
        } catch (IOException e) {
            if (firstAttempt && !hasDefaut) {
                CLSLog.warn("Tried to get the resource but failed! (" + identifier + ") because " + e.getClass());
            }
            return null;
        }
    }

    public static InputStream getInputStream(Identifier identifier) throws FileNotFoundException {
        if (identifier == null) {
            throw new NullPointerException("Identifier provided shouldn't have been null!");
        }
        if ("config".equals(identifier.getNamespace())) {
            File file = new File("config/customloadingscreen", identifier.getPath());
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException fnfe) {
                throw fnfe;
            } catch (IOException io) {
                FileNotFoundException fnfe = new FileNotFoundException();
                fnfe.initCause(io);
                throw fnfe;
            }
        }

        try {
            Resource res = ClsManager.getResource(identifier);
            return new ResourceWrappingInputStream(res);
        } catch (FileNotFoundException fnfe) {
            throw fnfe;
        } catch (IOException io) {
            FileNotFoundException fnfe = new FileNotFoundException();
            fnfe.initCause(io);
            throw fnfe;
        }
    }

    private static String getTextResource(Identifier identifier, boolean hasDefaut) {
        if (identifier == null) throw new NullPointerException("Identifier provided shouldn't have been null!");
        if (cache.containsKey(identifier)) {
            return cache.get(identifier);
        }
        if (failedCache.containsKey(identifier)) {
            String attempt = getFirst(identifier, false, hasDefaut);
            if (attempt != null) {
                failedCache.remove(identifier);
                cache.put(identifier, attempt);
            }
            return attempt;
        }
        String actual = getFirst(identifier, true, hasDefaut);
        if (actual == null) failedCache.put(identifier, null);
        else cache.put(identifier, actual);
        return actual;
    }

    @SuppressWarnings("unchecked")
    static <T extends JsonConfigurable<T, ?>> T getAsT(EType type, String location) throws InvalidExpressionException {
        if (StringUtils.isEmpty(location)) {
            CLSLog.warn("Location was given as null!", new Throwable());
            throw new JsonSyntaxException("Invalid location '" + location + "'");
        }
        CLSLog.info("Getting " + location + " as " + type);
        Identifier loc = getLocation(type, location);
        String text = getTextResource(loc, type.hasDefault());
        if (text == null) {
            JsonConfigurable<?, ?> failed = type.getNotFound(location);
            if (failed != null) {
                failed.setLocation(loc);
                return (T) failed;
            }
            CLSLog.warn("The text inside of \"" + loc + "\" was null!");
            throw new JsonSyntaxException("Invalid location '" + location + "': the text inside it was null!");
        }
        try {
            T t = (T) GSON_ADAPTORS.fromJson(text, type.clazz);
            t.setLocation(loc);
            t.setSource(text);
            return t;
        } catch (JsonSyntaxException t) {
            throw new InvalidSourceException("Failed to read from " + loc + "\n" + text, t);
        }
    }

    public static JsonRenderingPart getAsRenderingPart(String location) throws InvalidExpressionException {
        return getAsT(EType.RENDERING_PART, location);
    }

    public static JsonFactory getAsFactory(String location) throws InvalidExpressionException {
        return getAsT(EType.FACTORY, location);
    }

    public static JsonRender getAsImage(String location) throws InvalidExpressionException {
        return getAsT(EType.IMAGE, location);
    }

    public static JsonInsn getAsInsn(String location) throws InvalidExpressionException {
        return getAsT(EType.INSTRUCTION, location);
    }

    public static JsonAction getAsAction(String location) throws InvalidExpressionException {
        return getAsT(EType.ACTION, location);
    }

    public static JsonConfig getAsConfig(String location) throws InvalidExpressionException {
        return getAsT(EType.CONFIG, location);
    }

    public static Identifier getLocation(EType type, String base) {
        String namespace = "customloadingscreen";
        String path;
        int colon = base.indexOf(':');
        int slash = base.indexOf('/');

        if (colon > 0 && (colon < slash || slash < 0)) {
            namespace = base.substring(0, colon);
            base = base.substring(colon + 1);

            if ("config".equals(namespace) && type == EType.CONFIG) {
                path = base;
            } else {
                path = type.resourceBase + "/" + base;
            }
        } else {
            if (base.startsWith("builtin/")) {
                path = "builtin/" + type.resourceBase + base.substring(slash);
            } else if (base.startsWith("sample/")) {
                path = "sample/" + type.resourceBase + base.substring(slash);
            } else if (base.startsWith("config/")) {
                if (type == EType.CONFIG) {
                    path = base.substring(slash + 1);
                } else {
                    path = type.resourceBase + base.substring(slash);
                }
                namespace = "config";
            } else {
                path = type.resourceBase + "/" + base;
            }
        }

        return new Identifier(namespace, path + ".json");
    }
}
