package alexiil.mc.mod.load;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Random;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import alexiil.mc.mod.load.frame.FrameDisplayer;
import alexiil.mc.mod.load.render.MainSplashRenderer;

public class CustomLoadingScreen implements ClientModInitializer {
    public static final String MOD_ID = "customloadingscreen";

    public static boolean shouldWait = true;
    public static boolean useFrame = false;
    public static boolean useCustom = true;
    public static boolean darkMode = false;
    public static boolean debugResourceLoading = false;
    public static String customConfigPath = "sample/default";
    public static int fpsLimit = 75;
    public static int textureClearInterval = 10;

    private static FrameDisplayer displayer;
    public static CustomLoadingScreen instance;

    @Override
    public void onInitializeClient() {
        instance = this;
        CLSLog.info("Initializing Custom Loading Screen for Legacy Fabric...");
        loadConfig();
        MainSplashRenderer.onReachConstruct();
    }

    private static void loadConfig() {
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        File configFile = new File(configDir, "customloadingscreen.properties");
        Properties props = new Properties();

        if (configFile.exists()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                props.load(in);
            } catch (IOException e) {
                CLSLog.warn("Failed to load customloadingscreen.properties", e);
            }
        }

        useCustom = Boolean.parseBoolean(props.getProperty("use_custom", "true"));
        useFrame = Boolean.parseBoolean(props.getProperty("use_frame", "false"));
        shouldWait = Boolean.parseBoolean(props.getProperty("smooth_init", "true"));
        debugResourceLoading = Boolean.parseBoolean(props.getProperty("debug_resource_loading", "false"));

        try {
            fpsLimit = Math.max(2, Math.min(300, Integer.parseInt(props.getProperty("fps_limit", "75"))));
        } catch (NumberFormatException ignored) {
            fpsLimit = 75;
        }

        try {
            textureClearInterval = Math.max(0, Integer.parseInt(props.getProperty("texture_clear_interval", "10")));
        } catch (NumberFormatException ignored) {
            textureClearInterval = 10;
        }

        String customName = props.getProperty("screen_config", "builtin/random");
        if ("builtin/random".equals(customName)) {
            String[] possible = { "sample/default", "sample/white", "sample/scrolling", "sample_panorama_lower" };
            customConfigPath = possible[new Random().nextInt(possible.length)];
        } else {
            customConfigPath = customName == null ? "sample/default" : customName;
        }

        // 儲存預設/現有設定檔
        props.setProperty("use_custom", String.valueOf(useCustom));
        props.setProperty("use_frame", String.valueOf(useFrame));
        props.setProperty("smooth_init", String.valueOf(shouldWait));
        props.setProperty("fps_limit", String.valueOf(fpsLimit));
        props.setProperty("texture_clear_interval", String.valueOf(textureClearInterval));
        props.setProperty("screen_config", customName);
        props.setProperty("debug_resource_loading", String.valueOf(debugResourceLoading));

        try (FileOutputStream out = new FileOutputStream(configFile)) {
            props.store(out, "Custom Loading Screen Configuration");
        } catch (IOException e) {
            CLSLog.warn("Failed to save customloadingscreen.properties", e);
        }

        if (useFrame) {
            displayer = new FrameDisplayer();
            displayer.start();
        }

        // 初始化範例主題 JSON 目錄
        File clsRoot = new File(configDir, "customloadingscreen");
        if (!clsRoot.exists()) {
            clsRoot.mkdirs();
        }

