package client.ui.admin;

import client.service.UserService;
import common.model.User;
import common.model.Role;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.input.KeyCode;

/**
 * Controller for user management (admin panel)
 */
public class UsersViewController {
    private static final Logger logger = LoggerFactory.getLogger(UsersViewController.class);
    
    @FXML
    private TableView<User> usersTable;
    
    @FXML
    private TableColumn<User, Long> idColumn;
    
    @FXML
    private TableColumn<User, String> loginColumn;
    
    @FXML
    private TableColumn<User, String> fullNameColumn;
    
    @FXML
    private TableColumn<User, String> emailColumn;
    
    @FXML
    private TableColumn<User, String> roleColumn;
    
    @FXML
    private TableColumn<User, String> registrationDateColumn;
    
    @FXML
    private TextField searchField;
    
    @FXML
    private Button addUserButton;
    
    @FXML
    private Button editUserButton;
    
    @FXML
    private Button deleteUserButton;
    
    @FXML
    private Button searchButton;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Button closeButton;
    
    private UserService userService;
    private ObservableList<User> usersList = FXCollections.observableArrayList();
    private ObservableList<User> filteredUsersList = FXCollections.observableArrayList();
    private String authToken;
    
    /**
     * Sets the authorization token and initializes the service
     * @param authToken authorization token
     */
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
        userService = new UserService(authToken);
        loadUsers();
    }
    
    /**
     * Initializes the controller
     */
    @FXML
    private void initialize() {
        // Configure the table
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        loginColumn.setCellValueFactory(new PropertyValueFactory<>("login"));
        fullNameColumn.setCellValueFactory(cellData -> {
            User user = cellData.getValue();
            String fullName = user.getFirstName() + " " + user.getLastName();
            return new javafx.beans.property.SimpleStringProperty(fullName.trim());
        });
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        
        // Custom cell value factory for Role object
        roleColumn.setCellValueFactory(cellData -> {
            Role role = cellData.getValue().getRole();
            return new javafx.beans.property.SimpleStringProperty(
                role != null ? role.getName() : "USER");
        });
        
        // Registration date formatting
        registrationDateColumn.setCellValueFactory(cellData -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            return new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getRegistrationDate() != null ? 
                cellData.getValue().getRegistrationDate().format(formatter) : "");
        });
        
        usersTable.setItems(usersList);
        
        // Disable edit and delete buttons when no row is selected
        editUserButton.disableProperty().bind(usersTable.getSelectionModel().selectedItemProperty().isNull());
        deleteUserButton.disableProperty().bind(usersTable.getSelectionModel().selectedItemProperty().isNull());
        
        // Add search field listener for immediate filtering
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            handleSearch();
        });
        
        // Allow pressing Enter in search field
        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleSearch();
            }
        });
    }
    
    /**
     * Loads all users
     */
    private void loadUsers() {
        statusLabel.setText("Loading users...");
        
        userService.getAllUsers()
                .thenAccept(users -> {
                    Platform.runLater(() -> {
                        usersList.clear();
                        usersList.addAll(users);
                        filterUsers(searchField.getText());
                        statusLabel.setText("Total users: " + users.size());
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        logger.error("Error loading users", e);
                        statusLabel.setText("Error loading users");
                        
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Error loading users");
                        alert.setContentText("Failed to load user list: " + e.getMessage());
                        alert.showAndWait();
                    });
                    return null;
                });
    }
    
    /**
     * Handles search button click
     */
    @FXML
    private void handleSearch() {
        String searchText = searchField.getText();
        filterUsers(searchText);
    }
    
    /**
     * Filters users based on search text
     * @param searchText text to search for
     */
    private void filterUsers(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            usersTable.setItems(usersList);
            statusLabel.setText("Total users: " + usersList.size());
            return;
        }
        
        String lowerCaseSearch = searchText.toLowerCase();
        List<User> filtered = usersList.stream()
            .filter(user -> 
                (user.getLogin() != null && user.getLogin().toLowerCase().contains(lowerCaseSearch)) ||
                (user.getFirstName() != null && user.getFirstName().toLowerCase().contains(lowerCaseSearch)) ||
                (user.getLastName() != null && user.getLastName().toLowerCase().contains(lowerCaseSearch)) ||
                (user.getEmail() != null && user.getEmail().toLowerCase().contains(lowerCaseSearch)))
            .collect(Collectors.toList());
        
        filteredUsersList.clear();
        filteredUsersList.addAll(filtered);
        usersTable.setItems(filteredUsersList);
        statusLabel.setText("Found " + filtered.size() + " of " + usersList.size() + " users");
    }
    
    /**
     * Handles add user button click
     */
    @FXML
    private void handleAddUser() {
        User user = new User();
        
        if (showUserEditDialog(user, "Add User")) {
            statusLabel.setText("Creating user...");
            
            userService.createUser(user)
                .thenAccept(createdUser -> {
                    Platform.runLater(() -> {
                        if (createdUser != null) {
                            usersList.add(createdUser);
                            usersTable.getSelectionModel().select(createdUser);
                            filterUsers(searchField.getText());
                            
                            statusLabel.setText("User created successfully");
                            showAlert(Alert.AlertType.INFORMATION, "Success", 
                                    "User created successfully");
                        } else {
                            statusLabel.setText("Error creating user");
                            showAlert(Alert.AlertType.ERROR, "Error", 
                                    "Failed to create user");
                        }
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        logger.error("Error creating user", e);
                        statusLabel.setText("Error creating user");
                        
                        showAlert(Alert.AlertType.ERROR, "Error", 
                                "Failed to create user: " + e.getMessage());
                    });
                    return null;
                });
        }
    }
    
    /**
     * Handles edit user button click
     */
    @FXML
    private void handleEditUser() {
        User selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            return;
        }
        
        // Create a copy of the user to edit
        User editUser = new User();
        editUser.setId(selectedUser.getId());
        editUser.setLogin(selectedUser.getLogin());
        editUser.setEmail(selectedUser.getEmail());
        editUser.setFirstName(selectedUser.getFirstName());
        editUser.setLastName(selectedUser.getLastName());
        if (selectedUser.getRole() != null) {
            Role role = new Role();
            role.setId(selectedUser.getRole().getId());
            role.setName(selectedUser.getRole().getName());
            editUser.setRole(role);
        }
        editUser.setActive(selectedUser.isActive());
        
        if (showUserEditDialog(editUser, "Edit User")) {
            statusLabel.setText("Updating user...");
            
            userService.updateUser(editUser)
                .thenAccept(updatedUser -> {
                    Platform.runLater(() -> {
                        if (updatedUser != null) {
                            int index = usersList.indexOf(selectedUser);
                            if (index >= 0) {
                                usersList.set(index, updatedUser);
                                usersTable.getSelectionModel().select(index);
                            }
                            filterUsers(searchField.getText());
                            
                            statusLabel.setText("User updated successfully");
                            showAlert(Alert.AlertType.INFORMATION, "Success", 
                                    "User updated successfully");
                        } else {
                            statusLabel.setText("Error updating user");
                            showAlert(Alert.AlertType.ERROR, "Error", 
                                    "Failed to update user");
                        }
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        logger.error("Error updating user", e);
                        statusLabel.setText("Error updating user");
                        
                        showAlert(Alert.AlertType.ERROR, "Error", 
                                "Failed to update user: " + e.getMessage());
                    });
                    return null;
                });
        }
    }
    
    /**
     * Handles delete user button click
     */
    @FXML
    private void handleDeleteUser() {
        User selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            return;
        }
        
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Confirmation");
        confirmationAlert.setHeaderText("Delete User");
        confirmationAlert.setContentText("Are you sure you want to delete user " + 
                selectedUser.getLogin() + "?");
        
        Optional<ButtonType> result = confirmationAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            statusLabel.setText("Deleting user...");
            
            userService.deleteUser(selectedUser.getId())
                .thenAccept(success -> {
                    Platform.runLater(() -> {
                        if (success) {
                            usersList.remove(selectedUser);
                            filterUsers(searchField.getText());
                            
                            statusLabel.setText("User deleted successfully");
                            showAlert(Alert.AlertType.INFORMATION, "Success", 
                                    "User deleted successfully");
                        } else {
                            statusLabel.setText("Error deleting user");
                            showAlert(Alert.AlertType.ERROR, "Error", 
                                    "Failed to delete user");
                        }
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        logger.error("Error deleting user", e);
                        statusLabel.setText("Error deleting user");
                        
                        showAlert(Alert.AlertType.ERROR, "Error", 
                                "Failed to delete user: " + e.getMessage());
                    });
                    return null;
                });
        }
    }
    
    /**
     * Shows dialog for editing a user
     * @param user User to edit
     * @param title Dialog title
     * @return true if user was edited, false if cancelled
     */
    private boolean showUserEditDialog(User user, String title) {
        // Create the custom dialog
        Dialog<Boolean> dialog = new Dialog<>();
            dialog.setTitle(title);
        dialog.setHeaderText("Enter user information:");

        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create the form grid and fields
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            TextField loginField = new TextField();
        loginField.setPromptText("Login");
            if (user.getLogin() != null) {
                loginField.setText(user.getLogin());
            }

        TextField firstNameField = new TextField();
        firstNameField.setPromptText("First Name");
        if (user.getFirstName() != null) {
            firstNameField.setText(user.getFirstName());
        }
        
        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Last Name");
        if (user.getLastName() != null) {
            lastNameField.setText(user.getLastName());
            }

            TextField emailField = new TextField();
            emailField.setPromptText("Email");
            if (user.getEmail() != null) {
                emailField.setText(user.getEmail());
            }

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password" + (user.getId() != null ? " (leave empty to keep current)" : ""));
        
            ComboBox<String> roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll("ADMIN", "MANAGER", "STAFF", "USER");
        roleComboBox.setValue(user.getRole() != null ? user.getRole().getName() : "USER");
        
        CheckBox activeCheckBox = new CheckBox("Active");
        activeCheckBox.setSelected(user.isActive());
        
        grid.add(new Label("Login:*"), 0, 0);
            grid.add(loginField, 1, 0);
        grid.add(new Label("First Name:"), 0, 1);
        grid.add(firstNameField, 1, 1);
        grid.add(new Label("Last Name:"), 0, 2);
        grid.add(lastNameField, 1, 2);
        grid.add(new Label("Email:*"), 0, 3);
            grid.add(emailField, 1, 3);
        grid.add(new Label("Password:" + (user.getId() != null ? " (optional)" : "*")), 0, 4);
        grid.add(passwordField, 1, 4);
        grid.add(new Label("Role:*"), 0, 5);
            grid.add(roleComboBox, 1, 5);
        grid.add(activeCheckBox, 1, 6);

            dialog.getDialogPane().setContent(grid);

        // Request focus on the login field by default
        Platform.runLater(loginField::requestFocus);

        // Validate the fields before enabling Save button
            Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(loginField.getText().trim().isEmpty() || emailField.getText().trim().isEmpty());
        
        // Add validation listeners
        loginField.textProperty().addListener((observable, oldValue, newValue) -> {
            saveButton.setDisable(newValue.trim().isEmpty() || emailField.getText().trim().isEmpty() || 
                    (user.getId() == null && passwordField.getText().trim().isEmpty()));
        });
        
        emailField.textProperty().addListener((observable, oldValue, newValue) -> {
            saveButton.setDisable(newValue.trim().isEmpty() || loginField.getText().trim().isEmpty() || 
                    (user.getId() == null && passwordField.getText().trim().isEmpty()));
        });
        
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (user.getId() == null) { // New user, password required
                saveButton.setDisable(newValue.trim().isEmpty() || loginField.getText().trim().isEmpty() || 
                        emailField.getText().trim().isEmpty());
            }
        });
        
        // Convert the result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String login = loginField.getText().trim();
                String firstName = firstNameField.getText().trim();
                String lastName = lastNameField.getText().trim();
                String email = emailField.getText().trim();
                String password = passwordField.getText();
                String roleName = roleComboBox.getValue();
                boolean active = activeCheckBox.isSelected();
                
                if (login.isEmpty() || email.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Error", 
                            "Login and email fields are required");
                    return false;
                }
                
                if (user.getId() == null && password.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Error", 
                            "Password is required for new users");
                    return false;
                }
                
                // Update the user object
                user.setLogin(login);
                user.setFirstName(firstName);
                user.setLastName(lastName);
                user.setEmail(email);
                
                if (!password.isEmpty()) {
                    user.setPassword(password);
                }
                
                // Set role
                Role role = new Role();
                role.setName(roleName);
                // Set role ID based on name
                if ("ADMIN".equals(roleName)) {
                    role.setId(1L);
                } else if ("MANAGER".equals(roleName)) {
                    role.setId(2L);
                } else if ("STAFF".equals(roleName)) {
                    role.setId(3L);
                } else {
                    role.setId(4L); // USER
                }
                
                user.setRole(role);
                user.setActive(active);
                
                return true;
            }
            return false;
        });
        
        Optional<Boolean> result = dialog.showAndWait();
        return result.isPresent() && result.get();
    }
    
    /**
     * Shows an alert dialog
     * @param type Alert type
     * @param title Dialog title
     * @param message Dialog message
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Handles closing the window
     */
    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
} 