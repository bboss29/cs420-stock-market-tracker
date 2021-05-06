import javafx.collections.FXCollections;
import javafx.scene.chart.*;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import json_simple.*;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;

public class Main extends Application {

    Button btn_rem;
    Button btn_add;
    ChoiceBox cb;

    URL url;
    String sym;
    funcs graph = funcs.TIME_SERIES_MONTHLY_ADJUSTED;

    enum funcs {
        OVERVIEW,
        TIME_SERIES_MONTHLY_ADJUSTED,
        TIME_SERIES_WEEKLY_ADJUSTED
    }

    @Override
    public void start(Stage primaryStage) {

        class lineChart {
            LineChart<?, ?> lc;

            public lineChart() {
                CategoryAxis xAxis = new CategoryAxis();
                xAxis.setLabel("Time");
                NumberAxis yAxis = new NumberAxis();
                yAxis.setLabel("Price (USD)");

                LineChart<?, ?> linechart = new LineChart(xAxis, yAxis);
                linechart.setAxisSortingPolicy(LineChart.SortingPolicy.X_AXIS);
                linechart.setCreateSymbols(false);

                lc = linechart;
            }
            private void addSeries() {
                XYChart.Series series = new XYChart.Series();
                series.setName(sym + ": " + (graph == funcs.TIME_SERIES_WEEKLY_ADJUSTED ? "Weekly" : "Monthly"));

                ArrayList<String[]> in = new ArrayList<>();
                try {
                    formUrl(graph); // TODO get selected graph type
                    in = getPriceData(url2json());
                    if (in == null) return;
                    in.sort(Comparator.comparing(a -> LocalDate.parse(a[0])));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                for (String[] ar : in) {
                    series.getData().add(new XYChart.Data(ar[0], Double.parseDouble(ar[1])));
                }

                lc.getData().add(series);
                lc.getData().sort(Comparator.comparing(a -> LocalDate.parse(a.getData().get(0).getXValue().toString())));
            }
        }

        lineChart line = new lineChart();

        GridPane root = new GridPane();
        primaryStage.setTitle("Stock Market Tracker");
        primaryStage.setScene(new Scene(root, 500,400));

        TextField tf = new TextField();
        tf.setPromptText("Stock Symbol");
        tf.setOnKeyTyped(event -> {
            sym = tf.getText();
        });

        btn_rem = new Button();
        btn_rem.setText("Remove Stock");
        btn_rem.setOnAction(event -> {
            if (!line.lc.getData().isEmpty()) line.lc.getData().remove(0);
        });

        btn_add = new Button();
        btn_add.setText("Add Stock");
        btn_add.setOnAction(event -> {
                line.addSeries();
                tf.setText("");
        });

        cb = new ChoiceBox(FXCollections.observableArrayList(
                funcs.TIME_SERIES_MONTHLY_ADJUSTED,
                funcs.TIME_SERIES_WEEKLY_ADJUSTED
        ));
        cb.setValue(funcs.TIME_SERIES_MONTHLY_ADJUSTED);
        cb.setOnAction(event -> graph = funcs.valueOf(cb.getValue().toString()));


//        try {
//            formUrl(funcs.OVERVIEW);
//            System.out.println(getOverviewData(url2json(url)).get("Exchange"));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


//        root.setGridLinesVisible(true);
        root.add(line.lc, 0,0,3,1);
        root.add(new Label("Frequency: "),0,1);
        root.add(cb,1,1);
        root.add(new Label("Stock Symbol: "),0,2);
        root.add(tf,1,2);
        root.add(btn_rem,2,1);
        root.add(btn_add,2,2);


        primaryStage.show();
    }

    private void formUrl(funcs func) throws MalformedURLException {
        this.url = new URL("https://www.alphavantage.co/query?" +
                "function=" + func +
                "&symbol=" + sym +
                "&apikey=" +
//                    "demo"); // demo key
//                    System.getenv("api_key")); // security
                    "7KBA9K70FJCEIVWP");
    }

    private ArrayList<String[]> getPriceData(JsonObject doc){
        ArrayList<String[]> out = new ArrayList<>();
        String type = graph.toString().contains("WEEKLY") ? "Weekly" : "Monthly";
        JsonObject res = (JsonObject) doc.get(type + " Adjusted Time Series");
        if (res == null) return null;
        for (String key : res.keySet()) {
            if (res.get(key) instanceof JsonObject) {
                JsonObject r = (JsonObject) res.get(key);
                out.add(new String[]{key, r.get("4. close").toString()});
            }
        }
        return out;
    }

    // TODO finish and implement this
    private static HashMap<String, String> getOverviewData(JsonObject doc){
        HashMap<String, String> out = new HashMap<>();
        for (String key : doc.keySet()) {
            out.put(key, doc.get(key).toString());
        }
        return out;
    }

    private JsonObject url2json() throws Exception {
        String contents = "";
        Scanner urlScanner = new Scanner(url.openStream());
        while (urlScanner.hasNextLine()) {
            contents += urlScanner.nextLine();
        }
        urlScanner.close();
        JsonObject json = null;
        try {
            json = (JsonObject) Jsoner.deserialize(contents);
        } catch (JsonException e){
            e.printStackTrace();
        }
        return json;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
