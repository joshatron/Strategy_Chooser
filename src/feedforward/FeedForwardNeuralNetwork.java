package feedforward;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.Random;

/**
 * Created by joshua on 9/22/15.
 */
public class FeedForwardNeuralNetwork
{
    private double momentum;
    private double learningRate;
    private double[] weights;
    private int hiddenLayers;
    private int[] sizes;
    private int biggestSize;
    private ActivationFunction activationFunction;
    private double biasNum = 1.;

    public FeedForwardNeuralNetwork(File file) throws IOException
    {
        this(getJSONFromFile(file));
    }

    public FeedForwardNeuralNetwork(JSONObject net)
    {
        momentum = net.getDouble("momentum");
        learningRate = net.getDouble("learningRate");
        hiddenLayers = net.getInt("hiddenLayers");

        String activation = net.getString("activationFunction");
        if(activation.equals("LINEAR"))
        {
            activationFunction = ActivationFunction.LINEAR;
        }
        else if(activation.equals("LOGISTIC"))
        {
            activationFunction = ActivationFunction.LOGISTIC;
        }
        else
        {
            System.out.println("Illegal activation function.");
        }

        JSONArray sizeArray = net.getJSONArray("sizes");
        if(sizeArray.length() == hiddenLayers + 2)
        {
            sizes = new int[hiddenLayers + 2];
            biggestSize = 0;
            for(int k = 0; k < hiddenLayers + 2; k++)
            {
                int size = sizeArray.getInt(k);
                sizes[k] = size;
                if(size > biggestSize)
                {
                    biggestSize = size;
                }
            }
        }
        else
        {
            System.out.println("Illegal number of layer sizes.");
        }

        if(net.has("weights"))
        {
            int totalWeights = 0;
            for(int k = 0; k < sizes.length - 1; k++)
            {
                totalWeights += sizes[k] * sizes[k + 1];
                totalWeights += sizes[k];
            }
            totalWeights += sizes[sizes.length - 1];
            JSONArray weightArray = net.getJSONArray("weights");
            if(weightArray.length() == totalWeights)
            {
                weights = new double[totalWeights];
                for(int k = 0; k < totalWeights; k++)
                {
                    weights[k] = weightArray.getDouble(k);
                }
            }
            else
            {
                System.out.println("Illegal number of weights");
            }
        }
        else
        {
            generateRandomWeights();
        }
    }

    public FeedForwardNeuralNetwork(int hiddenLayers, int[] sizes, ActivationFunction activationFunction, double momentum, double learningRate)
    {
        this.hiddenLayers = hiddenLayers;
        this.sizes = sizes;
        this.activationFunction = activationFunction;
        this.momentum = momentum;
        this.learningRate = learningRate;

        biggestSize = 0;
        for(int k = 0; k < sizes.length; k++)
        {
            if(sizes[k] > biggestSize)
            {
                biggestSize = sizes[k];
            }
        }

        generateRandomWeights();
    }

    private void generateRandomWeights()
    {
        int totalWeights = 0;
        for(int k = 0; k < sizes.length - 1; k++)
        {
            totalWeights += sizes[k] * sizes[k + 1];
            totalWeights += sizes[k];
        }
        totalWeights += sizes[sizes.length - 1];

        int lowest = -1;
        int highest = 1;
        Random rand = new Random();
        weights = new double[totalWeights];

        for(int k = 0; k < totalWeights; k++)
        {
            weights[k] = lowest + ((highest - lowest) * rand.nextDouble());
        }
    }

    private static JSONObject getJSONFromFile(File file) throws IOException
    {
        String input = FileUtils.readFileToString(file);
        return new JSONObject(input);
    }

    public JSONObject export()
    {
        JSONObject net = new JSONObject();
        net.put("momentum", momentum);
        net.put("learningRate", learningRate);
        net.put("hiddenLayers", hiddenLayers);
        net.put("activationFunction", activationFunction.name());
        net.put("sizes", new JSONArray(sizes));
        net.put("weights", new JSONArray(weights));

        return net;
    }

    public void export(File file) throws IOException
    {
        JSONObject net = export();

        BufferedWriter out = new BufferedWriter(new PrintWriter(new FileWriter(file)));
        out.write(net.toString(4));
        out.close();
    }

    public double[] compute(double[] inputs)
    {
        if(inputs.length != sizes[0])
        {
            System.out.println("Invalid number of inputs");
            return null;
        }

        int lastLayer = sizes[0];
        double[] layerOut = new double[biggestSize];
        for(int k = 0; k < lastLayer; k++)
        {
            layerOut[k] = inputs[k];
        }

        for(int k = 1; k < hiddenLayers + 2; k++)
        {
            double[] tempOut = new double[sizes[k]];
            for(int a = 0; a < sizes[k]; a++)
            {
                double sum = 0;
                for(int t = 0; t < lastLayer; t++)
                {
                    sum += layerOut[t] * getWeight(k - 1, t, k, a);
                }
                sum += biasNum * getWeight(-1, 0, k, a);
                tempOut[a] = applyActivationFunction(sum);
            }
            lastLayer = sizes[k];
            for(int a = 0; a < lastLayer; a++)
            {
                layerOut[a] = tempOut[a];
            }
        }

        return layerOut;
    }

    private double getWeight(int layerStart, int start, int layerEnd, int end)
    {
        if(layerStart != -1)
        {
            int index = 0;
            for(int k = 0; k < layerStart; k++)
            {
                index += sizes[k] * sizes[k + 1];
            }

            index += start * sizes[layerEnd];
            index += end;

            return weights[index];
        }
        else
        {
            int index = 0;
            for(int k = 0; k < hiddenLayers + 1; k++)
            {
                index += sizes[k] * sizes[k + 1];
            }

            for(int k = 0; k < layerEnd; k++)
            {
                index += sizes[k];
            }

            index += end;

            return weights[index];
        }
    }

    private double applyActivationFunction(double sum)
    {
        switch(activationFunction)
        {
            case LINEAR:
                double slope = 1.0;
                return slope * sum;
            case LOGISTIC:
                return 1.0 / (1.0 * Math.pow(Math.E, sum * -1.0));
        }

        System.out.println("Failed to apply activation function");
        return -9999;
    }
}
