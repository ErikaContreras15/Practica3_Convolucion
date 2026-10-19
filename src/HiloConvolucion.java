/** Hilo que procesa una franja de filas. Cada hilo escribe solo sus filas. */
public class HiloConvolucion extends Thread {
    private final int[] src, dst;
    private final int w, h, y0, y1;

    HiloConvolucion(int[] src, int[] dst, int w, int h, int y0, int y1) {
        this.src = src; this.dst = dst;
        this.w = w; this.h = h; this.y0 = y0; this.y1 = y1;
    }

    @Override
    public void run() {
        Convolucion.aplicar(src, dst, w, h, y0, y1);
    }
}
