package zzhao.jcodec;

import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class HttpFlvDownloader {

    public static void main(String[] args) {

        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("http://hdl.9158.com/live/b06c7246f0a11f45b9376e8f741d8878.flv");

        try {
            CloseableHttpResponse response1 = client.execute(httpGet);
            HttpEntity entity = response1.getEntity();
            if (entity != null) {
                InputStream inputStream = entity.getContent();
                FileOutputStream fs = new FileOutputStream("e:/test2.flv");
                IOUtils.copy(inputStream, fs);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }
    

}
