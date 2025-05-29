package server.service;

import common.model.Card;
import common.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.db.dao.CardDao;
import server.db.dao.CardDaoImpl;
import server.db.dao.TransactionDao;
import server.db.dao.TransactionDaoImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Реализация сервиса для работы с транзакциями
 */
public class TransactionServiceImpl implements TransactionService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionServiceImpl.class);
    private final TransactionDao transactionDao;
    private final CardDao cardDao;
    
    public TransactionServiceImpl() {
        this.transactionDao = new TransactionDaoImpl();
        this.cardDao = new CardDaoImpl();
    }
    
    public TransactionServiceImpl(TransactionDao transactionDao, CardDao cardDao) {
        this.transactionDao = transactionDao;
        this.cardDao = cardDao;
    }

    @Override
    public Transaction createTransaction(Long cardId, Transaction.Type type, int points, String description) {
        logger.debug("Создание новой транзакции типа {} для карты {} на {} баллов", type, cardId, points);
        
        try {
            // Проверяем, существует ли карта
            Optional<Card> cardOpt = cardDao.findById(cardId);
            if (cardOpt.isEmpty()) {
                logger.debug("Карта с id {} не найдена", cardId);
                throw new IllegalArgumentException("Карта не найдена");
            }
            
            // Создаем транзакцию
            Transaction transaction = new Transaction();
            transaction.setCard(cardOpt.get());
            transaction.setType(type);
            transaction.setPoints(points);
            transaction.setTimestamp(LocalDateTime.now());
            transaction.setDescription(description);
            
            Transaction createdTransaction = transactionDao.save(transaction);
            logger.info("Создана новая транзакция: {}", createdTransaction.getId());
            
            return createdTransaction;
        } catch (Exception e) {
            logger.error("Ошибка при создании транзакции для карты: {}", cardId, e);
            throw new RuntimeException("Ошибка при создании транзакции", e);
        }
    }

    @Override
    public Optional<Transaction> getTransactionById(Long id) {
        logger.debug("Получение транзакции по id: {}", id);
        return transactionDao.findById(id);
    }

    @Override
    public List<Transaction> getTransactionsByCardId(Long cardId) {
        logger.debug("Получение транзакций для карты: {}", cardId);
        return transactionDao.findByCardId(cardId);
    }

    @Override
    public List<Transaction> getTransactionsByCardIdAndPeriod(Long cardId, LocalDateTime from, LocalDateTime to) {
        logger.debug("Получение транзакций для карты {} в период с {} по {}", cardId, from, to);
        return transactionDao.findByCardIdAndPeriod(cardId, from, to);
    }

    @Override
    public List<Transaction> getTransactionsByType(Transaction.Type type) {
        logger.debug("Получение транзакций по типу: {}", type);
        return transactionDao.findByType(type);
    }

    @Override
    public List<Transaction> getAllTransactions() {
        logger.debug("Получение всех транзакций");
        return transactionDao.findAll();
    }

    @Override
    public boolean deleteTransaction(Long id) {
        logger.debug("Удаление транзакции с id: {}", id);
        
        try {
            boolean deleted = transactionDao.deleteById(id);
            if (deleted) {
                logger.info("Транзакция удалена: {}", id);
            } else {
                logger.error("Не удалось удалить транзакцию: {}", id);
            }
            
            return deleted;
        } catch (Exception e) {
            logger.error("Ошибка при удалении транзакции: {}", id, e);
            return false;
        }
    }
} 