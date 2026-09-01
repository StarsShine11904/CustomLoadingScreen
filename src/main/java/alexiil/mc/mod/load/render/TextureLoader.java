package alexiil.mc.mod.load.render;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.KHRDebug;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.AbstractResourcePack;
import net.minecraft.client.resource.DirectoryResourcePack;
import net.minecraft.client.resource.FallbackResourceManager;
import net.minecraft.client.resource.LegacyResourcePackAdapter;
import net.minecraft.client.resource.ReloadableResourceManagerImpl;
import net.minecraft.client.resource.Resource;
import net.minecraft.client.resource.ResourceManager;
import net.minecraft.client.resource.ResourcePack;
import net.minecraft.client.resource.ZipResourcePack;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

import alexiil.mc.mod.load.CLSLog;
import alexiil.mc.mod.load.CustomLoadingScreen;
import alexiil.mc.mod.load.json.ResourceWrappingInputStream;

public final class TextureLoader {

    private static final Field FIELD_RES_MANAGER_MAP;
    private static final Field FIELD_FALLBACK_LIST;
    private static final Field FIELD_ABS_PACK_FILE;
    private static final Field FIELD_LEGACY_ADAPTOR_PACK;
    private static final Method METHOD_FILE_PACK_GETTER;

    static {
        Class<ZipResourcePack> filePack = ZipResourcePack.class;
        Method filePackGetter = null;

        FIELD_RES_MANAGER_MAP = getField(ReloadableResourceManagerImpl.class, Map.class);
        FIELD_FALLBACK_LIST = getField(FallbackResourceManager.class, List.class);
        FIELD_ABS_PACK_FILE = getField(AbstractResourcePack.class, File.class);
        FIELD_LEGACY_ADAPTOR_PACK = getField(LegacyResourcePackAdapter.class, ResourcePack.class);

        for (Method m : filePack.getDeclaredMethods()) {
            if ((m.getModifiers() & Modifier.STATIC) != 0) {
                continue;
            }
            if (!m.getReturnType().equals(ZipFile.class)) {
                continue;
            }
            if (m.getParameterCount() != 0) {
                continue;
            }
            filePackGetter = m;
            break;
        }

        if (filePackGetter != null) {
            filePackGetter.setAccessible(true);
        }
        METHOD_FILE_PACK_GETTER = filePackGetter;
    }

    private static Field getField(Class<?> in, Class<?> fldType) {
        for (Field f : in.getDeclaredFields()) {
            if ((f.getModifiers() & Modifier.STATIC) != 0) {
                continue;
            }
            if (fldType.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                return f;
            }
        }
        return null;
    }

