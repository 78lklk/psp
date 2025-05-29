-- ==========================================
-- ПОЛНАЯ ИНИЦИАЛИЗАЦИЯ БАЗЫ ДАННЫХ
-- СИСТЕМА ЛОЯЛЬНОСТИ КОМПЬЮТЕРНОГО КЛУБА
-- ==========================================

-- Подключение к серверу PostgreSQL (выполнить отдельно)
-- CREATE DATABASE loyalty_db;
-- \c loyalty_db;

-- Установка кодировки
SET client_encoding = 'UTF8';

-- ==========================================
-- УДАЛЕНИЕ СУЩЕСТВУЮЩИХ ТАБЛИЦ
-- ==========================================

DROP TABLE IF EXISTS audit_log CASCADE;
DROP TABLE IF EXISTS offline_queue CASCADE;
DROP TABLE IF EXISTS report_cache CASCADE;
DROP TABLE IF EXISTS backups CASCADE;
DROP TABLE IF EXISTS card_promotions CASCADE;
DROP TABLE IF EXISTS promo_codes CASCADE;
DROP TABLE IF EXISTS promotions CASCADE;
DROP TABLE IF EXISTS transactions CASCADE;
DROP TABLE IF EXISTS sessions CASCADE;
DROP TABLE IF EXISTS cards CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS tiers CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
DROP TABLE IF EXISTS settings CASCADE;

-- ==========================================
-- СОЗДАНИЕ ТАБЛИЦ
-- ==========================================

-- Роли пользователей
CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Пользователи системы
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    login VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,  -- Пароли в прямом виде
    role_id INTEGER NOT NULL REFERENCES roles(id),
    full_name VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(20),
    registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

-- Уровни лояльности
CREATE TABLE tiers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    min_points INTEGER NOT NULL,
    discount_pct INTEGER NOT NULL,
    description TEXT,
    color VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Карты клиентов
CREATE TABLE cards (
    id SERIAL PRIMARY KEY,
    number VARCHAR(50) NOT NULL UNIQUE,
    user_id INTEGER NOT NULL REFERENCES users(id),
    tier_id INTEGER NOT NULL REFERENCES tiers(id),
    points INTEGER NOT NULL DEFAULT 0,
    issue_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expiry_date TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    total_spent DECIMAL(10, 2) DEFAULT 0,
    total_sessions INTEGER DEFAULT 0
);

-- Игровые сессии
CREATE TABLE sessions (
    id SERIAL PRIMARY KEY,
    card_id INTEGER NOT NULL REFERENCES cards(id),
    user_id INTEGER NOT NULL REFERENCES users(id),
    start_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    minutes INTEGER NOT NULL,
    points_earned INTEGER DEFAULT 0,
    price DECIMAL(10, 2) NOT NULL DEFAULT 0,
    computer_number INTEGER,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_by INTEGER REFERENCES users(id),
    notes TEXT
);

-- Транзакции по баллам
CREATE TABLE transactions (
    id SERIAL PRIMARY KEY,
    card_id INTEGER NOT NULL REFERENCES cards(id),
    type VARCHAR(20) NOT NULL, -- 'EARN', 'REDEEM', 'ADJUST', 'BONUS'
    points INTEGER NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(255),
    operator_id INTEGER REFERENCES users(id),
    session_id INTEGER REFERENCES sessions(id),
    promotion_id INTEGER,
    promo_code_id INTEGER
);

-- Акции и промо-кампании
CREATE TABLE promotions (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    bonus_pct INTEGER DEFAULT 0,
    bonus_points INTEGER DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by INTEGER REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    max_uses INTEGER,
    min_points_required INTEGER DEFAULT 0,
    usage_count INTEGER DEFAULT 0
);

-- Участие карт в акциях
CREATE TABLE card_promotions (
    id SERIAL PRIMARY KEY,
    card_id INTEGER NOT NULL REFERENCES cards(id),
    promotion_id INTEGER NOT NULL REFERENCES promotions(id),
    activation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_points INTEGER DEFAULT 0,
    bonus_points INTEGER DEFAULT 0,
    is_completed BOOLEAN DEFAULT FALSE,
    UNIQUE(card_id, promotion_id)
);

