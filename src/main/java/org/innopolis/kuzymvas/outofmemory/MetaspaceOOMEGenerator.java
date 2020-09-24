package org.innopolis.kuzymvas.outofmemory;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Класс для вызова Out of Memory Error: Metaspace.
 * Использует тот факт, что загруженный класс уникален для каждого из ClassLoader-ов.
 * Тем самым загружая множество ClassLoader-ов один и тот же класс можно получить
 * множество его копий в Metaspace
 */
public class MetaspaceOOMEGenerator {

    private final PrintStream output;

    private final int reportPeriod;
    private final List<MetaspaceWaster> wasters;
    private final MemoryPoolMXBean metaspace;
    private final Path pathToWasteClass;

    /**
     * Создает новый генератор ошибоки Out of Memory Error: Metaspace.
     *
     * @param reportPeriod     - период в тактах между выводами состояния Metaspace в выводной поток
     * @param output           - выводной поток для отчетов
     * @param pathToWasteClass - путь к файлу класса, используегому для заполнения Metaspace
     */
    public MetaspaceOOMEGenerator(int reportPeriod, PrintStream output, Path pathToWasteClass) {
        this.reportPeriod = reportPeriod;
        List<MemoryPoolMXBean> memPool = ManagementFactory.getMemoryPoolMXBeans();
        final Optional<MemoryPoolMXBean> metaspaceBean = memPool.stream().filter(
                (bean) -> bean.getName().equals("Metaspace")).findAny();
        if (!metaspaceBean.isPresent()) {
            System.out.println("Metaspace MXBean not found. Can't monitor metaspace usage");
        }
        this.metaspace = metaspaceBean.get();
        this.output = output;
        this.pathToWasteClass = pathToWasteClass;
        wasters = new ArrayList<>();
    }

    /**
     * Начниает заполнение памяти Metaspace мусором
     */
    public void generateOOME() {
        int reportCounter = 0;
        while (true) {
            wasters.add(new MetaspaceWaster(pathToWasteClass));
            Class<?> c = null;
            try {
                c = wasters.get(wasters.size() - 1).findClass(this.getClass().getName());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return;
            }
            if (metaspace != null && reportCounter == reportPeriod) {
                output.println("Current metaspace size: " + metaspace.getUsage());
                output.println("============================");
                reportCounter = 0;
            }
            reportCounter++;
        }
    }

    /**
     * Загрузчик классов для загрузки единственного мусорного класса занимающего Metaspace, пока сам загрузчик не будет
     * собран GC
     */
    private static class MetaspaceWaster extends ClassLoader {

        private final Path pathToWasteClass;

        /**
         * Создает новый загрузчик с указанным путем к мусорному классу
         *
         * @param pathToWasteClass
         */
        public MetaspaceWaster(Path pathToWasteClass) {
            this.pathToWasteClass = pathToWasteClass;
        }

        /**
         * Возвращает мусорный класс
         *
         * @param name - игнорируется
         * @return - загруженный мусорный класс
         * @throws ClassNotFoundException - если не удалось загрузить мусорный класс
         */
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            try {
                final byte[] bytes = IOUtils.toByteArray(this.getClass().getResourceAsStream("UberClass.class"));
                /*Files.readAllBytes(
                        pathToWasteClass);
                */return defineClass(null, bytes, 0, bytes.length);
            } catch (IOException e) {
                throw new ClassNotFoundException();
            }
        }
    }
}
