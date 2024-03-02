package com.datamining;

import jakarta.xml.bind.JAXBException;
import org.jpmml.evaluator.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PredictionModel {
    public static void main(String[] args) {
        System.out.println("Prediction Model");

        File pmmFile = new File("C:\\Users\\dluis\\Documents\\Docs\\Universidade\\M 2 ano\\Thesis\\DISS\\Classification Model\\pipeline.pmml");

        Evaluator evaluator = null;
        try {
            evaluator = new LoadingModelEvaluatorBuilder()
                    .load(pmmFile)
                    .build();
        } catch (IOException | SAXException | JAXBException | ParserConfigurationException e) {
            e.printStackTrace();
        }
        
        evaluator.verify();

        ArrayList<Double> values = new ArrayList<>();
        values.add(0.0);
        values.add(0.44);
        values.add(0.45);
        values.add(0.46);
        values.add(1.0);


        double[] input = {0.0, 0.44, 0.45, 0.46, 1.0};

        // Printing input (x1, x2, .., xn) fields
        List<InputField> inputFields = evaluator.getInputFields();
        System.out.println("Input fields: " + inputFields);

        // Printing primary result (y) field(s)
        List<TargetField> targetFields = evaluator.getTargetFields();
        System.out.println("Target field(s): " + targetFields);

        // Printing secondary result (eg. probability(y), decision(y)) fields
        List<OutputField> outputFields = evaluator.getOutputFields();
        System.out.println("Output fields: " + outputFields);

        evaluator.evaluate()
    }

    public PredictionModel() {
        System.out.println("Prediction Model");
    }
}
