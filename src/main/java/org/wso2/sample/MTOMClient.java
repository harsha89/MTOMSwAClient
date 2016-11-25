
/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.sample;
import com.ctc.wstx.api.WstxOutputProperties;
import org.apache.axiom.om.*;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.attachments.Attachments;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.httpclient.Header;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import javax.activation.FileDataSource;
import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MTOMClient {

    private static final int BUFFER = 2048;
    public static final String FS = System.getProperty("file.separator");
    public static final String LF = "\n";
    public static final String CR = "\r";
    public static final String CRLF = CR + LF;

    private static String getProperty(String name, String def) {
        String result = System.getProperty(name);
        if (result == null || result.length() == 0) {
            result = def;
        }
        return result;
    }

    private static ServiceClient createServiceClient() throws AxisFault {
        //String repo = getProperty("repository", "client_repo");
        String repo = "/home/harsha/wso2/qsp/wso2esb-5.0.0/samples/axis2Client/client_repo";
        if (repo != null && !"null".equals(repo)) {
            ConfigurationContext configContext =
                    ConfigurationContextFactory.
                            createConfigurationContextFromFileSystem(repo,
                                    repo + File.separator + "conf" + File.separator + "axis2.xml");
            return new ServiceClient(configContext, null);
        } else {
            return new ServiceClient();
        }
    }

    public static void main(String[] args) throws Exception {

        String targetEPR = getProperty("opt_url", "http://localhost:8283/services/RiskPortfolioManagement");
        String fileName = getProperty("opt_file", "/home/harsha/msci-files/RML4-one.xml");
        String mode = getProperty("opt_mode", "mtom");

        if (args.length > 0) mode = args[0];
        if (args.length > 1) targetEPR = args[1];
        if (args.length > 2) fileName = args[2];
        sendUsingSwA(fileName, targetEPR);
        // let the server read the stream before exit
        Thread.sleep(1000);
        System.exit(0);
    }

    public static MessageContext sendUsingSwA(String fileName, String targetEPR) throws IOException {

        Options options = new Options();
        options.setTo(new EndpointReference(targetEPR));
        options.setAction("urn:RiskMetricsWS:1.0:PortfolioManagement:UploadPortfolio");
        options.setProperty(Constants.Configuration.ENABLE_SWA, Constants.VALUE_TRUE);
        List list = new ArrayList();

        Header headerT = new Header("Accept-Encoding", "gzip,deflate");
        Header headerM = new Header("MIME-Version", "1.0");
        Header headerC = new Header("Connection", "Keep-Alive");
        list.add(headerT);
        list.add(headerM);
        list.add(headerC);
        options.setProperty(org.apache.axis2.transport.http.HTTPConstants.HTTP_HEADERS, list);

        ServiceClient sender = createServiceClient();
        sender.getOptions().setProperty(HTTPConstants.CHUNKED, false);
        sender.setOptions(options);
        OperationClient mepClient = sender.createClient(ServiceClient.ANON_OUT_IN_OP);

        MessageContext mc = new MessageContext();

        System.out.println("Sending file : " + fileName + " as SwA");
        FileDataSource fileDataSource = new FileDataSource(new File(fileName));
        String fileContent = FileUtils.readFileToString(new File(fileName));
        fileContent = StringUtils.replace(fileContent, LF, CRLF);
        //XMLOutputFactory xmlOutputFactory = StAXUtils.getXMLOutputFactory();
        //xmlOutputFactory.setProperty(WstxOutputProperties.P_OUTPUT_ESCAPE_CR, true);
        DataHandler dataHandler = new DataHandler(fileContent, "text/xml");
        mc.addAttachment(dataHandler);

        Attachments attachment2 = mc.getAttachmentMap();
        String[] contentId = attachment2.getAllContentIDs();

        SOAPFactory factory = OMAbstractFactory.getSOAP12Factory();
        OMNamespace ns = factory.createOMNamespace("http://ondemand.riskmetrics.com/WSGateway/PortfolioManagement/1.0/wsdl", "wsdl");
        SOAPEnvelope env = factory.getDefaultEnvelope();
        env.declareNamespace(ns);
        OMElement payload = factory.createOMElement(new QName("wsdl:UploadPortfolio"));
        OMElement uri = factory.createOMElement(new QName("wsdl:URI"));
        OMElement portfolioID = factory.createOMElement(new QName("wsdl:PortfolioID"));
        OMElement asOfDate = factory.createOMElement(new QName("wsdl:AsOfDate"));
        asOfDate.setText("20161124");
        uri.setText("cid:" + contentId[0]);
        portfolioID.setText(contentId[0]);
        payload.addChild(uri);
        payload.addChild(portfolioID);
        payload.addChild(asOfDate);
        env.getBody().addChild(payload);
        mc.setEnvelope(env);

        mepClient.addMessageContext(mc);
        mepClient.execute(true);
        MessageContext response = mepClient.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
        SOAPEnvelope envelope = response.getEnvelope();
        return response;
    }

}
