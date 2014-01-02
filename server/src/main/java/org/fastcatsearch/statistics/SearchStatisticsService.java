package org.fastcatsearch.statistics;

import java.io.File;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.fastcatsearch.control.JobService;
import org.fastcatsearch.env.Environment;
import org.fastcatsearch.exception.FastcatSearchException;
import org.fastcatsearch.ir.query.Query;
import org.fastcatsearch.ir.search.SearchStatistics;
import org.fastcatsearch.job.statistics.CollectSearchStatisticsLogsJob;
import org.fastcatsearch.service.AbstractService;
import org.fastcatsearch.service.ServiceManager;
import org.fastcatsearch.settings.SettingFileNames;
import org.fastcatsearch.settings.Settings;
import org.fastcatsearch.settings.StatisticsSettings;
import org.fastcatsearch.settings.StatisticsSettings.Category;
import org.fastcatsearch.util.DateUtils;
import org.fastcatsearch.util.JAXBConfigs;

/**
 * 검색통계를 내기위해 CategoryStatistics를 이용하여 컬렉션별로 검색어 로그를 5분단위, 하루단위로 만들어 놓는다. 외부 job이 주기적으로 5분에 한번씩 실시간 인기검색어를 통계내고, 하루 자정에 한번 일간 인기검색어 통계를 내어 KeywordService의 db에 저장한다. KeywordService는
 * 갱신된 인기검색어를 service-node로 전송하여 서비스할 인기검색어를 사전형태로 메모리에 띄워 서비스한다.
 * */
public class SearchStatisticsService extends AbstractService {

	private static final DummySearchStatistics fallBackSearchStatistics = new DummySearchStatistics();
	private File statisticsHome;
	private SearchStatistics searchStatistics;
	private StatisticsSettings statisticsSettings;
	private Map<String, CategoryStatistics> categoryStatisticsMap;

	public SearchStatisticsService(Environment environment, Settings settings, ServiceManager serviceManager) {
		super(environment, settings, serviceManager);
		statisticsHome = environment.filePaths().getStatisticsRoot().file();
		statisticsHome.mkdir();
	}

	public Collection<CategoryStatistics> getCategoryStatisticsList() {
		return categoryStatisticsMap.values();
	}

	@Override
	protected boolean doStart() throws FastcatSearchException {
		categoryStatisticsMap = new HashMap<String, CategoryStatistics>();
		File statisticsSettingFile = environment.filePaths().configPath().path(SettingFileNames.statisticsConfig).file();
		try {
			statisticsSettings = JAXBConfigs.readConfig(statisticsSettingFile, StatisticsSettings.class);
		} catch (JAXBException e) {
			logger.error("statisticsSetting read error.", e);
			return false;
		}
		if (statisticsSettings == null) {
			logger.error("Cannot load statistics setting file >> {}", statisticsSettingFile);
			return false;
		}

		List<Category> categoryList = statisticsSettings.getCategoryList();
		for (Category category : categoryList) {
			String categoryId = category.getId();
			CategoryStatistics categoryStatistics = new CategoryStatistics(category, statisticsHome);
			categoryStatisticsMap.put(categoryId, categoryStatistics);
			logger.debug("> {}", category);
		}

		searchStatistics = new SearchStatisticsImpl(statisticsHome, categoryStatisticsMap);

		//마스터 노드만 수집을 한다.
		boolean isMaster = environment.isMasterNode();
		if (isMaster) {
			// 수집 스케쥴을 건다.
			// Realtime 정시에서 시작하여 5분단위.
			Calendar calendar = DateUtils.getLatestTimeLargerThanNow(5);
			calendar.add(Calendar.MINUTE, 1);
			Date nextTimeForRealtimeLog = calendar.getTime();
			JobService.getInstance().schedule(new CollectSearchStatisticsLogsJob(CollectSearchStatisticsLogsJob.TYPE_REALTIME), nextTimeForRealtimeLog,
					DateUtils.getSecondsByMinutes(1)); // 5분주기.
			// Daily 매 정시기준으로 1시간 단위.
			calendar = DateUtils.getNextDayHour(0); // 다음날 0시.
			calendar.add(Calendar.MINUTE, 1);
			Date nextTimeForDailyLog = calendar.getTime();
			//1일주기.
			JobService.getInstance().schedule(new CollectSearchStatisticsLogsJob(CollectSearchStatisticsLogsJob.TYPE_DAILY), nextTimeForDailyLog, DateUtils.getSecondsByDays(1)); 
		}

		return true;
	}

	@Override
	protected boolean doStop() throws FastcatSearchException {
		for (CategoryStatistics categoryStatistics : categoryStatisticsMap.values()) {
			categoryStatistics.close();
		}
		return true;
	}

	@Override
	protected boolean doClose() throws FastcatSearchException {

		return true;
	}

	public StatisticsSettings statisticsSettings() {
		return statisticsSettings;
	}

	public SearchStatistics searchStatistics() {
		return searchStatistics != null ? searchStatistics : fallBackSearchStatistics;
	}

	public CategoryStatistics categoryStatistics(String categoryId) {
		return categoryStatisticsMap.get(categoryId);
	}

	private static class DummySearchStatistics implements SearchStatistics {

		@Override
		public void add(Query q) {
			// ignore.
		}

	}

}
