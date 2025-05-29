package client.ui.admin;

import common.model.Promotion;
import client.ui.MainViewController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Диалог для создания и редактирования акций
 */
public class PromotionDialog extends Dialog<Promotion> {
    
    @FXML
    private TextField nameField;
    
    @FXML
    private TextArea descriptionArea;
    
    @FXML
    private DatePicker startDatePicker;
    
    @FXML
    private DatePicker endDatePicker;
    
    @FXML
    private CheckBox activeCheckBox;
    
    @FXML
    private TextField bonusPercentField;
    
    @FXML
    private TextField bonusPointsField;
    
    private Promotion promotion;
    
    /**
     * Создает диалог для редактирования акции
     * @param promotion акция для редактирования или null для создания новой
     */
    public PromotionDialog(Promotion promotion) {
        this.promotion = promotion != null ? promotion : new Promotion();
        
        setTitle(promotion != null && promotion.getId() != null ? "Редактирование акции" : "Создание акции");
        
        try {
            // Загрузка интерфейса из FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin/PromotionDialog.fxml"));
            loader.setController(this);
            
            DialogPane dialogPane = loader.load();
            setDialogPane(dialogPane);
            
            // Устанавливаем кнопки
            ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
            dialogPane.getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
            
            // При нажатии кнопки "Сохранить" обновляем модель и возвращаем результат
            setResultConverter(buttonType -> {
                if (buttonType == saveButtonType) {
                    updatePromotionFromInputs();
                    return promotion;
                }
                return null;
            });
            
            // Инициализация полей текущими значениями
            initializeFields();
            
            // Применяем глобальную тему к диалогу
            MainViewController.applyGlobalThemeToWindow(dialogPane.getScene());
            
            // Валидация при нажатии кнопки "Сохранить"
            Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);
            saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                if (!validateInputs()) {
                    event.consume();
                }
            });
            
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Ошибка при загрузке диалога создания акции", e);
        }
    }
    
    /**
     * Инициализирует поля диалога текущими значениями акции
     */
    private void initializeFields() {
        nameField.setText(promotion.getName() != null ? promotion.getName() : "");
        descriptionArea.setText(promotion.getDescription() != null ? promotion.getDescription() : "");
        
        startDatePicker.setValue(promotion.getStartDate() != null ? 
                promotion.getStartDate() : LocalDate.now());
        
        endDatePicker.setValue(promotion.getEndDate() != null ? 
                promotion.getEndDate() : LocalDate.now().plusMonths(1));
        
        activeCheckBox.setSelected(promotion.isActive());
        
        bonusPercentField.setText(promotion.getBonusPercent() != null ? 
                promotion.getBonusPercent().toString() : "");
        
        bonusPointsField.setText(promotion.getBonusPoints() != null ? 
                promotion.getBonusPoints().toString() : "");
    }
    
    /**
     * Обновляет модель акции данными из полей ввода
     */
    private void updatePromotionFromInputs() {
        promotion.setName(nameField.getText().trim());
        promotion.setDescription(descriptionArea.getText().trim());
        promotion.setStartDate(startDatePicker.getValue());
        promotion.setEndDate(endDatePicker.getValue());
        promotion.setActive(activeCheckBox.isSelected());
        
        try {
            if (!bonusPercentField.getText().trim().isEmpty()) {
                promotion.setBonusPercent(Integer.parseInt(bonusPercentField.getText().trim()));
            } else {
                promotion.setBonusPercent(null);
            }
        } catch (NumberFormatException e) {
            promotion.setBonusPercent(null);
        }
        
        try {
            if (!bonusPointsField.getText().trim().isEmpty()) {
                promotion.setBonusPoints(Integer.parseInt(bonusPointsField.getText().trim()));
            } else {
                promotion.setBonusPoints(null);
            }
        } catch (NumberFormatException e) {
            promotion.setBonusPoints(null);
        }
    }
    
    /**
     * Проверяет корректность введенных данных
     * @return true, если данные корректны
     */
    private boolean validateInputs() {
        if (nameField.getText().trim().isEmpty()) {
            showValidationError("Название акции не может быть пустым");
            return false;
        }
        
        if (startDatePicker.getValue() == null) {
            showValidationError("Необходимо указать дату начала акции");
            return false;
        }
        
        if (endDatePicker.getValue() == null) {
            showValidationError("Необходимо указать дату окончания акции");
            return false;
        }
        
        if (endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
            showValidationError("Дата окончания не может быть раньше даты начала");
            return false;
        }
        
        // Проверка числовых полей
        if (!bonusPercentField.getText().trim().isEmpty()) {
            try {
                int percent = Integer.parseInt(bonusPercentField.getText().trim());
                if (percent < 0 || percent > 100) {
                    showValidationError("Процент бонуса должен быть от 0 до 100");
                    return false;
                }
            } catch (NumberFormatException e) {
                showValidationError("Процент бонуса должен быть целым числом");
                return false;
            }
        }
        
        if (!bonusPointsField.getText().trim().isEmpty()) {
            try {
                int points = Integer.parseInt(bonusPointsField.getText().trim());
                if (points < 0) {
                    showValidationError("Количество бонусных баллов не может быть отрицательным");
                    return false;
                }
            } catch (NumberFormatException e) {
                showValidationError("Количество бонусных баллов должно быть целым числом");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Показывает окно с ошибкой валидации
     * @param message сообщение об ошибке
     */
    private void showValidationError(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Ошибка валидации");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
} 