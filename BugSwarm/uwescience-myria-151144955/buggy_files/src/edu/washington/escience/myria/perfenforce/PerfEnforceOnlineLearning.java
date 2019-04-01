/**
 *
 */
package edu.washington.escience.myria.perfenforce;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Context;

import org.slf4j.LoggerFactory;

import edu.washington.escience.myria.parallel.Server;

/**
 *
 */
public class PerfEnforceOnlineLearning {

  @Context private Server server;

  List<String> previousDataPoints;
  private QueryMetaData currentQuery;
  private final QueryMetaData previousQuery;
  private final String onlineLearningPath;
  private final Double[] queryPredictions;
  private final int tier;
  private int clusterSize;
  private int queryCounter;

  protected static final org.slf4j.Logger LOGGER =
      LoggerFactory.getLogger(PerfEnforceOnlineLearning.class);

  public PerfEnforceOnlineLearning(final int tier) {
    this.tier = tier;
    clusterSize = PerfEnforceDriver.configurations.get(tier);
    currentQuery = new QueryMetaData();
    previousQuery = new QueryMetaData();
    queryPredictions = new Double[PerfEnforceDriver.configurations.size()];
    onlineLearningPath =
        Paths.get(PerfEnforceDriver.configurationPath.toString(), "ScalingAlgorithms", "Live")
            .toString();
  }

  public void findSLA(final String querySQL) {

    String highestFeatures = PerfEnforceUtils.getMaxFeature(querySQL, clusterSize);

    PrintWriter featureWriter =
        new PrintWriter(Paths.get(onlineLearningPath, "TESTING.arff").toString(), "UTF-8");

    featureWriter.write("@relation testing \n");

    featureWriter.write("@attribute numberTables numeric \n");
    featureWriter.write("@attribute postgesEstCostMin numeric \n");
    featureWriter.write("@attribute postgesEstCostMax numeric \n");
    featureWriter.write("@attribute postgesEstNumRows numeric \n");
    featureWriter.write("@attribute postgesEstWidth numeric \n");
    featureWriter.write("@attribute numberOfWorkers numeric \n");
    featureWriter.write("@attribute realTime numeric \n");

    featureWriter.write("\n");
    featureWriter.write("@data \n");
    featureWriter.write(highestFeatures + "\n");
    featureWriter.close();

    // predict the runtime
    String[] cmd = {
      "java",
      "-cp",
      onlineLearningPath + "weka.jar",
      "weka.classifiers.rules.M5Rules",
      "-M",
      "4.0",
      "-t",
      onlineLearningPath + "WekaTraining/" + clusterSize + "_Workers/TRAINING.arff",
      "-T",
      onlineLearningPath + "TESTING.arff",
      "-p",
      "0",
      "-classifications",
      " weka.classifiers.evaluation.output.prediction.CSV -file \""
          + onlineLearningPath
          + "results.txt"
          + "\""
    };
    ProcessBuilder pb = new ProcessBuilder(cmd);

    Process p = pb.start();
    p.waitFor();

    String querySLA = "";
    BufferedReader predictionReader =
        new BufferedReader(new FileReader(Paths.get(onlineLearningPath, "results.txt").toString()));
    predictionReader.readLine();
    querySLA = predictionReader.readLine().split(",")[2];
    predictionReader.close();

    for (int c : PerfEnforceDriver.configurations) {
      String maxFeatureForConfiguration = PerfEnforceUtils.getMaxFeature(querySQL, c);
      FileWriter featureWriterForConfiguration;
      featureWriterForConfiguration =
          new FileWriter(
              Paths.get(onlineLearningPath, "OMLFiles", "features", String.valueOf(c)).toString());
      featureWriterForConfiguration.write(maxFeatureForConfiguration + '\n');
      featureWriterForConfiguration.close();
    }
    currentQuery = new QueryMetaData(queryCounter, Double.parseDouble(querySLA));
    findBestClusterSize(currentQuery);
  }

