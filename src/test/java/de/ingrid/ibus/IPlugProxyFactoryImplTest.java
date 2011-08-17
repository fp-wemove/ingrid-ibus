/*
 * Copyright (c) 1997-2005 by media style GmbH
 * 
 * $Source: $
 */

package de.ingrid.ibus;

import junit.framework.TestCase;
import net.weta.components.communication.configuration.ClientConfiguration;
import net.weta.components.communication.configuration.ServerConfiguration;
import net.weta.components.communication.configuration.ClientConfiguration.ClientConnection;
import net.weta.components.communication.reflect.ProxyService;
import net.weta.components.communication.tcp.TcpCommunication;
import de.ingrid.ibus.net.IPlugProxyFactoryImpl;
import de.ingrid.utils.IPlug;
import de.ingrid.utils.IngridHits;
import de.ingrid.utils.PlugDescription;
import de.ingrid.utils.query.IngridQuery;
import de.ingrid.utils.queryparser.QueryStringParser;

/**
 * 
 */
public class IPlugProxyFactoryImplTest extends TestCase {

    private int fHowMuchPlugs = 3;

    private Bus fBus;

    private PlugDescription[] plugDescriptions = new PlugDescription[this.fHowMuchPlugs];

    private TcpCommunication[] fPlugComms = new TcpCommunication[this.fHowMuchPlugs];

    private TcpCommunication fBusComm;

    protected void setUp() throws Exception {
        this.fBusComm = new TcpCommunication();
        ServerConfiguration serverConfiguration = new ServerConfiguration();
		serverConfiguration.setPort(9191);
		serverConfiguration.setName("/101tec-group:ibus1");
		this.fBusComm.configure(serverConfiguration);
        this.fBusComm.setPeerName("/101tec-group:ibus1");
        this.fBusComm.startup();

        this.fBus = new Bus(new IPlugProxyFactoryImpl(this.fBusComm));
        for (int i = 0; i < this.plugDescriptions.length; i++) {
            this.fPlugComms[i] = new TcpCommunication();
            ClientConfiguration clientConfiguration = new ClientConfiguration();
			clientConfiguration.setName("/101tec-group:iplug" + i);
			ClientConnection clientConnection = clientConfiguration.new ClientConnection();
			clientConnection.setServerIp("127.0.0.1");
			clientConnection.setServerPort(9191);
			clientConnection.setServerName("/101tec-group:ibus1");
			clientConfiguration.addClientConnection(clientConnection);
			this.fPlugComms[i].configure(clientConfiguration);
            this.fPlugComms[i].setPeerName("/101tec-group:iplug" + i);
            this.fPlugComms[i].startup();

            ProxyService.createProxyServer(this.fPlugComms[i], IPlug.class, new DummyIPlug());
            this.plugDescriptions[i] = new PlugDescription();
            this.plugDescriptions[i].setProxyServiceURL("/101tec-group:iplug" + i);
            this.plugDescriptions[i].setIPlugClass(DummyIPlug.class.getName());
            this.plugDescriptions[i].setRecordLoader(false);
            this.plugDescriptions[i].addField("ort");
            this.fBus.getIPlugRegistry().addPlugDescription(this.plugDescriptions[i]);
            this.fBus.getIPlugRegistry().activatePlug("/101tec-group:iplug"+i);
        }
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        for (int i = 0; i < this.plugDescriptions.length; i++) {
            this.fPlugComms[i].shutdown();
        }
        this.fBusComm.shutdown();
        this.fBus.close();
    }

    /**
     * @throws Exception
     */
    public void testSearch() throws Exception {
        IngridQuery query = QueryStringParser.parse("fische ort:halle");
        for (int i = 0; i < 3; i++) {
            IngridHits hits = this.fBus.search(query, 10, 1, Integer.MAX_VALUE, 1000);
            assertEquals(this.plugDescriptions.length * 2, hits.getHits().length);
        }
    }
}