package com.datamining;

import com.analysis.metrics.ClassMetrics;
import com.analysis.metrics.MethodMetrics;
import com.core.Pair;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static com.datamining.Utils.getMethodMetricsFromFile;

public class Database {
    //private static final String DATABASE_FILE_PATH = "C:\\Users\\dluis\\Documents\\Docs\\Universidade\\M 2 ano\\Thesis\\DISS\\LiveRefactoring\\src\\main\\resources\\metrics.db";
    //private static final String DATABASE_FILE_PATH = "C:\\Users\\dluis\\.gradle\\caches\\modules-2\\files-2.1\\com.jetbrains.intellij.idea\\ideaIC\\2021.1.1\\e051d885e757b286781f50305504d7b8db3e1dba\\ideaIC-2021.1.1\\bin\\tmp\\metrics.db";
    //private static final String DATABASE_URL = "jdbc:sqlite:" + Values.dataFolder + "metrics.db";
    private static final String DATABASE_URL = "jdbc:sqlite:C:\\Users\\dluis\\Documents\\Docs\\Universidade\\M 2 ano\\Thesis\\DISS\\LiveRefactoring\\src\\main\\resources\\metrics.db";

    public static void main(String[] args) {
        //createDatabase();
        //addTestData();

//        ArrayList<AuthorInfo> authors = getAuthorsPerModel("Model 1");
//        for(AuthorInfo author : authors){
//            System.out.println(author + " - " + author.isSelected());
//        }

        countMetrics();
        System.out.println("Number of authors: " + getAllAuthors().size());
        System.out.println("Number of models: " + getNumberOfModels());
        System.out.println("Selected model: " + getSelectedModelName());

//        deleteModel("new");
//        deleteModel("fafaasdfsds");
//        deleteModel("asdasdasd");
//        deleteModel("fg");
    }

