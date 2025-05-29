package client.service;

import client.model.ScheduleEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сервис для работы с расписанием компьютеров
 * Простая "база данных" в файле
 */
public class ScheduleService {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleService.class);
    private static final String DATA_DIRECTORY = "data";
    private static final String FILE_NAME = "schedule.dat";
    
    private List<ScheduleEntry> scheduleEntries;
    private final String authToken; // не используется, но нужен для совместимости
    
    /**
     * Конструктор сервиса расписания
     */
    public ScheduleService(String authToken) {
        this.authToken = authToken;
        this.scheduleEntries = new ArrayList<>();
        
        // Создаем директорию для данных, если она не существует
        File dataDir = new File(DATA_DIRECTORY);
        if (!dataDir.exists()) {
            dataDir.mkdir();
        }
        
        // Загружаем данные из файла
        loadData();
    }
    
    /**
     * Получает записи расписания на указанную дату
     */
    public List<ScheduleEntry> getEntriesByDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        
        return scheduleEntries.stream()
                .filter(entry -> entry.isActive())
                .filter(entry -> entry.getStartTime() != null &&
                        (entry.getStartTime().isEqual(startOfDay) || entry.getStartTime().isAfter(startOfDay)) &&
                        entry.getStartTime().isBefore(endOfDay))
                .collect(Collectors.toList());
    }
    
    /**
     * Получает все компьютеры с записями на указанную дату
     */
    public Map<String, List<ScheduleEntry>> getComputerEntriesByDate(LocalDate date) {
        List<ScheduleEntry> entries = getEntriesByDate(date);
        Map<String, List<ScheduleEntry>> computerEntries = new HashMap<>();
        
        // Создаем список компьютеров по умолчанию
        for (int i = 1; i <= 5; i++) {
            computerEntries.put("ПК #" + i, new ArrayList<>());
        }
        
        // Добавляем записи для каждого компьютера
        for (ScheduleEntry entry : entries) {
            String computerName = entry.getComputerName();
            if (!computerEntries.containsKey(computerName)) {
                computerEntries.put(computerName, new ArrayList<>());
            }
            computerEntries.get(computerName).add(entry);
        }
        
        return computerEntries;
    }
    
    /**
     * Добавляет новую запись в расписание
     */
    public boolean addEntry(ScheduleEntry entry) {
        try {
            // Проверка на пересечение с другими записями
            if (hasOverlap(entry)) {
                logger.warn("Не удалось создать запись в расписании: пересечение с существующими записями");
                return false;
            }
            
            // Генерируем ID
            long maxId = 0;
            for (ScheduleEntry e : scheduleEntries) {
                if (e.getId() != null && e.getId() > maxId) {
                    maxId = e.getId();
                }
            }
            entry.setId(maxId + 1);
            
            // Добавляем запись
            scheduleEntries.add(entry);
            
            // Сохраняем данные
            saveData();
            logger.info("Запись успешно добавлена: {}", entry);
            
            return true;
        } catch (Exception e) {
            logger.error("Ошибка при добавлении записи в расписание", e);
            return false;
        }
    }
    
    /**
     * Отменяет запись в расписании
     */
    public boolean cancelEntry(Long entryId) {
        try {
            boolean found = false;
            
            for (ScheduleEntry entry : scheduleEntries) {
                if (entry.getId() != null && entry.getId().equals(entryId)) {
                    entry.setActive(false);
                    found = true;
                    break;
                }
            }
            
            if (found) {
                saveData();
                logger.info("Запись с ID {} отменена", entryId);
                return true;
            } else {
                logger.warn("Не удалось отменить запись в расписании: запись с ID {} не найдена", entryId);
                return false;
            }
        } catch (Exception e) {
            logger.error("Ошибка при отмене записи в расписании", e);
            return false;
        }
    }
    
    /**
     * Создает запись в расписании с картой
     */
    public boolean createBookingWithCard(String cardNumber, String computerName,
                                      String clientName, LocalDateTime startTime,
                                      int durationHours) {
        // Создаем запись
        ScheduleEntry entry = new ScheduleEntry(
                computerName,
                clientName,
                cardNumber,
                startTime,
                durationHours
        );
        
        return addEntry(entry);
    }
    
    /**
     * Создает запись в расписании без карты
     */
    public boolean createBooking(String computerName, String clientName, 
                              LocalDateTime startTime, int durationHours) {
        ScheduleEntry entry = new ScheduleEntry(
                computerName,
                clientName,
                null,
                startTime,
                durationHours
        );
        
        return addEntry(entry);
    }
    
    /**
     * Проверяет, пересекается ли запись с другими записями
     */
    private boolean hasOverlap(ScheduleEntry entry) {
        if (entry.getStartTime() == null || entry.getDurationHours() == null) {
            return false;
        }
        
        LocalDateTime entryStart = entry.getStartTime();
        LocalDateTime entryEnd = entry.getEndTime();
        
        for (ScheduleEntry existingEntry : scheduleEntries) {
            // Пропускаем неактивные записи или записи для других компьютеров
            if (!existingEntry.isActive() || !existingEntry.getComputerName().equals(entry.getComputerName())) {
                continue;
            }
            
            LocalDateTime existingStart = existingEntry.getStartTime();
            LocalDateTime existingEnd = existingEntry.getEndTime();
            
            if (existingStart == null || existingEnd == null) {
                continue;
            }
            
            // Проверяем пересечение интервалов
            boolean overlap = (entryStart.isBefore(existingEnd) && entryEnd.isAfter(existingStart));
            
            if (overlap) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Сохраняет данные в файл
     */
    private void saveData() {
        try {
            File file = new File(DATA_DIRECTORY + File.separator + FILE_NAME);
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                oos.writeObject(scheduleEntries);
            }
            logger.debug("Данные расписания сохранены в файл");
        } catch (Exception e) {
            logger.error("Ошибка при сохранении данных расписания", e);
        }
    }
    
    /**
     * Загружает данные из файла
     */
    @SuppressWarnings("unchecked")
    private void loadData() {
        File file = new File(DATA_DIRECTORY + File.separator + FILE_NAME);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                Object obj = ois.readObject();
                if (obj instanceof List) {
                    scheduleEntries = (List<ScheduleEntry>) obj;
                    logger.debug("Данные расписания загружены из файла: {} записей", scheduleEntries.size());
                }
            } catch (Exception e) {
                logger.error("Ошибка при загрузке данных расписания", e);
                // Создаем новый пустой список, если не удалось загрузить данные
                scheduleEntries = new ArrayList<>();
            }
        } else {
            logger.debug("Файл с данными расписания не найден, создан новый пустой список");
            scheduleEntries = new ArrayList<>();
        }
    }
    
    /**
     * Получает запись, активную в указанное время для указанного компьютера
     */
    public ScheduleEntry getActiveEntry(String computerName, LocalDateTime time) {
        return scheduleEntries.stream()
                .filter(ScheduleEntry::isActive)
                .filter(entry -> entry.getComputerName().equals(computerName))
                .filter(entry -> entry.containsTime(time))
                .findFirst()
                .orElse(null);
    }
} 