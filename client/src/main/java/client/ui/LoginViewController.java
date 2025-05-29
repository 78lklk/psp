package client.ui;

import client.MainApp;
import client.service.AuthService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Контроллер экрана авторизации
 */
public class LoginViewController {
    private static final Logger logger = LoggerFactory.getLogger(LoginViewController.class);
    
    @FXML
    private TextField loginField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private Button loginButton;
    
    private MainApp mainApp;
    private AuthService authService;
    
    /**
     * Инициализация контроллера
     */
    @FXML
    private void initialize() {
        // Настройка обработчика нажатия клавиши Enter в поле пароля
        passwordField.setOnKeyPressed(this::handleKeyPressed);
    }
    
    /**
     * Обрабатывает нажатие клавиш
     * @param event событие нажатия клавиши
     */
    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleLogin();
        }
    }
    
    /**
     * Обрабатывает нажатие кнопки входа
     * @param event событие
     */
    @FXML
    private void handleLoginButtonAction(ActionEvent event) {
        handleLogin();
    }
    
    /**
     * Выполняет вход в систему
     */
    private void handleLogin() {
        String login = loginField.getText().trim();
        String password = passwordField.getText();
        
        if (login.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Ошибка ввода", "Заполните все поля");
            return;
        }
        
        loginButton.setDisable(true);
        
        authService.authenticate(login, password)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        if (response.isSuccess()) {
                            mainApp.showMainView(response);
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Ошибка входа", response.getErrorMessage());
                            loginButton.setDisable(false);
                        }
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Ошибка подключения", 
                                "Не удалось подключиться к серверу");
                        loginButton.setDisable(false);
                    });
                    return null;
                });
    }
    
    /**
     * Показывает диалоговое окно
     * @param type тип окна
     * @param title заголовок
     * @param message сообщение
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }
    
    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }
} 