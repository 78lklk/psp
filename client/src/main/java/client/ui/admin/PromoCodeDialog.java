package client.ui.admin;

import common.model.PromoCode;
import client.ui.MainViewController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Диалог для создания и редактирования промокодов
 */
public class PromoCodeDialog extends Dialog<PromoCode> {
    
    @FXML
    private TextField codeField;
    
    @FXML
    private TextArea descriptionArea;
    
    @FXML
    private TextField bonusPointsField;
    
    @FXML
    private TextField discountPercentField;
    
    @FXML
    private DatePicker expiryDatePicker;
    
    @FXML
    private TextField usesLimitField;
    
    @FXML
    private CheckBox activeCheckBox;
    
    private PromoCode promoCode;
    
    /**
     * Создает диалог для редактирования промокода
     * @param promoCode промокод для редактирования или null для создания нового
     */
    public PromoCodeDialog(PromoCode promoCode) {
        this.promoCode = promoCode != null ? promoCode : new PromoCode();
        
        setTitle(promoCode != null && promoCode.getId() != null ? "Редактирование промокода" : "Создание промокода");
        
        try {
            // Загрузка интерфейса из FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin/PromoCodeDialog.fxml"));
            loader.setController(this);
            
            DialogPane dialogPane = loader.load();
            setDialogPane(dialogPane);
            
            // Устанавливаем кнопки
            ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
            dialogPane.getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
            
            // При нажатии кнопки "Сохранить" обновляем модель и возвращаем результат
            setResultConverter(buttonType -> {
                if (buttonType == saveButtonType) {
                    updatePromoCodeFromInputs();
                    return promoCode;
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
            throw new RuntimeException("Ошибка при загрузке диалога создания промокода", e);
        }
    }
    
    /**
     * Инициализирует поля диалога текущими значениями промокода
     */
    private void initializeFields() {
        codeField.setText(promoCode.getCode() != null ? promoCode.getCode() : "");
        descriptionArea.setText(promoCode.getDescription() != null ? promoCode.getDescription() : "");
        
        bonusPointsField.setText(promoCode.getBonusPoints() != null ? 
                promoCode.getBonusPoints().toString() : "");
        
        discountPercentField.setText(promoCode.getDiscountPercent() != null ? 
                promoCode.getDiscountPercent().toString() : "");
        
        expiryDatePicker.setValue(promoCode.getExpiryDate() != null ? 
                promoCode.getExpiryDate() : LocalDate.now().plusMonths(3));
        
        usesLimitField.setText(promoCode.getUsesLimit() != null ? 
                promoCode.getUsesLimit().toString() : "");
        
        activeCheckBox.setSelected(promoCode.isActive());
    }
    
    /**
     * Обновляет модель промокода данными из полей ввода
     */
    private void updatePromoCodeFromInputs() {
        promoCode.setCode(codeField.getText().trim());
        promoCode.setDescription(descriptionArea.getText().trim());
        promoCode.setExpiryDate(expiryDatePicker.getValue());
        promoCode.setActive(activeCheckBox.isSelected());
        
        try {
            if (!bonusPointsField.getText().trim().isEmpty()) {
                promoCode.setBonusPoints(Integer.parseInt(bonusPointsField.getText().trim()));
            } else {
                promoCode.setBonusPoints(null);
            }
        } catch (NumberFormatException e) {
            promoCode.setBonusPoints(null);
        }
        
        try {
            if (!discountPercentField.getText().trim().isEmpty()) {
                promoCode.setDiscountPercent(Double.parseDouble(discountPercentField.getText().trim()));
            } else {
                promoCode.setDiscountPercent(null);
            }
        } catch (NumberFormatException e) {
            promoCode.setDiscountPercent(null);
        }
        
        try {
            if (!usesLimitField.getText().trim().isEmpty()) {
                promoCode.setUsesLimit(Integer.parseInt(usesLimitField.getText().trim()));
            } else {
                promoCode.setUsesLimit(null);
            }
        } catch (NumberFormatException e) {
            promoCode.setUsesLimit(null);
        }
    }
    
    /**
     * Проверяет корректность введенных данных
     * @return true, если данные корректны
     */
    private boolean validateInputs() {
        if (codeField.getText().trim().isEmpty()) {
            showValidationError("Код промокода не может быть пустым");
            return false;
        }
        
        if (expiryDatePicker.getValue() == null) {
            showValidationError("Необходимо указать срок действия промокода");
            return false;
        }
        
        if (expiryDatePicker.getValue().isBefore(LocalDate.now())) {
            showValidationError("Срок действия не может быть раньше текущей даты");
            return false;
        }
        
        // Проверка, что указан хотя бы один из типов бонуса
        if (bonusPointsField.getText().trim().isEmpty() && discountPercentField.getText().trim().isEmpty()) {
            showValidationError("Необходимо указать либо бонусные баллы, либо процент скидки");
            return false;
        }
        
        // Проверка числовых полей
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
        
        if (!discountPercentField.getText().trim().isEmpty()) {
            try {
                double percent = Double.parseDouble(discountPercentField.getText().trim());
                if (percent < 0 || percent > 100) {
                    showValidationError("Процент скидки должен быть от 0 до 100");
                    return false;
                }
            } catch (NumberFormatException e) {
                showValidationError("Процент скидки должен быть числом");
                return false;
            }
        }
        
        if (!usesLimitField.getText().trim().isEmpty()) {
            try {
                int uses = Integer.parseInt(usesLimitField.getText().trim());
                if (uses < 0) {
                    showValidationError("Лимит использований не может быть отрицательным");
                    return false;
                }
            } catch (NumberFormatException e) {
                showValidationError("Лимит использований должен быть целым числом");
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