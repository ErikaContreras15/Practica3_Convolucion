/**
 * Filtro Sobel 3x3 (convolucion hecha a mano, sin OpenCV).
 * Version secuencial y paralela con Thread.
 */
public class Convolucion {

    // Mascaras de Sobel: bordes horizontales (GX) y verticales (GY)
    static final int[][] GX = {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
    static final int[][] GY = {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};

    /** Aplica la mascara a las filas [y0, y1). */
    static void aplicar(int[] src, int[] dst, int w, int h, int y0, int y1) {
        for (int y = y0; y < y1; y++) {
            for (int x = 0; x < w; x++) {
                float sx = 0, sy = 0;
                for (int ky = 0; ky < 3; ky++) {
                    int fila = limite(y + ky - 1, h) * w;   // halo: fila vecina
                    for (int kx = 0; kx < 3; kx++) {
                        int p = src[fila + limite(x + kx - 1, w)];
                        float gris = 0.299f * ((p >> 16) & 0xFF) + 0.587f * ((p >> 8) & 0xFF) + 0.114f * (p & 0xFF);
                        sx += gris * GX[ky][kx];
                        sy += gris * GY[ky][kx];
                    }
                }
                int m = (int) Math.min(255, Math.sqrt(sx * sx + sy * sy));
                dst[y * w + x] = (m << 16) | (m << 8) | m;
            }
        }
    }

    static void secuencial(int[] src, int[] dst, int w, int h) {
        aplicar(src, dst, w, h, 0, h);
    }

    /** Divide la imagen en N franjas de filas, una por hilo. */
    static void paralelo(int[] src, int[] dst, int w, int h, int n) throws InterruptedException {
        HiloConvolucion[] hilos = new HiloConvolucion[n];
        int fila = 0;
        for (int i = 0; i < n; i++) {
            int filas = h / n + (i < h % n ? 1 : 0);
            hilos[i] = new HiloConvolucion(src, dst, w, h, fila, fila + filas);
            hilos[i].start();
            fila += filas;
        }
        for (HiloConvolucion t : hilos) t.join();   // esperar a todos
    }

    static int limite(int v, int max) {
        return v < 0 ? 0 : (v >= max ? max - 1 : v);
    }
}
