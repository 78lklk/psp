package server.service;

import common.model.Backup;
import common.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с резервными копиями базы данных
 */
public interface BackupService {

    /**
     * Создает резервную копию базы данных
     * @param user пользователь, инициировавший создание резервной копии
     * @param description описание резервной копии
     * @return созданная резервная копия или null в случае ошибки
     */
    Backup createBackup(User user, String description);

    /**
     * Восстанавливает базу данных из резервной копии
     * @param backupId ID резервной копии
     * @param user пользователь, инициировавший восстановление
     * @return true если восстановление успешно
     */
    boolean restoreFromBackup(Long backupId, User user);

    /**
     * Получает список всех резервных копий
     * @return список резервных копий
     */
    List<Backup> getAllBackups();

    /**
     * Получает резервную копию по ID
     * @param id ID резервной копии
     * @return резервная копия или пустой Optional, если не найдена
     */
    Optional<Backup> getBackupById(Long id);

    /**
     * Удаляет резервную копию
     * @param id ID резервной копии
     * @return true если удаление успешно
     */
    boolean deleteBackup(Long id);

    /**
     * Получает информацию о директории с резервными копиями
     * @return информация о директории
     */
    BackupDirectoryInfo getBackupDirectoryInfo();

    /**
     * Информация о директории с резервными копиями
     */
    class BackupDirectoryInfo {
        private long totalSize;
        private int backupCount;
        private String path;
        private long freeSpace;
        
        public BackupDirectoryInfo() {
        }
        
        public BackupDirectoryInfo(long totalSize, int backupCount, String path, long freeSpace) {
            this.totalSize = totalSize;
            this.backupCount = backupCount;
            this.path = path;
            this.freeSpace = freeSpace;
        }
        
        public long getTotalSize() {
            return totalSize;
        }
        
        public void setTotalSize(long totalSize) {
            this.totalSize = totalSize;
        }
        
        public int getBackupCount() {
            return backupCount;
        }
        
        public void setBackupCount(int backupCount) {
            this.backupCount = backupCount;
        }
        
        public String getPath() {
            return path;
        }
        
        public void setPath(String path) {
            this.path = path;
        }
        
        public long getFreeSpace() {
            return freeSpace;
        }
        
        public void setFreeSpace(long freeSpace) {
            this.freeSpace = freeSpace;
        }
    }
} 