# Mandelbrot Explorer

An interactive Mandelbrot set viewer implemented in Java using LWJGL3 and OpenGL 4.5.

## Features

* Real‐time rendering of the Mandelbrot fractal on the GPU
* Smooth HSV→RGB coloring based on iteration count
* Mouse‐centered zooming and click‐and‐drag panning
* Adjustable maximum iteration count (`+`/`–` keys)
* Fullscreen toggle (F11) and dynamic viewport resizing
* Clean, well‐commented code to illustrate modern OpenGL and GLSL usage

## Prerequisites

* Java Development Kit (JDK 11 or newer)
* Git
* (Optional) [Gradle Wrapper](https://gradle.org/) (included)
* Internet access to download LWJGL3 dependencies

## Getting Started

1. **Clone the repository**

   ```bash
   git clone https://github.com/<your-username>/mandelbrot-explorer.git
   cd mandelbrot-explorer
   ```

2. **Build & run with Gradle**

   ```bash
   # Linux / macOS
   ./gradlew run

   # Windows
   gradlew.bat run
   ```

3. **IDE setup (IntelliJ / Eclipse)**

   * Import the project as a Gradle project
   * Ensure your Project SDK is set to Java 11+
   * Run the `com.example.MandelbrotViewer` main class

## Controls

| Action              | Key / Mouse                      |
| ------------------- | -------------------------------- |
| Exit                | `Esc`                            |
| Increase iterations | `+` or `=`                       |
| Decrease iterations | `–` or `_` (minus)               |
| Zoom in / out       | Mouse wheel (centered on cursor) |
| Pan view            | Left‐click + drag                |
| Toggle fullscreen   | `F11`                            |

## How It Works

1. **GLFW window & context**
   Creates an 800×600 window with OpenGL 4.5 core profile, loads function pointers via `GL.createCapabilities()`, and sets up resize callback.

2. **Shaders**

   * **Vertex shader** renders a full‐screen quad.
   * **Fragment shader** maps each pixel to a complex coordinate, performs Mandelbrot iteration (`z ← z² + c`), and colors by iteration count using HSV→RGB.

3. **Rendering pipeline**

   * A VAO/VBO holds four 2D vertices covering NDC [–1,1]².
   * `glDrawArrays(GL_TRIANGLE_FAN, …)` issues two triangles to fill the screen.
   * Uniforms (`u_center`, `u_scale`, `u_maxIter`, `u_viewport`) are updated each frame.

4. **Interaction**

   * Scroll callback computes the complex coordinate under the cursor and adjusts zoom & center.
   * Mouse‐drag translates the view in the complex plane.
   * Key callbacks adjust iteration count, exit, and toggle fullscreen.

## Project Structure

```
mandelbrot-explorer/
├── build.gradle
├── gradlew
├── gradlew.bat
├── settings.gradle
└── src
    └── main
        ├── java
        │   └── etf/rg2/dz1/
        │       └── MandelbrotViewer.java
        └── resources
            └── shaders
                ├── mandelbrot.vert
                └── mandelbrot.frag
```

## References

* [LWJGL 3 Guide](https://www.lwjgl.org/guide)
* [LearnOpenGL – Shaders](https://learnopengl.com/Getting-started/Shaders)
* [Khronos GLSL Reference](https://www.khronos.org/opengl/wiki/GLSL)

## License

This project is released under the MIT License. See [LICENSE](LICENSE) for details.
