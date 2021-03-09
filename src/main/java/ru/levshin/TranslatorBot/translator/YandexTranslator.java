package ru.levshin.TranslatorBot.translator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class YandexTranslator implements Translator{

    //todo get token automatically, because it expires in every 12 hours

    // Token for authorization in Yandex Cloud API
    @Value("${yandex.iam_token}")
    private String token;

    @Override
    public String getTranslation(String text) {

        // Now primary translation directions: Any Language -> Russian, Russian -> English
        String targetLanguageCode = "";
        if (getCurrentLanguageCode(text).equals("ru")) {
            targetLanguageCode = "en";
        } else {
            targetLanguageCode = "ru";
        }

        /*
          Request body should be like this, according to Yandex Translation API documentation
          {
              "folder_id": "b1gvmob95yysaplct532",
              "texts": ["Hello", "World"],
              "targetLanguageCode": "ru"
          }
          but I use only one String in translation, so I don't need to send array of String
        */
        Map<String, String> parameters = new HashMap<>();
        parameters.put("targetLanguageCode", targetLanguageCode);
        parameters.put("format", "PLAIN_TEXT");
        parameters.put("texts", text);
        parameters.put("folder_id", "b1gfdaa0q358cduv9tb8");

        String json = getJsonString(parameters);

        JSONObject jsonObject = getJsonResponseObject(json, "https://translate.api.cloud.yandex.net/translate/v2/translate");

        String translatedText = jsonObject.optString("translations");

        //todo replace this kludge by smart json parsing
        translatedText = translatedText
                .substring(2, translatedText.length() - 2)
                .split(",")[0]
                .split(":")[1];
        translatedText = translatedText.substring(1, translatedText.length() - 1);

        return translatedText;
    }

    private String getCurrentLanguageCode(String text) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("text", text);
        parameters.put("folderId", "b1gfdaa0q358cduv9tb8");

        String json = getJsonString(parameters);

        JSONObject jsonObject = getJsonResponseObject(json, "https://translate.api.cloud.yandex.net/translate/v2/detect");

        /*
          JSONObject will be received in this format
          {
              "languageCode": "en"
          }
        */
        return jsonObject.optString("languageCode");
    }

    // Create JSON for HttpEntity
    private String getJsonString(Map<String, String> parameters) {
        String json = "";
        try {
            json = new ObjectMapper().writeValueAsString(parameters);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return json;
    }


    private HttpEntity<String> getStringHttpEntity(String json, String s) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<?> entity = new HttpEntity<>(json, headers);

        RestTemplate restTemplate = new RestTemplate();
        return restTemplate
                .exchange(s,
                        HttpMethod.POST, entity, String.class);
    }

    private JSONObject getJsonResponseObject(String json, String s) throws JSONException {
        HttpEntity<String> response = getStringHttpEntity(json,
                s);

        return new JSONObject(response.getBody());
    }

}
