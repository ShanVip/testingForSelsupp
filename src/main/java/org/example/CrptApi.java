package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final Semaphore requestSemaphore;
    private final long requestIntervalMillis;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.requestSemaphore = new Semaphore(requestLimit);
        this.requestIntervalMillis = timeUnit.toMillis(1) / requestLimit;
    }

    public void createDocument(String document, String signature) {
        try {

            requestSemaphore.acquire();
            try {
                System.out.println("Thread " + Thread.currentThread().getId() + " - Sending API request");
                sendApiRequest(document, signature);


                Thread.sleep(requestIntervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {

                requestSemaphore.release();
                System.out.println("Thread " + Thread.currentThread().getId() + " - Released permit");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void sendApiRequest(String document, String signature) {

        String apiUrl = "https://fairsign.com/api/createDocument";


        String requestBody = createRequestBody(document, signature);


        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(apiUrl);
            httpPost.setEntity(new StringEntity(requestBody));

            HttpResponse response = httpClient.execute(httpPost);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String createRequestBody(String document, String signature) {

        ObjectMapper objectMapper = new ObjectMapper();

        return "{ \"document\": " + document + ", \"signature\": \"" + signature + "\" }";
    }


    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 10);


        String document = "{ \"item\": \"Product\" }";
        String signature = "sample_signature";


        for (int i = 0; i < 20; i++) {
            Thread thread = new Thread(() -> crptApi.createDocument(document, signature));
            thread.start();
            System.out.println("Thread " + thread.getId() + " - Started");
        }
    }
}
