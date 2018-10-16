/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.es;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;
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
import org.himalayas.filereader.util.FieldType;

/**
 *
 * @author ghfan
 */
public
	class ESConnection {

	private
		RestHighLevelClient client = null;

	private
		SearchRequest searchUnitRequest = null;
	private
		SearchRequest searchFileRequest = null;

	private
		String unitIDName = null;
	private
		String startTimeName = null;
	final
		Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1L));
	final
		CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
	private
		LotInfo lotInfo = new LotInfo();
	private
		int unitNo = 0;
	private
		String testTimeName = null;
	private
		AggregationBuilder aggregation = null;
	private
		SearchResponse searchResponse = null;

	public
		ESConnection() {
	}

	public
		boolean init() {
		StatusLogger.getLogger().setLevel(Level.INFO);
		this.initClient();
		this.initSearchUnitRequest();
		this.initSearchFileRequest();
		return true;
	}

	private
		void initClient() {
		credentialsProvider.setCredentials(AuthScope.ANY,
			new UsernamePasswordCredentials("admin", "admin"));

		RestClientBuilder builder = RestClient.builder(
			new HttpHost("10.72.1.239", 9200, "http"),
			new HttpHost("10.72.1.237", 9200, "http"),
			new HttpHost("10.72.1.238", 9200, "http"));

		builder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
			@Override
			public
				HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
				return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
			}
		});

		this.client = new RestHighLevelClient(builder);

	}

	private
		void initSearchUnitRequest() {
		this.searchUnitRequest = new SearchRequest();
		this.searchUnitRequest.indices("*-test-*");
		this.searchUnitRequest.scroll(scroll);

		// source unit_id, bin_type
		this.unitIDName = Config.getFTFormat().getUnit().getUnitIdNode().getName();
		this.startTimeName = Config.getFTFormat().getUnit().getStartTimeNode().getName();
		this.testTimeName = Config.getFTFormat().getUnit().getTestTimeNode().getName();
		String[] includeFields = new String[]{unitIDName, FieldType.BinType, startTimeName, "UnitSeq", this.testTimeName};
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
		this.searchFileRequest.indices("*-test-*");
		this.searchFileRequest.source(new SearchSourceBuilder()
			.size(0)
			.timeout(new TimeValue(180, TimeUnit.SECONDS))
			.fetchSource(false));

//		searchSourceBuilder.profile(true);
//		this.aggregation = AggregationBuilders.global("LotInfo")
//			.subAggregation(AggregationBuilders.sum(FieldType.GrossTime).field(FieldType.GrossTime))
//			.subAggregation(AggregationBuilders.sum(FieldType.UnitCnt).field(FieldType.Unit));
		this.aggregation = AggregationBuilders.sum(FieldType.GrossTime).field(FieldType.GrossTime);

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
		boolean getLotData(String lotNumber, String operation, Config.DataTypes dataType) {
		this.getLotInfo().getDataSets().clear();
		this.unitNo = 0;

		if (dataType.equals(Config.DataTypes.ATE)) {
//			searchSourceBuilder.query(this.getFTQueryBuilder("HG50099B", "FT-FUSE"));
			this.searchUnitRequest.source().query(this.getFTQueryBuilder(lotNumber, operation, FieldType.Unit));
		}
		else if (dataType.equals(Config.DataTypes.SLT)) {
			this.searchUnitRequest.source().query(this.getSLTQueryBuilder(lotNumber, operation, FieldType.Unit));
		}
		else {
			this.searchUnitRequest.source().query(this.getSORTQueryBuilder(lotNumber, operation, FieldType.Unit));
		}
		try {
			searchResponse = client.search(searchUnitRequest, RequestOptions.DEFAULT);

			RestStatus status = searchResponse.status();
			TimeValue took = searchResponse.getTook();
			Boolean terminatedEarly = searchResponse.isTerminatedEarly();
			boolean timedOut = searchResponse.isTimedOut();

			int totalShards = searchResponse.getTotalShards();
			int successfulShards = searchResponse.getSuccessfulShards();
			int failedShards = searchResponse.getFailedShards();
			for (ShardSearchFailure failure : searchResponse.getShardFailures()) {
				// failures should be handled here
			}

			String scrollId = searchResponse.getScrollId();
			SearchHit[] searchHits = searchResponse.getHits().getHits();
			this.fillData(searchHits);

			while (searchHits != null && searchHits.length > 0) {
				SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
				scrollRequest.scroll(scroll);
				searchResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);
				scrollId = searchResponse.getScrollId();

				searchHits = searchResponse.getHits().getHits();
				this.fillData(searchHits);
			}
			ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
			clearScrollRequest.addScrollId(scrollId);
			ClearScrollResponse clearScrollResponse = client.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
			boolean succeeded = clearScrollResponse.isSucceeded();

			//client.close();
		}
		catch (IOException ex) {
			Logger.getLogger(ESConnection.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
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
		boolean getLotAggData(String lotNumber, String operation, Config.DataTypes dataType) {
		this.unitNo = 0;

		if (dataType.equals(Config.DataTypes.ATE)) {
//			searchSourceBuilder.query(this.getFTQueryBuilder("HG50099B", "FT-FUSE"));
			this.searchFileRequest.source().query(this.getFTAggQueryBuilder(lotNumber, operation, FieldType.File));
		}
		else if (dataType.equals(Config.DataTypes.SLT)) {
			this.searchFileRequest.source().query(this.getSLTAggQueryBuilder(lotNumber, operation, FieldType.File));
		}
		else {
			this.searchFileRequest.source().query(this.getSORTAggQueryBuilder(lotNumber, operation, FieldType.File));
		}
		try {

			this.searchFileRequest.source().aggregation(aggregation);
			searchResponse = client.search(searchFileRequest, RequestOptions.DEFAULT);

			RestStatus status = searchResponse.status();
			TimeValue took = searchResponse.getTook();
			Boolean terminatedEarly = searchResponse.isTerminatedEarly();
			boolean timedOut = searchResponse.isTimedOut();

			int totalShards = searchResponse.getTotalShards();
			int successfulShards = searchResponse.getSuccessfulShards();
			int failedShards = searchResponse.getFailedShards();
			for (ShardSearchFailure failure : searchResponse.getShardFailures()) {
				// failures should be handled here
			}

			long totalHits = searchResponse.getHits().getTotalHits();
			this.getLotInfo().setTotalFileCnt(totalHits);

			this.fillLotData();
		}
		catch (IOException ex) {
			Logger.getLogger(ESConnection.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
			return false;
		}
		return true;

	}

	private
		void fillLotData() {
		Aggregations aggregations = this.searchResponse.getAggregations();
////		Global lotAggInfo = aggregations.get("LotInfo");
		for (Aggregation agg : aggregations) {
			String name = agg.getName();
			if (name.equals(FieldType.GrossTime)) {
				this.getLotInfo().setGrossTestTime(((Sum) agg).getValue());
			}
		}
	}

	private
		void fillData(SearchHit[] searchHits) {
		for (SearchHit hit : searchHits) {
			// do something with the SearchHit
			String index = hit.getIndex();
			String id = hit.getId();
			Map<String, Object> sourceAsMap = hit.getSourceAsMap();
			String unitID = (String) sourceAsMap.get(unitIDName);
			String startTime = (String) sourceAsMap.get(startTimeName);
			int binType = Integer.valueOf((String) sourceAsMap.get(FieldType.BinType));
			double testTime = Double.valueOf((String) sourceAsMap.get(this.testTimeName));

			System.out.printf("UnitNo= %d, %s\n", ++this.unitNo, hit.getSourceAsString());

			if (unitID == null) {
				unitID = id;
			}

			DataSet dataSet = this.getLotInfo().getDataSets().get(unitID);

			if (dataSet == null) {
				dataSet = new DataSet(unitID);
				this.getLotInfo().getDataSets().put(unitID, dataSet);
			}
			dataSet.getUnitData().add(new Doc(id, binType, startTime, index, testTime));
		}
	}

	public
		void updateInto() {

		try {
			BulkRequest bulkRequest = new BulkRequest();
			bulkRequest.timeout(TimeValue.timeValueMinutes(5));

			for (DataSet dataSet : this.getLotInfo().getDataSets().values()) {
				this.getLotInfo().totalUniqueUnitCntInc(dataSet.getUnitData().size());
				for (Doc doc : dataSet.getUnitData()) {
					/**
					 * generate the bulk update request
					 */
					Map<String, Object> jsonMap = new HashMap<>();
					jsonMap.put(FieldType.Rank, doc.getMotherLotInsertion());
					System.out.printf("%s,Time=%s,%s\n", dataSet.getUnitId(), doc.getMotherLotInsertion(), doc.getStartTime());
					bulkRequest.add(new UpdateRequest(
						doc.getIndex(),
						"doc",
						doc.getId()).doc(jsonMap));
					/**
					 * cal lot kpi
					 */
					this.getLotInfo().totalUnitTestTimeInc(doc.getTestTime());
					this.getLotInfo().totalTestedUnitCntInc(1);

				}
			}

			BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
			if (bulkResponse.hasFailures()) {
				for (BulkItemResponse bulkItemResponse : bulkResponse) {
					if (bulkItemResponse.isFailed()) {
						BulkItemResponse.Failure failure = bulkItemResponse.getFailure();
					}
				}
			}

			System.out.println("successed to update the " + FieldType.Rank);
		}
		catch (IOException ex) {
			Logger.getLogger(ESConnection.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		}

	}

	public
		void close() {
		try {
			this.client.close();

		}
		catch (IOException ex) {
			Logger.getLogger(ESConnection.class
				.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		}
	}

	public
		QueryBuilder getFTQueryBuilder(String lotNumber, String operation, String nodeType) {
		QueryBuilder queryBuilder = QueryBuilders.boolQuery()
			//                .must(QueryBuilders.termsQuery("date", LocalDate.now().minusDays(1).toString(DateTimeFormat.forPattern("yyyyMMdd")),  LocalDate.now().toString(DateTimeFormat.forPattern("yyyyMMdd"))))
			.must(QueryBuilders.termQuery(Config.getFTFormat().getLotNumberNode().getName(), lotNumber))
			.must(QueryBuilders.termQuery(Config.getFTFormat().getOperationNode().getName(), operation))
			//			.must(QueryBuilders.termQuery(FieldType.DieType, FieldType.MasterDie))
			.must(QueryBuilders.termQuery(FieldType.Type, nodeType));
		return queryBuilder;
	}

	public
		QueryBuilder getSLTQueryBuilder(String lotNumber, String operation, String nodeType) {
		QueryBuilder queryBuilder = QueryBuilders.boolQuery()
			//                .must(QueryBuilders.termsQuery("date", LocalDate.now().minusDays(1).toString(DateTimeFormat.forPattern("yyyyMMdd")),  LocalDate.now().toString(DateTimeFormat.forPattern("yyyyMMdd"))))
			.must(QueryBuilders.termQuery(Config.getSLTFormat().getLotNumberNode().getName(), lotNumber))
			.must(QueryBuilders.termQuery(Config.getSLTFormat().getOperationNode().getName(), operation))
			//			.must(QueryBuilders.termQuery(FieldType.DieType, FieldType.MasterDie))
			.must(QueryBuilders.termQuery(FieldType.Type, nodeType));
		return queryBuilder;
	}

	public
		QueryBuilder getSORTQueryBuilder(String lotNumber, String operation, String nodeType) {
		QueryBuilder queryBuilder = QueryBuilders.boolQuery()
			//                .must(QueryBuilders.termsQuery("date", LocalDate.now().minusDays(1).toString(DateTimeFormat.forPattern("yyyyMMdd")),  LocalDate.now().toString(DateTimeFormat.forPattern("yyyyMMdd"))))
			.must(QueryBuilders.termQuery(Config.watFormat.getLotNumberNode().getName(), lotNumber))
			.must(QueryBuilders.termQuery(Config.watFormat.getOperationNode().getName(), operation))
			.must(QueryBuilders.termQuery(FieldType.Type, nodeType));
		return queryBuilder;
	}

	public
		QueryBuilder getFTAggQueryBuilder(String lotNumber, String operation, String nodeType) {
		QueryBuilder queryBuilder = QueryBuilders.boolQuery()
			//                .must(QueryBuilders.termsQuery("date", LocalDate.now().minusDays(1).toString(DateTimeFormat.forPattern("yyyyMMdd")),  LocalDate.now().toString(DateTimeFormat.forPattern("yyyyMMdd"))))
			.must(QueryBuilders.termQuery(Config.getFTFormat().getLotNumberNode().getName(), lotNumber))
			//			.must(QueryBuilders.termQuery(Config.getFTFormat().getOperationNode().getName(), operation))
			.must(QueryBuilders.termQuery("MfgStep", operation))
			//			.must(QueryBuilders.termQuery(FieldType.DieType, FieldType.MasterDie))
			.must(QueryBuilders.termQuery(FieldType.Type, nodeType));
		return queryBuilder;
	}

	public
		QueryBuilder getSLTAggQueryBuilder(String lotNumber, String operation, String nodeType) {
		QueryBuilder queryBuilder = QueryBuilders.boolQuery()
			//                .must(QueryBuilders.termsQuery("date", LocalDate.now().minusDays(1).toString(DateTimeFormat.forPattern("yyyyMMdd")),  LocalDate.now().toString(DateTimeFormat.forPattern("yyyyMMdd"))))
			.must(QueryBuilders.termQuery(Config.getSLTFormat().getLotNumberNode().getName(), lotNumber))
			.must(QueryBuilders.termQuery(Config.getSLTFormat().getOperationNode().getName(), operation))
			//			.must(QueryBuilders.termQuery(FieldType.DieType, FieldType.MasterDie))
			.must(QueryBuilders.termQuery(FieldType.Type, nodeType));
		return queryBuilder;
	}

	public
		QueryBuilder getSORTAggQueryBuilder(String lotNumber, String operation, String nodeType) {
		QueryBuilder queryBuilder = QueryBuilders.boolQuery()
			//                .must(QueryBuilders.termsQuery("date", LocalDate.now().minusDays(1).toString(DateTimeFormat.forPattern("yyyyMMdd")),  LocalDate.now().toString(DateTimeFormat.forPattern("yyyyMMdd"))))
			.must(QueryBuilders.termQuery(Config.watFormat.getLotNumberNode().getName(), lotNumber))
			.must(QueryBuilders.termQuery(Config.watFormat.getOperationNode().getName(), operation))
			.must(QueryBuilders.termQuery(FieldType.Type, nodeType));
		return queryBuilder;
	}

	public
		LotInfo getLotInfo() {
		return lotInfo;
	}

	public static
		void main(String[] args) throws IOException {
		new Config("config/dataformat.xml");
		ESConnection es = new ESConnection();
		es.init();
		es.getLotData("HG50099B", "FT-FUSE", Config.DataTypes.ATE);
		es.getLotInfo().calInsertion();
		es.updateInto();
		es.getLotAggData("HG50099B", "FT-FUSE", Config.DataTypes.ATE);
		es.close();

	}

}