    /**
     * Connects to the sqlite database
     * @return The connection
     */
    private static Connection connect() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DATABASE_URL);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    /**
     * Creates the sqlite database
     */
    public static void createDatabase() {
        try (Connection conn = connect()) {
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                System.out.println("The driver name is " + meta.getDriverName());
                System.out.println("A new database has been created.");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        createMetricsTable();
        createAuthorsTable();
        createModelsTable();
        createAuthorsModelsTable();
    }

    /**
     * Add test data to the database
     */
    private static void addTestData() {
        String authorsInsertSQL = "INSERT INTO authors (name, email) VALUES (?, ?);";
        String modelsInsertSQL = "INSERT INTO models (name, path, selected) VALUES (?, ?, ?);";
        String authorsModelsInsertSQL = "INSERT INTO authors_models (author_id, model_id) VALUES (?, ?);";

        try (Connection conn = connect()) {
            if (conn != null) {
                PreparedStatement pstmt = conn.prepareStatement(authorsInsertSQL);

                pstmt.setString(1, "Diana");
                pstmt.setString(2, "d@g.com");
                pstmt.executeUpdate();

                pstmt.setString(1, "Luis");
                pstmt.setString(2, "l@g.com");
                pstmt.executeUpdate();

                pstmt.setString(1, "Ana");
                pstmt.setString(2, "a@g.com");
                pstmt.executeUpdate();

                pstmt.setString(1, "Joao");
                pstmt.setString(2, "j@g.com");
                pstmt.executeUpdate();

                pstmt.setString(1, "Andre");
                pstmt.setString(2, "a@g.com");
                pstmt.executeUpdate();

                pstmt = conn.prepareStatement(modelsInsertSQL);

                pstmt.setString(1, "Model 1");
                pstmt.setString(2, "models/model1.joblib");
                pstmt.setInt(3, 1);
                pstmt.executeUpdate();

                pstmt.setString(1, "Model 2");
                pstmt.setString(2, "models/model2.joblib");
                pstmt.setInt(3, 0);
                pstmt.executeUpdate();

                pstmt = conn.prepareStatement(authorsModelsInsertSQL);

                pstmt.setInt(1, 1);
                pstmt.setInt(2, 1);
                pstmt.executeUpdate();

                pstmt.setInt(1, 1);
                pstmt.setInt(2, 2);
                pstmt.executeUpdate();

                pstmt.setInt(1, 2);
                pstmt.setInt(2, 1);
                pstmt.executeUpdate();

                pstmt.setInt(1, 3);
                pstmt.setInt(2, 2);
                pstmt.executeUpdate();

                pstmt.setInt(1, 4);
                pstmt.setInt(2, 3);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


    }


    /* METRICS TABLE */

    /**
     * Create the metrics table
     */
    public static void createMetricsTable() {
        String deleteTableSQL = "DROP TABLE IF EXISTS metrics;";

        String createTableSQL = "CREATE TABLE IF NOT EXISTS metrics (\n" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "    author INTEGER,\n" +
                "    numberLinesOfCodeBef INTEGER,\n" +
                "    numberCommentsBef INTEGER,\n" +
                "    numberBlankLinesBef INTEGER,\n" +
                "    totalLinesBef INTEGER,\n" +
                "    numParametersBef INTEGER,\n" +
                "    numStatementsBef INTEGER,\n" +
                "    halsteadLengthBef REAL,\n" +
                "    halsteadVocabularyBef REAL,\n" +
                "    halsteadVolumeBef REAL,\n" +
                "    halsteadDifficultyBef REAL,\n" +
                "    halsteadEffortBef REAL,\n" +
                "    halsteadLevelBef REAL,\n" +
                "    halsteadTimeBef REAL,\n" +
                "    halsteadBugsDeliveredBef REAL,\n" +
                "    halsteadMaintainabilityBef REAL,\n" +
                "    cyclomaticComplexityBef INTEGER,\n" +
                "    cognitiveComplexityBef INTEGER,\n" +
                "    lackOfCohesionInMethodBef INTEGER,\n" +
                "    numberLinesOfCodeAft INTEGER,\n" +
                "    numberCommentsAft INTEGER,\n" +
                "    numberBlankLinesAft INTEGER,\n" +
                "    totalLinesAft INTEGER,\n" +
                "    numParametersAft INTEGER,\n" +
                "    numStatementsAft INTEGER,\n" +
                "    halsteadLengthAft REAL,\n" +
                "    halsteadVocabularyAft REAL,\n" +
                "    halsteadVolumeAft REAL,\n" +
                "    halsteadDifficultyAft REAL,\n" +
                "    halsteadEffortAft REAL,\n" +
                "    halsteadLevelAft REAL,\n" +
                "    halsteadTimeAft REAL,\n" +
                "    halsteadBugsDeliveredAft REAL,\n" +
                "    halsteadMaintainabilityAft REAL,\n" +
                "    cyclomaticComplexityAft INTEGER,\n" +
                "    cognitiveComplexityAft INTEGER,\n" +
                "    lackOfCohesionInMethodAft REAL,\n" +
                "    FOREIGN KEY (author) REFERENCES authors(id)\n" +
                ");";

        String createIndexSQL = "CREATE INDEX IF NOT EXISTS author_index ON metrics (author);";

        try (Connection conn = connect()) {
            if (conn != null) {
                conn.createStatement().executeUpdate(deleteTableSQL);
                conn.createStatement().executeUpdate(createTableSQL);
                conn.createStatement().executeUpdate(createIndexSQL);
            }
        } catch (SQLException e) {
            System.out.println("Create metrics table: " + e.getMessage());
        }
    }

    /**
     * Save the metrics of a refactoring
     * @param beforeInfo The information before the change
     * @param afterInfo The information after the change
     * @throws SQLException If there is an error with the database
     */
    public static void saveMetrics(RefactoringInfo beforeInfo, RefactoringInfo afterInfo) throws SQLException {
        Pair<ClassMetrics, MethodMetrics> beforeMetrics = getMethodMetricsFromFile(beforeInfo.getBeforeFile(),
                beforeInfo.getMethodName(), beforeInfo.getClassName());

        Pair<ClassMetrics, MethodMetrics> afterMetrics = afterInfo != null ? getMethodMetricsFromFile(afterInfo.getAfterFile(),
                afterInfo.getMethodName(), afterInfo.getClassName()) : null;

        MethodMetrics beforeMethodMetrics = beforeMetrics.getSecond();
        MethodMetrics afterMethodMetrics = afterMetrics != null ? afterMetrics.getSecond() : null;

        AuthorInfo author = beforeInfo.getAuthor();

        saveMetrics(new Pair<>(author.getAuthorName(), author.getAuthorEmail()), beforeMethodMetrics, afterMethodMetrics);
    }

    /**
     * Save the metrics of a refactoring
     * @param author The author of the refactoring
     * @param beforeMethodMetrics The metrics before the refactoring
     * @param afterMethodMetrics The metrics after the refactoring
     * @throws SQLException If there is an error with the database
     */
    public static void saveMetrics(Pair<String, String> author, MethodMetrics beforeMethodMetrics, MethodMetrics afterMethodMetrics) throws SQLException {
        int beforeTotalLines = beforeMethodMetrics.numberLinesOfCode + beforeMethodMetrics.numberComments +
                beforeMethodMetrics.numberBlankLines;
        int afterTotalLines = afterMethodMetrics != null ? afterMethodMetrics.numberLinesOfCode +
                afterMethodMetrics.numberComments + afterMethodMetrics.numberBlankLines : 0;

        String insertSQL = "INSERT INTO metrics (author, numberLinesOfCodeBef, numberCommentsBef, numberBlankLinesBef, " +
                "totalLinesBef, numParametersBef, numStatementsBef, halsteadLengthBef, halsteadVocabularyBef, " +
                "halsteadVolumeBef, halsteadDifficultyBef, halsteadEffortBef, halsteadLevelBef, halsteadTimeBef, " +
                "halsteadBugsDeliveredBef, halsteadMaintainabilityBef, cyclomaticComplexityBef, cognitiveComplexityBef, " +
                "lackOfCohesionInMethodBef, numberLinesOfCodeAft, numberCommentsAft, numberBlankLinesAft, totalLinesAft, " +
                "numParametersAft, numStatementsAft, halsteadLengthAft, halsteadVocabularyAft, halsteadVolumeAft, " +
                "halsteadDifficultyAft, halsteadEffortAft, halsteadLevelAft, halsteadTimeAft, halsteadBugsDeliveredAft, " +
                "halsteadMaintainabilityAft, cyclomaticComplexityAft, cognitiveComplexityAft, lackOfCohesionInMethodAft) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                "?, ?, ?, ?, ?, ?, ?, ?)";


        int authorId;
        if(author == null)
            authorId = -2;
        else
            authorId = findAuthorByNameAndEmail(author.getFirst(), author.getSecond());

        try (Connection conn = connect()) {
            PreparedStatement pstmt = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS);

            if(authorId == -1){
                authorId = insertAuthor(author.getFirst(), author.getSecond());
            }

            if (authorId == -2) {
                pstmt.setNull(1, Types.INTEGER);
            } else {
                pstmt.setInt(1, authorId);
            }

            pstmt.setInt(2, beforeMethodMetrics.numberLinesOfCode);
            pstmt.setInt(3, beforeMethodMetrics.numberComments);
            pstmt.setInt(4, beforeMethodMetrics.numberBlankLines);
            pstmt.setInt(5, beforeTotalLines);
            pstmt.setInt(6, beforeMethodMetrics.numParameters);
            pstmt.setInt(7, beforeMethodMetrics.numberOfStatements);
            pstmt.setDouble(8, beforeMethodMetrics.halsteadLength);
            pstmt.setDouble(9, beforeMethodMetrics.halsteadVocabulary);
            pstmt.setDouble(10, beforeMethodMetrics.halsteadVolume);
            pstmt.setDouble(11, beforeMethodMetrics.halsteadDifficulty);
            pstmt.setDouble(12, beforeMethodMetrics.halsteadEffort);
            pstmt.setDouble(13, beforeMethodMetrics.halsteadLevel);
            pstmt.setDouble(14, beforeMethodMetrics.halsteadTime);
            pstmt.setDouble(15, beforeMethodMetrics.halsteadBugsDelivered);
            pstmt.setDouble(16, beforeMethodMetrics.halsteadMaintainability);
            pstmt.setInt(17, beforeMethodMetrics.complexityOfMethod);
            pstmt.setInt(18, beforeMethodMetrics.cognitiveComplexity);
            pstmt.setDouble(19, beforeMethodMetrics.lackOfCohesionInMethod);

            if(afterMethodMetrics == null){
                pstmt.setNull(20, Types.INTEGER);
                pstmt.setNull(21, Types.INTEGER);
                pstmt.setNull(22, Types.INTEGER);
                pstmt.setNull(23, Types.INTEGER);
                pstmt.setNull(24, Types.INTEGER);
                pstmt.setNull(25, Types.INTEGER);
                pstmt.setNull(26, Types.REAL);
                pstmt.setNull(27, Types.REAL);
                pstmt.setNull(28, Types.REAL);
                pstmt.setNull(29, Types.REAL);
                pstmt.setNull(30, Types.REAL);
                pstmt.setNull(31, Types.REAL);
                pstmt.setNull(32, Types.REAL);
                pstmt.setNull(33, Types.REAL);
                pstmt.setNull(34, Types.REAL);
                pstmt.setNull(35, Types.INTEGER);
                pstmt.setNull(36, Types.INTEGER);
                pstmt.setNull(37, Types.REAL);
            }
            else {
                pstmt.setInt(20, afterMethodMetrics.numberLinesOfCode);
                pstmt.setInt(21, afterMethodMetrics.numberComments);
                pstmt.setInt(22, afterMethodMetrics.numberBlankLines);
                pstmt.setInt(23, afterTotalLines);
                pstmt.setInt(24, afterMethodMetrics.numParameters);
                pstmt.setInt(25, afterMethodMetrics.numberOfStatements);
                pstmt.setDouble(26, afterMethodMetrics.halsteadLength);
                pstmt.setDouble(27, afterMethodMetrics.halsteadVocabulary);
                pstmt.setDouble(28, afterMethodMetrics.halsteadVolume);
                pstmt.setDouble(29, afterMethodMetrics.halsteadDifficulty);
                pstmt.setDouble(30, afterMethodMetrics.halsteadEffort);
                pstmt.setDouble(31, afterMethodMetrics.halsteadLevel);
                pstmt.setDouble(32, afterMethodMetrics.halsteadTime);
                pstmt.setDouble(33, afterMethodMetrics.halsteadBugsDelivered);
                pstmt.setDouble(34, afterMethodMetrics.halsteadMaintainability);
                pstmt.setInt(35, afterMethodMetrics.complexityOfMethod);
                pstmt.setInt(36, afterMethodMetrics.cognitiveComplexity);
                pstmt.setDouble(37, afterMethodMetrics.lackOfCohesionInMethod);
            }

            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if(rs.next()){
                int id = rs.getInt(1);

                String deleteSQL = "DELETE FROM metrics WHERE id = ? AND CAST(halsteadLevelBef AS CHARACTER) ='Inf';";

                PreparedStatement deletePstmt = conn.prepareStatement(deleteSQL);

                deletePstmt.setInt(1, id);

                deletePstmt.executeUpdate();
            }

        }
    }

    /**
     * Count and print the number of metrics in the database
     */
    public static void countMetrics() {
        String selectSQL = "SELECT COUNT(*) FROM metrics;";

        try (Connection conn = connect()) {
            if (conn != null) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(selectSQL);

                System.out.println("Number of metrics: " + rs.getInt(1));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Deletes all the metrics in the database
     */
    public static void deleteAllMetrics() {
        String deleteSQL = "DELETE FROM metrics;";

        try (Connection conn = connect()) {
            if (conn != null) {
                conn.createStatement().executeUpdate(deleteSQL);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Deletes the metrics with infinity values on the 'halsteadLevelBef' column
     */
    public static void deleteMetricsWithInfinity() {
        //This column is created with 1/value, so it can create errors (though it's rare)
        String deleteSQL = "DELETE FROM metrics WHERE CAST(halsteadLevelBef AS CHARACTER) ='Inf';";

        try (Connection conn = connect()) {
            if (conn != null) {
                conn.createStatement().executeUpdate(deleteSQL);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


    /* AUTHORS TABLE */

    /**
     * Create the authors table
     */
    public static void createAuthorsTable() {
        String deleteTableSQL = "DROP TABLE IF EXISTS authors;";

        String createTableSQL = "CREATE TABLE IF NOT EXISTS authors (\n" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "name TEXT,\n" +
                "email TEXT,\n" +
                "UNIQUE (name, email)\n" +
                ");";

        String createIndexSQL = "CREATE INDEX IF NOT EXISTS authors_name_email ON authors (name, email);";

        try (Connection conn = connect()) {
            if (conn != null) {
                conn.createStatement().executeUpdate(deleteTableSQL);
                conn.createStatement().executeUpdate(createTableSQL);
                conn.createStatement().executeUpdate(createIndexSQL);
            }
        } catch (SQLException e) {
            System.out.println("Create authors table: " + e.getMessage());
        }
    }

    /**
     * Finds the author via the name and email (unique)
     * @param name The name of the author
     * @param email The email of the author
     * @return The id of the author
     */
    private static int findAuthorByNameAndEmail(String name, String email){
        String selectSQL = "SELECT id FROM authors WHERE name = ? AND email = ?;";

        try (Connection conn = connect()) {
            if (conn != null) {
                PreparedStatement pstmt = conn.prepareStatement(selectSQL);
                pstmt.setString(1, name);
                pstmt.setString(2, email);

                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    return rs.getInt("id");
                } else {
                    return -1;
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return -1;
    }

    /**
     * Inserts an author into the database
     * @param name The name of the author
     * @param email The email of the author
     * @return The id of the author
     */
    private static int insertAuthor(String name, String email){
        String insertSQL = "INSERT INTO authors (name, email) VALUES (?, ?);";

        try (Connection conn = connect()) {
            if (conn != null) {
                PreparedStatement pstmt = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS);
                pstmt.setString(1, name);
                pstmt.setString(2, email);

                pstmt.executeUpdate();

                ResultSet rs = pstmt.getGeneratedKeys();
                if(rs.next()){
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return -1;
    }

    /**
     * Gets all the authors in the database
     * @return The list of authors
     */
    public static ArrayList<AuthorInfo> getAllAuthors() {
        String selectSQL = "SELECT id, name, email FROM authors;";

        try (Connection conn = connect()) {
            if (conn != null) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(selectSQL);

                ArrayList<AuthorInfo> authors = new ArrayList<>();
                while(rs.next()){
                    authors.add(new AuthorInfo(rs.getInt("id"), rs.getString("name"),
                            rs.getString("email"), null));
                }
                return authors;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return null;
    }


    /* MODELS TABLE */

    /**
     * Create the models table
     */
    public static void createModelsTable() {
        String deleteTableSQL = "DROP TABLE IF EXISTS models;";

        String createTableSQL = "CREATE TABLE IF NOT EXISTS models (\n" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "name TEXT UNIQUE,\n" +
                "path TEXT NOT NULL,\n" +
                "selected INTEGER NOT NULL,\n" +
                "UNIQUE (path)\n" +
                ");";

        String addDefaultModelSQL = "INSERT INTO models (name, path, selected) VALUES ('Default', 'models/model.joblib', 1);";

        try (Connection conn = connect()) {
            if (conn != null) {
                conn.createStatement().executeUpdate(deleteTableSQL);
                conn.createStatement().executeUpdate(createTableSQL);
                conn.createStatement().executeUpdate(addDefaultModelSQL);
            }
        } catch (SQLException e) {
            System.out.println("Create models table: " + e.getMessage());
        }
    }

    /**
     * Gets the file path of the current model in use
     * @return The file path of the model
     */
    public static String getSelectedModelFilePath() {
        String selectSQL = "SELECT path FROM models WHERE selected = 1;";

        try (Connection conn = connect()) {
            if (conn != null) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(selectSQL);

                return rs.getString("path");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return null;
    }

    /**
     * Gets the name of the current model in use
     * @return The name of the model
     */
    public static String getSelectedModelName() {
        String selectSQL = "SELECT name FROM models WHERE selected = 1;";

        try (Connection conn = connect()) {
            if (conn != null) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(selectSQL);

                return rs.getString("name");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return null;
    }

    /**
     * Gets the name and path of the selected model
     * @return A pair with <name, path> of the selected model
     */
    public static Pair<String, String> getSelectedModel(){
        String selectSQL = "SELECT name, path FROM models WHERE selected = 1;";

        try (Connection conn = connect()) {
            if (conn != null) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(selectSQL);

                return new Pair<>(rs.getString("name"), rs.getString("path"));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return null;
    }

    /**
     * Gets all the model names
     * @return The list of model names
     */
    public static ArrayList<String> getAllModels() {
        String selectSQL = "SELECT name FROM models;";

        try (Connection conn = connect()) {
            if (conn != null) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(selectSQL);

                ArrayList<String> models = new ArrayList<>();
                while(rs.next()){
                    models.add(rs.getString("name"));
                }

                return models;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return null;
    }

    /**
     * Deletes the model from the database
     * @param modelName The name of the model
     */
    public static void deleteModel(String modelName) {
        String deleteAuthorsModelsSQL = "DELETE FROM authors_models WHERE model_id = (SELECT id FROM models WHERE name = ?);";
        String deleteModelSQL = "DELETE FROM models WHERE name = ?;";

        try (Connection conn = connect()) {
            if (conn != null) {
                PreparedStatement pstmt = conn.prepareStatement(deleteAuthorsModelsSQL);
                pstmt.setString(1, modelName);
                pstmt.executeUpdate();

                pstmt = conn.prepareStatement(deleteModelSQL);
                pstmt.setString(1, modelName);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Creates a new model
     * @param name The name of the model
     * @param path The path of the model
     */
    public static void createModel(String name, String path) {
        String insertSQL = "INSERT INTO models (name, path, selected) VALUES (?, ?, 0);";

        try (Connection conn = connect()) {
            if (conn != null) {
                PreparedStatement pstmt = conn.prepareStatement(insertSQL);
                pstmt.setString(1, name);
                pstmt.setString(2, path);

                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Sets the selected model
     * @param name The name of the model
     */
    public static void setSelectedModel(String name) {
        String updateSQL = "UPDATE models SET selected = 0;";
        String selectSQL = "UPDATE models SET selected = 1 WHERE name = ?;";

        try (Connection conn = connect()) {
            if (conn != null) {
                conn.createStatement().executeUpdate(updateSQL);

                PreparedStatement pstmt = conn.prepareStatement(selectSQL);
                pstmt.setString(1, name);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Gets the path of the model by its name
     * @param name The name of the model
     * @return The path of the model
     */
    public static String getModelPathByName(String name) {
        String selectSQL = "SELECT path FROM models WHERE name = ?;";

        try (Connection conn = connect()) {
            if (conn != null) {
                PreparedStatement pstmt = conn.prepareStatement(selectSQL);
                pstmt.setString(1, name);

                ResultSet rs = pstmt.executeQuery();

                return rs.getString("path");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return null;
    }

    /**
     * Gets the name of any model
     * @return The name of the model
     */
    public static String getAnyModelName() {
        String selectSQL = "SELECT name FROM models LIMIT 1;";

        try (Connection conn = connect()) {
            if (conn != null) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(selectSQL);

                return rs.getString("name");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return null;
    }

    /**
     * Gets the number of models in the database
     * @return The number of models
     */
    public static int getNumberOfModels() {
        String selectSQL = "SELECT COUNT(*) FROM models;";

        try (Connection conn = connect()) {
            if (conn != null) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(selectSQL);

                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return 0;
    }


    /* AUTHORS MODELS TABLE */

    /**
     * Create the authors_models table which connects authors to models to understand the bias
     */
    public static void createAuthorsModelsTable() {
        String deleteTableSQL = "DROP TABLE IF EXISTS authors_models;";

        String createTableSQL = "CREATE TABLE IF NOT EXISTS authors_models (\n" +
                "author_id INTEGER,\n" +
                "model_id INTEGER,\n" +
                "FOREIGN KEY (author_id) REFERENCES authors(id),\n" +
                "FOREIGN KEY (model_id) REFERENCES models(id),\n" +
                "PRIMARY KEY (author_id, model_id)\n" +
                ");";

        try (Connection conn = connect()) {
            if (conn != null) {
                conn.createStatement().executeUpdate(deleteTableSQL);
                conn.createStatement().executeUpdate(createTableSQL);
            }
        } catch (SQLException e) {
            System.out.println("Create authors_models table: " + e.getMessage());
        }
    }

    /**
     * Gets all the authors per model, checking the selected boolean if the author is selected for the model
     * @param modelName The name of the model
     * @return The list of authors
     */
    public static ArrayList<AuthorInfo> getAuthorsPerModel(String modelName) {
        ArrayList<AuthorInfo> authorList = new ArrayList<>();

        String selectSQL = "SELECT id, name, email, selected\n" +
                "FROM (\n" +
                "    SELECT a.id, a.name, a.email, \n" +
                "           CASE WHEN (am.model_id IS NOT NULL AND m.name = ?) THEN 1 ELSE 0 END AS selected,\n" +
                "           ROW_NUMBER() OVER (PARTITION BY a.id ORDER BY CASE WHEN (am.model_id IS NOT NULL AND m.name = ?) THEN 1 ELSE 0 END DESC) AS rn\n" +
                "    FROM authors a \n" +
                "    LEFT JOIN authors_models am ON a.id = am.author_id \n" +
                "    LEFT JOIN models m ON am.model_id = m.id\n" +
                ") AS subquery\n" +
                "WHERE rn = 1;";

        try (Connection conn = connect()) {
            if (conn != null) {
                PreparedStatement pstmt = conn.prepareStatement(selectSQL);
                pstmt.setString(1, modelName);
                pstmt.setString(2, modelName);

                ResultSet rs = pstmt.executeQuery();

                while(rs.next()){
                    authorList.add(new AuthorInfo(rs.getInt("id"), rs.getString("name"),
                            rs.getString("email"), rs.getInt("selected") == 1));
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return authorList;
    }

    /**
     * Gets the selected authors for a model
     * @param modelName The name of the model
     * @return The set of authors
     */
    public static Set<AuthorInfo> getSelectedAuthorsPerModel(String modelName){
        Set<AuthorInfo> authors = new HashSet<>();

        String selectSQL = "SELECT a.id, a.name, a.email\n" +
                "FROM authors a\n" +
                "JOIN authors_models am ON a.id = am.author_id\n" +
                "JOIN models m ON am.model_id = m.id\n" +
                "WHERE m.name = ?;";

        try (Connection conn = connect()) {
            if (conn != null) {
                PreparedStatement pstmt = conn.prepareStatement(selectSQL);
                pstmt.setString(1, modelName);

                ResultSet rs = pstmt.executeQuery();

                while(rs.next()){
                    authors.add(new AuthorInfo(rs.getInt("id"), rs.getString("name"),
                            rs.getString("email"), true));
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());

        }

        return authors;
    }

    /**
     * Updates the authors for a model
     * @param modelName The name of the model
     * @param authors The set of authors
     */
    public static void updateAuthorsPerModel(String modelName, Set<AuthorInfo> authors) {
        String deleteSQL = "DELETE FROM authors_models WHERE model_id = (SELECT id FROM models WHERE name = ?);";

        //This is used with the checkboxes, so we don't have access to their id
        String insertSQL = "INSERT INTO authors_models (author_id, model_id) VALUES" +
                "((SELECT id FROM authors WHERE name = ? AND email = ?)," +
                " (SELECT id FROM models WHERE name = ?));";

        try (Connection conn = connect()) {
            if (conn != null) {
                PreparedStatement pstmt = conn.prepareStatement(deleteSQL);
                pstmt.setString(1, modelName);
                pstmt.executeUpdate();

                pstmt = conn.prepareStatement(insertSQL);
                for(AuthorInfo author : authors){
                    pstmt.setString(1, author.getAuthorName());
                    pstmt.setString(2, author.getAuthorEmail());
                    pstmt.setString(3, modelName);
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
