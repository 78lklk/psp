package client;

import client.service.AuthService;
import client.ui.LoginViewController;
import client.ui.MainViewController;
import common.dto.AuthResponse;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Главный класс клиентского приложения системы лояльности
 */
public class MainApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);
    private Stage primaryStage;
    private AuthService authService;
    
    @Override
    public void start(Stage primaryStage) {
        try {
            this.primaryStage = primaryStage;
            this.authService = new AuthService();
            
            // Запуск окна логина
            showLoginView();
            
            primaryStage.setTitle("Система лояльности компьютерного клуба");
            primaryStage.show();
            
            logger.info("Приложение запущено");
        } catch (Exception e) {
            logger.error("Ошибка при запуске приложения", e);
        }
    }
    
    /**
     * Показывает окно логина
     */
    public void showLoginView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            LoginViewController controller = loader.getController();
            controller.setMainApp(this);
            controller.setAuthService(authService);
            
            primaryStage.setScene(scene);
        } catch (Exception e) {
            logger.error("Ошибка при загрузке окна логина", e);
        }
    }
    
    /**
     * Показывает главное окно приложения
     * @param authResponse результат аутентификации
     */
    public void showMainView(AuthResponse authResponse) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            MainViewController controller = loader.getController();
            controller.setMainApp(this);
            controller.setAuthResponse(authResponse);
            controller.init();
            
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
        } catch (Exception e) {
            logger.error("Ошибка при загрузке главного окна", e);
        }
    }
    
    /**
     * Выполняет выход из системы
     */
    public void logout() {
        showLoginView();
        primaryStage.setMaximized(false);
        primaryStage.sizeToScene();
    }
    
    public Stage getPrimaryStage() {
        return primaryStage;
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
