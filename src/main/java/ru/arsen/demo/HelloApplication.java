package ru.arsen.tpr452;// PROMETHEEApp.java
import javafx.application.Application;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;

import java.util.*;

public class HelloApplication extends Application {

    private TextField alternativesInput;
    private TextField criteriaInput;
    private TextField weightsInput;
    private TableView<List<Double>> dataTable;
    private TextArea resultsText;
    private Label resultOutput;
    private Spinner<Integer> countCrit;
    private Spinner<Integer> countAlter;
    private TableView<List<String>> tableCrit;
    private TableView<List<String>> tableAlter;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("PROMETHEE Method");

        // Основной layout
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));

        // Поля для ввода данных
        countCrit = new Spinner<>(1, 10, 3);  // Default to 3 criteria
        countAlter = new Spinner<>(1, 10, 3); // Default to 3 alternatives

        Label crtLabel = new Label("Количество критерий:");
        Label altLabel = new Label("Количество альтернатив:");

        // Initialize the table
        tableCrit = new TableView<>();
        tableCrit.setEditable(true);

        tableCrit.setPrefSize(300, 90);

        tableAlter = new TableView<>();
        tableAlter.setEditable(true);
        tableAlter.setPrefSize(300,60);



        // Button to create the table
        Button btnCreateTableCrit = new Button("Ввести данные");
        btnCreateTableCrit.setOnAction(event -> {createCritTable(); createAlterTable();});



        // Layout for spinners
        FlowPane valueCrt = new FlowPane(10, 10, crtLabel, countCrit);
        FlowPane valueAlter = new FlowPane(10, 10, altLabel, countAlter);

        // Combine spinner layouts
        FlowPane value = new FlowPane(40, 10, valueAlter, valueCrt);

        // Overall layout
        VBox root = new VBox(10, value, btnCreateTableCrit, tableCrit,tableAlter);
        root.setPadding(new Insets(10));

        layout.getChildren().addAll(root);

        // Кнопка для создания таблицы
        Button createTableButton = new Button("Создать таблицу");
        createTableButton.setOnAction(e -> createDataTable());
        layout.getChildren().add(createTableButton);

        // Таблица для ввода данных
        dataTable = new TableView<>();
        dataTable.setPrefHeight(230);
        layout.getChildren().add(dataTable);

        // Кнопка для расчета
        Button calculateButton = new Button("Рассчитать");
        calculateButton.setOnAction(e -> calculate());
        layout.getChildren().add(calculateButton);

        // Поле для вывода промежуточных результатов
        resultsText = new TextArea();
        resultsText.setEditable(false);
        resultsText.setPrefHeight(350);
//        layout.getChildren().add(resultsText);

        // Поле для вывода итоговых результатов
