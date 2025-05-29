package server.db.dao;

import common.model.Transaction;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DAO для работы с транзакциями
 */
public interface TransactionDao extends Dao<Transaction, Long> {
    /**
     * Найти транзакции карты
     * @param cardId ID карты
     * @return список транзакций
     */
    List<Transaction> findByCardId(Long cardId);
    
    /**
     * Найти транзакции карты за указанный период
     * @param cardId ID карты
     * @param from начало периода
     * @param to конец периода
     * @return список транзакций
     */
    List<Transaction> findByCardIdAndPeriod(Long cardId, LocalDateTime from, LocalDateTime to);
    
    /**
     * Найти транзакции по типу
     * @param type тип транзакции
     * @return список транзакций
     */
    List<Transaction> findByType(Transaction.Type type);
} 