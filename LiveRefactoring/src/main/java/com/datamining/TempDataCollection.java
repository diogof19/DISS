package com.datamining;

import org.eclipse.jgit.api.Git;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

public class TempDataCollection {
    private static ArrayList<String> repos = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        repos.add("https://github.com/apache/commons-rdf.git");
        repos.add("https://github.com/apache/ant-ivy");
        repos.add("https://github.com/apache/commons-math");
        repos.add("https://github.com/apache/mahout");
        repos.add("https://github.com/apache/opennlp.git");
        repos.add("https://github.com/apache/calcite.git");
        repos.add("https://github.com/apache/cayenne.git");
        repos.add("https://github.com/apache/archiva.git");
        repos.add("https://github.com/apache/bigtop.git");
        repos.add("https://github.com/apache/commons-bcel.git");
        repos.add("https://github.com/apache/commons-beanutils.git");
        repos.add("https://github.com/apache/commons-codec.git");
        repos.add("https://github.com/apache/commons-collections.git");
        repos.add("https://github.com/apache/commons-compress.git");
        repos.add("https://github.com/apache/commons-configuration.git");
        repos.add("https://github.com/apache/commons-dbcp.git");
        repos.add("https://github.com/apache/commons-digester.git");
        repos.add("https://github.com/apache/commons-io.git");
        repos.add("https://github.com/apache/commons-jcs.git");
        repos.add("https://github.com/apache/commons-lang.git");
        repos.add("https://github.com/apache/commons-net.git");
        repos.add("https://github.com/apache/commons-validator.git");
        repos.add("https://github.com/apache/commons-vfs.git");
        repos.add("https://github.com/apache/giraph.git");
        repos.add("https://github.com/apache/gora.git");
        repos.add("https://github.com/apache/parquet-mr.git");
        repos.add("https://github.com/apache/wss4j.git");
        repos.add("https://github.com/apache/kylin.git");
        repos.add("https://github.com/apache/nutch.git");
        repos.add("https://github.com/apache/tika.git");
        repos.add("https://github.com/apache/deltaspike.git");
        repos.add("https://github.com/apache/systemml.git");
        repos.add("https://github.com/apache/lens.git");
        repos.add("https://github.com/apache/knox.git");
        repos.add("https://github.com/apache/jspwiki.git");
        repos.add("https://github.com/apache/manifoldcf.git");
        repos.add("https://github.com/apache/eagle.git");
        repos.add("https://github.com/apache/santuario-java.git");
        repos.add("https://github.com/apache/kafka.git");
        repos.add("https://github.com/apache/zeppelin.git");
        repos.add("https://github.com/apache/struts.git");
        repos.add("https://github.com/apache/pig.git");
        repos.add("https://github.com/apache/falcon.git");
        repos.add("https://github.com/apache/xerces2-j.git");
        repos.add("https://github.com/apache/tez.git");
        repos.add("https://github.com/apache/storm.git");
        repos.add("https://github.com/apache/flume.git");
        repos.add("https://github.com/apache/pdfbox.git");
        repos.add("https://github.com/apache/nifi.git");
        repos.add("https://github.com/apache/commons-imaging.git");
        repos.add("https://github.com/apache/derby.git");
        repos.add("https://github.com/apache/ranger.git");
        repos.add("https://github.com/apache/directory-fortress-core.git");
        repos.add("https://github.com/apache/phoenix.git");
        repos.add("https://github.com/apache/helix.git");
        repos.add("https://github.com/apache/jena.git");
        repos.add("https://github.com/apache/httpcomponents-client.git");
        repos.add("https://github.com/apache/httpcomponents-core.git");
        repos.add("https://github.com/apache/streams.git");
        repos.add("https://github.com/apache/samza.git");
        repos.add("https://github.com/apache/roller.git");
        repos.add("https://github.com/apache/mina-sshd.git");
        repos.add("https://github.com/apache/jackrabbit.git");
        repos.add("https://github.com/apache/oozie.git");
        repos.add("https://github.com/apache/activemq.git");
        repos.add("https://github.com/apache/maven.git");
        repos.add("https://github.com/apache/airavata.git");
        repos.add("https://github.com/apache/openjpa.git");
        repos.add("https://github.com/apache/directory-studio.git");
        repos.add("https://github.com/apache/xmlgraphics-batik.git");
        repos.add("https://github.com/apache/curator.git");
        repos.add("https://github.com/apache/directory-kerby.git");
        repos.add("https://github.com/apache/fineract.git");
        repos.add("https://github.com/apache/freemarker.git");
        repos.add("https://github.com/apache/openwebbeans.git");
        repos.add("https://github.com/apache/commons-jexl.git");
        repos.add("https://github.com/apache/commons-scxml.git");
        repos.add("https://github.com/apache/cxf-fediz.git");
        repos.add("https://github.com/atrautsch/zookeeper.git");
        repos.add("https://github.com/apache/tapestry-5.git");
        repos.add("https://github.com/apache/reef.git");
        repos.add("https://github.com/apache/juddi.git");
        repos.add("https://github.com/apache/velocity-engine.git");
        repos.add("https://github.com/apache/velocity-tools.git");
        repos.add("https://github.com/apache/karaf-cellar.git");
        repos.add("https://github.com/apache/maven-archetype.git");
        repos.add("https://github.com/apache/maven-doxia.git");
        repos.add("https://github.com/apache/maven-scm.git");
        repos.add("https://github.com/apache/maven-surefire.git");
        repos.add("https://github.com/apache/maven-wagon.git");
        repos.add("https://github.com/apache/oodt.git");
        repos.add("https://github.com/apache/portals-pluto.git");
        repos.add("https://github.com/apache/qpid-jms.git");
        repos.add("https://github.com/apache/sis.git");
        repos.add("https://github.com/apache/uima-ducc.git");
        repos.add("https://github.com/apache/avro.git");
        repos.add("https://github.com/apache/usergrid.git");
        repos.add("https://github.com/apache/james-project.git");


