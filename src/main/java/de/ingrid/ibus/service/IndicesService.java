package de.ingrid.ibus.service;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.annotation.PreDestroy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import de.ingrid.ibus.model.ElasticsearchInfo;
import de.ingrid.ibus.model.Index;
import de.ingrid.ibus.model.IndexState;
import de.ingrid.ibus.model.SearchResult;

@Service
public class IndicesService {

    private Logger log = LogManager.getLogger( IndicesService.class );

    @Autowired
    private SettingsService settingsService;

    private static final String INDEX_FIELD_LAST_INDEXED = "lastIndexed";
    private static final String INDEX_FIELD_INDEXING_STATE = "indexingState";
    private static final String INDEX_FIELD_ADMIN_URL = "adminUrl";
    private static final String INDEX_FIELD_LAST_HEARTBEAT = "lastHeartbeat";
    private static final String INDEX_FIELD_IPLUG_NAME = "iPlugName";
    private static final String INDEX_FIELD_ALIAS = "indexAlias";
    private static final String INDEX_INFO_NAME = "ingrid_meta";
    private TransportClient client;

    @Value("${index.prefix.filter:}")
    private String indexPrefixFilter;

    public IndicesService(@Value("${elasticsearch.addresses:http://localhost:9300}") final String[] elasticAddresses) throws IOException {
        
        Settings settings = Settings.builder().put( "cluster.name", "ingrid" ).build();
        client = new PreBuiltTransportClient( settings );
        
        for (String address : elasticAddresses) {
            UrlResource url = new UrlResource( address );
            client.addTransportAddress( new InetSocketTransportAddress( InetAddress.getByName( url.getURL().getHost() ), url.getURL().getPort() ) );
        }

    }

    @PreDestroy
    public void destroy() {
        client.close();
    }

    public ElasticsearchInfo getElasticsearchInfo() {
        ElasticsearchInfo info = new ElasticsearchInfo();
        List<Index> indices = new ArrayList<Index>();

        // IndicesAdminClient esIndices = client.admin().indices();
        // ClusterHealthResponse clusterHealthResponse = client.admin().cluster().prepareHealth().get();
        // clusterHealthResponse.getIndices().values();

        client.admin().indices().prepareGetSettings( "catalog" ).get();

        ImmutableOpenMap<String, IndexMetaData> esIndices = client.admin().cluster().prepareState().execute().actionGet().getState().getMetaData().getIndices();
        esIndices.forEach( (indexMap) -> {
            System.out.println( "Index: " + indexMap.key );

            // skip indices that do not start with the configured prefix, since we don't want to have all indices of a cluster
            if (!indexMap.key.startsWith( indexPrefixFilter )) {
                return;
            }

            Index index = new Index();

            addDefaultIndexInfo( indexMap.key, index, indexMap.value.getSettings() );

//            applyAdditionalData( indexMap.key, index, false );

            indices.add( index );
        } );
        
        addComponentData( indices );

        info.setIndices( indices );

        return info;
    }

    // public List<Index> getIndices() {
    // SearchResponse response = client.prepareSearch( INDEX_INFO_NAME )
    // .setTypes( "info" )
    // .setFetchSource( new String[] { "indexAlias" }, null )
    // .setSize( 1000 )
    // .get();
    //
    // long totalHits = response.getHits().totalHits;
    // }

    public Index getIndexDetail(String indexId) throws InterruptedException, ExecutionException {
        Index index = new Index();

        GetSettingsResponse getSettingsResponse = client.admin().indices().prepareGetSettings( indexId ).execute().get();
        addDefaultIndexInfo( indexId, index, getSettingsResponse.getIndexToSettings().get( indexId ) );

        applyAdditionalData( indexId, index );

        applyDetailedIndexInfo( indexId, index );

        return index;
    }

    private void applyDetailedIndexInfo(String indexName, Index index) {

        index.setMapping( getMapping( indexName ) );
    }