    public static InputStream openResourceStream(Identifier location) throws IOException {
        if (CustomLoadingScreen.debugResourceLoading) {
            CLSLog.info("[debug] Opening resource " + location);
        }

        if ("config".equals(location.getNamespace())) {
            File fle = new File("config/customloadingscreen/" + location.getPath());
            if (fle.exists()) {
                if (CustomLoadingScreen.debugResourceLoading) {
                    CLSLog.info("[debug]   - Found resource file at " + fle);
                }
                try {
                    return new FileInputStream(fle);
                } catch (FileNotFoundException fnfe) {
                    if (CustomLoadingScreen.debugResourceLoading) {
                        CLSLog.warn("[debug]   x Missing file!!", fnfe);
                    }
                }
            } else {
                if (CustomLoadingScreen.debugResourceLoading) {
                    CLSLog.info("[debug]   x Missing file at " + fle + ", falling back to resources.");
                }
            }
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        ResourceManager resManager = mc != null ? mc.getResourceManager() : null;

        if (resManager != null) {
            try {
                Resource res = resManager.getResource(location);
                if (res != null) {
                    if (CustomLoadingScreen.debugResourceLoading) {
                        CLSLog.info("[debug]   - Found resource: " + res);
                    }
                    return new ResourceWrappingInputStream(res);
                }
            } catch (IOException e) {
                if (CustomLoadingScreen.debugResourceLoading) {
                    CLSLog.warn("[debug]   x Failed to find resource, falling back to manual iteration....", e);
                }
            }

            if (resManager instanceof ReloadableResourceManagerImpl && FIELD_RES_MANAGER_MAP != null && FIELD_FALLBACK_LIST != null) {
                ReloadableResourceManagerImpl srm = (ReloadableResourceManagerImpl) resManager;
                Map<?, ?> map;
                try {
                    map = (Map<?, ?>) FIELD_RES_MANAGER_MAP.get(srm);
                } catch (Exception e) {
                    return null;
                }

                if (map != null) {
                    Object value = map.get(location.getNamespace());
                    if (value instanceof FallbackResourceManager) {
                        FallbackResourceManager fallback = (FallbackResourceManager) value;
                        List<?> list = null;
                        try {
                            list = (List<?>) FIELD_FALLBACK_LIST.get(fallback);
                        } catch (Exception ignored) {}

                        if (list != null) {
                            for (Object o : list) {
                                ResourcePack pack = (ResourcePack) o;

                                if (pack instanceof LegacyResourcePackAdapter && FIELD_LEGACY_ADAPTOR_PACK != null) {
                                    try {
                                        pack = (ResourcePack) FIELD_LEGACY_ADAPTOR_PACK.get(pack);
                                    } catch (Exception ignored) {}
                                }

                                if (pack instanceof AbstractResourcePack && FIELD_ABS_PACK_FILE != null) {
                                    File file = null;
                                    try {
                                        file = (File) FIELD_ABS_PACK_FILE.get(pack);
                                    } catch (Exception ignored) {}

                                    String realPath = "assets/" + location.getNamespace() + "/" + location.getPath();

                                    if (pack instanceof ZipResourcePack && METHOD_FILE_PACK_GETTER != null) {
                                        ZipFile zip;
                                        try {
                                            zip = (ZipFile) METHOD_FILE_PACK_GETTER.invoke(pack);
                                        } catch (Exception e) {
                                            continue;
                                        }

                                        if (zip != null) {
                                            ZipEntry entry = zip.getEntry(realPath);
                                            if (entry != null) {
                                                return zip.getInputStream(entry);
                                            }
                                        }
                                    } else if (pack instanceof DirectoryResourcePack && file != null) {
                                        File target = new File(file, realPath);
                                        if (target.isFile()) {
                                            return new FileInputStream(target);
                                        }
                                    }
                                }

                                try {
                                    return pack.open(location);
                                } catch (IOException ignored) {}
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    public static void bindTexture(TextureManager manager, Identifier location) {
        AbstractTexture current = manager.getTexture(location);

        if (current != null) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, current.getGlId());
            return;
        }

        ResourceTexture texture = new ClsTexture(location);

        if (GLContext.getCapabilities().GL_KHR_debug) {
            KHRDebug.glPushDebugGroup(KHRDebug.GL_DEBUG_SOURCE_APPLICATION, 10, "CLS_LoadCustomTexture");
        }
        manager.registerTexture(location, texture);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getGlId());

        if (GLContext.getCapabilities().GL_KHR_debug) {
            KHRDebug.glPopDebugGroup();
        }
    }

    public static PreScannedImageData preScan(Identifier res) {
        try {
            ClsTexture clsTexture = new ClsTexture(res);
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.getResourceManager() != null) {
                clsTexture.load(mc.getResourceManager());
            }
            return new PreScannedImageData(clsTexture);
        } catch (IOException io) {
            CLSLog.warn("Failed to pre-load the texture " + res, io);
            return null;
        }
    }

    public static class PreScannedImageData {
        public final ClsTexture texture;

        public PreScannedImageData(ClsTexture texture) {
            this.texture = texture;
        }

        public void bind(TextureManager manager) {
            AbstractTexture current = manager.getTexture(texture.location());
            if (current != null) {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, current.getGlId());
            } else {
                manager.registerTexture(texture.location(), texture);
            }
        }
    }
}