  public void findBestClusterSize(final QueryMetaData query) {
    List<Thread> threadList = new ArrayList<Thread>();
    for (int i = 0; i < PerfEnforceDriver.configurations.size(); i++) {
      final int clusterIndex = i;
      Thread thread =
          new Thread(
              new Runnable() {
                @Override
                public void run() {
                  trainOnlineQueries(clusterIndex, query.id);
                }
              });
      threadList.add(thread);
    }

    for (Thread t : threadList) {
      t.start();
    }

    for (Thread t : threadList) {
      try {
        t.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    double maxScore = 0;
    int winnerIndex = 0;
    for (int currentState = 0;
        currentState < PerfEnforceDriver.configurations.size();
        currentState++) {
      double onlinePrediction = queryPredictions[currentState];

      onlinePrediction = (onlinePrediction < 0) ? 0 : onlinePrediction;

      double currentRatio = 0;
      if (query.slaRuntime == 0) {
        currentRatio = onlinePrediction / 1;
      } else {
        currentRatio = onlinePrediction / query.slaRuntime;
      }
      double currentScore = closeToOneScore(currentRatio);

      if (currentScore > maxScore) {
        winnerIndex = currentState;
        maxScore = currentScore;
      }
    }

    // Officially record the winner
    clusterSize = PerfEnforceDriver.configurations.get(winnerIndex);
  }

  public void trainOnlineQueries(final int clusterIndex, final int queryID) throws IOException {
    String MOAFileName = Paths.get(onlineLearningPath, "OMLFiles", "moa.jar").toString();
    String trainingFileName = Paths.get(onlineLearningPath, "OMLFiles", "training.arff").toString();
    String modifiedTrainingFileName =
        Paths.get(onlineLearningPath, "OMLFiles", "training-modified-" + clusterIndex + ".arff")
            .toString();
    String predictionsFileName =
        Paths.get(onlineLearningPath, "OMLFiles", "predictions" + clusterIndex + ".txt").toString();

    PrintWriter outputWriter = new PrintWriter(modifiedTrainingFileName);
    outputWriter.close();

    // copy training file to new file
    FileChannel src = new FileInputStream(trainingFileName).getChannel();
    FileChannel dest = new FileOutputStream(modifiedTrainingFileName).getChannel();
    dest.transferFrom(src, 0, src.size());
    src.close();
    dest.close();

    // Append all previous data points
    FileWriter appendDataWriter = new FileWriter(modifiedTrainingFileName, true);
    for (String s : previousDataPoints) {
      appendDataWriter.write(s + "\n");
    }

    // Append the current point
    String newPoint = getQueryFeature(clusterIndex, queryID, 0);
    appendDataWriter.write(newPoint + "\n");
    appendDataWriter.close();

    String moaCommand =
        String.format(
            "EvaluatePrequentialRegression -l (rules.functions.Perceptron  -d -l %s) -s (ArffFileStream -f %s) -e (WindowRegressionPerformanceEvaluator -w 1) -f 1 -o %s",
            .04,
            modifiedTrainingFileName,
            predictionsFileName);
    String[] arrayCommand =
        new String[] {"java", "-classpath", MOAFileName, "moa.DoTask", moaCommand};

    Process p = Runtime.getRuntime().exec(arrayCommand);

    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
    while ((reader.readLine()) != null) {}

    parsingOnlineFile(clusterIndex, predictionsFileName);
  }

  public void parsingOnlineFile(final int clusterIndex, final String predictionFileName)
      throws IOException {
    BufferedReader streamReader = new BufferedReader(new FileReader(predictionFileName));
    String currentLine = "";
    double nextQueryPrediction = 0;
    while ((currentLine = streamReader.readLine()) != null) {
      nextQueryPrediction = Double.parseDouble((currentLine.split(",")[0]).split(":")[1]);
    }
    streamReader.close();
    queryPredictions[clusterIndex] = nextQueryPrediction;
  }

  public String getQueryFeature(
      final int clusterIndex, final int queryID, final double queryRuntime) throws IOException {
    String featureFilePath =
        Paths.get(onlineLearningPath, "OMLFiles", "features", String.valueOf(clusterSize))
            .toString();

    BufferedReader featureReader = new BufferedReader(new FileReader(featureFilePath));
    for (int i = 0; i < queryID; i++) {
      featureReader.readLine();
    }
    String result = featureReader.readLine();
    if (queryRuntime != 0) {
      String[] parts = result.split(",");
      result =
          parts[0]
              + ","
              + parts[1]
              + ","
              + parts[2]
              + ","
              + parts[3]
              + ","
              + parts[4]
              + ","
              + parts[5]
              + ","
              + queryRuntime;
    }

    featureReader.close();
    return result;
  }

  public double closeToOneScore(final double ratio) {
    if (ratio == 1.0) {
      return Double.MAX_VALUE;
    } else {
      return Math.abs(1 / (ratio - 1.0));
    }
  }

  public void recordRealRuntime(final double queryRuntime) {
    previousDataPoints.add(
        getQueryFeature(
            PerfEnforceDriver.configurations.indexOf(clusterSize), currentQuery.id, queryRuntime));
  }

  public QueryMetaData getPreviousQuery() {
    return previousQuery;
  }

  public QueryMetaData getCurrentQuery() {
    return currentQuery;
  }

  public int getClusterSize() {
    return clusterSize;
  }
}