    private Map<String, Object> getMapping(String indexName) {
        GetMappingsResponse response = client.admin().indices()
                .prepareGetMappings( indexName )
                .execute().actionGet();

        ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = response.mappings();
        Map<String, Object> indexMapping = new HashMap<String, Object>();
        mappings.get( indexName ).forEach( (type -> {
            try {
                indexMapping.put( type.key, type.value.getSourceAsMap() );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }) );

        return indexMapping;
    }

    private void addDefaultIndexInfo(String indexName, Index index, Settings settings) {
        SearchResponse searchResponse = client.prepareSearch( indexName ).setSize( 0 ).get();

        String created = settings.get( IndexMetaData.SETTING_CREATION_DATE );

        index.setName( indexName );
        index.setNumberDocs( searchResponse.getHits().getTotalHits() );
        index.setCreated( new Date( Long.valueOf( created ) ) );
    }

    /**
     * Request data from special collection in Elasticsearch, where additional metadata is stored about an index and the corresponding
     * iPlug, that delivers the data of the index.
     * 
     * @param key
     * @param index
     */
    @SuppressWarnings("unchecked")
    private void applyAdditionalData(String indexName, Index index) {
        SearchResponse response = client.prepareSearch( INDEX_INFO_NAME )
                .setTypes( "info" )
                .setQuery( QueryBuilders.termQuery( INDEX_FIELD_ALIAS, indexName ) )
                .setFetchSource( new String[] { "*" }, null )
                .setSize( 1 )
                .get();

        long totalHits = response.getHits().totalHits;
        if (totalHits == 1) {
            Map<String, Object> hitSource = response.getHits().getAt( 0 ).getSource();
            String id = response.getHits().getAt( 0 ).getId();

            index.setId( id );
            index.setLongName( hitSource.get( INDEX_FIELD_IPLUG_NAME ).toString() );
            index.setLastIndexed( mapDate( (String) hitSource.get( INDEX_FIELD_LAST_INDEXED ) ) );
            index.setActive( settingsService.isActive( id ) );

            index.setLastHeartbeat( mapDate( hitSource.get( INDEX_FIELD_LAST_HEARTBEAT ).toString() ) );
            index.setAdminUrl( hitSource.get( INDEX_FIELD_ADMIN_URL ).toString() );
            index.setIndexingState( mapIndexingState( (Map<String, Object>) hitSource.get( INDEX_FIELD_INDEXING_STATE ) ) );
            index.setHasAdditionalInfo( true );

        } else {
            // throw new RuntimeException( "Number of iPlugInfo-Hits should be 1, but was: " + totalHits );
            log.error( "Number of iPlugInfo-Hits should be 1, but was: " + totalHits );
        }

    }

    private void addComponentData(List<Index> indices) {
        SearchResponse response = client.prepareSearch( INDEX_INFO_NAME )
                .setTypes( "info" )
                .setFetchSource( new String[] { "*" }, null )
                .setSize( 1000 )
                .get();
        
        SearchHit[] hits = response.getHits().getHits();
        
        for (SearchHit hit : hits) {
            Map<String, Object> hitSource = hit.getSource();
            String indexName = (String) hit.getSource().get( INDEX_FIELD_ALIAS );
            
            Index indexItem = indices.stream()
                .filter( index -> index.getName().equals( indexName ) )
                .findFirst()
                .get();
            
            if (indexItem != null) {
                indexItem.setId( hit.getId() );
                indexItem.setLongName( hitSource.get( INDEX_FIELD_IPLUG_NAME ).toString() );
                indexItem.setLastIndexed( mapDate( (String) hitSource.get( INDEX_FIELD_LAST_INDEXED ) ) );
                indexItem.setActive( settingsService.isActive( hit.getId() ) );
                indexItem.setHasAdditionalInfo( true );
            }
        }
    }

    private IndexState mapIndexingState(Map<String, Object> state) {
        IndexState indexState = new IndexState();
        // indexState.setMessage( message );
        indexState.setNumProcessed( Integer.valueOf( state.get( "numProcessed" ).toString() ) );
        indexState.setRunning( state.get( "running" ).equals( true ) );
        indexState.setTotalDocs( Integer.valueOf( state.get( "totalDocs" ).toString() ) );
        return indexState;
    }

    private Date mapDate(String date) {
        if (date != null) {
            return new DateTime( date ).toDate();
        } else {
            return null;
        }
    }

    public List<SearchResult> search(String query) {
        String[] indices = getActiveIndices();
        
        SearchResponse response = client.prepareSearch( indices )
                .setQuery( QueryBuilders.queryStringQuery( query.trim().length() == 0 ? "*" : query ) )
                .setFetchSource( new String[] { "*" }, null )
                .setSize( 10 )
                .get();
        
        SearchHit[] hits = response.getHits().getHits();
        
        List<SearchResult> results = new ArrayList<SearchResult>();
        for (SearchHit hit : hits) {
            SearchResult result = new SearchResult();
            result.setTitle( (String) hit.getSource().get( "title" ) );
            result.setSummary( (String) hit.getSource().get( "summary" ) );
            result.setSource( (String) hit.getSource().get( "???" ) );
            results.add( result );
        }
        return results;
    }

    private String[] getActiveIndices() {
        List<String> result = new ArrayList<String>();
        
        // get active components
        Set<String> activeComponents = settingsService.getActiveIndices();
        
        IdsQueryBuilder idsQuery = QueryBuilders.idsQuery().addIds( activeComponents.toArray( new String[0] ) );
        
        // get real index names from active components
        SearchResponse response = client.prepareSearch( INDEX_INFO_NAME )
            .setTypes( "info" )
            .setQuery( idsQuery )
            .setFetchSource( new String[] { INDEX_FIELD_ALIAS }, null )
            .get();

        // collect all referenced indices
        response.getHits().forEach( hit -> {
            result.add( (String) hit.getSource().get( INDEX_FIELD_ALIAS ) );
        });
        
        return result.toArray(new String[0]);
    }

}