        File clsExample = new File(clsRoot, "example.json");
        if (!clsExample.exists()) {
            try (OutputStream out = new FileOutputStream(clsExample);
                 BufferedOutputStream bos = new BufferedOutputStream(out);
                 BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(bos, StandardCharsets.UTF_8))) {
                writeExampleCfg(bw);
                bw.flush();
            } catch (IOException e) {
                CLSLog.warn("Failed to write the example config file!", e);
            }
        }
    }

    public static void finish() {
        if (displayer != null) {
            displayer.finish();
        }
    }

    private static void ln(BufferedWriter bw, String str) throws IOException {
        bw.write(str.replace('#', '"'));
        bw.newLine();
    }

    private static void writeExampleCfg(BufferedWriter bw) throws IOException {
        ln(bw, "{");
        ln(bw, "    #renders#: [");
        ln(bw, "        {");
        ln(bw, "            #image#: {");
        ln(bw, "                #parent#: #builtin/panorama#,");
        ln(bw, "                #image#: #textures/gui/title/background/panorama_x.png#");
        ln(bw, "            }");
        ln(bw, "        },");
        ln(bw, "        {");
        ln(bw, "            #image#: {");
        ln(bw, "                #parent#: #builtin/image#,");
        ln(bw, "                #image#: #customloadingscreen:textures/generic/darkened_blur_horizontal_strip.png#,");
        ln(bw, "                #position_type#: #CENTER#,");
        ln(bw, "                #offset_pos#: #CENTER#,");
        ln(bw, "                #position#: {");
        ln(bw, "                    #x#: #0#,");
        ln(bw, "                    #y#: #0#,");
        ln(bw, "                    #width#: #screen_width#,");
        ln(bw, "                    #height#: #100#");
        ln(bw, "                },");
        ln(bw, "                #texture#: {");
        ln(bw, "                    #x#: #0#,");
        ln(bw, "                    #y#: #0#,");
        ln(bw, "                    #width#: #1#,");
        ln(bw, "                    #height#: #1#");
        ln(bw, "                }");
        ln(bw, "            }");
        ln(bw, "        },");
        ln(bw, "        {");
        ln(bw, "            #image#: {");
        ln(bw, "                #parent#:#builtin/image#,");
        ln(bw, "                #image#: #customloadingscreen:textures/progress_bars.png#,");
        ln(bw, "                #position_type#: #CENTER#,");
        ln(bw, "                #offset_pos#: #CENTER#,");
        ln(bw, "                #position#:{");
        ln(bw, "                    #x#: #0#,");
        ln(bw, "                    #y#:#20#,");
        ln(bw, "                    #width#:#182 * 2#,");
        ln(bw, "                    #height#:#20#");
        ln(bw, "                },");
        ln(bw, "                #texture#:{");
        ln(bw, "                    #x#: #0#,");
        ln(bw, "                    #y#: #70 / 256.0#,");
        ln(bw, "                    #width#: #182 / 256.0#,");
        ln(bw, "                    #height#: #10 / 256.0#");
        ln(bw, "                }");
        ln(bw, "            }");
        ln(bw, "        },");
        ln(bw, "        {");
        ln(bw, "            #image#: {");
        ln(bw, "                #parent#: #builtin/image#,");
        ln(bw, "                #image#: #customloadingscreen:textures/progress_bars.png#,");
        ln(bw, "                #position_type#: #CENTER#,");
        ln(bw, "                #offset_pos#: #CENTER#,");
        ln(bw, "                #position#:{");
        ln(bw, "                    #x#:#percentage * 182 - 182#,");
        ln(bw, "                    #y#:#20#,");
        ln(bw, "                    #width#:#percentage * 182 * 2#,");
        ln(bw, "                    #height#:#20#");
        ln(bw, "                },");
        ln(bw, "                #texture#:{");
        ln(bw, "                    #x#:#0#,");
        ln(bw, "                    #y#:#80 / 256.0#,");
        ln(bw, "                    #width#: #percentage * 182 / 256.0#,");
        ln(bw, "                    #height#:#10 / 256.0#");
        ln(bw, "                }");
        ln(bw, "            }");
        ln(bw, "        },");
        ln(bw, "        {");
        ln(bw, "            #image#: {");
        ln(bw, "                #parent#: #builtin/text#,");
        ln(bw, "                #image#: #textures/font/ascii.png#,");
        ln(bw, "                #position_type#: #CENTER#,");
        ln(bw, "                #offset_pos#: #CENTER#,");
        ln(bw, "                #text#: #is_reloading ? status : (status  + ': ' + sub_status)#,");
        ln(bw, "                #position#: {");
        ln(bw, "                    #x#: #0#,");
        ln(bw, "                    #y#: #-20#,");
        ln(bw, "                    #width#: #0#,");
        ln(bw, "                    #height#: #0#");
        ln(bw, "                },");
        ln(bw, "                #colour#:#0xFF_FF_FF_FF#");
        ln(bw, "            }");
        ln(bw, "        },");
        ln(bw, "        {");
        ln(bw, "            #image#: {");
        ln(bw, "                #parent#: #builtin/text#,");
        ln(bw, "                #image#: #textures/font/ascii.png#,");
        ln(bw, "                #position_type#: #CENTER#,");
        ln(bw, "                #offset_pos#: #CENTER#,");
        ln(bw, "                #text#: #is_reloading ? sub_status : ''#,");
        ln(bw, "                #position#: {");
        ln(bw, "                    #x#: #0#,");
        ln(bw, "                    #y#: #0#,");
        ln(bw, "                    #width#: #0#,");
        ln(bw, "                    #height#: #0#");
        ln(bw, "                },");
        ln(bw, "                #colour#:#0xFF_FF_FF_FF#");
        ln(bw, "            }");
        ln(bw, "        },");
        ln(bw, "        {");
        ln(bw, "            #image#: {");
        ln(bw, "                #parent#: #builtin/text#,");
        ln(bw, "                #image#: #textures/font/ascii.png#,");
        ln(bw, "                #position_type#: #CENTER#,");
        ln(bw, "                #offset_pos#: #CENTER#,");
        ln(bw, "                #text#: #(floor(percentage * 100)) + '%'#,");
        ln(bw, "                #position#: {");
        ln(bw, "                    #x#: #0#,");
        ln(bw, "                    #y#: #-10#,");
        ln(bw, "                    #width#: #0#,");
        ln(bw, "                    #height#: #0#");
        ln(bw, "                },");
        ln(bw, "                #colour#:#0xFF_FF_FF_FF#");
        ln(bw, "            }");
        ln(bw, "        }");
        ln(bw, "    ],");
        ln(bw, "    #functions#:[");
        ln(bw, "    ],");
        ln(bw, "    #factories#:[");
        ln(bw, "    ],");
        ln(bw, "    #actions#:[");
        ln(bw, "    ],");
        ln(bw, "    #variables#:{");
        ln(bw, "    }");
        ln(bw, "}");
    }
}
