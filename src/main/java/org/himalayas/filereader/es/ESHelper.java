/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.es;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.metrics.sum.Sum;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.himalayas.filereader.util.Config;
import org.himalayas.filereader.util.DataFormat;
import org.himalayas.filereader.util.FieldType;

/**
 *
 * @author ghfan
 */
public
    class ESHelper {

    private final
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

    private
        RestHighLevelClient productionClient = null;
    private
        RestHighLevelClient testClient = null;
    private
        SearchRequest searchUnitRequest = null;
    private
        SearchRequest searchFileRequest = null;
    private
        SearchRequest searchLotRequest = null;
    private
        AggregationBuilder grossTimeAgg = null;
    private
        SearchResponse searchResponse = null;
    final
        Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1L));
    private
        LotInfo lotInfo = new LotInfo();
    private
        ArrayList<LotInfo> lotList = new ArrayList();
    private
        DataFormat dataFormat = null;
    private static
        ESHelper instance = null;

    private
        ESHelper() {
    }

    public static
        ESHelper getInstance() {
        if (ESHelper.instance == null) {
            ESHelper.instance = new ESHelper();
        }
        return ESHelper.instance;
    }

    public
        boolean init() {

        if (!initProductionClient()) {
            System.out.printf("%s: failed to connect production es host!\n", LocalDateTime.now().toString());
            System.out.printf("Please make sure those host are available: %s\n", Arrays.toString(Config.productionHost.toArray()));
            return false;
        }
        if (!initTestClient()) {
            System.out.printf("%s: failed to connect test es host!\n", LocalDateTime.now().toString());
            System.out.printf("Please make sure those host are available: %s\n", Config.testHost);
            return false;
        }
        this.initSearchUnitRequest();
        this.initSearchFileRequest();
        this.initSearchLotRequest();
        return true;
    }

    private
        boolean initProductionClient() {
        try {
            this.credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("admin", "TDnI@Admin"));
            HttpHost[] httpHosts = new HttpHost[Config.productionHost.size()];
            int hostNo = 0;
            for (String host : Config.productionHost) {
                httpHosts[hostNo] = new HttpHost(host, 9200, "http");
            }
            RestClientBuilder builder = RestClient.builder(httpHosts);
            builder.setMaxRetryTimeoutMillis(6 * 60 * 1000);
            builder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                @Override
                public
                    HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                    //return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
                        .setConnectTimeout(2 * 60 * 1000)
                        .setSocketTimeout(2 * 60 * 1000)
                        .setConnectionRequestTimeout(1 * 60 * 1000);
                    httpClientBuilder.setDefaultRequestConfig(requestConfigBuilder.build())
                        .setDefaultCredentialsProvider(credentialsProvider);
                    return httpClientBuilder;
                }
            });
            this.productionClient = new RestHighLevelClient(builder);
            return productionClient.ping(RequestOptions.DEFAULT);

        }
        catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private
        boolean initTestClient() {
        try {
            HttpHost[] httpHosts = new HttpHost[1];
            httpHosts[0] = new HttpHost(Config.testHost, 9200, "http");

            RestClientBuilder builder = RestClient.builder(httpHosts);
            builder.setMaxRetryTimeoutMillis(6 * 60 * 1000);
            builder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                @Override
                public
                    HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                    RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
                        .setConnectTimeout(2 * 60 * 1000)
                        .setSocketTimeout(2 * 60 * 1000)
                        .setConnectionRequestTimeout(1 * 60 * 1000);
                    httpClientBuilder.setDefaultRequestConfig(requestConfigBuilder.build())
                        .setDefaultCredentialsProvider(credentialsProvider);
                    return httpClientBuilder;
                }
            });
            this.testClient = new RestHighLevelClient(builder);
            return this.testClient.ping(RequestOptions.DEFAULT);

        }
        catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public
        boolean closeConn() {
        try {
            if (productionClient != null) {
                productionClient.close();
                System.out.printf("%s:%s\n", LocalDateTime.now().toString(), "successed to close the test client");
            }
            if (testClient != null) {
                testClient.close();
                System.out.printf("%s:%s\n", LocalDateTime.now().toString(), "successed to close the test client");
            }
            return true;
        }
        catch (Exception ex) {
            System.out.printf("%s:%s\n", LocalDateTime.now().toString(), "failed to close the client");
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * private boolean handlerInit() { this.lotNumber =
     * camData.getOrDefault("Lot", null); this.operation =
     * operStepMap.getOrDefault(camData.getOrDefault("Oper", "UnknowOper"),
     * null); switch (operation) { case "FT": { lotIndexName =
     * EnvConf.getFtLotIndexName(); unitIndexName =
     * EnvConf.getFtUnitIndexName(); dataFormat = ftDataFormat; break; } case
     * "FT2": { lotIndexName = EnvConf.getFtLotIndexName(); unitIndexName =
     * EnvConf.getFtUnitIndexName(); dataFormat = ftDataFormat; break; } case
     * "FT-FUSE": { lotIndexName = EnvConf.getFtLotIndexName(); unitIndexName =
     * EnvConf.getFtUnitIndexName(); dataFormat = ftDataFormat; break; } case
     * "SLT": { lotIndexName = EnvConf.getSltLotIndexName(); unitIndexName =
     * EnvConf.getSltUnitIndexName(); dataFormat = sltDataFormat; break; } case
     * "CESLT": { lotIndexName = EnvConf.getSltLotIndexName(); unitIndexName =
     * EnvConf.getSltUnitIndexName(); dataFormat = sltDataFormat; break; }
     * default: { lotIndexName = null; unitIndexName = null; dataFormat = null;
     * break; } }
     *
     * if (lotNumber == null || operation == null || dataFormat == null ||
     * lotIndexName == null || unitIndexName == null) { StringBuilder strBud =
     * new StringBuilder(); if (lotNumber == null) { strBud.append("Lot Number
     * is Null!\n"); } if (operation == null) { strBud.append("Operation is
     * Null!\n"); } if (dataFormat == null) { strBud.append("Data Format is
     * Null\n"); } if (lotIndexName == null) { strBud.append("Lot Index Name is
     * Null\n"); } if (unitIndexName == null) { strBud.append("Unit Index Name
     * is Null\n"); } logger.info(strBud.toString()); strBud.setLength(0);
     * return false; }
     *
     * this.setDataFormat(dataFormat);
     * this.initSearchUnitRequest(unitIndexName);
     * this.initSearchFileRequest(unitIndexName); return true; }
     */
    private
        void initSearchUnitRequest() {
        this.searchUnitRequest = new SearchRequest();
        this.searchUnitRequest.indices(this.dataFormat.getTestIndexName());
        this.searchUnitRequest.scroll(this.scroll);

        // source unit_id, bin_type
        String unitIDName = this.dataFormat.getUnit().getUnitIdNode().getName();
        String startTimeName = this.dataFormat.getUnit().getStartTimeNode().getName();
        String endTimeName = this.dataFormat.getUnit().getEndTimeNode().getName();
        String testTimeName = this.dataFormat.getUnit().getTestTimeNode().getName();
        String[] includeFields = new String[]{unitIDName, FieldType.BinType, startTimeName, endTimeName, testTimeName, FieldType.DieType};
        String[] excludeFields = new String[]{"_type"};

        this.searchUnitRequest.source(new SearchSourceBuilder()
            .size(500)
            .timeout(new TimeValue(180, TimeUnit.SECONDS))
            .fetchSource(true)
            .fetchSource(includeFields, excludeFields));

    }

    private
        void initSearchFileRequest() {
        this.searchFileRequest = new SearchRequest();
        this.searchFileRequest.indices(this.dataFormat.getTestIndexName());
        this.searchFileRequest.source(new SearchSourceBuilder()
            .size(0)
            .timeout(new TimeValue(180, TimeUnit.SECONDS))
            .fetchSource(false));

//		searchSourceBuilder.profile(true);
//		this.aggregation = AggregationBuilders.global("LotInfo")
//			.subAggregation(AggregationBuilders.sum(FieldType.GrossTime).field(FieldType.GrossTime))
//			.subAggregation(AggregationBuilders.sum(FieldType.UnitCnt).field(FieldType.Unit));
        this.grossTimeAgg = AggregationBuilders.sum(FieldType.GrossTime).field(FieldType.GrossTime);

    }

    private
        void initSearchLotRequest() {
        this.searchLotRequest = new SearchRequest();
        this.searchLotRequest.indices(this.dataFormat.getLotIndexName());
        this.searchLotRequest.scroll(this.scroll);

        // source unit_id, bin_type
        String lotNumberName = this.dataFormat.getLotNumberNode().getName();
        String operationName = this.dataFormat.getOperationNode().getName();
        String camLotName = Config.camFormat.getLotNumberNode().getName();

        String[] includeFields = new String[]{lotNumberName, operationName, camLotName};
        String[] excludeFields = new String[]{"_type"};

        this.searchLotRequest.source(new SearchSourceBuilder()
            .size(500)
            .timeout(new TimeValue(180, TimeUnit.SECONDS))
            .fetchSource(true)
            .fetchSource(includeFields, excludeFields));

    }

    /**
     *
     * @param lotNumber
     * @param operation
     */
    private
        void InitLot(String lotNumber, String operation) {
        this.getLotInfo().reset();
        this.getLotInfo().setLotNumber(lotNumber);
        this.getLotInfo().setLotNumberName(this.dataFormat.getLotNumberNode().getName());
        this.getLotInfo().setOperation(operation);
        this.getLotInfo().setOperationName(this.dataFormat.getOperationNode().getName());
    }

    /**
     * query unit data with lotNumber, operation and dataType
     *
     * @param lotNumber
     * @param operation
     * @param dataType
     * @return
     */
    private
        boolean getUnitData() {
        this.getLotInfo().setWaferSort(this.dataFormat.getDataType().equals(Config.DataTypes.WaferSort));

        this.searchUnitRequest.source().query(this.getQueryBuilder(this.getLotInfo().getLotNumber(), this.getLotInfo().getOperation(), FieldType.Unit));
        try {
            this.searchResponse = this.productionClient.search(this.searchUnitRequest, RequestOptions.DEFAULT);

            RestStatus status = this.searchResponse.status();
            TimeValue took = this.searchResponse.getTook();
            Boolean terminatedEarly = this.searchResponse.isTerminatedEarly();
            boolean timedOut = this.searchResponse.isTimedOut();

            int totalShards = this.searchResponse.getTotalShards();
            int successfulShards = this.searchResponse.getSuccessfulShards();
            int failedShards = this.searchResponse.getFailedShards();
            for (ShardSearchFailure failure : this.searchResponse.getShardFailures()) {
                // failures should be handled here
            }

            String scrollId = this.searchResponse.getScrollId();
            SearchHit[] searchHits = this.searchResponse.getHits().getHits();

            if (searchHits == null || searchHits.length < 1) {
                System.out.printf("%s: there's no any unit data found in this query\n", LocalDateTime.now().toString());
                return false;
            }

            this.fillUnitData(searchHits);

            while (searchHits != null && searchHits.length > 0) {
                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(scroll);
                this.searchResponse = this.productionClient.scroll(scrollRequest, RequestOptions.DEFAULT);
                scrollId = this.searchResponse.getScrollId();

                searchHits = this.searchResponse.getHits().getHits();
                this.fillUnitData(searchHits);
            }

            ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
            clearScrollRequest.addScrollId(scrollId);
            ClearScrollResponse clearScrollResponse = this.productionClient.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
            boolean succeeded = clearScrollResponse.isSucceeded();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;

    }

    /**
     *
     * @param lotNumber
     * @param operation
     * @param dataType
     * @return
     */
    private
        boolean getLotGrossTimeAggData() {
        this.searchFileRequest.source().query(this.getAggQueryBuilder(this.getLotInfo().getLotNumber(), this.getLotInfo().getOperation(), FieldType.File));
        try {

            this.searchFileRequest.source().aggregation(this.grossTimeAgg);
            searchResponse = productionClient.search(searchFileRequest, RequestOptions.DEFAULT);

            RestStatus status = this.searchResponse.status();
            TimeValue took = this.searchResponse.getTook();
            Boolean terminatedEarly = this.searchResponse.isTerminatedEarly();
            boolean timedOut = this.searchResponse.isTimedOut();

            int totalShards = this.searchResponse.getTotalShards();
            int successfulShards = this.searchResponse.getSuccessfulShards();
            int failedShards = this.searchResponse.getFailedShards();
            for (ShardSearchFailure failure : this.searchResponse.getShardFailures()) {
                // failures should be handled here
            }

            long totalHits = this.searchResponse.getHits().getTotalHits();
            this.getLotInfo().setTotalFileCnt(totalHits);
            this.fillLotGrossTimeData();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;

    }

    /**
     * this method is to get all the uncaled lot list
     *
     * @return
     */
    private
        boolean getLotListData() {

        this.searchLotRequest.source().query(QueryBuilders.boolQuery()
            .must(QueryBuilders.termQuery(FieldType.IsCaled, "N"))
            .must(QueryBuilders.termQuery(FieldType.DataType, this.dataFormat.getDataType().toString()))
        );
        try {
            this.searchResponse = this.productionClient.search(this.searchLotRequest, RequestOptions.DEFAULT);

            RestStatus status = this.searchResponse.status();
            TimeValue took = this.searchResponse.getTook();
            Boolean terminatedEarly = this.searchResponse.isTerminatedEarly();
            boolean timedOut = this.searchResponse.isTimedOut();

            int totalShards = this.searchResponse.getTotalShards();
            int successfulShards = this.searchResponse.getSuccessfulShards();
            int failedShards = this.searchResponse.getFailedShards();
            for (ShardSearchFailure failure : this.searchResponse.getShardFailures()) {
                // failures should be handled here
            }

            String scrollId = this.searchResponse.getScrollId();
            SearchHit[] searchHits = this.searchResponse.getHits().getHits();
            if (searchHits == null || searchHits.length < 1) {
                System.out.printf("%s: there's no any unit data found in this query\n", LocalDateTime.now().toString());
                return false;
            }
            this.fillLotListData(searchHits);

            while (searchHits != null && searchHits.length > 0) {
                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(scroll);
                this.searchResponse = this.productionClient.scroll(scrollRequest, RequestOptions.DEFAULT);
                scrollId = this.searchResponse.getScrollId();

                searchHits = this.searchResponse.getHits().getHits();
                this.fillLotListData(searchHits);
            }

            ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
            clearScrollRequest.addScrollId(scrollId);
            ClearScrollResponse clearScrollResponse = this.productionClient.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
            boolean succeeded = clearScrollResponse.isSucceeded();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;

    }

    private
        void fillLotGrossTimeData() {
        Aggregations aggregations = this.searchResponse.getAggregations();
        for (Aggregation agg : aggregations) {
            String name = agg.getName();
            if (name.equals(FieldType.GrossTime)) {
                this.getLotInfo().setGrossTestTime(((Sum) agg).getValue());
            }
        }
    }

    private
        void fillUnitData(SearchHit[] searchHits) {
        for (SearchHit hit : searchHits) {
            String index = hit.getIndex();
            String id = hit.getId();
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();

            String unitID = (String) sourceAsMap.get(this.dataFormat.getUnit().getUnitIdNode().getName());
            String startTime = (String) sourceAsMap.get(this.dataFormat.getUnit().getStartTimeNode().getName());
            String endTime = (String) sourceAsMap.get(this.dataFormat.getUnit().getEndTimeNode().getName());
            int binType = Integer.valueOf((String) sourceAsMap.get(FieldType.BinType));
            boolean masterDie = ((String) sourceAsMap.get(FieldType.DieType)).equals(FieldType.MasterDie);
            double testTime = Double.valueOf((String) sourceAsMap.get(this.dataFormat.getUnit().getTestTimeNode().getName()));

            // no unit id case here
            if (unitID == null) {
                unitID = id;
            }

            DataSet dataSet = this.getLotInfo().getDataSets().get(unitID);

            if (dataSet == null) {
                dataSet = new DataSet(unitID);
                this.getLotInfo().getDataSets().put(unitID, dataSet);
                dataSet.setMasterDie(masterDie);
            }
            dataSet.getUnitData().add(new Doc(id, binType, startTime, endTime, index, testTime));
        }
    }

    private
        void fillLotListData(SearchHit[] searchHits) {
        for (SearchHit hit : searchHits) {
            String index = hit.getIndex();
            String id = hit.getId();
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();

            String lotNumber = (String) sourceAsMap.get(this.dataFormat.getLotNumberNode().getName());
            String operation = (String) sourceAsMap.get(this.dataFormat.getOperationNode().getName());
            String camLot = (String) sourceAsMap.get(Config.camFormat.getLotNumberNode().getName());

            // missmatch camstar lot case here
            if (camLot == null || camLot.isEmpty() || camLot.equalsIgnoreCase("null")) {
                System.out.printf("Warnings: there's no camstar lot for this kdf lot, %s=%s, %=%s\n",
                    this.dataFormat.getLotNumberNode().getName(), lotNumber,
                    this.dataFormat.getOperationNode().getName(), operation);
                continue;
            }
            LotInfo lot = new LotInfo();
            lot.setLotNumber(lotNumber);
            lot.setOperation(operation);
            this.lotList.add(lot);
        }
    }

    private
        boolean updateUnitData() {

        try {
            BulkRequest bulkRequest = new BulkRequest();
            bulkRequest.timeout(TimeValue.timeValueSeconds(60));
            int docCnt = 0;
            for (DataSet dataSet : this.getLotInfo().getDataSets().values()) {
                for (Doc doc : dataSet.getUnitData()) {
                    /**
                     * generate the bulk update request
                     */
                    Map<String, Object> jsonMap = new HashMap<>();
                    jsonMap.put(FieldType.Rank, doc.getMotherLotInsertion());
                    bulkRequest.add(new UpdateRequest(
                        doc.getIndex(),
                        "doc",
                        doc.getId()).doc(jsonMap));
                    docCnt++;
                }
            }

            BulkResponse bulkResponse = productionClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (bulkResponse.hasFailures()) {
                System.out.printf("%s: failed to update the unit %s\n", LocalDateTime.now().toString(), FieldType.Rank);
                for (BulkItemResponse bulkItemResponse : bulkResponse) {
                    if (bulkItemResponse.isFailed()) {
                        BulkItemResponse.Failure failure = bulkItemResponse.getFailure();
                        System.out.printf("%s:%s\n", LocalDateTime.now().toString(), failure.getMessage());
                    }
                }
                return false;
            }
            else {
                return true;
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * insert or update mother lot info doc_id: lotNumber + operation
     *
     * @return
     */
    private
        boolean updateLotData() {
        /**
         * try { Map<String, Object> jsonMap = this.getLotInfo().getJsonMap();
         *
         * if (camData != null && camData.size() > 0) { for
         * (Map.Entry<String, String> entry : camData.entrySet()) { String
         * fieldName = entry.getKey(); String fieldValue = entry.getValue(); if
         * ("IsLotMatched".equals(fieldName) || "IsLotCal".equals(fieldName)) {
         * continue; } String aliasName =
         * camFieldConfByName.containsKey(fieldName) ?
         * camFieldConfByName.get(fieldName).get("Alias") : "unknow"; if
         * ("unknow".equalsIgnoreCase(aliasName)) { jsonMap.put(fieldName,
         * fieldValue); } else { jsonMap.put(aliasName, fieldValue); }
         *
         * }
         * }
         *
         * UpdateRequest request = new
         * UpdateRequest(this.dataFormat.getLotIndexName(), "doc",
         * this.getLotInfo().getDoc_Id()).doc(jsonMap);
         * request.timeout(TimeValue.timeValueSeconds(20));
         * request.docAsUpsert(true);
         *
         * String lotNumOper = this.lotInfo.getLotNumber() + " --- " +
         * this.lotInfo.getOperation(); UpdateResponse updateResponse =
         * testClient.update(request, RequestOptions.DEFAULT);
         *
         * if (null != updateResponse.getResult()) { switch
         * (updateResponse.getResult()) { case CREATED: logger.info("Lot Created
         * : " + lotNumOper); break; case UPDATED: logger.info("Lot Updated : "
         * + lotNumOper); break; case DELETED: logger.info("Lot Deleted : " +
         * lotNumOper); break; case NOOP: logger.info("Lot Noop : " +
         * lotNumOper); break; default: break; } }
         *
         * logger.info("Update Lot Data PASS : " +
         * this.getLotInfo().toString()); } catch (Exception ex) {
         * logger.info("Update Lot Data FAIL : " +
         * this.getLotInfo().toString()); logger.error(ex.getMessage(), ex);
         * return false; }
         *
         */
        return true;
    }

    public
        void proceedUncaledLot() {
        this.getLotListData();
        for (LotInfo lot : this.lotList) {
            logLotCalEvent2ES(this.calLot(lot.getLotNumber(), lot.getOperation(), this.dataFormat), lot);
        }
    }

    private
        void logLotCalEvent2ES(boolean result, LotInfo lot) {
        System.out.printf("%s=%s,%s=%s,%s=%s,%s=%s,%s=%s\n",
            FieldType.EventType, (result ? Config.EventType.KDFException : Config.EventType.KDFException),
            FieldType.DoneTime, ZonedDateTime.now().toOffsetDateTime(),
            this.dataFormat.getLotNumberNode().getName(), lot.getLotNumber(),
            this.dataFormat.getOperationNode().getName(), lot.getOperation(),
            FieldType.DataType, this.dataFormat.getDataType().toString());
    }

    /**
     * this method is to update or insert the lot doc isCaled to 'N' the key is
     * the
     *
     * @param lotNumber
     * @param operation
     * @param dataFormat
     * @return
     */
    public
        boolean upsertLotIsCalFlag2N(String lotNumber, String operation, DataFormat dataFormat) {
        String lotIndex = dataFormat.getLotIndexName();
        //@todo upsert lot IsCaled to N
        return true;
    }

    /**
     * proceed lot is only responsible for below 2 tasks: 1) update the unit
     * Rank 2) update the lot level doc
     *
     * here we use the 'upsert' to insert or update info to a lot level doc
     * please use another method to update camstar info to this doc if needed
     *
     * @return
     */
    private
        boolean calLot(String lotNumber, String operation, DataFormat dataFromat) {
        if (lotNumber == null
            || operation == null
            || dataFormat == null) {
            return false;
        }

        this.dataFormat = dataFormat;
        this.InitLot(lotNumber, operation);

        if (!this.getUnitData()) {
            return false;
        }

        if (!getLotGrossTimeAggData()) {
            return false;
        }

        //camData.put("IsLotMatched", "Y");
        this.getLotInfo().calInsertion();
        this.getLotInfo().calKPI();
        this.getLotInfo().calStartEndTime();

        if (!updateUnitData()) {
            return false;
        }

        if (!updateLotData()) {
            return false;
        }

        //camData.put("IsLotCal", "Y");
        return true;
    }

    private
        QueryBuilder getQueryBuilder(String lotNumber, String operation, String nodeType) {
        QueryBuilder queryBuilder = QueryBuilders.boolQuery()
            //                .must(QueryBuilders.termsQuery("date", LocalDate.now().minusDays(1).toString(DateTimeFormat.forPattern("yyyyMMdd")),  LocalDate.now().toString(DateTimeFormat.forPattern("yyyyMMdd"))))
            .must(QueryBuilders.termQuery(this.dataFormat.getLotNumberNode().getName(), lotNumber))
            .must(QueryBuilders.termQuery(this.dataFormat.getOperationNode().getName(), operation))
            //			.must(QueryBuilders.termQuery(FieldType.DieType, FieldType.MasterDie))
            .must(QueryBuilders.termQuery(FieldType.Type, nodeType));
        return queryBuilder;
    }

    /**
     * public QueryBuilder getFTQueryBuilder(String lotNumber, String operation,
     * String nodeType) { QueryBuilder queryBuilder = QueryBuilders.boolQuery()
     * // .must(QueryBuilders.termsQuery("date",
     * LocalDate.now().minusDays(1).toString(DateTimeFormat.forPattern("yyyyMMdd")),
     * LocalDate.now().toString(DateTimeFormat.forPattern("yyyyMMdd"))))
     * .must(QueryBuilders.termQuery(Config.getFTFormat().getLotNumberNode().getName(),
     * lotNumber))
     * .must(QueryBuilders.termQuery(Config.getFTFormat().getOperationNode().getName(),
     * operation)) //	.must(QueryBuilders.termQuery(FieldType.DieType,
     * FieldType.MasterDie)) .must(QueryBuilders.termQuery(FieldType.Type,
     * nodeType)); return queryBuilder; }
     *
     * public QueryBuilder getSLTQueryBuilder(String lotNumber, String
     * operation, String nodeType) { QueryBuilder queryBuilder =
     * QueryBuilders.boolQuery() // .must(QueryBuilders.termsQuery("date",
     * LocalDate.now().minusDays(1).toString(DateTimeFormat.forPattern("yyyyMMdd")),
     * LocalDate.now().toString(DateTimeFormat.forPattern("yyyyMMdd"))))
     * .must(QueryBuilders.termQuery(Config.getSLTFormat().getLotNumberNode().getName(),
     * lotNumber))
     * .must(QueryBuilders.termQuery(Config.getSLTFormat().getOperationNode().getName(),
     * operation)) //	.must(QueryBuilders.termQuery(FieldType.DieType,
     * FieldType.MasterDie)) .must(QueryBuilders.termQuery(FieldType.Type,
     * nodeType)); return queryBuilder; }
     *
     * public QueryBuilder getSORTQueryBuilder(String lotNumber, String
     * operation, String nodeType) { QueryBuilder queryBuilder =
     * QueryBuilders.boolQuery() // .must(QueryBuilders.termsQuery("date",
     * LocalDate.now().minusDays(1).toString(DateTimeFormat.forPattern("yyyyMMdd")),
     * LocalDate.now().toString(DateTimeFormat.forPattern("yyyyMMdd"))))
     * .must(QueryBuilders.termQuery(Config.watFormat.getLotNumberNode().getName(),
     * lotNumber))
     * .must(QueryBuilders.termQuery(Config.watFormat.getOperationNode().getName(),
     * operation)) .must(QueryBuilders.termQuery(FieldType.Type, nodeType));
     * return queryBuilder; }
     *
     * public QueryBuilder getFTAggQueryBuilder(String lotNumber, String
     * operation, String nodeType) { QueryBuilder queryBuilder =
     * QueryBuilders.boolQuery() // .must(QueryBuilders.termsQuery("date",
     * LocalDate.now().minusDays(1).toString(DateTimeFormat.forPattern("yyyyMMdd")),
     * LocalDate.now().toString(DateTimeFormat.forPattern("yyyyMMdd"))))
     * .must(QueryBuilders.termQuery(Config.getFTFormat().getLotNumberNode().getName(),
     * lotNumber)) //
     * .must(QueryBuilders.termQuery(Config.getFTFormat().getOperationNode().getName(),
     * operation)) .must(QueryBuilders.termQuery("MfgStep", operation)) //
     * .must(QueryBuilders.termQuery(FieldType.DieType, FieldType.MasterDie))
     * .must(QueryBuilders.termQuery(FieldType.Type, nodeType)); return
     * queryBuilder; }
     *
     * public QueryBuilder getSLTAggQueryBuilder(String lotNumber, String
     * operation, String nodeType) { QueryBuilder queryBuilder =
     * QueryBuilders.boolQuery() // .must(QueryBuilders.termsQuery("date",
     * LocalDate.now().minusDays(1).toString(DateTimeFormat.forPattern("yyyyMMdd")),
     * LocalDate.now().toString(DateTimeFormat.forPattern("yyyyMMdd"))))
     * .must(QueryBuilders.termQuery(Config.getSLTFormat().getLotNumberNode().getName(),
     * lotNumber))
     * .must(QueryBuilders.termQuery(Config.getSLTFormat().getOperationNode().getName(),
     * operation)) //	.must(QueryBuilders.termQuery(FieldType.DieType,
     * FieldType.MasterDie)) .must(QueryBuilders.termQuery(FieldType.Type,
     * nodeType)); return queryBuilder; }
     *
     * public QueryBuilder getSORTAggQueryBuilder(String lotNumber, String
     * operation, String nodeType) { QueryBuilder queryBuilder =
     * QueryBuilders.boolQuery() // .must(QueryBuilders.termsQuery("date",
     * LocalDate.now().minusDays(1).toString(DateTimeFormat.forPattern("yyyyMMdd")),
     * LocalDate.now().toString(DateTimeFormat.forPattern("yyyyMMdd"))))
     * .must(QueryBuilders.termQuery(Config.watFormat.getLotNumberNode().getName(),
     * lotNumber))
     * .must(QueryBuilders.termQuery(Config.watFormat.getOperationNode().getName(),
     * operation)) .must(QueryBuilders.termQuery(FieldType.Type, nodeType));
     * return queryBuilder; }
     *
     */
    private
        QueryBuilder getAggQueryBuilder(String lotNumber, String operation, String nodeType) {
        QueryBuilder queryBuilder = QueryBuilders.boolQuery()
            //                .must(QueryBuilders.termsQuery("date", LocalDate.now().minusDays(1).toString(DateTimeFormat.forPattern("yyyyMMdd")),  LocalDate.now().toString(DateTimeFormat.forPattern("yyyyMMdd"))))
            .must(QueryBuilders.termQuery(this.dataFormat.getLotNumberNode().getName(), lotNumber))
            .must(QueryBuilders.termQuery(this.dataFormat.getOperationNode().getName(), operation))
            .must(QueryBuilders.termQuery(FieldType.Type, nodeType));
        return queryBuilder;
    }

    private
        LotInfo getLotInfo() {
        return lotInfo;
    }

    public
        void setDataFormat(DataFormat dataFormat) {
        this.dataFormat = dataFormat;
    }

    public static
        void main(String[] args) {
        ESHelper es = ESHelper.getInstance();
        if (!es.init()) {
            return;
        }
//        String lotNumber = null;
//        String operation = null;
//        DataFormat format = null;
//        es.calLot(lotNumber, operation, format);
        for (DataFormat dataFormat : Config.dataFormats.values()) {
            if (dataFormat.getDataType().equals(Config.DataTypes.CAMSTAR)
                || dataFormat.getDataType().equals(Config.DataTypes.SMAP)
                || dataFormat.getDataType().equals(Config.DataTypes.WAT)) {
                continue;
            }
            es.proceedUncaledLot();
        }

    }

}