-- Промокоды
CREATE TABLE promo_codes (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    promotion_id INTEGER REFERENCES promotions(id),
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    used_by INTEGER REFERENCES cards(id),
    used_date TIMESTAMP,
    expiry_date DATE,
    created_by INTEGER REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    bonus_points INTEGER DEFAULT 0,
    discount_percent DECIMAL(5, 2) DEFAULT 0,
    uses_limit INTEGER DEFAULT 1,
    uses_count INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE
);

-- Настройки системы
CREATE TABLE settings (
    id SERIAL PRIMARY KEY,
    key VARCHAR(50) NOT NULL UNIQUE,
    value TEXT NOT NULL,
    description VARCHAR(255),
    category VARCHAR(50) DEFAULT 'general',
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by INTEGER REFERENCES users(id)
);

-- Резервные копии
CREATE TABLE backups (
    id SERIAL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by INTEGER REFERENCES users(id),
    file_size BIGINT NOT NULL,
    file_path VARCHAR(500),
    backup_type VARCHAR(50) DEFAULT 'manual',
    is_valid BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT
);

-- Кэш отчетов
CREATE TABLE report_cache (
    id SERIAL PRIMARY KEY,
    report_type VARCHAR(50) NOT NULL,
    parameters TEXT NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    data TEXT NOT NULL,
    generated_by INTEGER REFERENCES users(id),
    UNIQUE(report_type, parameters)
);

-- Очередь офлайн операций
CREATE TABLE offline_queue (
    id SERIAL PRIMARY KEY,
    action_type VARCHAR(50) NOT NULL,
    data TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3
);

-- Журнал аудита
CREATE TABLE audit_log (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id),
    action_type VARCHAR(50) NOT NULL,
    action_details TEXT,
    ip_address VARCHAR(50),
    user_agent TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    target_entity VARCHAR(50),
    target_id INTEGER,
    old_values TEXT,
    new_values TEXT
);

-- ==========================================
-- СОЗДАНИЕ ИНДЕКСОВ ДЛЯ ПРОИЗВОДИТЕЛЬНОСТИ
-- ==========================================

CREATE INDEX idx_cards_number ON cards(number);
CREATE INDEX idx_cards_user_id ON cards(user_id);
CREATE INDEX idx_sessions_card_id ON sessions(card_id);
CREATE INDEX idx_sessions_start_time ON sessions(start_time);
CREATE INDEX idx_sessions_status ON sessions(status);
CREATE INDEX idx_transactions_card_id ON transactions(card_id);
CREATE INDEX idx_transactions_timestamp ON transactions(timestamp);
CREATE INDEX idx_transactions_type ON transactions(type);
CREATE INDEX idx_promotions_dates ON promotions(start_date, end_date);
CREATE INDEX idx_promotions_active ON promotions(is_active);
CREATE INDEX idx_promo_codes_code ON promo_codes(code);
CREATE INDEX idx_promo_codes_active ON promo_codes(is_active);
CREATE INDEX idx_audit_log_timestamp ON audit_log(timestamp);
CREATE INDEX idx_audit_log_user_id ON audit_log(user_id);

-- ==========================================
-- ЗАПОЛНЕНИЕ НАЧАЛЬНЫМИ ДАННЫМИ
-- ==========================================

-- Роли пользователей
INSERT INTO roles (id, name, description) VALUES 
(1, 'ADMIN', 'Администратор системы с полными правами'),
(2, 'MANAGER', 'Менеджер с правами управления клиентами и отчетами'),
(3, 'STAFF', 'Сотрудник с базовыми правами обслуживания'),
(4, 'CLIENT', 'Клиент компьютерного клуба');

-- Уровни лояльности
INSERT INTO tiers (id, name, min_points, discount_pct, description, color) VALUES 
(1, 'Бронза', 0, 0, 'Начальный уровень для всех новых клиентов', '#CD7F32'),
(2, 'Серебро', 100, 5, 'Уровень для постоянных клиентов', '#C0C0C0'),
(3, 'Золото', 500, 10, 'Уровень для активных игроков', '#FFD700'),
(4, 'Платина', 1000, 15, 'Уровень для лояльных клиентов', '#E5E4E2'),
(5, 'Алмаз', 2000, 20, 'Элитный статус с максимальными привилегиями', '#B9F2FF');

