package server.service;

import common.model.Transaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Интерфейс сервиса для работы с транзакциями
 */
public interface TransactionService {
    /**
     * Создать новую транзакцию
     * @param cardId ID карты
     * @param type тип транзакции
     * @param points количество баллов
     * @param description описание транзакции
     * @return созданная транзакция
     */
    Transaction createTransaction(Long cardId, Transaction.Type type, int points, String description);
    
    /**
     * Получить транзакцию по ID
     * @param id ID транзакции
     * @return транзакция или пустой Optional, если не найдена
     */
    Optional<Transaction> getTransactionById(Long id);
    
    /**
     * Получить транзакции карты
     * @param cardId ID карты
     * @return список транзакций
     */
    List<Transaction> getTransactionsByCardId(Long cardId);
    
    /**
     * Получить транзакции карты за указанный период
     * @param cardId ID карты
     * @param from начало периода
     * @param to конец периода
     * @return список транзакций
     */
    List<Transaction> getTransactionsByCardIdAndPeriod(Long cardId, LocalDateTime from, LocalDateTime to);
    
    /**
     * Получить транзакции по типу
     * @param type тип транзакции
     * @return список транзакций
     */
    List<Transaction> getTransactionsByType(Transaction.Type type);
    
    /**
     * Получить все транзакции
     * @return список всех транзакций
     */
    List<Transaction> getAllTransactions();
    
    /**
     * Удалить транзакцию
     * @param id ID транзакции
     * @return true, если транзакция успешно удалена
     */
    boolean deleteTransaction(Long id);
} 