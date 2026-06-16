import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class ThreadSafetyTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Iniciando testes de thread safety ===");

        testSynchronizedRGB();
        System.out.println();
        testImmutableRGB();

        System.out.println("=== Testes concluídos com sucesso ===");
    }

    private static void testSynchronizedRGB() throws Exception {
        final SynchronizedRGB color = new SynchronizedRGB(10, 20, 30, "Initial");
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final int THREADS = 4;
        final int ITERATIONS = 100_000;

        Thread[] writers = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            writers[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < ITERATIONS; j++) {
                        int r = (j * 11) % 256;
                        int g = (j * 17) % 256;
                        int b = (j * 23) % 256;
                        color.set(r, g, b, "S-" + Thread.currentThread().getId() + "-" + j);
                        int rgb = color.getRGB();
                        String name = color.getName();
                        if (rgb < 0 || rgb > 0x00FFFFFF) {
                            throw new IllegalStateException("RGB fora do intervalo: " + rgb);
                        }
                        if (name == null) {
                            throw new IllegalStateException("Nome nulo detectado");
                        }
                    }
                } catch (Throwable t) {
                    error.compareAndSet(null, t);
                }
            }, "Writer-" + i);
            writers[i].start();
        }

        Thread[] readers = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            readers[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < ITERATIONS; j++) {
                        int rgb = color.getRGB();
                        String name = color.getName();
                        if (rgb < 0 || rgb > 0x00FFFFFF) {
                            throw new IllegalStateException("RGB fora do intervalo: " + rgb);
                        }
                        if (name == null) {
                            throw new IllegalStateException("Nome nulo detectado");
                        }
                    }
                } catch (Throwable t) {
                    error.compareAndSet(null, t);
                }
            }, "Reader-" + i);
            readers[i].start();
        }

        for (Thread writer : writers) {
            writer.join();
        }
        for (Thread reader : readers) {
            reader.join();
        }

        if (error.get() != null) {
            throw new AssertionError("Falha durante o teste de SynchronizedRGB", error.get());
        }

        System.out.println("SynchronizedRGB: sem falhas de concorrência detectadas.");
        System.out.println("Estado final: " + color.getName() + " 0x" + toHex(color.getRGB()));
    }

    private static void testImmutableRGB() throws Exception {
        final AtomicReference<ImmutableRGB> colorRef = new AtomicReference<>(new ImmutableRGB(100, 150, 200, "InitialImmutable"));
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final int THREADS = 4;
        final int ITERATIONS = 100_000;

        Thread[] workers = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            workers[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < ITERATIONS; j++) {
                        ImmutableRGB current = colorRef.get();
                        if (current.getRGB() < 0 || current.getRGB() > 0x00FFFFFF) {
                            throw new IllegalStateException("RGB fora do intervalo: " + current.getRGB());
                        }
                        ImmutableRGB inverted = current.invert();
                        if (inverted.getRGB() < 0 || inverted.getRGB() > 0x00FFFFFF) {
                            throw new IllegalStateException("RGB invertido fora do intervalo: " + inverted.getRGB());
                        }
                        colorRef.set(inverted);
                    }
                } catch (Throwable t) {
                    error.compareAndSet(null, t);
                }
            }, "ImmutableWorker-" + i);
            workers[i].start();
        }

        for (Thread worker : workers) {
            worker.join();
        }

        if (error.get() != null) {
            throw new AssertionError("Falha durante o teste de ImmutableRGB", error.get());
        }

        ImmutableRGB finalColor = colorRef.get();
        System.out.println("ImmutableRGB: sem falhas de concorrência detectadas.");
        System.out.println("Estado final: " + finalColor.getName() + " 0x" + toHex(finalColor.getRGB()));
    }

    private static String toHex(int rgb) {
        return String.format("%06X", rgb & 0xFFFFFF);
    }
}