-- Пользователи системы (пароли в прямом виде)
INSERT INTO users (id, login, password, role_id, full_name, email, phone, is_active) VALUES 
(1, 'admin', 'admin123', 1, 'Системный Администратор', 'admin@gameclub.ru', '+7-900-123-45-67', true),
(2, 'manager', 'manager123', 2, 'Иван Петров', 'manager@gameclub.ru', '+7-900-123-45-68', true),
(3, 'staff1', 'staff123', 3, 'Мария Сидорова', 'staff1@gameclub.ru', '+7-900-123-45-69', true),
(4, 'staff2', 'staff123', 3, 'Алексей Козлов', 'staff2@gameclub.ru', '+7-900-123-45-70', true),
(5, 'client1', 'password123', 4, 'Дмитрий Волков', 'dmitry.volkov@email.ru', '+7-911-234-56-78', true),
(6, 'client2', 'password123', 4, 'Елена Романова', 'elena.romanova@email.ru', '+7-911-234-56-79', true),
(7, 'admin1', 'admin123', 4, 'Тестовый Пользователь', 'test@email.ru', '+7-911-234-56-80', true),
(8, 'afsdas', 'password123', 4, 'Анна Смирнова', 'anna.smirnova@email.ru', '+7-911-234-56-81', true),
(9, 'sadfafsd', 'password123', 4, 'Сергей Николаев', 'sergey.nikolaev@email.ru', '+7-911-234-56-82', true),
(10, 'viktor_game', 'viktor123', 4, 'Виктор Игроков', 'viktor@email.ru', '+7-911-234-56-83', true),
(11, 'sadfщ', 'password123', 4, 'Олег Тестеров', 'oleg@email.ru', '+7-911-234-56-84', true);

-- Карты клиентов
INSERT INTO cards (id, number, user_id, tier_id, points, total_spent, total_sessions, is_active) VALUES 
(1, 'LC001', 5, 2, 150, 5500.00, 45, true),
(2, 'LC002', 6, 3, 750, 12300.00, 89, true),
(3, 'LC003', 7, 1, 25, 800.00, 12, true),
(4, 'LC004', 8, 2, 320, 6800.00, 52, true),
(5, 'LC005', 9, 1, 45, 1200.00, 18, true),
(6, 'LC006', 10, 4, 1250, 25600.00, 156, true),
(7, 'LC007', 11, 1, 15, 450.00, 8, true),
(8, 'LC008', 1, 5, 2500, 50000.00, 300, true),
(9, 'LC009', 2, 4, 1500, 30000.00, 200, true),
(10, 'LC010', 3, 3, 800, 15000.00, 120, true);

-- Акции
INSERT INTO promotions (id, name, description, start_date, end_date, bonus_pct, bonus_points, is_active, created_by, max_uses, min_points_required, usage_count) VALUES 
(1, 'Выходные x2', 'Двойные баллы в выходные дни', '2025-01-01', '2025-12-31', 100, 0, true, 1, 1000, 0, 45),
(2, 'Ночная смена', 'Бонус за игру с 22:00 до 06:00', '2025-01-15', '2025-06-15', 50, 0, true, 1, 500, 0, 23),
(3, 'Новичок', 'Приветственный бонус для новых клиентов', '2025-01-01', '2025-12-31', 0, 100, true, 1, null, 0, 12),
(4, 'Лояльный клиент', 'Бонус для клиентов с картой более 6 месяцев', '2025-02-01', '2025-08-31', 25, 50, true, 2, 200, 100, 67),
(5, 'Турнирные очки', 'Дополнительные баллы за участие в турнирах', '2025-03-01', '2025-05-31', 0, 200, false, 1, 50, 500, 8);

-- Участие в акциях
INSERT INTO card_promotions (card_id, promotion_id, activation_date, used_points, bonus_points, is_completed) VALUES 
(1, 1, '2025-01-15 14:30:00', 0, 75, false),
(2, 1, '2025-01-20 16:45:00', 0, 150, false),
(6, 2, '2025-02-01 23:15:00', 0, 45, true),
(3, 3, '2025-01-05 10:00:00', 0, 100, true),
(5, 3, '2025-01-12 11:30:00', 0, 100, true),
(2, 4, '2025-02-15 13:20:00', 50, 75, false),
(6, 4, '2025-02-18 15:45:00', 100, 125, true),
(6, 5, '2025-03-05 18:00:00', 200, 200, true);

