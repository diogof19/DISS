package com.datamining;

import com.analysis.metrics.ClassMetrics;
import com.analysis.metrics.MethodMetrics;
import com.core.Pair;

import java.io.File;
import java.io.IOException;
import java.sql.*;

import static com.datamining.Utils.extractFile;
import static com.datamining.Utils.getMethodMetricsFromFile;

public class Database {
    private static String DATABASE_FILE_PATH;
    private static String DATABASE_URL;

    public static void main(String[] args) throws SQLException {

    }

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
     * Create a sqlite database
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
    }

    /**
     * Create the metrics table
     */
    public static void createMetricsTable() {
        String deleteTableSQL = "DROP TABLE IF EXISTS metrics;";

        String createTableSQL = "CREATE TABLE IF NOT EXISTS metrics (\n" +
                "    author TEXT,\n" +
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
                "    lackOfCohesionInMethodAft REAL\n" +
                ");";

        String createIndexSQL = "CREATE INDEX IF NOT EXISTS author_index ON metrics (author);";

        try (Connection conn = connect()) {
            if (conn != null) {
                conn.createStatement().executeUpdate(deleteTableSQL);
                conn.createStatement().executeUpdate(createTableSQL);
                conn.createStatement().executeUpdate(createIndexSQL);
                System.out.println("Table created");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void databaseExtraction() throws IOException {
        String tempDatabasePath = "tmp/metrics.db";

        File database = new File(tempDatabasePath);
        if (database.exists()) {
            DATABASE_FILE_PATH = tempDatabasePath;
            DATABASE_URL = "jdbc:sqlite:" + DATABASE_FILE_PATH;
            return;
        }

        DATABASE_FILE_PATH = extractFile("metrics.db");
        DATABASE_URL = "jdbc:sqlite:" + DATABASE_FILE_PATH;
    }

    public static void saveMetrics(RefactoringInfo beforeInfo, RefactoringInfo afterInfo) throws SQLException, IOException {
        if (DATABASE_URL == null) {
            databaseExtraction();
        }

        Pair<ClassMetrics, MethodMetrics> beforeMetrics = getMethodMetricsFromFile(beforeInfo.getBeforeFile(),
                beforeInfo.getMethodName(), beforeInfo.getClassName());

        Pair<ClassMetrics, MethodMetrics> afterMetrics = afterInfo != null ? getMethodMetricsFromFile(afterInfo.getAfterFile(),
                afterInfo.getMethodName(), afterInfo.getClassName()) : null;

        MethodMetrics beforeMethodMetrics = beforeMetrics.getSecond();
        MethodMetrics afterMethodMetrics = afterMetrics != null ? afterMetrics.getSecond() : null;

        String author = beforeInfo.getAuthor().toString();

        saveMetrics(author, beforeMethodMetrics, afterMethodMetrics);
    }

    public static void saveMetrics(String author, MethodMetrics beforeMethodMetrics, MethodMetrics afterMethodMetrics) throws SQLException {
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

        try (Connection conn = connect()) {
            PreparedStatement pstmt = conn.prepareStatement(insertSQL);
            pstmt.setString(1, author);
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

        }
    }
}
