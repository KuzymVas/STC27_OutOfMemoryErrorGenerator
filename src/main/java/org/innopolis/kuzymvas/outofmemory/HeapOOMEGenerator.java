package org.innopolis.kuzymvas.outofmemory;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс для вызова Out of Memory Error: Heap.
 * Создает относительно постоянный поток данных, загружающихся в кучу.
 * Может быть настроен в режиме, позволяющем GC освобождать часть занимаемой памяти
 */
public class HeapOOMEGenerator {

    private final static int INT_SIZE = 4;

    private final int unrecoverableMemoryPerTick;
    private final int recoverableMemoryPerTick;
    private final int reportPeriod;
    private final int recoverPeriod;
    private final PrintStream output;

    private final List<MemoryWaster> wasters;

    /**
     * Создает новый генератор ошибоки Out of Memory Error: Heap.
     *
     * @param unrecoverableMemoryPerTick - объем в байтах занимаемой в такт работы (~1 мс) памяти, недоступной для GC
     * @param recoverableMemoryPerTick   - объем в байтах занимаемой в такт работы (~1 мс) памяти, периодически освобождаемой для GC
     * @param reportPeriod               - период в тактах между выводами состояния кучи в выводной поток
     * @param recoverPeriod              - период в тактах между освобожденями памяти для GC
     * @param output                     - выводной поток для отчетов
     */
    public HeapOOMEGenerator(
            int unrecoverableMemoryPerTick, int recoverableMemoryPerTick, int reportPeriod,
            int recoverPeriod, PrintStream output) {
        this.unrecoverableMemoryPerTick = unrecoverableMemoryPerTick / INT_SIZE;
        this.recoverableMemoryPerTick = recoverableMemoryPerTick / INT_SIZE;
        this.reportPeriod = reportPeriod;
        this.recoverPeriod = recoverPeriod;
        this.output = output;
        wasters = new ArrayList<>();
    }

    /**
     * Начинает заполнение памяти кучи мусором
     */
    public void generateOOME() {
        int reportCounter = 0;
        int recoverCounter = 0;
        while (true) {
            wasters.add(new MemoryWaster(unrecoverableMemoryPerTick, recoverableMemoryPerTick));
            if (recoverCounter == recoverPeriod) {
                for (int i = 0; i < recoverPeriod; i++) {
                    wasters.get(wasters.size() - recoverPeriod + i).recover();
                }
                recoverCounter = 0;
            }
            recoverCounter++;
            if (reportCounter == reportPeriod) {
                output.println("Current heap size: " + Runtime.getRuntime().totalMemory());
                output.println("Maximum heap size: " + Runtime.getRuntime().maxMemory());
                output.println("Free heap size: " + Runtime.getRuntime().freeMemory());
                output.println("============================");
                reportCounter = 0;
            }
            reportCounter++;
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                System.out.println("Out of memory error generation was interrupted. Aborting.");
                return;
            }
        }
    }

    /**
     * Класс мусора. Состоит из очищаемой и неочищаемой части
     */
    private static class MemoryWaster {

        private final int[] unrecoverable;
        private int[] recoverable;

        /**
         * Создает новый мусорный объект с заданными размерами очищаемой и не очищаемой части
         *
         * @param unrecoverableSize - размер неочищаемой части в int-ах (двойных словах = 4 байта)
         * @param recoverableSize   - размер очищаемой части в int-ах (двойных словах = 4 байта)
         */
        public MemoryWaster(int unrecoverableSize, int recoverableSize) {
            unrecoverable = new int[unrecoverableSize];
            recoverable = new int[recoverableSize];
        }

        /**
         * Освобождает очищаемую часть для сбора GC
         */
        public void recover() {
            recoverable = null;
        }
    }
}
