package com.xbi.contents.mining.tools;

import org.deeplearning4j.ui.UiServer;

/**
 * Created by usr0101862 on 2016/06/12.
 */
public class DataVisServer {

    public static void main(String[] args) throws Exception {
        UiServer server = UiServer.getInstance();
        System.out.println("Started on port " + server.getPort());
    }

}