        String baseDir = "E:/repos";

        String errorLog = "E:/errors.txt";
        File errorLogFile = new File(errorLog);
        BufferedWriter errorWriter = Files.newBufferedWriter(errorLogFile.toPath());

        long overallStartTime = System.currentTimeMillis();

        for (int i = 0; i < repos.size(); i++){
            String repoUrl = repos.get(i);
            long startTime = System.currentTimeMillis();

            System.out.println("(" + getTimeString(startTime) +  ") Start cloning repository " + (i + 1) + ": " + repoUrl);

            //create directory
            String repoName = repoUrl.split("github.com/")[1].split(".git")[0];

            File repoDir = new File(baseDir + "/" + repoName);
            Files.createDirectories(repoDir.toPath());

            try {
                Git.cloneRepository()
                        .setURI(repoUrl)
                        .setDirectory(repoDir)
                        .call();
            } catch (Exception e) {
                errorWriter.write(repoUrl + "\n");
                errorWriter.write(e.getMessage() + "\n\n");
            }

            long endTime = System.currentTimeMillis();

            System.out.println("(" + getTimeString(endTime) +  ") Finish cloning repository: " + repoUrl);

            long elapsedTime = endTime - startTime;
            long elapsedMinutes = (elapsedTime / 1000) / 60;
            long elapsedSeconds = (elapsedTime / 1000) % 60;
            System.out.println("Elapsed time: " + elapsedMinutes + "m " + elapsedSeconds + "s\n\n");

        }

        long overallEndTime = System.currentTimeMillis();

        long overallElapsedTime = overallEndTime - overallStartTime;
        long overallElapsedMinutes = (overallElapsedTime / 1000) / 60;
        long overallElapsedSeconds = (overallElapsedTime / 1000) % 60;

        System.out.println("(" + getTimeString(overallEndTime) + ") Finished cloning all repositories");
        System.out.println("Overall elapsed time: " + overallElapsedMinutes + "m " + overallElapsedSeconds + "s\n\n");
    }

    private static String getTimeString(long timestamp) {
        return new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date(timestamp));
    }
}
