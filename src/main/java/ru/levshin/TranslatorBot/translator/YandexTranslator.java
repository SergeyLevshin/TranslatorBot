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
public class YandexTranslator implements Translator {

    //todo get token automatically, because it expires in every 12 hours

    // Token for authorization in Yandex Cloud API
    @Value("${yandex.iam_token}")
    private String token;
    @Value( "${folder.id}")
    private String folderId;

    @Override
    public String getTranslation(String text) {

        // Now a primary translation directions: Any Language -> Russian, Russian -> English
        String targetLanguageCode = "";
        if (getCurrentLanguageCode(text).equals("ru")) {
            targetLanguageCode = "en";
        } else {
            targetLanguageCode = "ru";
        }

        /*
          A Request body should be like this, according to Yandex Translation API documentation
          {
              "folder_id": "b1gvmob95yysaplct532",
              "format": "string",
              "texts": ["Hello", "World"],
              "targetLanguageCode": "ru"
          }
          but I use only single String in translation, so I don't need to send an array of Strings
        */
        Map<String, String> parameters = new HashMap<>();
        parameters.put("targetLanguageCode", targetLanguageCode);
        parameters.put("format", "PLAIN_TEXT");
        parameters.put("texts", text);
        parameters.put("folder_id", folderId);

        String json = getJsonString(parameters);

        JSONObject jsonObject = getJsonResponseObject(json, "https://translate.api.cloud.yandex.net/translate/v2/translate");

        /*
            JSONObject will be received in this format
        {
            "translations": [
            {
              "text": "string",
              "detectedLanguageCode": "string"
            }
          ]
        }
         */
        return jsonObject.getJSONArray("translations")
                .getJSONObject(0).getString("text");
    }

    private String getCurrentLanguageCode(String text) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("text", text);
        parameters.put("folderId", folderId);

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
