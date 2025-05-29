package server;

import server.util.DataGenerator;

/**
 * Основной класс для запуска генератора дополнительных данных
 */
public class DataGeneratorMain {
    public static void main(String[] args) {
        System.out.println("=== ГЕНЕРАТОР ТЕСТОВЫХ ДАННЫХ ===");
        System.out.println("Система лояльности компьютерного клуба");
        System.out.println("====================================");
        
        try {
            DataGenerator.main(args);
        } catch (Exception e) {
            System.err.println("Критическая ошибка: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
} 