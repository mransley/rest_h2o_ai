package io.swiftsure;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import hex.genmodel.MojoModel;
import hex.genmodel.easy.EasyPredictModelWrapper;
import hex.genmodel.easy.RowData;
import hex.genmodel.easy.prediction.RegressionModelPrediction;

/**
 * Restful controller for wrapper H20.ai Models
 *
 */
@RestController
@EnableAutoConfiguration
public class PredictorController {

    private static Log log = LogFactory.getLog(PredictorController.class);
    private static ObjectMapper mapper = new ObjectMapper();
    private static EasyPredictModelWrapper modelWrapper = null;
    @Value("${model.path}") private String modelPath;
    
    public static void main(String[] args) {
        SpringApplication.run(PredictorController.class, args);
    }
    
    private synchronized EasyPredictModelWrapper loadModel() throws IOException {
        if (modelWrapper == null) {
            log.info("Loading model " + modelPath);
            MojoModel mojo = MojoModel.load(modelPath);
//            URL url = PredictorController.class.getResource("/model.zip");
//            log.info("Model URL=" + url.toString());
//            MojoReaderBackend reader = MojoReaderBackendFactory.createReaderBackend(url, MojoReaderBackendFactory.CachingStrategy.MEMORY);
//            MojoModel model = ModelMojoReader.readFrom(reader);
            modelWrapper = new EasyPredictModelWrapper(mojo);
        }
        return modelWrapper;
    }
    
    private Object parseValue(String input) {
        return Double.valueOf(input);
    }
    
    private RowData jsonToRowData(String raw_json) throws IOException {
        log.info("Performing prediction on \n" + raw_json);
        JsonNode json = mapper.readTree(raw_json);
        Iterator<Entry<String, JsonNode>> fields = json.fields();
        RowData row = new RowData();
        while (fields.hasNext()) {
            Entry<String, JsonNode> field = fields.next();
            row.put(field.getKey(), parseValue(field.getValue().asText()));
        }
        return row;
    }

    @RequestMapping(value="/regression", 
                    method=RequestMethod.POST,
                    consumes=MediaType.APPLICATION_JSON_VALUE,
                    produces=MediaType.APPLICATION_JSON_VALUE)
    public String regressionPrediction(@RequestBody String rawJson) throws Exception {
        EasyPredictModelWrapper model = loadModel();
        RowData inputData = jsonToRowData(rawJson);
        RegressionModelPrediction prediction = model.predictRegression(inputData);
        Map<String, Object> result = new HashMap<>();
        result.put("prediction", prediction.value);
        result.put("input", inputData);
        return mapper.writeValueAsString(result);
    }
}
