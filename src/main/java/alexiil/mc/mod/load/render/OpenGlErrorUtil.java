package alexiil.mc.mod.load.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.KHRDebug;
import org.lwjgl.opengl.KHRDebugCallback;

/**
 * Enable by adding the system property "-Dcustom_loading_screen.opengl_error_checking=true"
 */
public class OpenGlErrorUtil {

    private static final String FLAG = "custom_loading_screen.opengl_error_checking";

    /** Severity levels. */
    private static final int GL_DEBUG_SEVERITY_HIGH = 0x9146, GL_DEBUG_SEVERITY_MEDIUM = 0x9147,
        GL_DEBUG_SEVERITY_LOW = 0x9148, GL_DEBUG_SEVERITY_NOTIFICATION = 0x826B;

    /** Sources. */
    private static final int GL_DEBUG_SOURCE_API = 0x8246, GL_DEBUG_SOURCE_WINDOW_SYSTEM = 0x8247,
        GL_DEBUG_SOURCE_SHADER_COMPILER = 0x8248, GL_DEBUG_SOURCE_THIRD_PARTY = 0x8249,
        GL_DEBUG_SOURCE_APPLICATION = 0x824A, GL_DEBUG_SOURCE_OTHER = 0x824B;

    /** Types. */
    private static final int GL_DEBUG_TYPE_ERROR = 0x824C, GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR = 0x824D,
        GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR = 0x824E, GL_DEBUG_TYPE_PORTABILITY = 0x824F,
        GL_DEBUG_TYPE_PERFORMANCE = 0x8250, GL_DEBUG_TYPE_OTHER = 0x8251, GL_DEBUG_TYPE_MARKER = 0x8268;

    private static void onMessage(int source, int type, int id, int severity, String message) {

        if (id == 131185) {
            // NVIDIA buffer creation
            return;
        }

        String description;
        switch (source) {
            case GL_DEBUG_SOURCE_API:
                description = "API";
                break;
            case GL_DEBUG_SOURCE_WINDOW_SYSTEM:
                description = "WINDOW SYSTEM";
                break;
            case GL_DEBUG_SOURCE_SHADER_COMPILER:
                description = "SHADER COMPILER";
                break;
            case GL_DEBUG_SOURCE_THIRD_PARTY:
                description = "THIRD PARTY";
                break;
            case GL_DEBUG_SOURCE_APPLICATION:
                description = "APPLICATION";
                // We don't care about application generated messages - CLS has a lot of them, for example
                return;
            case GL_DEBUG_SOURCE_OTHER:
                description = "OTHER";
                break;
            default:
                description = "UNKNOWN 0x" + Integer.toString(source);
        }
        String er0 = description;

        switch (type) {
            case GL_DEBUG_TYPE_ERROR:
                description = "ERROR";
                break;
            case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR:
                description = "DEPRECATED BEHAVIOR";
                break;
            case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR:
                description = "UNDEFINED BEHAVIOR";
                break;
            case GL_DEBUG_TYPE_PORTABILITY:
                description = "PORTABILITY";
                break;
            case GL_DEBUG_TYPE_PERFORMANCE:
                description = "PERFORMANCE";
                break;
            case GL_DEBUG_TYPE_OTHER:
                description = "OTHER";
                break;
            case GL_DEBUG_TYPE_MARKER:
                description = "MARKER";
                break;
            default:
                description = "UNKNOWN 0x" + Integer.toString(type);
        }
        String er1 = description;

        switch (severity) {
            case GL_DEBUG_SEVERITY_HIGH:
                description = "HIGH";
                break;
            case GL_DEBUG_SEVERITY_MEDIUM:
                description = "MEDIUM";
                break;
            case GL_DEBUG_SEVERITY_LOW:
                description = "LOW";
                break;
            case GL_DEBUG_SEVERITY_NOTIFICATION:
                description = "NOTIFICATION";
                break;
            default:
                description = "UNKNOWN 0x" + Integer.toString(severity);
        }
        String er2 = description;

        String msg = er0 + " " + er1 + " " + er2 + " " + message;
        if (severity == KHRDebug.GL_DEBUG_SEVERITY_HIGH) {
            new Throwable("OpenGL Error: " + msg).printStackTrace();
        } else {
            System.out.println(id + ":" + msg);
        }
    }

    public static void setupIfFlag() {
        if (Boolean.getBoolean(FLAG)) {
            setup();
        }
    }

    private static void setup() {
        int flags = GL11.glGetInteger(GL30.GL_CONTEXT_FLAGS);
        System.out.println("debugging context = " + ((flags & GL43.GL_CONTEXT_FLAG_DEBUG_BIT) != 0));
        GL11.glEnable(GL43.GL_DEBUG_OUTPUT);
        GL11.glEnable(GL43.GL_DEBUG_OUTPUT_SYNCHRONOUS);
        GL43.glDebugMessageControl(GL11.GL_DONT_CARE, GL11.GL_DONT_CARE, GL11.GL_DONT_CARE, null, true);
        GL43.glDebugMessageCallback(new KHRDebugCallback(new KHRDebugCallback.Handler() {
            @Override
            public void handleMessage(int source, int type, int id, int severity, String message) {
                onMessage(source, type, id, severity, message);
            }
        }));
    }
}