-- Промокоды
INSERT INTO promo_codes (id, code, promotion_id, is_used, used_by, used_date, expiry_date, created_by, bonus_points, discount_percent, uses_limit, uses_count, is_active) VALUES 
(1, 'WELCOME2025', 3, true, 3, '2025-01-05 10:00:00', '2025-12-31', 1, 100, 0, 1, 1, true),
(2, 'NIGHT50', 2, false, null, null, '2025-06-15', 1, 0, 50, 10, 0, true),
(3, 'WEEKEND20', 1, true, 1, '2025-01-15 14:30:00', '2025-12-31', 1, 0, 20, 100, 1, true),
(4, 'BONUS100', null, false, null, null, '2025-08-31', 2, 100, 0, 50, 0, true),
(5, 'DISCOUNT15', null, true, 2, '2025-02-20 16:45:00', '2025-07-31', 1, 0, 15, 25, 1, true),
(6, 'NEWBIE50', 3, true, 5, '2025-01-12 11:30:00', '2025-12-31', 1, 50, 0, 1, 1, true),
(7, 'LOYALTY25', 4, false, null, null, '2025-08-31', 2, 25, 25, 200, 0, true);

-- Игровые сессии
INSERT INTO sessions (id, card_id, user_id, start_time, end_time, minutes, points_earned, price, computer_number, status, created_by) VALUES 
(1, 1, 5, '2025-01-15 14:00:00', '2025-01-15 16:30:00', 150, 15, 450.00, 1, 'COMPLETED', 3),
(2, 2, 6, '2025-01-20 16:00:00', '2025-01-20 19:45:00', 225, 22, 675.00, 3, 'COMPLETED', 3),
(3, 6, 10, '2025-02-01 22:30:00', '2025-02-02 01:15:00', 165, 25, 495.00, 5, 'COMPLETED', 4),
(4, 3, 7, '2025-01-05 10:00:00', '2025-01-05 12:00:00', 120, 12, 360.00, 2, 'COMPLETED', 3),
(5, 5, 9, '2025-01-12 11:00:00', '2025-01-12 13:30:00', 150, 15, 450.00, 4, 'COMPLETED', 4),
(6, 8, 1, '2025-02-10 15:00:00', '2025-02-10 20:00:00', 300, 60, 900.00, 7, 'COMPLETED', 3),
(7, 9, 2, '2025-02-12 18:00:00', '2025-02-12 22:30:00', 270, 54, 810.00, 8, 'COMPLETED', 4),
(8, 10, 3, '2025-02-15 13:00:00', '2025-02-15 17:00:00', 240, 48, 720.00, 6, 'COMPLETED', 3),
(9, 4, 8, '2025-02-18 14:30:00', null, 0, 0, 0, 9, 'ACTIVE', 3),
(10, 7, 11, '2025-02-20 16:00:00', '2025-02-20 17:30:00', 90, 9, 270.00, 10, 'COMPLETED', 4);

