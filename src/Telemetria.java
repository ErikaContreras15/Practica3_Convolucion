import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;

/** Medicion de RAM y un hilo monitor que registra el pico de memoria. */
public class Telemetria {
    static final double MB = 1024.0 * 1024.0;

    static double usadaMB() {
        Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / MB;
    }

    static double maximaMB() { return Runtime.getRuntime().maxMemory() / MB; }

    static OperatingSystemMXBean so() {
        return (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    }

    static double fisicaTotalMB() { return so().getTotalMemorySize() / MB; }
    static double fisicaLibreMB() { return so().getFreeMemorySize() / MB; }

    static void limpiar() throws InterruptedException {
        System.gc();
        Thread.sleep(100);
    }

    /** Mide la RAM cada 5 ms mientras corre el filtro. */
    static class Monitor extends Thread {
        private volatile boolean activo = true;
        private volatile double pico = 0;

        Monitor() { setDaemon(true); }

        @Override
        public void run() {
            while (activo) {
                pico = Math.max(pico, usadaMB());
                try { Thread.sleep(5); } catch (InterruptedException e) { break; }
            }
        }

        double detener() throws InterruptedException {
            activo = false;
            interrupt();
            join();
            return pico;
        }
    }
}
