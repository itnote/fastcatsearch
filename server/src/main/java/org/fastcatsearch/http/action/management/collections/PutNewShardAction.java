package org.fastcatsearch.http.action.management.collections;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.fastcatsearch.cluster.NodeService;
import org.fastcatsearch.control.JobService;
import org.fastcatsearch.control.ResultFuture;
import org.fastcatsearch.http.ActionMapping;
import org.fastcatsearch.http.action.ActionRequest;
import org.fastcatsearch.http.action.ActionResponse;
import org.fastcatsearch.http.action.AuthAction;
import org.fastcatsearch.ir.IRService;
import org.fastcatsearch.ir.config.CollectionContext;
import org.fastcatsearch.ir.config.ShardConfig;
import org.fastcatsearch.ir.config.ShardContext;
import org.fastcatsearch.ir.search.ShardHandler;
import org.fastcatsearch.job.cluster.SyncShardDataNodeJob;
import org.fastcatsearch.service.ServiceManager;
import org.fastcatsearch.util.CollectionContextUtil;
import org.fastcatsearch.util.ResponseWriter;

@ActionMapping("/management/collections/shard-add")
public class PutNewShardAction extends AuthAction {

	@Override
	public void doAuthAction(ActionRequest request, ActionResponse response) throws Exception {
		
		String collectionId = request.getParameter("collectionId");
		String shardId = request.getParameter("shardId");
		String shardName = request.getParameter("shardName");
		String filter = request.getParameter("filter");
		String dataNodeList = request.getParameter("dataNodeList");
		
		IRService irService = ServiceManager.getInstance().getService(IRService.class);
		
		CollectionContext collectionContext = irService.collectionContext(collectionId);
		
		String errorMessage = null;
		boolean isSuccess = true;
		
		ShardContext oldShardContext = collectionContext.getShardContext(shardId);
		
		if(oldShardContext == null){
			ShardConfig shardConfig = new ShardConfig();
			shardConfig.setId(shardId);
			shardConfig.setName(shardName);
			shardConfig.setFilter(filter);
			List<String> dataNodeIdList = new ArrayList<String>();
			
			for(String dataNodeLabel : dataNodeList.split(",")){
				dataNodeLabel = dataNodeLabel.trim();
				if(dataNodeLabel.length() > 0){
					dataNodeIdList.add(dataNodeLabel);
				}
			}
			shardConfig.setDataNodeList(dataNodeIdList);
			
			try{
				ShardContext shardContext = CollectionContextUtil.addNewShard(collectionContext, shardConfig);
				ShardHandler shardHandler = new ShardHandler(collectionContext.schema(), shardContext);
				shardHandler.load();
				irService.collectionHandler(collectionId).shardHandlerMap().put(shardId, shardHandler);
				
//				NodeService nodeService = ServiceManager.getInstance().getService(NodeService.class);
//				nodeService.updateLoadBalance(shardId, dataNodeIdList);
				
				//dataNode Sync Job 수행.
				SyncShardDataNodeJob job = new SyncShardDataNodeJob(shardId, dataNodeIdList);
				ResultFuture resultFuture = JobService.getInstance().offer(job);
				
				//동기화 될때까지 기다린다.
				Object result = resultFuture.take();
				
			}catch(Exception e){
				logger.error("{}", e);
				isSuccess = false;
				errorMessage = e.getMessage();
			}
			
		}else{
			isSuccess = false;
			errorMessage = "Shard ["+shardId+"] already exists.";
		}
		
		Writer writer = response.getWriter();
		ResponseWriter responseWriter = getDefaultResponseWriter(writer);
		responseWriter.object()
		.key("collectionId").value(collectionId)
		.key("shardId").value(shardId)
		.key("success").value(isSuccess)
		.key("errorMessage").value(errorMessage)
		.endObject();
		responseWriter.done();
	}

}
