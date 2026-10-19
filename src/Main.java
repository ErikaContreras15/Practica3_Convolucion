import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * Practica 3 - Filtro Sobel secuencial vs paralelo con Java Threads.
 * Uso: java -Xmx4g -cp bin Main [imagen] [repeticiones] [hilos]
 */
public class Main {

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);
        String ruta = args.length > 0 ? args[0] : "data/imagen.jpg";
        int reps = args.length > 1 ? Integer.parseInt(args[1]) : 5;
        int[] hilos = args.length > 2
                ? Arrays.stream(args[2].split(",")).mapToInt(Integer::parseInt).toArray()
                : new int[]{1, 2, 4, 8};

        // 1) Cargar la imagen (fuera de la medicion)
        Telemetria.limpiar();
        double ramInicio = Telemetria.usadaMB();
        BufferedImage img = ImageIO.read(new File(ruta));
        int w = img.getWidth(), h = img.getHeight();
        int[] origen = img.getRGB(0, 0, w, h, null, 0, w);
        int[] referencia = new int[w * h];
        int[] destino = new int[w * h];
        Telemetria.limpiar();
        double ramImagen = Telemetria.usadaMB();
        double mbCopia = w * (double) h * 4 / Telemetria.MB;

        System.out.println("===== CONFIGURACION =====");
        System.out.println("Imagen: " + new File(ruta).getName());
        System.out.printf("Tamano: %d x %d (%.1f megapixeles)%n", w, h, w * (double) h / 1e6);
        System.out.println("Filtro: Sobel 3x3 (halo de 1 px)");
        System.out.println("Repeticiones: " + reps + " | Hilos: " + Arrays.toString(hilos));
        System.out.println("Nucleos disponibles: " + Runtime.getRuntime().availableProcessors());
        System.out.println();

        System.out.println("===== TELEMETRIA DE RAM =====");
        System.out.printf("RAM fisica del equipo:        %.0f MB (libre: %.0f MB)%n",
                Telemetria.fisicaTotalMB(), Telemetria.fisicaLibreMB());
        System.out.printf("Maximo para la JVM (-Xmx):    %.0f MB%n", Telemetria.maximaMB());
        System.out.printf("RAM usada al iniciar:         %.1f MB%n", ramInicio);
        System.out.printf("RAM con la imagen cargada:    %.1f MB%n", ramImagen);
        System.out.printf("Una copia de la imagen:       %.1f MB (%d x %d x 4 bytes)%n", mbCopia, w, h);
        System.out.printf("Estimado teorico (4 copias):  %.1f MB%n", mbCopia * 4);
        System.out.println();

        // 2) Referencia secuencial y calentamiento de la JVM (no se mide)
        Convolucion.secuencial(origen, referencia, w, h);
        for (int c = 0; c < 2; c++)
            for (int n : hilos) ejecutar(origen, destino, w, h, n);

        // 3) Medicion: cada configuracion se repite 'reps' veces y se promedia
        System.out.println("===== RESULTADOS =====");
        System.out.printf("%-16s %-22s %-14s %-12s %-16s%n",
                "Hilos", "Tiempo promedio (ms)", "Aceleracion", "Eficiencia", "RAM pico (MB)");
        double ts = 0;
        for (int n : hilos) {
            Telemetria.limpiar();
            Telemetria.Monitor monitor = new Telemetria.Monitor();
            monitor.start();
            long suma = 0;
            for (int r = 0; r < reps; r++) {
                long t0 = System.nanoTime();
                ejecutar(origen, destino, w, h, n);
                suma += System.nanoTime() - t0;
                if (!Arrays.equals(destino, referencia))
                    System.out.println("ERROR: el resultado con " + n + " hilos no coincide");
            }
            double pico = monitor.detener();
            double prom = suma / (double) reps / 1e6;
            if (n == 1) ts = prom;
            double s = ts / prom;
            System.out.printf("%-16s %-22.2f %-14.2f %-12s %-16.1f%n",
                    n == 1 ? "1 (secuencial)" : String.valueOf(n),
                    prom, s, String.format("%.0f%%", 100 * s / n), pico);
        }

        // 4) Guardar la imagen filtrada
        new File("salida").mkdirs();
        BufferedImage res = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        res.setRGB(0, 0, w, h, referencia, 0, w);
        ImageIO.write(res, "jpg", new File("salida/resultado.jpg"));
    }

    static void ejecutar(int[] o, int[] d, int w, int h, int n) throws InterruptedException {
        if (n == 1) Convolucion.secuencial(o, d, w, h);
        else Convolucion.paralelo(o, d, w, h, n);
    }
}