-- Транзакции
INSERT INTO transactions (id, card_id, type, points, timestamp, description, operator_id, session_id, promotion_id, promo_code_id) VALUES 
(1, 1, 'EARN', 15, '2025-01-15 16:30:00', 'Заработано за сессию', 3, 1, null, null),
(2, 1, 'BONUS', 75, '2025-01-15 16:30:00', 'Бонус выходного дня', 3, 1, 1, 3),
(3, 2, 'EARN', 22, '2025-01-20 19:45:00', 'Заработано за сессию', 3, 2, null, null),
(4, 2, 'BONUS', 150, '2025-01-20 19:45:00', 'Бонус выходного дня', 3, 2, 1, null),
(5, 6, 'EARN', 25, '2025-02-02 01:15:00', 'Заработано за сессию', 4, 3, null, null),
(6, 6, 'BONUS', 45, '2025-02-02 01:15:00', 'Ночной бонус', 4, 3, 2, null),
(7, 3, 'EARN', 12, '2025-01-05 12:00:00', 'Заработано за сессию', 3, 4, null, null),
(8, 3, 'BONUS', 100, '2025-01-05 12:00:00', 'Приветственный бонус', 3, 4, 3, 1),
(9, 5, 'EARN', 15, '2025-01-12 13:30:00', 'Заработано за сессию', 4, 5, null, null),
(10, 5, 'BONUS', 100, '2025-01-12 13:30:00', 'Приветственный бонус', 4, 5, 3, 6),
(11, 8, 'EARN', 60, '2025-02-10 20:00:00', 'Заработано за сессию', 3, 6, null, null),
(12, 9, 'EARN', 54, '2025-02-12 22:30:00', 'Заработано за сессию', 4, 7, null, null),
(13, 10, 'EARN', 48, '2025-02-15 17:00:00', 'Заработано за сессию', 3, 8, null, null),
(14, 7, 'EARN', 9, '2025-02-20 17:30:00', 'Заработано за сессию', 4, 10, null, null),
(15, 2, 'REDEEM', -50, '2025-02-15 13:20:00', 'Погашение баллов для участия в акции', 2, null, 4, null),
(16, 2, 'BONUS', 75, '2025-02-15 13:20:00', 'Бонус лояльного клиента', 2, null, 4, null),
(17, 6, 'REDEEM', -100, '2025-02-18 15:45:00', 'Погашение баллов для участия в акции', 2, null, 4, null),
(18, 6, 'BONUS', 125, '2025-02-18 15:45:00', 'Бонус лояльного клиента', 2, null, 4, null),
(19, 6, 'REDEEM', -200, '2025-03-05 18:00:00', 'Участие в турнире', 1, null, 5, null),
(20, 6, 'BONUS', 200, '2025-03-05 18:00:00', 'Турнирные очки', 1, null, 5, null);

-- Настройки системы
INSERT INTO settings (key, value, description, category, updated_by) VALUES 
('points_per_hour', '10', 'Количество баллов за час игры', 'loyalty', 1),
('price_per_hour', '180', 'Стоимость часа игры (в рублях)', 'pricing', 1),
('max_session_hours', '12', 'Максимальная длительность сессии (часы)', 'gaming', 1),
('backup_retention_days', '30', 'Количество дней хранения резервных копий', 'system', 1),
('report_cache_hours', '24', 'Время кэширования отчетов (часы)', 'system', 1),
('theme', 'light', 'Тема интерфейса по умолчанию', 'ui', 1),
('language', 'ru', 'Язык системы по умолчанию', 'ui', 1),
('session_timeout', '3600', 'Время бездействия до автоматического завершения сессии (секунды)', 'security', 1),
('min_password_length', '6', 'Минимальная длина пароля', 'security', 1),
('club_name', 'GameZone Pro', 'Название компьютерного клуба', 'general', 1),
('club_address', 'г. Москва, ул. Геймерская, д. 42', 'Адрес клуба', 'general', 1),
('club_phone', '+7-495-123-45-67', 'Телефон клуба', 'general', 1),
('notification_email', 'admin@gameclub.ru', 'Email для системных уведомлений', 'general', 1),
('loyalty_multiplier_weekend', '2.0', 'Множитель баллов в выходные', 'loyalty', 1),
('loyalty_multiplier_night', '1.5', 'Множитель баллов в ночное время (22:00-06:00)', 'loyalty', 1);

-- ==========================================
-- ОБНОВЛЕНИЕ ПОСЛЕДОВАТЕЛЬНОСТЕЙ
-- ==========================================

SELECT setval('roles_id_seq', (SELECT MAX(id) FROM roles));
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('tiers_id_seq', (SELECT MAX(id) FROM tiers));
SELECT setval('cards_id_seq', (SELECT MAX(id) FROM cards));
SELECT setval('sessions_id_seq', (SELECT MAX(id) FROM sessions));
SELECT setval('transactions_id_seq', (SELECT MAX(id) FROM transactions));
SELECT setval('promotions_id_seq', (SELECT MAX(id) FROM promotions));
SELECT setval('promo_codes_id_seq', (SELECT MAX(id) FROM promo_codes));
SELECT setval('settings_id_seq', (SELECT MAX(id) FROM settings));

-- ==========================================
-- СОЗДАНИЕ ПРЕДСТАВЛЕНИЙ ДЛЯ ОТЧЕТОВ
-- ==========================================

