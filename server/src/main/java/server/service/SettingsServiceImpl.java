package server.service;

import common.model.Setting;
import common.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.db.dao.SettingDao;
import server.db.dao.SettingDaoImpl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реализация сервиса настроек с использованием базы данных
 */
public class SettingsServiceImpl implements SettingsService {
    private static final Logger logger = LoggerFactory.getLogger(SettingsServiceImpl.class);
    
    // DAO для работы с настройками в БД
    private final SettingDao settingDao;
    
    public SettingsServiceImpl() {
        this.settingDao = new SettingDaoImpl();
        ensureRequiredSettings();
    }
    
    /**
     * Проверяет наличие обязательных настроек и создает их, если отсутствуют
     */
    private void ensureRequiredSettings() {
        try {
            logger.info("Проверка наличия обязательных настроек");
            ensureSetting("THEME", "Светлая", "Тема оформления (Светлая/Темная/Системная)");
            ensureSetting("BACKUP_DIR", "backups", "Путь к директории резервных копий");
            ensureSetting("REPORTS_DIR", "reports", "Путь к директории для сохранения отчетов");
            ensureSetting("DEFAULT_BONUS_PERCENT", "5", "Процент начисления бонусов по умолчанию");
            ensureSetting("MIN_CARD_BALANCE", "0", "Минимальный баланс карты лояльности");
            ensureSetting("SESSION_TIMEOUT_MINUTES", "60", "Таймаут сессии в минутах");
            ensureSetting("LOG_LEVEL", "INFO", "Уровень логирования (DEBUG/INFO/WARN/ERROR)");
            ensureSetting("backup.path", System.getProperty("user.home") + "/backups", "Путь для хранения резервных копий");
            logger.info("Проверка обязательных настроек завершена");
        } catch (Exception e) {
            logger.error("Ошибка при проверке обязательных настроек", e);
        }
    }
    
    /**
     * Проверяет наличие настройки и создает ее, если отсутствует
     */
    private void ensureSetting(String key, String defaultValue, String description) {
        try {
            Optional<Setting> setting = settingDao.findByKey(key);
            
            if (setting.isEmpty()) {
                Setting newSetting = new Setting(key, defaultValue, description);
                newSetting.setLastUpdated(LocalDateTime.now());
                
                boolean saved = settingDao.save(newSetting);
                if (saved) {
                    logger.info("Создана настройка по умолчанию: {}", key);
                } else {
                    logger.error("Не удалось создать настройку по умолчанию: {}", key);
                }
            }
        } catch (Exception e) {
            logger.error("Ошибка при создании настройки по умолчанию: {}", key, e);
        }
    }
    
    @Override
    public Map<String, Setting> getAllSettings() {
        try {
            List<Setting> settingsList = settingDao.findAll();
            Map<String, Setting> result = new HashMap<>();
            
            for (Setting setting : settingsList) {
                result.put(setting.getKey(), setting);
            }
            
            logger.debug("Получены все настройки из базы данных. Количество: {}", result.size());
            return result;
        } catch (Exception e) {
            logger.error("Ошибка при получении всех настроек из базы данных", e);
            return new HashMap<>();
        }
    }
    
    @Override
    public Optional<Setting> getSetting(String key) {
        if (key == null || key.isEmpty()) {
            logger.warn("Запрос настройки с пустым ключом");
            return Optional.empty();
        }
        
        try {
            logger.debug("Запрос настройки по ключу из базы данных: {}", key);
            return settingDao.findByKey(key);
        } catch (Exception e) {
            logger.error("Ошибка при получении настройки по ключу из базы данных: {}", key, e);
            return Optional.empty();
        }
    }
    
    @Override
    public boolean saveSetting(Setting setting) {
        if (setting == null || setting.getKey() == null || setting.getKey().isEmpty()) {
            logger.warn("Попытка сохранить некорректную настройку");
            return false;
        }
        
        try {
            setting.setLastUpdated(LocalDateTime.now());
            
            // Сохраняем в БД
            boolean success = settingDao.save(setting);
            
            if (success) {
                logger.debug("Настройка сохранена в базу данных: {}", setting.getKey());
            } else {
                logger.warn("Не удалось сохранить настройку в базу данных: {}", setting.getKey());
            }
            
            return success;
        } catch (Exception e) {
            logger.error("Ошибка при сохранении настройки в базу данных: {}", setting.getKey(), e);
            return false;
        }
    }
    
    @Override
    public boolean deleteSetting(String key) {
        if (key == null || key.isEmpty()) {
            logger.warn("Попытка удалить настройку с пустым ключом");
            return false;
        }
        
        try {
            // Удаляем из БД
            boolean success = settingDao.delete(key);
            
            if (success) {
                logger.debug("Настройка удалена из базы данных: {}", key);
            } else {
                logger.warn("Настройка для удаления не найдена в базе данных: {}", key);
            }
            
            return success;
        } catch (Exception e) {
            logger.error("Ошибка при удалении настройки из базы данных: {}", key, e);
            return false;
        }
    }
} 