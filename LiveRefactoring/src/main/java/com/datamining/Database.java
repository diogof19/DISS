package com.datamining;

import com.analysis.metrics.ClassMetrics;
import com.analysis.metrics.MethodMetrics;
import com.core.Pair;
import com.utils.importantValues.Values;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static com.datamining.Utils.getMethodMetricsFromFile;

public class Database {
    private static final String DATABASE_URL = "jdbc:sqlite:" + Values.dataFolder + "metrics.db";

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

        createAuthorsTable();
        createMethodMetricsTable();
        createClassMetricsTable();
        createModelsTable();
        createAuthorsModelsTable();
    }

    /**
     * Add test data to the database
     */
    private static void addTestData() {
        String authorsInsertSQL = "INSERT INTO authors (name, email) VALUES (?, ?);";
        String modelsInsertSQL = "INSERT INTO models (name, pathEM, pathEC, selected) VALUES (?, ?, ?, ?);";
        String authorsModelsInsertSQL = "INSERT INTO authors_models (author_id, model_id) VALUES (?, ?);";

        try (Connection conn = connect()) {
            if (conn != null) {
                PreparedStatement pstmt = conn.prepareStatement(authorsInsertSQL);

                pstmt.setString(1, "Taylor Swift");
                pstmt.setString(2, "no_its_becky@gmail.com");
                //pstmt.executeUpdate();

                pstmt.setString(1, "Stiles Stilinski");
                pstmt.setString(2, "sparky@gmail.com");
                //pstmt.executeUpdate();

                pstmt.setString(1, "Niklaus Mikaelson");
                pstmt.setString(2, "thehybrid@outlook.com");
                //pstmt.executeUpdate();

                pstmt.setString(1, "Piper Halliwell");
                pstmt.setString(2, "freeze@outlook.com");
                //pstmt.executeUpdate();

                pstmt.setString(1, "Derek Hale");
                pstmt.setString(2, "sourwolf@gmail.com");
                //pstmt.executeUpdate();

                pstmt = conn.prepareStatement(modelsInsertSQL);

                pstmt.setString(1, "Team");
                pstmt.setString(2, "models/TeamEM.joblib");
                pstmt.setString(3, "models/TeamEC.joblib");
                pstmt.setInt(4, 1);
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


    /* METRICS TABLES */

    /**
     * Create the method metrics table
     */
    public static void createMethodMetricsTable() {
        String deleteTableSQL = "DROP TABLE IF EXISTS methodMetrics;";

        String createTableSQL = "CREATE TABLE IF NOT EXISTS methodMetrics (\n" +
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

        String createIndexSQL = "CREATE INDEX IF NOT EXISTS author_index ON methodMetrics (author);";

        try (Connection conn = connect()) {
            if (conn != null) {
                conn.createStatement().executeUpdate(deleteTableSQL);
                conn.createStatement().executeUpdate(createTableSQL);
                conn.createStatement().executeUpdate(createIndexSQL);
            }
        } catch (SQLException e) {
            System.out.println("Create method metrics table: " + e.getMessage());
        }
    }

    /**
     * Create the class metrics table
     */
    public static void createClassMetricsTable() {
        String deleteTableSQL = "DROP TABLE IF EXISTS classMetrics;";

        String createTableSQL = "CREATE TABLE IF NOT EXISTS classMetrics (\n" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "    author INTEGER,\n" +
                "    numProperties INTEGER,\n" +
                "    numPublicAttributes INTEGER,\n" +
                "    numPublicMethods INTEGER,\n" +
                "    numProtectedFields INTEGER,\n" +
                "    numProtectedMethods INTEGER,\n" +
                "    numLongMethods INTEGER,\n" +
                "    numLinesCode INTEGER,\n" +
                "    lackOfCohesion REAL,\n" +
                "    cyclomaticComplexity REAL,\n" +
                "    cognitiveComplexity REAL,\n" +
                "    numMethods INTEGER,\n" +
                "    numConstructors INTEGER,\n" +
                "    halsteadLength REAL,\n" +
                "    halsteadVocabulary REAL,\n" +
                "    halsteadVolume REAL,\n" +
                "    halsteadDifficulty REAL,\n" +
                "    halsteadEffort REAL,\n" +
                "    halsteadLevel REAL,\n" +
                "    halsteadTime REAL,\n" +
                "    halsteadBugsDelivered REAL,\n" +
                "    halsteadMaintainability REAL,\n" +
                "    numMethodsToExtract INTEGER,\n" +
                "    numFieldsToExtract INTEGER,\n" +
                "    FOREIGN KEY (author) REFERENCES authors(id)\n" +
                ");";

        String createIndexSQL = "CREATE INDEX IF NOT EXISTS author_index ON classMetrics (author);";

        try (Connection conn = connect()) {
            if (conn != null) {
                conn.createStatement().executeUpdate(deleteTableSQL);
                conn.createStatement().executeUpdate(createTableSQL);
                conn.createStatement().executeUpdate(createIndexSQL);
            }
        } catch (SQLException e) {
            System.out.println("Create class metrics table: " + e.getMessage());
        }
    }

    /**
     * Save the metrics of a refactoring
     * @param beforeInfo The information before the change
     * @param afterInfo The information after the change
     */
    public static void saveMethodMetrics(RefactoringInfo beforeInfo, RefactoringInfo afterInfo) {
        MethodMetrics beforeMethodMetrics = getMethodMetricsFromFile(beforeInfo.getBeforeFile(),
                beforeInfo.getMethodName(), beforeInfo.getClassName());

        MethodMetrics afterMethodMetrics = afterInfo != null ? getMethodMetricsFromFile(afterInfo.getAfterFile(),
                afterInfo.getMethodName(), afterInfo.getClassName()) : null;

        AuthorInfo author = beforeInfo.getAuthor();

        saveMethodMetrics(new Pair<>(author.getAuthorName(), author.getAuthorEmail()), beforeMethodMetrics, afterMethodMetrics);
    }

    /**
     * Save the metrics of an Extract Method refactoring
     * @param author The author of the refactoring
     * @param beforeMethodMetrics The metrics before the refactoring
     * @param afterMethodMetrics The metrics after the refactoring
     * @return The id of the refactoring
     */
    public static Integer saveMethodMetrics(Pair<String, String> author, MethodMetrics beforeMethodMetrics, MethodMetrics afterMethodMetrics) {
        int beforeTotalLines = beforeMethodMetrics.numberLinesOfCode + beforeMethodMetrics.numberComments +
                beforeMethodMetrics.numberBlankLines;
        int afterTotalLines = afterMethodMetrics != null ? afterMethodMetrics.numberLinesOfCode +
                afterMethodMetrics.numberComments + afterMethodMetrics.numberBlankLines : 0;

        String insertSQL = "INSERT INTO methodMetrics (author, numberLinesOfCodeBef, numberCommentsBef, numberBlankLinesBef, " +
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

                String deleteSQL = "DELETE FROM methodMetrics WHERE id = ? AND CAST(halsteadLevelBef AS CHARACTER) ='Inf';";

                PreparedStatement deletePstmt = conn.prepareStatement(deleteSQL);

                deletePstmt.setInt(1, id);

                int numDeleledRows = deletePstmt.executeUpdate();

                if(numDeleledRows > 0){
                    return null;
                } else
                    return id;
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return null;
    }

    /**
     * Saves the metrics of an Extract Class refactoring
     * @param author The author of the refactoring
     * @param beforeClassMetrics The metrics before the refactoring
     * @param afterClassMetrics The metrics after the refactoring
     * @return The id of the refactoring
     */
    public static Integer saveClassMetrics(Pair<String, String> author, ClassMetrics beforeClassMetrics, ClassMetrics afterClassMetrics) {
        int numMethodsToExtract = beforeClassMetrics.numMethods - afterClassMetrics.numMethods;
        int numFieldsToExtract = beforeClassMetrics.numProperties - afterClassMetrics.numProperties;

        String insertSQL = "INSERT INTO classMetrics (author, numProperties, numPublicAttributes, numPublicMethods, " +
                "numProtectedFields, numProtectedMethods, numLongMethods, numLinesCode, lackOfCohesion, " +
                "cyclomaticComplexity, cognitiveComplexity, numMethods, numConstructors, halsteadLength, " +
                "halsteadVocabulary, halsteadVolume, halsteadDifficulty, halsteadEffort, halsteadLevel, " +
                "halsteadTime, halsteadBugsDelivered, halsteadMaintainability, numMethodsToExtract, " +
                "numFieldsToExtract) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

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

            pstmt.setInt(1, authorId);
            pstmt.setInt(2, beforeClassMetrics.numProperties);
            pstmt.setInt(3, beforeClassMetrics.numPublicAttributes);
            pstmt.setInt(4, beforeClassMetrics.numPublicMethods);
            pstmt.setInt(5, beforeClassMetrics.numProtectedFields);
            pstmt.setInt(6, beforeClassMetrics.numProtectedMethods);
            pstmt.setInt(7, beforeClassMetrics.numLongMethods);
            pstmt.setInt(8, beforeClassMetrics.numLinesCode);
            pstmt.setDouble(9, beforeClassMetrics.lackOfCohesion);
            pstmt.setDouble(10, beforeClassMetrics.complexity);
            pstmt.setDouble(11, beforeClassMetrics.cognitiveComplexity);
            pstmt.setInt(12, beforeClassMetrics.numMethods);
            pstmt.setInt(13, beforeClassMetrics.numConstructors);
            pstmt.setDouble(14, beforeClassMetrics.halsteadLength);
            pstmt.setDouble(15, beforeClassMetrics.halsteadVocabulary);
            pstmt.setDouble(16, beforeClassMetrics.halsteadVolume);
            pstmt.setDouble(17, beforeClassMetrics.halsteadDifficulty);
            pstmt.setDouble(18, beforeClassMetrics.halsteadEffort);
            pstmt.setDouble(19, beforeClassMetrics.halsteadLevel);
            pstmt.setDouble(20, beforeClassMetrics.halsteadTime);
            pstmt.setDouble(21, beforeClassMetrics.halsteadBugsDelivered);
            pstmt.setDouble(22, beforeClassMetrics.halsteadMaintainability);
            pstmt.setInt(23, numMethodsToExtract);
            pstmt.setInt(24, numFieldsToExtract);

            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if(rs.next()){
                int id = rs.getInt(1);

                String deleteSQL = "DELETE FROM classMetrics WHERE id = ? AND CAST(cyclomaticComplexity AS CHARACTER) ='Inf';";

                PreparedStatement deletePstmt = conn.prepareStatement(deleteSQL);

                deletePstmt.setInt(1, id);

                int numDeletedRows = deletePstmt.executeUpdate();

                if(numDeletedRows > 0){
                    return null;
                } else
                    return id;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return null;
    }

    /**
     * Count and print the number of metrics in the database
     */
    public static void countMetrics() {
        String countMethodMetricsSQL = "SELECT COUNT(*) FROM methodMetrics;";
        String countClassMetricsSQL = "SELECT COUNT(*) FROM classMetrics;";

        try (Connection conn = connect()) {
            if (conn != null) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(countMethodMetricsSQL);

                System.out.println("Number of method metrics: " + rs.getInt(1));

                rs = stmt.executeQuery(countClassMetricsSQL);

                System.out.println("Number of class metrics: " + rs.getInt(1));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Deletes all the method metrics in the database
     */
    public static void deleteAllMethodMetrics() {
        String deleteSQL = "DELETE FROM methodMetrics;";

        try (Connection conn = connect()) {
            if (conn != null) {
                conn.createStatement().executeUpdate(deleteSQL);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Deletes the method metrics with infinity values on the 'halsteadLevelBef' column
     */
    public static void deleteMethodMetricsWithInfinity() {
        //This column is created with 1/value, so it can create errors (though it's rare)
        String deleteSQL = "DELETE FROM methodMetrics WHERE CAST(halsteadLevelBef AS CHARACTER) ='Inf';";

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
                "name TEXT UNIQUE NOT NULL,\n" +
                "pathEM TEXT UNIQUE NOT NULL,\n" +
                "pathEC TEXT UNIQUE NOT NULL,\n" +
                "selected INTEGER NOT NULL\n" +
                ");";

        try (Connection conn = connect()) {
            if (conn != null) {
                conn.createStatement().executeUpdate(deleteTableSQL);
                conn.createStatement().executeUpdate(createTableSQL);
            }
        } catch (SQLException e) {
            System.out.println("Create models table: " + e.getMessage());
        }
    }

    /**
     * Gets the name and path of the selected model
     * @return A pair with <name, path> of the selected model
     */
    public static ModelInfo getSelectedModel(){
        String selectSQL = "SELECT name, pathEM, pathEC FROM models WHERE selected = 1;";

        try (Connection conn = connect()) {
            if (conn != null) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(selectSQL);

                return new ModelInfo(rs.getString("name"), rs.getString("pathEM"),
                        rs.getString("pathEC"), true);
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
    public static ArrayList<ModelInfo> getAllModels() {
        String selectSQL = "SELECT * FROM models;";

        try (Connection conn = connect()) {
            if (conn != null) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(selectSQL);

                ArrayList<ModelInfo> models = new ArrayList<>();
                while(rs.next()){
                    models.add(new ModelInfo(rs.getString("name"), rs.getString("pathEM"),
                            rs.getString("pathEC"), rs.getInt("selected") == 1));
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
     * @param modelInfo The information of the model
     */
    public static void createModel(ModelInfo modelInfo) {
        String insertSQL = "INSERT INTO models (name, pathEM, pathEC, selected) VALUES (?, ?, ?, ?);";

        try (Connection conn = connect()) {
            if (conn != null) {
                PreparedStatement pstmt = conn.prepareStatement(insertSQL);
                pstmt.setString(1, modelInfo.getName());
                pstmt.setString(2, modelInfo.getPathEM());
                pstmt.setString(3, modelInfo.getPathEC());
                pstmt.setInt(4, modelInfo.isSelected() ? 1 : 0);

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
    public static ModelInfo getModelByName(String name) {
        String selectSQL = "SELECT pathEM, pathEC, selected FROM models WHERE name = ?;";

        try (Connection conn = connect()) {
            if (conn != null) {
                PreparedStatement pstmt = conn.prepareStatement(selectSQL);
                pstmt.setString(1, name);

                ResultSet rs = pstmt.executeQuery();

                return new ModelInfo(name, rs.getString("pathEM"), rs.getString("pathEC"),
                        rs.getInt("selected") == 1);
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
    public static ModelInfo getAnyModel() {
        String selectSQL = "SELECT name, pathEM, pathEC FROM models LIMIT 1;";

        try (Connection conn = connect()) {
            if (conn != null) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(selectSQL);

                return new ModelInfo(rs.getString("name"), rs.getString("pathEM"),
                        rs.getString("pathEC"), false);
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


    /* UTIL METRICS LIVEREF TABLES */

    public static void createMetricsLiveRefTable() {
        String deleteTableSQL = "DROP TABLE IF EXISTS metricsLiveRef;";

        String createTableSQL = "CREATE TABLE IF NOT EXISTS metricsLiveRef (\n" +
                "    id INTEGER PRIMARY KEY,\n" +
                "    numberLinesOfCode INTEGER,\n" +
                "    numberComments INTEGER,\n" +
                "    numberBlankLines INTEGER,\n" +
                "    totalLines INTEGER,\n" +
                "    numParameters INTEGER,\n" +
                "    numStatements INTEGER,\n" +
                "    halsteadLength REAL,\n" +
                "    halsteadVocabulary REAL,\n" +
                "    halsteadVolume REAL,\n" +
                "    halsteadDifficulty REAL,\n" +
                "    halsteadEffort REAL,\n" +
                "    halsteadLevel REAL,\n" +
                "    halsteadTime REAL,\n" +
                "    halsteadBugsDelivered REAL,\n" +
                "    halsteadMaintainability REAL,\n" +
                "    cyclomaticComplexity INTEGER,\n" +
                "    cognitiveComplexity INTEGER,\n" +
                "    lackOfCohesionInMethod INTEGER,\n" +
                "    sameBeforeAfter INTEGER,\n" +
                "    FOREIGN KEY (id) REFERENCES methodMetrics(id)\n" +
                ");";

        try (Connection conn = connect()) {
            if (conn != null) {
                conn.createStatement().executeUpdate(deleteTableSQL);
                conn.createStatement().executeUpdate(createTableSQL);
            }
        } catch (SQLException e) {
            System.out.println("Create metrics liveRef table: " + e.getMessage());
        }
    }

    public static void saveAfterLiveRefMetrics(MethodMetrics methodMetrics, int id, boolean same) {
        String insertSQL = "INSERT INTO metricsLiveRef (id, numberLinesOfCode, numberComments, numberBlankLines, " +
                "totalLines, numParameters, numStatements, halsteadLength, halsteadVocabulary, " +
                "halsteadVolume, halsteadDifficulty, halsteadEffort, halsteadLevel, halsteadTime, " +
                "halsteadBugsDelivered, halsteadMaintainability, cyclomaticComplexity, cognitiveComplexity, " +
                "lackOfCohesionInMethod, sameBeforeAfter) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

        try (Connection conn = connect()) {
            if(conn != null) {
                PreparedStatement pstmt = conn.prepareStatement(insertSQL);

                pstmt.setInt(1, id);
                if(methodMetrics != null) {
                    pstmt.setInt(2, methodMetrics.numberLinesOfCode);
                    pstmt.setInt(3, methodMetrics.numberComments);
                    pstmt.setInt(4, methodMetrics.numberBlankLines);
                    pstmt.setInt(5, methodMetrics.numberLinesOfCode + methodMetrics.numberComments + methodMetrics.numberBlankLines);
                    pstmt.setInt(6, methodMetrics.numParameters);
                    pstmt.setInt(7, methodMetrics.numberOfStatements);
                    pstmt.setDouble(8, methodMetrics.halsteadLength);
                    pstmt.setDouble(9, methodMetrics.halsteadVocabulary);
                    pstmt.setDouble(10, methodMetrics.halsteadVolume);
                    pstmt.setDouble(11, methodMetrics.halsteadDifficulty);
                    pstmt.setDouble(12, methodMetrics.halsteadEffort);
                    pstmt.setDouble(13, methodMetrics.halsteadLevel);
                    pstmt.setDouble(14, methodMetrics.halsteadTime);
                    pstmt.setDouble(15, methodMetrics.halsteadBugsDelivered);
                    pstmt.setDouble(16, methodMetrics.halsteadMaintainability);
                    pstmt.setInt(17, methodMetrics.complexityOfMethod);
                    pstmt.setInt(18, methodMetrics.cognitiveComplexity);
                    pstmt.setDouble(19, methodMetrics.lackOfCohesionInMethod);
                } else {
                    pstmt.setNull(2, Types.INTEGER);
                    pstmt.setNull(3, Types.INTEGER);
                    pstmt.setNull(4, Types.INTEGER);
                    pstmt.setNull(5, Types.INTEGER);
                    pstmt.setNull(6, Types.INTEGER);
                    pstmt.setNull(7, Types.INTEGER);
                    pstmt.setNull(8, Types.REAL);
                    pstmt.setNull(9, Types.REAL);
                    pstmt.setNull(10, Types.REAL);
                    pstmt.setNull(11, Types.REAL);
                    pstmt.setNull(12, Types.REAL);
                    pstmt.setNull(13, Types.REAL);
                    pstmt.setNull(14, Types.REAL);
                    pstmt.setNull(15, Types.REAL);
                    pstmt.setNull(16, Types.REAL);
                    pstmt.setNull(17, Types.INTEGER);
                    pstmt.setNull(18, Types.INTEGER);
                    pstmt.setNull(19, Types.REAL);

                }
                pstmt.setInt(20, same ? 1 : 0);

                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void createMetricsLiveRefNewTable() {
        String deleteTableSQL = "DROP TABLE IF EXISTS metricsLiveRefNew;";

        String createTableSQL = "CREATE TABLE IF NOT EXISTS metricsLiveRefNew (\n" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "    extracted INTEGER,\n" +
                "    sameBeforeAfter INTEGER\n" +
                ");";

        try (Connection conn = connect()) {
            if (conn != null) {
                conn.createStatement().executeUpdate(deleteTableSQL);
                conn.createStatement().executeUpdate(createTableSQL);
            }
        } catch (SQLException e) {
            System.out.println("Create metrics liveRef table: " + e.getMessage());
        }
    }

    public static void saveAfterNewLiveRefMetrics(boolean extracted, boolean same) {
        String insertSQL = "INSERT INTO metricsLiveRefNew (extracted, sameBeforeAfter) " +
                "VALUES (?, ?);";

        try (Connection conn = connect()) {
            if(conn != null) {
                PreparedStatement pstmt = conn.prepareStatement(insertSQL);

                pstmt.setInt(1, extracted ? 1 : 0);
                pstmt.setInt(2, same ? 1 : 0);

                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void createClassMetricsLiveRefTable() {
        String deleteTableSQL = "DROP TABLE IF EXISTS classMetricsLiveRef;";

        String createTableSQL = "CREATE TABLE IF NOT EXISTS classMetricsLiveRef (\n" +
                "    id INTEGER PRIMARY KEY,\n" +
                "    numProperties INTEGER,\n" +
                "    numPublicAttributes INTEGER,\n" +
                "    numPublicMethods INTEGER,\n" +
                "    numProtectedFields INTEGER,\n" +
                "    numProtectedMethods INTEGER,\n" +
                "    numLongMethods INTEGER,\n" +
                "    numLinesCode INTEGER,\n" +
                "    lackOfCohesion REAL,\n" +
                "    cyclomaticComplexity REAL,\n" +
                "    cognitiveComplexity REAL,\n" +
                "    numMethods INTEGER,\n" +
                "    numConstructors INTEGER,\n" +
                "    halsteadLength REAL,\n" +
                "    halsteadVocabulary REAL,\n" +
                "    halsteadVolume REAL,\n" +
                "    halsteadDifficulty REAL,\n" +
                "    halsteadEffort REAL,\n" +
                "    halsteadLevel REAL,\n" +
                "    halsteadTime REAL,\n" +
                "    halsteadBugsDelivered REAL,\n" +
                "    halsteadMaintainability REAL,\n" +
                "    FOREIGN KEY (id) REFERENCES classMetrics(id)\n" +
                ");";

        try (Connection conn = connect()) {
            if (conn != null) {
                conn.createStatement().executeUpdate(deleteTableSQL);
                conn.createStatement().executeUpdate(createTableSQL);
            }
        } catch (SQLException e) {
            System.out.println("Create class metrics liveRef table: " + e.getMessage());
        }
    }

    public static void saveAfterClassLiveRefMetrics(ClassMetrics classMetrics, int id) {
        String insertSQL = "INSERT INTO classMetricsLiveRef (id, numProperties, numPublicAttributes, numPublicMethods, " +
                "numProtectedFields, numProtectedMethods, numLongMethods, numLinesCode, lackOfCohesion, " +
                "cyclomaticComplexity, cognitiveComplexity, numMethods, numConstructors, halsteadLength, " +
                "halsteadVocabulary, halsteadVolume, halsteadDifficulty, halsteadEffort, halsteadLevel, " +
                "halsteadTime, halsteadBugsDelivered, halsteadMaintainability) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

        try (Connection conn = connect()) {
            if (conn != null) {
                PreparedStatement pstmt = conn.prepareStatement(insertSQL);

                pstmt.setInt(1, id);
                if (classMetrics != null) {
                    pstmt.setInt(2, classMetrics.numProperties);
                    pstmt.setInt(3, classMetrics.numPublicAttributes);
                    pstmt.setInt(4, classMetrics.numPublicMethods);
                    pstmt.setInt(5, classMetrics.numProtectedFields);
                    pstmt.setInt(6, classMetrics.numProtectedMethods);
                    pstmt.setInt(7, classMetrics.numLongMethods);
                    pstmt.setInt(8, classMetrics.numLinesCode);
                    pstmt.setDouble(9, classMetrics.lackOfCohesion);
                    pstmt.setDouble(10, classMetrics.complexity);
                    pstmt.setDouble(11, classMetrics.cognitiveComplexity);
                    pstmt.setInt(12, classMetrics.numMethods);
                    pstmt.setInt(13, classMetrics.numConstructors);
                    pstmt.setDouble(14, classMetrics.halsteadLength);
                    pstmt.setDouble(15, classMetrics.halsteadVocabulary);
                    pstmt.setDouble(16, classMetrics.halsteadVolume);
                    pstmt.setDouble(17, classMetrics.halsteadDifficulty);
                    pstmt.setDouble(18, classMetrics.halsteadEffort);
                    pstmt.setDouble(19, classMetrics.halsteadLevel);
                    pstmt.setDouble(20, classMetrics.halsteadTime);
                    pstmt.setDouble(21, classMetrics.halsteadBugsDelivered);
                    pstmt.setDouble(22, classMetrics.halsteadMaintainability);
                } else {
                    pstmt.setNull(2, Types.INTEGER);
                    pstmt.setNull(3, Types.INTEGER);
                    pstmt.setNull(4, Types.INTEGER);
                    pstmt.setNull(5, Types.INTEGER);
                    pstmt.setNull(6, Types.INTEGER);
                    pstmt.setNull(7, Types.INTEGER);
                    pstmt.setNull(8, Types.INTEGER);
                    pstmt.setNull(9, Types.REAL);
                    pstmt.setNull(10, Types.REAL);
                    pstmt.setNull(11, Types.REAL);
                    pstmt.setNull(12, Types.INTEGER);
                    pstmt.setNull(13, Types.INTEGER);
                    pstmt.setNull(14, Types.REAL);
                    pstmt.setNull(15, Types.REAL);
                    pstmt.setNull(16, Types.REAL);
                    pstmt.setNull(17, Types.REAL);
                    pstmt.setNull(18, Types.REAL);
                    pstmt.setNull(19, Types.REAL);
                    pstmt.setNull(20, Types.REAL);
                    pstmt.setNull(21, Types.REAL);
                    pstmt.setNull(22, Types.REAL);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void createClassMetricsLiveRefSaraYesNo() {
        String deleteTableSQL = "DROP TABLE IF EXISTS classMetricsLiveRefSaraYesNo;";

        String createTableSQL = "CREATE TABLE IF NOT EXISTS classMetricsLiveRefSaraYesNo (\n" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "    extracted INTEGER\n" +
                ");";

        try (Connection conn = connect()) {
            if (conn != null) {
                conn.createStatement().executeUpdate(deleteTableSQL);
                conn.createStatement().executeUpdate(createTableSQL);
            }
        } catch (SQLException e) {
            System.out.println("Create class metrics liveRef table: " + e.getMessage());
        }
    }

    public static void saveClassMetricsLiveRefSaraYesNo(boolean extracted){
        String insertSQL = "INSERT INTO classMetricsLiveRefSaraYesNo (extracted) " +
                "VALUES (?);";

        try (Connection conn = connect()) {
            if (conn != null) {
                PreparedStatement pstmt = conn.prepareStatement(insertSQL);

                pstmt.setInt(1, extracted ? 1 : 0);

                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}