-- Представление для статистики карт
CREATE OR REPLACE VIEW v_card_statistics AS
SELECT 
    c.id,
    c.number,
    u.full_name,
    u.email,
    t.name as tier_name,
    c.points,
    c.total_spent,
    c.total_sessions,
    c.issue_date,
    EXTRACT(DAYS FROM (CURRENT_TIMESTAMP - c.issue_date)) as days_since_issue,
    CASE 
        WHEN c.total_sessions > 0 THEN ROUND(c.total_spent / c.total_sessions, 2)
        ELSE 0 
    END as avg_session_cost
FROM cards c
JOIN users u ON c.user_id = u.id
JOIN tiers t ON c.tier_id = t.id
WHERE c.is_active = true;

-- Представление для активных акций
CREATE OR REPLACE VIEW v_active_promotions AS
SELECT 
    p.*,
    u.full_name as created_by_name,
    COUNT(cp.id) as participants_count,
    COALESCE(SUM(cp.bonus_points), 0) as total_bonus_given
FROM promotions p
LEFT JOIN users u ON p.created_by = u.id
LEFT JOIN card_promotions cp ON p.id = cp.promotion_id
WHERE p.is_active = true 
    AND p.start_date <= CURRENT_DATE 
    AND p.end_date >= CURRENT_DATE
GROUP BY p.id, u.full_name;

-- Представление для статистики пользователей
CREATE OR REPLACE VIEW v_user_activity AS
SELECT 
    u.id,
    u.login,
    u.full_name,
    r.name as role_name,
    u.registration_date,
    u.last_login,
    CASE 
        WHEN u.role_id = 4 THEN (
            SELECT COUNT(*) 
            FROM sessions s 
            JOIN cards c ON s.card_id = c.id 
            WHERE c.user_id = u.id
        )
        ELSE 0
    END as total_sessions,
    CASE 
        WHEN u.role_id = 4 THEN (
            SELECT COALESCE(SUM(points), 0) 
            FROM cards 
            WHERE user_id = u.id AND is_active = true
        )
        ELSE 0
    END as total_points
FROM users u
JOIN roles r ON u.role_id = r.id
WHERE u.is_active = true;

-- ==========================================
-- ФУНКЦИИ ДЛЯ АВТОМАТИЧЕСКИХ ОПЕРАЦИЙ
-- ==========================================

-- Функция для автоматического обновления уровня карты
CREATE OR REPLACE FUNCTION update_card_tier() RETURNS TRIGGER AS $$
DECLARE
    new_tier_id INTEGER;
BEGIN
    -- Определяем новый уровень на основе количества баллов
    SELECT id INTO new_tier_id
    FROM tiers 
    WHERE NEW.points >= min_points 
    ORDER BY min_points DESC 
    LIMIT 1;
    
    -- Обновляем уровень если он изменился
    IF new_tier_id IS NOT NULL AND new_tier_id != NEW.tier_id THEN
        NEW.tier_id = new_tier_id;
        
        -- Логируем изменение уровня
        INSERT INTO audit_log (user_id, action_type, action_details, target_entity, target_id)
        VALUES (
            COALESCE(NEW.user_id, 1),
            'TIER_UPGRADE',
            FORMAT('Карта %s повышена до уровня %s', NEW.number, (SELECT name FROM tiers WHERE id = new_tier_id)),
            'cards',
            NEW.id
        );
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Триггер для автоматического обновления уровня
CREATE TRIGGER trigger_update_card_tier
    BEFORE UPDATE OF points ON cards
    FOR EACH ROW
    EXECUTE FUNCTION update_card_tier();

-- ==========================================
-- ПРАВА ДОСТУПА (при необходимости)
-- ==========================================

-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO loyalty_app;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO loyalty_app;

-- ==========================================
-- ЗАВЕРШЕНИЕ ИНИЦИАЛИЗАЦИИ
-- ==========================================

-- Вывод статистики
SELECT 'Инициализация завершена успешно!' as status;
SELECT 
    'Создано пользователей: ' || COUNT(*) as users_created 
FROM users;
SELECT 
    'Создано карт: ' || COUNT(*) as cards_created 
FROM cards;
SELECT 
    'Создано акций: ' || COUNT(*) as promotions_created 
FROM promotions;
SELECT 
    'Создано промокодов: ' || COUNT(*) as promocodes_created 
FROM promo_codes;

-- Вывод информации о базе данных
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC; 