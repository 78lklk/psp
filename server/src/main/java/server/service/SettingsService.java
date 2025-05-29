package server.service;

import common.model.Setting;

import java.util.Map;
import java.util.Optional;

/**
 * Сервис для работы с настройками системы
 */
public interface SettingsService {

    /**
     * Получает все настройки
     * @return карта настроек (ключ -&gt; значение)
     */
    Map<String, Setting> getAllSettings();

    /**
     * Получает настройку по ключу
     * @param key ключ настройки
     * @return настройка или пустой Optional если не найдена
     */
    Optional<Setting> getSetting(String key);

    /**
     * Сохраняет настройку
     * @param setting настройка для сохранения
     * @return true если сохранено успешно
     */
    boolean saveSetting(Setting setting);

    /**
     * Удаляет настройку
     * @param key ключ настройки для удаления
     * @return true если удалено успешно
     */
    boolean deleteSetting(String key);
} 