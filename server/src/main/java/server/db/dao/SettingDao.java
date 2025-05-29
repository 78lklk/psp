package server.db.dao;

import common.model.Setting;

import java.util.List;
import java.util.Optional;

/**
 * DAO интерфейс для работы с настройками в базе данных
 */
public interface SettingDao {
    
    /**
     * Получает все настройки из базы данных
     * @return список всех настроек
     */
    List<Setting> findAll();
    
    /**
     * Находит настройку по ключу
     * @param key ключ настройки
     * @return настройка или empty если не найдена
     */
    Optional<Setting> findByKey(String key);
    
    /**
     * Сохраняет или обновляет настройку
     * @param setting настройка для сохранения
     * @return true если операция успешна
     */
    boolean save(Setting setting);
    
    /**
     * Удаляет настройку по ключу
     * @param key ключ настройки
     * @return true если настройка была удалена
     */
    boolean delete(String key);
} 