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
		SearchSourceBuilder searchSourceBuilder = null;
	private
		SearchRequest searchRequest = null;
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

	public
		ESConnection() {
	}

	public
		boolean init() {
		StatusLogger.getLogger().setLevel(Level.INFO);

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
		this.searchRequest = new SearchRequest();
		this.searchRequest.indices("*-test-*");
		this.searchRequest.scroll(scroll);

		this.searchSourceBuilder = new SearchSourceBuilder();

		//max = 10000
		this.searchSourceBuilder.size(500);
		this.searchSourceBuilder.timeout(new TimeValue(180, TimeUnit.SECONDS));
		this.searchSourceBuilder.fetchSource(true);

		// source unit_id, bin_type
		this.unitIDName = Config.getFTFormat().getUnit().getUnitIdNode().getName();
		this.startTimeName = Config.getFTFormat().getUnit().getStartTimeNode().getName();
		String[] includeFields = new String[]{unitIDName, FieldType.BinType, startTimeName, "UnitSeq"};
		String[] excludeFields = new String[]{"_type"};
		this.searchSourceBuilder.fetchSource(includeFields, excludeFields);

		this.searchRequest.source(searchSourceBuilder);
//		searchSourceBuilder.profile(true);

		return true;
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
			this.searchSourceBuilder.query(this.getFTQueryBuilder(lotNumber, operation));
		}
		else if (dataType.equals(Config.DataTypes.SLT)) {
			this.searchSourceBuilder.query(this.getSLTQueryBuilder(lotNumber, operation));
		}
		else {
			this.searchSourceBuilder.query(this.getSORTQueryBuilder(lotNumber, operation));
		}
		try {
			SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

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

			System.out.printf("UnitNo= %d, %s\n", ++this.unitNo, hit.getSourceAsString());

			if (unitID == null) {
				unitID = id;
			}

			DataSet dataSet = this.getLotInfo().getDataSets().get(unitID);

			if (dataSet == null) {
				dataSet = new DataSet(unitID);
				this.getLotInfo().getDataSets().put(unitID, dataSet);
			}
			dataSet.getUnitData().add(new Doc(id, binType, startTime, index));
		}
	}

	public
		void updateInto() {
		try {
			BulkRequest bulkRequest = new BulkRequest();
			bulkRequest.timeout(TimeValue.timeValueMinutes(5));

			for (DataSet dataSet : this.getLotInfo().getDataSets().values()) {
				for (Doc doc : dataSet.getUnitData()) {
					Map<String, Object> jsonMap = new HashMap<>();
					jsonMap.put(FieldType.Rank, doc.getMotherLotInsertion());
					System.out.printf("%s,Time=%s,%s\n", dataSet.getUnitId(), doc.getMotherLotInsertion(), doc.getStartTime());
					bulkRequest.add(new UpdateRequest(
						doc.getIndex(),
						"doc",
						doc.getId()).doc(jsonMap));
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
		QueryBuilder getFTQueryBuilder(String lotNumber, String operation) {
		QueryBuilder queryBuilder = QueryBuilders.boolQuery()
			//                .must(QueryBuilders.termsQuery("date", LocalDate.now().minusDays(1).toString(DateTimeFormat.forPattern("yyyyMMdd")),  LocalDate.now().toString(DateTimeFormat.forPattern("yyyyMMdd"))))
			.must(QueryBuilders.termQuery(Config.getFTFormat().getLotNumberNode().getName(), lotNumber))
			.must(QueryBuilders.termQuery(Config.getFTFormat().getOperationNode().getName(), operation))
//			.must(QueryBuilders.termQuery(FieldType.DieType, FieldType.MasterDie))
			.must(QueryBuilders.termQuery(FieldType.Type, FieldType.Unit));
		return queryBuilder;
	}

	public
		QueryBuilder getSLTQueryBuilder(String lotNumber, String operation) {
		QueryBuilder queryBuilder = QueryBuilders.boolQuery()
			//                .must(QueryBuilders.termsQuery("date", LocalDate.now().minusDays(1).toString(DateTimeFormat.forPattern("yyyyMMdd")),  LocalDate.now().toString(DateTimeFormat.forPattern("yyyyMMdd"))))
			.must(QueryBuilders.termQuery(Config.getSLTFormat().getLotNumberNode().getName(), lotNumber))
			.must(QueryBuilders.termQuery(Config.getSLTFormat().getOperationNode().getName(), operation))
//			.must(QueryBuilders.termQuery(FieldType.DieType, FieldType.MasterDie))
			.must(QueryBuilders.termQuery(FieldType.Type, FieldType.Unit));
		return queryBuilder;
	}

	public
		QueryBuilder getSORTQueryBuilder(String lotNumber, String operation) {
		QueryBuilder queryBuilder = QueryBuilders.boolQuery()
			//                .must(QueryBuilders.termsQuery("date", LocalDate.now().minusDays(1).toString(DateTimeFormat.forPattern("yyyyMMdd")),  LocalDate.now().toString(DateTimeFormat.forPattern("yyyyMMdd"))))
			.must(QueryBuilders.termQuery(Config.watFormat.getLotNumberNode().getName(), lotNumber))
			.must(QueryBuilders.termQuery(Config.watFormat.getOperationNode().getName(), operation))
			.must(QueryBuilders.termQuery(FieldType.Type, FieldType.Unit));
		return queryBuilder;
	}

	public
		LotInfo getLotInfo() {
		return lotInfo;
	}

	public static
		void main(String[] args) {
		new Config("config/dataformat.xml");
		ESConnection es = new ESConnection();
		es.init();
		es.getLotData("HG50099B", "FT-FUSE", Config.DataTypes.ATE);
		es.getLotInfo().calInsertion();
		es.updateInto();
		es.close();

	}

}
