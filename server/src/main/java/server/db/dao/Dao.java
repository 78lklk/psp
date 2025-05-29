package server.db.dao;

import java.util.List;
import java.util.Optional;

/**
 * Базовый интерфейс для DAO
 * @param <T> тип сущности
 * @param <ID> тип идентификатора
 */
public interface Dao<T, ID> {
    /**
     * Получить сущность по идентификатору
     * @param id идентификатор
     * @return сущность или пустой Optional, если не найдена
     */
    Optional<T> findById(ID id);
    
    /**
     * Получить все сущности
     * @return список сущностей
     */
    List<T> findAll();
    
    /**
     * Сохранить сущность
     * @param entity сущность
     * @return сохраненная сущность с заполненным идентификатором
     */
    T save(T entity);
    
    /**
     * Обновить сущность
     * @param entity сущность
     * @return true, если сущность обновлена успешно
     */
    boolean update(T entity);
    
    /**
     * Удалить сущность по идентификатору
     * @param id идентификатор
     * @return true, если сущность удалена успешно
     */
    boolean deleteById(ID id);
} 