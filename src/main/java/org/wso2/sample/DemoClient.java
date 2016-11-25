package org.wso2.sample;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class DemoClient {
    public static void main(String[] args) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost uploadFile = new HttpPost("http://localhost:8283/services/RiskPortfolioManagement");
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        Charset chars = Charset.forName("UTF-8");
        builder.setCharset(chars);
        String request = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsdl=\"http://ondemand.riskmetrics.com/WSGateway/PortfolioManagement/1.0/wsdl\">\n" +
                "   <soap:Header/>\n" +
                "   <soap:Body>\n" +
                "      <wsdl:UploadPortfolio>\n" +
                "         <wsdl:URI>cid:700464694665</wsdl:URI>\n" +
                "         <wsdl:PortfolioID>Yu-Test</wsdl:PortfolioID>\n" +
                "         <wsdl:AsOfDate>20161115</wsdl:AsOfDate>\n" +
                "      </wsdl:UploadPortfolio>\n" +
                "   </soap:Body>\n" +
                "</soap:Envelope>";

        builder.addTextBody(request, "yes", ContentType.create("multipart/related"));

// This attaches the file to the POST:
        File f = new File("/home/harsha/msci-files/RML4-one.xml");
        builder.addBinaryBody(
                "file",
                new FileInputStream(f),
                ContentType.TEXT_XML,
                f.getName()
        );

        HttpEntity multipart = builder.build();
        uploadFile.setEntity(multipart);
        CloseableHttpResponse response = httpClient.execute(uploadFile);
        HttpEntity responseEntity = response.getEntity();
    }
}
