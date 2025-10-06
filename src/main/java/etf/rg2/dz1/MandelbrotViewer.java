package etf.rg2.dz1;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.*;


import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL45.*;
import static org.lwjgl.system.MemoryUtil.*;


public class MandelbrotViewer {
    private int windowWidth  = 800;
    private int windowHeight = 600;


    private long window;

    private int program;

    private int uniCenter, uniScale, uniMaxIter, uniViewport;

    private double centerX = 0.0, centerY = 0.0;
    private double scale = 2.0;
    private int maxIter = 100;

    private boolean dragging = false;
    private double lastMouseX, lastMouseY;

    public static void main(String[] args) {
        new MandelbrotViewer().run();
    }

    public void run() {
        initGLFW();
        initOpenGL();
        loop();
        cleanup();
    }

    private void initGLFW() {
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 5);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        window = glfwCreateWindow(windowWidth, windowHeight, "Mandelbrot Explorer", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create GLFW window");

        // Make context current
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);

        // load all GL function pointers
        GL.createCapabilities();

        glViewport(0, 0, windowWidth, windowHeight);
        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            windowWidth  = w;
            windowHeight = h;
            glViewport(0, 0, w, h);
        });

        // Input callbacks
        glfwSetScrollCallback(window, this::scrollCallback);
        glfwSetMouseButtonCallback(window, this::mouseButtonCallback);
        glfwSetCursorPosCallback(window, this::cursorPosCallback);
        glfwSetKeyCallback(window, this::keyCallback);
    }

    private void initOpenGL() {

        // Compile shaders and link program
        int vs = compileShader(GL_VERTEX_SHADER,   vertexShaderSource);
        int fs = compileShader(GL_FRAGMENT_SHADER, fragmentShaderSource);
        program = glCreateProgram();
        glAttachShader(program, vs);
        glAttachShader(program, fs);
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == 0)
            throw new RuntimeException("Error linking program: " + glGetProgramInfoLog(program));

        glDeleteShader(vs);
        glDeleteShader(fs);


        uniCenter   = glGetUniformLocation(program, "u_center");
        uniScale    = glGetUniformLocation(program, "u_scale");
        uniMaxIter  = glGetUniformLocation(program, "u_maxIter");
        uniViewport = glGetUniformLocation(program, "u_viewport");

        float[] quad = { -1, -1, 1, -1, 1, 1, -1, 1 };
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, quad, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0L);
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT);

            glUseProgram(program);

            // 1) center
            glUniform2d(uniCenter, centerX, centerY);
            // 2) scale
            glUniform1d(uniScale, scale);
            // 3) max interations
            glUniform1i(uniMaxIter, maxIter);
            // 4) viewport dimensions
            glUniform2f(uniViewport, windowWidth, windowHeight);

            glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void cleanup() {
        glDeleteProgram(program);
        glfwDestroyWindow(window);
        glfwTerminate();
    }


    private boolean fullscreen = false;
    private int   prevX, prevY, prevW, prevH;
    private void keyCallback(long win, int key, int scancode, int action, int mods) {
        if (action != GLFW_PRESS) return;
        if (key == GLFW_KEY_F11) {
            fullscreen = !fullscreen;
            if (fullscreen) {
                // record previous window position and size
                IntBuffer bx = BufferUtils.createIntBuffer(1);
                IntBuffer by = BufferUtils.createIntBuffer(1);
                IntBuffer bw = BufferUtils.createIntBuffer(1);
                IntBuffer bh = BufferUtils.createIntBuffer(1);
                glfwGetWindowPos(win, bx, by);
                glfwGetWindowSize(win, bw, bh);
                prevX = bx.get(0); prevY = by.get(0);
                prevW = bw.get(0); prevH = bh.get(0);

                long monitor = glfwGetPrimaryMonitor();
                GLFWVidMode mode = glfwGetVideoMode(monitor);
                glfwSetWindowMonitor(win, monitor,
                        0, 0,
                        mode.width(), mode.height(),
                        mode.refreshRate());
            } else {
                glfwSetWindowMonitor(win, NULL,
                        prevX, prevY,
                        prevW, prevH,
                        0);
            }
        }

        if (key == GLFW_KEY_ESCAPE) glfwSetWindowShouldClose(win, true);
        else if (key == GLFW_KEY_KP_ADD || key == GLFW_KEY_EQUAL) {
            maxIter += 10;
        } else if ((key == GLFW_KEY_KP_SUBTRACT || key == GLFW_KEY_MINUS) && maxIter > 1) {
            maxIter = Math.max(1, maxIter - 10);
        }
    }

    private void scrollCallback(long win, double dx, double dy) {
        // fetch current mouse position
        double[] mx = new double[1], my = new double[1];
        glfwGetCursorPos(win, mx, my);

        // normalize coordinates to [0,1]
        double nx = mx[0] / windowWidth;
        double ny = my[0] / windowHeight;

        // calculate position in complex plane
        double relX = (nx - 0.5) * 2.0 * scale + centerX;
        double relY = (0.5 - ny) * 2.0 * scale + centerY;

        //zoom in or out
        double factor = dy > 0 ? 0.9 : 1.1;

        // adjust scale and center to keep (relX, relY) fixed
        scale *= factor;
        centerX = relX + (centerX - relX) * factor;
        centerY = relY + (centerY - relY) * factor;
    }


    private void mouseButtonCallback(long win, int button, int action, int mods) {
        if (button == GLFW_MOUSE_BUTTON_LEFT) {
            if (action == GLFW_PRESS) {
                dragging = true;
                double[] x = new double[1], y = new double[1];
                glfwGetCursorPos(win, x, y);
                lastMouseX = x[0];
                lastMouseY = y[0];
            } else if (action == GLFW_RELEASE) {
                dragging = false;
            }
        }
    }

    private void cursorPosCallback(long win, double xpos, double ypos) {
        if (dragging) {
            double dx = xpos - lastMouseX;
            double dy = ypos - lastMouseY;
            // adjust center based on mouse movement
            centerX -= dx / windowWidth  * 2.0 * scale;
            centerY -= dy / windowHeight * 2.0 * scale;
            lastMouseX = xpos;
            lastMouseY = ypos;
        }
    }


    // Shader utility

    private int compileShader(int type, String src) {
        int id = glCreateShader(type);
        glShaderSource(id, src);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == 0)
            throw new RuntimeException("Shader compile failed: " + glGetShaderInfoLog(id));
        return id;
    }

    // GLSL sources

    private static final String vertexShaderSource =
            "#version 450 core\n" +
                    "layout(location=0) in vec2 aPos;\n" +
                    "void main() {\n" +
                    "    gl_Position = vec4(aPos, 0.0, 1.0);\n" +
                    "}\n";

    private static final String fragmentShaderSource =
            "#version 450 core\n" +
                    "out vec4 FragColor;\n" +
                    "uniform dvec2 u_center;\n" +
                    "uniform double u_scale;\n" +
                    "uniform int u_maxIter;\n" +
                    "uniform vec2 u_viewport;\n" +
                    "\n" +
                    "// Convert HSV to RGB (h in [0,360), s,v in [0,1])\n" +
                    "vec3 hsv2rgb(float h, float s, float v) {\n" +
                    "    float c = v*s;\n" +
                    "    float x = c * (1.0 - abs(mod(h/60.0,2.0)-1.0));\n" +
                    "    float m = v - c;\n" +
                    "    vec3 rgb;\n" +
                    "    if (h<60.0) rgb = vec3(c,x,0);\n" +
                    "    else if (h<120.0) rgb = vec3(x,c,0);\n" +
                    "    else if (h<180.0) rgb = vec3(0,c,x);\n" +
                    "    else if (h<240.0) rgb = vec3(0,x,c);\n" +
                    "    else if (h<300.0) rgb = vec3(x,0,c);\n" +
                    "    else rgb = vec3(c,0,x);\n" +
                    "    return rgb + vec3(m);\n" +
                    "}\n" +
                    "\n" +
                    "void main() {\n" +
                    "    // Map frag coord to complex plane\n" +
                    "    vec2 uv = gl_FragCoord.xy / u_viewport;\n" +
                    "    double x0 = (uv.x - 0.5)*2.0*u_scale + u_center.x;\n" +
                    "    double y0 = (0.5 - uv.y)*2.0*u_scale + u_center.y;\n" +
                    "    double x=0.0, y=0.0;\n" +
                    "    int iter;\n" +
                    "    for (iter = 0; iter < u_maxIter; ++iter) {\n" +
                    "        double x2 = x*x - y*y + x0;\n" +
                    "        double y2 = 2.0*x*y + y0;\n" +
                    "        x = x2; y = y2;\n" +
                    "        if (x*x + y*y > 4.0) break;\n" +
                    "    }\n" +
                    "    if (iter == u_maxIter) {\n" +
                    "        FragColor = vec4(0,0,0,1);\n" +
                    "    } else {\n" +
                    "        // Hue based on iteration count\n" +
                    "        float h = float(iter) / float(u_maxIter) * 360.0;\n" +
                    "        vec3 col = hsv2rgb(h, 1.0, 1.0);\n" +
                    "        FragColor = vec4(col,1);\n" +
                    "    }\n" +
                    "}\n";
}
