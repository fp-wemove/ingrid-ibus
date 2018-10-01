/*-
 * **************************************************-
 * ingrid-ibus-backend
 * ==================================================
 * Copyright (C) 2014 - 2018 wemove digital solutions GmbH
 * ==================================================
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 * 
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl5
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * **************************************************#
 */
package de.ingrid.ibus.service;

import de.ingrid.elasticsearch.ElasticConfig;
import de.ingrid.elasticsearch.IndexInfo;
import de.ingrid.elasticsearch.IndexManager;
import de.ingrid.elasticsearch.search.IndexImpl;
import de.ingrid.ibus.comm.Bus;
import de.ingrid.ibus.comm.BusServer;
import de.ingrid.ibus.comm.registry.Registry;
import de.ingrid.ibus.comm.registry.RegistryConfigurable;
import de.ingrid.utils.*;
import de.ingrid.utils.dsc.Record;
import de.ingrid.utils.query.IngridQuery;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.Map;

@Service
public class SearchService implements IPlug, IRecordLoader, Serializable, RegistryConfigurable {
    
    private static Logger log = LogManager.getLogger( SearchService.class );
    
    public static final String CENTRAL_INDEX_ID = "__centralIndex__";

    /**
     * 
     */
    private static final long serialVersionUID = 7102378897547409841L;

    @Autowired
    private IndicesService indexService;
    
    @Autowired
    private IndexManager indexManager;

    @Autowired
    private IndexImpl indexUtils;
    
    @Autowired
    private ElasticConfig elasticConfig;
    
    private String[] fields = new String[] { "metainfo", "t01_object.obj_id", "t02_address","t02_address.adr_id", "capabilities_url","parent","city","iPlugId","organisation","kml","refering","title","content","t011_obj_geo","t02_address4","t02_address3","t02_address5","children","datatype","provider","additional_html_1","street","y1","y2","t021_communication","t011_obj_serv","t02_address2","t022_adr_adr3","summary","t011_obj_serv_op_connpoint","zip","publish_id","t022_adr_adr","t03_catalogue","t012_obj_adr","idf","title2","title3","dataSourceName","t01_object","partner","refering_service_uuid","x1","boost","x2","parent4","parent5","object_reference","parent2","parent3","incl_meta","t01_object.org_obj_id", "t01_object.obj_class","metaclass" };
    private String[] datatypes = new String[] { "metadata", "dsc_ecs", "default", "topics", "dsc_ecs_address", "address", "IDF_1.0" };
    private SimulatedLifesign currentSimulatedLifesign = null;

    @Override
    public void handleRegistryUpdate(Registry registry) {

        if (currentSimulatedLifesign != null) {
            currentSimulatedLifesign.close();
        }

        PlugDescription pd = new PlugDescription();
        pd.setProxyServiceURL( CENTRAL_INDEX_ID );
        pd.setDataSourceName("Central Index");
        pd.setIPlugClass( "igesearchplug" );
        pd.setRecordLoader( true );
        pd.setDataSourceDescription( "central index" );

        for (String datatype: datatypes) {
            pd.addDataType( datatype );
        }

        for (String field : fields) {
            pd.addField( field );
        }

        pd.put( "overrideProxy", this );
        registry.addPlugDescription( pd  );

        registry.activatePlug( CENTRAL_INDEX_ID );

        currentSimulatedLifesign = new SimulatedLifesign(registry, pd);
    }