//        Label resultLabel = new Label("Итоговые результаты:");
        resultOutput = new Label();
        layout.getChildren().addAll(resultOutput);

        // Установка сцены
        Scene scene = new Scene(layout, 1000, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void createDataTable() {
        try {

            String[] alternatives = getFirstRowAlt().toArray(new String[0]);
            String[] criteria = getFirstRowCr().toArray(new String[0]);

            if (alternatives.length == 0 || criteria.length == 0) {
                showAlert("Ошибка1", "Введите альтернативы и критерии перед созданием таблицы.");
                return;
            }

            dataTable.getColumns().clear();
            dataTable.getItems().clear();




            // Создаем колонки для таблицы
            for (String criterion : criteria) {
                TableColumn<List<Double>, Double> column = new TableColumn<>(criterion.trim());
                int colIndex = dataTable.getColumns().size();

                // Устанавливаем фабрику ячеек для вывода значений
                column.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().get(colIndex)));

                // Устанавливаем фабрику для редактирования ячеек
                column.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.DoubleStringConverter()));

                // Обрабатываем изменение данных в таблице
                column.setOnEditCommit(event -> {
                    List<Double> rowData = event.getRowValue();
                    rowData.set(colIndex, event.getNewValue()); // Обновляем значение в строке
                });

                dataTable.getColumns().add(column);
            }

            // Добавляем строки для каждой альтернативы
            for (String alternative : alternatives) {
                List<Double> row = new ArrayList<>(Collections.nCopies(criteria.length, 0.0));
                dataTable.getItems().add(row);
            }

            // Делаем таблицу редактируемой
            dataTable.setEditable(true);

        } catch (Exception e) {
            showAlert("Ошибка2", e.getMessage());
        }
    }

    private void calculate() {
        try {
            String[] alternatives = getFirstRowAlt().toArray(new String[0]);
            String[] criteria = getFirstRowCr().toArray(new String[0]);
            String[] weightsStr =getSecondRowCr().toArray(new String[0]);
            if (weightsStr.length != criteria.length) {
                throw new IllegalArgumentException("Количество весов должно совпадать с количеством критериев.");
            }
            double[] weights = Arrays.stream(weightsStr).mapToDouble(Double::parseDouble).toArray();

            // Считываем данные из таблицы
            List<List<Double>> data = new ArrayList<>();
            for (int i = 0; i < dataTable.getItems().size(); i++) {
                List<Double> row = new ArrayList<>();
                for (int j = 0; j < dataTable.getColumns().size(); j++) {
                    row.add((Double) dataTable.getColumns().get(j).getCellData(i));
                }
                data.add(row);
            }

            // PROMETHEE: Расчет разниц между альтернативами для каждого критерия
            Map<String, double[][]> differences = calculateDifferences(criteria, data);
            resultsText.setText("=== Разницы между альтернативами ===\n");
            for (String criterion : criteria) {
                resultsText.appendText("\nКритерий: " + criterion + "\n");
                resultsText.appendText(Arrays.deepToString(differences.get(criterion)) + "\n");
            }

            // PROMETHEE: Меры предпочтения
            Map<String, double[][]> preferences = calculatePreferenceFunctions(differences, criteria);
            resultsText.appendText("\n=== Меры предпочтения ===\n");
            for (String criterion : criteria) {
                resultsText.appendText("\nКритерий: " + criterion + "\n");
                resultsText.appendText(Arrays.deepToString(preferences.get(criterion)) + "\n");
            }

            // PROMETHEE: Индексы предпочтения
            double[][] preferenceIndices = calculatePreferenceIndices(preferences, weights, alternatives.length);
            resultsText.appendText("\n=== Индексы предпочтения ===\n");
            resultsText.appendText(Arrays.deepToString(preferenceIndices) + "\n");

            // PROMETHEE: Положительные, отрицательные и чистые потоки
            double[] phiPlus = calculatePositiveFlows(preferenceIndices);
            double[] phiMinus = calculateNegativeFlows(preferenceIndices);
            double[] phiNet = calculateNetFlows(phiPlus, phiMinus);
            resultsText.appendText("\n=== Положительные потоки ===\n" + Arrays.toString(phiPlus));
            resultsText.appendText("\n=== Отрицательные потоки ===\n" + Arrays.toString(phiMinus));
            resultsText.appendText("\n=== Чистые потоки ===\n" + Arrays.toString(phiNet));

            // PROMETHEE: Ранжирование альтернатив
            List<String> rankedAlternatives = rankAlternatives(phiNet, alternatives);
            resultOutput.setText("Ранжирование альтернатив: " + String.join(", ", rankedAlternatives));

        } catch (Exception e) {
            showAlert("Ошибка3", e.getMessage());
        }
    }

    private Map<String, double[][]> calculateDifferences(String[] criteria, List<List<Double>> data) {
        Map<String, double[][]> differences = new HashMap<>();
        for (int c = 0; c < criteria.length; c++) {
            double[][] diffMatrix = new double[data.size()][data.size()];
            for (int i = 0; i < data.size(); i++) {
                for (int j = 0; j < data.size(); j++) {
                    diffMatrix[i][j] = data.get(i).get(c) - data.get(j).get(c);
                }
            }
            differences.put(criteria[c], diffMatrix);
        }
        return differences;
    }

    private Map<String, double[][]> calculatePreferenceFunctions(Map<String, double[][]> differences, String[] criteria) {
        Map<String, double[][]> preferences = new HashMap<>();
        for (String criterion : criteria) {
            double[][] diffMatrix = differences.get(criterion);
            double[][] prefMatrix = new double[diffMatrix.length][diffMatrix.length];
            for (int i = 0; i < diffMatrix.length; i++) {
                for (int j = 0; j < diffMatrix.length; j++) {
                    prefMatrix[i][j] = diffMatrix[i][j] > 0 ? 1 : 0; // Простая функция предпочтения
                }
            }
            preferences.put(criterion, prefMatrix);
        }
        return preferences;
    }

    private double[][] calculatePreferenceIndices(Map<String, double[][]> preferences, double[] weights, int numAlternatives) {
        double[][] preferenceIndices = new double[numAlternatives][numAlternatives];
        for (int i = 0; i < numAlternatives; i++) {
            for (int j = 0; j < numAlternatives; j++) {
                double sum = 0;
                int c = 0;
                for (double[][] prefMatrix : preferences.values()) {
                    sum += prefMatrix[i][j] * weights[c++];
                }
                preferenceIndices[i][j] = sum;
            }
        }
        return preferenceIndices;
    }

    private double[] calculatePositiveFlows(double[][] preferenceIndices) {
        double[] phiPlus = new double[preferenceIndices.length];
        for (int i = 0; i < preferenceIndices.length; i++) {
            for (int j = 0; j < preferenceIndices.length; j++) {
                phiPlus[i] += preferenceIndices[i][j];
            }
        }
        return phiPlus;
    }

    private double[] calculateNegativeFlows(double[][] preferenceIndices) {
        double[] phiMinus = new double[preferenceIndices.length];
        for (int j = 0; j < preferenceIndices.length; j++) {
            for (int i = 0; i < preferenceIndices.length; i++) {
                phiMinus[j] += preferenceIndices[i][j];
            }
        }
        return phiMinus;
    }

    private double[] calculateNetFlows(double[] phiPlus, double[] phiMinus) {
        double[] phiNet = new double[phiPlus.length];
        for (int i = 0; i < phiNet.length; i++) {
            phiNet[i] = phiPlus[i] - phiMinus[i];
        }
        return phiNet;
    }

    private List<String> rankAlternatives(double[] phiNet, String[] alternatives) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < phiNet.length; i++) {
            indices.add(i);
        }
        indices.sort((i, j) -> Double.compare(phiNet[j], phiNet[i]));
        List<String> rankedAlternatives = new ArrayList<>();
        for (int index : indices) {
            rankedAlternatives.add(alternatives[index]);
        }
        return rankedAlternatives;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void createCritTable() {
        // Clear previous columns and data
        tableCrit.getColumns().clear();
        tableCrit.getItems().clear();

        int cntCrit = countCrit.getValue();
        int cntAlter = countAlter.getValue();

        // Create the first column for names (alternatives)
        TableColumn<List<String>, String> nameColumn = new TableColumn<>();
        nameColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().get(0))); // Get the first column value (name)

        // Set the cell factory to allow editing (if needed)
        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        // Add the first column to the table
        tableCrit.getColumns().add(nameColumn);

        // Create columns for the criteria
        for (int i = 0; i < cntCrit; i++) {
            TableColumn<List<String>, String> column = new TableColumn<>("Критерий " + (i + 1));
            int colIndex = i + 1; // Adjust to start from the second column

            // Set cell value factory to get the value from the corresponding list in the row
            column.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().get(colIndex)));

            // Set the cell factory to allow editing
            column.setCellFactory(TextFieldTableCell.forTableColumn());

            // Update the row data when editing a cell
            column.setOnEditCommit(event -> {
                List<String> rowData = event.getRowValue();
                rowData.set(colIndex, event.getNewValue());
            });

            // Add the column to the table
            tableCrit.getColumns().add(column);
        }

        // Create rows with default values
        for (int i = 0; i < 2; i++) {
            ArrayList<String> row = new ArrayList<>(Collections.nCopies(cntCrit + 1, " "));

            if (i == 0) row.set(0, "Критерии");
            if (i == 1) row.set(0, "Веса");

            tableCrit.getItems().add(row);
        }

        // Print the first and second rows

    }

    private List<String> getFirstRowCr() {
        // Получаем все строки из таблицы
        List<List<String>> rows = tableCrit.getItems();

        // Проверяем, что таблица не пуста, и возвращаем первую строку
        if (!rows.isEmpty()) {
            List<String> firstRow = new ArrayList<>(rows.get(0));
            firstRow.remove(0);

            return firstRow;  // Возвращаем первую строку
        }

        // Если таблица пуста, возвращаем null или пустой список
        return Collections.emptyList();
    }
    private List<String> getFirstRowAlt() {
        // Получаем все строки из таблицы
        List<List<String>> rows = tableAlter.getItems();

        // Проверяем, что таблица не пуста, и возвращаем первую строку
        if (!rows.isEmpty()) {
            List<String> firstRow = new ArrayList<>(rows.get(0));
            firstRow.remove(0);

            return firstRow;  // Возвращаем первую строку
        }

        // Если таблица пуста, возвращаем null или пустой список
        return Collections.emptyList();
    }

    private List<String> getSecondRowCr() {
        // Получаем все строки из таблицы
        List<List<String>> rows = tableCrit.getItems();

        // Проверяем, что таблица не пуста, и возвращаем первую строку
        if (!rows.isEmpty()) {
            List<String> secondRow = new ArrayList<>(rows.get(1));
            secondRow.remove(0);
            return secondRow; // Возвращаем первую строку
        }

        // Если таблица пуста, возвращаем null или пустой список
        return Collections.emptyList();
    }

    private void createAlterTable() {
        // Clear previous columns and data in the table for alternatives
        tableAlter.getColumns().clear();
        tableAlter.getItems().clear();

        int cntAlter = countAlter.getValue();

        // Create the first column for names (alternatives)
        TableColumn<List<String>, String> nameColumn = new TableColumn<>("Название");
        nameColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().get(0))); // Get the first column value (name)

        // Set the cell factory to allow editing (if needed)
        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        // Add the first column to the table
        tableAlter.getColumns().add(nameColumn);

        // Create columns for the alternatives
        for (int i = 0; i < cntAlter; i++) {
            TableColumn<List<String>, String> column = new TableColumn<>("Альтернатива " + (i + 1));
            int colIndex = i + 1; // Adjust to start from the second column

            // Set cell value factory to get the value from the corresponding list in the row
            column.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().get(colIndex)));

            // Set the cell factory to allow editing
            column.setCellFactory(TextFieldTableCell.forTableColumn());

            // Update the row data when editing a cell
            column.setOnEditCommit(event -> {
                List<String> rowData = event.getRowValue();
                rowData.set(colIndex, event.getNewValue());
            });

            // Add the column to the table
            tableAlter.getColumns().add(column);
        }

        // Create rows with default values
        for (int i = 0; i < 1; i++) {
            ArrayList<String> row = new ArrayList<>(Collections.nCopies(cntAlter + 1, " "));

            if (i == 0) row.set(0, "Название");

            tableAlter.getItems().add(row);
        }
    }
}
