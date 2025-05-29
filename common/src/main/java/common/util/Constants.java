package common.util;

/**
 * Константы, используемые в приложении
 */
public class Constants {
    // Сетевые настройки
    public static final String SERVER_HOST = "localhost";
    public static final int SERVER_PORT = 8090; // Changed from 8080 to avoid port conflicts
    
    // Настройки базы данных
    public static final String DB_HOST = "localhost";
    public static final int DB_PORT = 5432;
    public static final String DB_NAME = "loyalty_db";
    public static final String DB_USER = "postgres";
    public static final String DB_PASSWORD = "27932102300";
    
    // Настройки системы лояльности
    public static final int POINTS_PER_HOUR = 10; // Количество баллов за час игры
    public static final int POINTS_EXPIRY_MONTHS = 12; // Срок действия баллов в месяцах
    
    // Пути к файлам
    public static final String BACKUP_DIR = "backups"; // Директория для резервных копий
    public static final String REPORTS_DIR = "reports"; // Директория для отчетов
    public static final String PDF_DIR = "pdf"; // Директория для PDF-файлов
    
    // Роли пользователей
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_STAFF = "STAFF";
    public static final String ROLE_CLIENT = "CLIENT";
    
    // Ключи настроек
    public static final String SETTING_THEME = "THEME";
    public static final String SETTING_BACKUP_DIR = "BACKUP_DIR";
    public static final String SETTING_REPORTS_DIR = "REPORTS_DIR";
    public static final String SETTING_PDF_DIR = "PDF_DIR";
    
    // Значения настроек
    public static final String THEME_LIGHT = "LIGHT";
    public static final String THEME_DARK = "DARK";
    
    // Префиксы имен файлов
    public static final String BACKUP_FILE_PREFIX = "backup_";
    public static final String REPORT_FILE_PREFIX = "report_";
    public static final String PDF_RECEIPT_PREFIX = "receipt_";
    
    // Заголовки отчетов
    public static final String[] POINTS_REPORT_HEADERS = {"ID", "Карта", "Клиент", "Тип", "Баллы", "Дата"};
    public static final String[] SESSIONS_REPORT_HEADERS = {"ID", "Карта", "Клиент", "Начало", "Окончание", "Минуты", "Баллы"};
    public static final String[] PROMOTIONS_REPORT_HEADERS = {"ID", "Название", "Начало", "Окончание", "Бонус %", "Активаций"};
} 