    public IngridHits searchAndDetail(IngridQuery query, int hitsPerPage, int currentPage, int startHit, int maxMilliseconds, String[] requestedFields) {
        try {

            @SuppressWarnings("deprecation")
            IngridHits iPlugsResult = Bus.getInstance().searchAndDetail( query, 10, 0, 0, 30000, null );
            IngridHit[] iPlugHits = iPlugsResult.getHits();
            
            return new IngridHits( (int) iPlugsResult.length(), iPlugHits );
            
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
    public IngridHits search(IngridQuery query, int start, int length) throws Exception {
        
        elasticConfig.communicationProxyUrl = CENTRAL_INDEX_ID;
        elasticConfig.partner = new String[] { "???" };
        elasticConfig.provider = new String[] { "???" };

        try {
            elasticConfig.activeIndices = indexService.getActiveIndices();
            return indexUtils.search( query, start, length );
        } catch (NoNodeAvailableException ex) {
            log.warn("No search on elasticsearch since not connected to node");
            return new IngridHits( 0, new IngridHit[0] );
        }
    }

    @Override
    public IngridHitDetail getDetail(IngridHit hit, IngridQuery query, String[] requestedFields) throws Exception {
        return indexUtils.getDetail( hit, query, requestedFields );
    }

    @Override
    public IngridHitDetail[] getDetails(IngridHit[] hits, IngridQuery query, String[] requestedFields) throws Exception {
        return indexUtils.getDetails( hits, query, requestedFields );
    }

    @SuppressWarnings("unchecked")
    @Override
    public IngridDocument call(IngridCall targetInfo) throws Exception {
        IngridDocument doc = new IngridDocument();
        
        Map<String, Object> parameters = null;
        Object parameter = null;

        switch (targetInfo.getMethod()) {
        case "createIndex":
            parameters = (Map<String, Object>) targetInfo.getParameter();
            if (log.isDebugEnabled()) {
                log.debug("Create index: " + parameters.get( "name" ));
            }

            boolean success = indexManager.createIndex(
                    (String) parameters.get( "name" ),
                    "_default_",
                    (String) parameters.get( "mapping" ));

            doc.put( "result", success );
            break;
            
        case "getIndexNameFromAliasName":
            parameters = (Map<String, Object>) targetInfo.getParameter();
            String aliasName = indexManager.getIndexNameFromAliasName(
                    (String) parameters.get( "indexAlias" ), 
                    (String) parameters.get( "partialName" ) );
            doc.put( "result", aliasName );
            break;
            
        case "switchAlias":
            parameters = (Map<String, Object>) targetInfo.getParameter();
            if (log.isDebugEnabled()) {
                log.debug("Switch alias: " + parameters.get( "aliasName" ) +
                        " from: " + parameters.get( "oldIndex" ) +
                        " to: " + parameters.get( "newIndex" ));
            }

            indexManager.switchAlias(
                    (String) parameters.get( "aliasName" ),
                    (String) parameters.get( "oldIndex" ),
                    (String) parameters.get( "newIndex" ) );
            break;
            
        case "checkAndCreateInformationIndex":
            indexManager.checkAndCreateInformationIndex();
            break;
            
        case "getIndexTypeIdentifier":
            parameter = targetInfo.getParameter();
            String resultIndexTypeIdent = indexManager.getIndexTypeIdentifier( (IndexInfo) parameter );
            doc.put( "result", resultIndexTypeIdent );
            break;
            
        case "update":
            parameters = (Map<String, Object>) targetInfo.getParameter();
            if (log.isDebugEnabled()) {
                IndexInfo indexinfo = (IndexInfo) parameters.get("indexinfo");
                log.debug("Update document: " + ((ElasticDocument)parameters.get( "doc" )).get(indexinfo.getDocIdField()));
            }

            indexManager.update(
                    (IndexInfo) parameters.get( "indexinfo" ),
                    (ElasticDocument) parameters.get( "doc" ),
                    (boolean) parameters.get( "updateOldIndex" ) );
            break;
            
        case "updateIPlugInformation":
            parameters = (Map<String, Object>) targetInfo.getParameter();
            indexManager.updateIPlugInformation(
                    (String) parameters.get( "id" ),
                    (String) parameters.get( "info" ) );
            break;
            
        case "flush":
            indexManager.flush();
            break;
            
        case "deleteIndex":
            parameter = targetInfo.getParameter();
            if (log.isDebugEnabled()) {
                log.debug("Delete index: " + parameter);
            }

            indexManager.deleteIndex( (String) parameter );
            break;
            
        case "getMapping":
            parameter = targetInfo.getParameter();
            Map<String, Object> resultMapping = indexManager.getMapping( (IndexInfo) parameter );
            doc.put( "result", resultMapping );
            break;
            
        case "updateHearbeatInformation":
            parameter = targetInfo.getParameter();
            indexManager.updateHearbeatInformation( (Map<String, String>) parameter );
            break;
            
        default:
            log.error( "Calling method not supported: " + targetInfo.getMethod() );
        }
        return doc;
    }

    @Override
    public void configure(PlugDescription plugDescription) throws Exception {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Record getRecord(IngridHit hit) throws Exception {
        Record record = new Record();
        
        ElasticDocument doc = indexUtils.getDocById( hit.getDocumentId() );
        String data = (String) doc.get( "idf" );
        record.put( "data", data );
        record.put( "compressed", "false" );
        return record;
    }
}