package org.zkoss.demo.tablet.mock;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.zkoss.demo.tablet.AbstractServer;
import org.zkoss.demo.tablet.Utility;
import org.zkoss.demo.tablet.vo.ContentVO;
import org.zkoss.demo.tablet.vo.ThreadVO;
import org.zkoss.zk.ui.WebApps;

public class MockServer extends AbstractServer {
	private static final Map<String, List<ThreadVO>> threadData = new HashMap<String, List<ThreadVO>>();

	private final Map<String, List<ThreadVO>> _threadData = new HashMap<String, List<ThreadVO>>();

	public MockServer() {
		if (MockServer.threadData.isEmpty()) {
			updateThreadData(MockServer.threadData);
		}
		for (String key : threadData.keySet()) {
			//ThreadVO never set data, so just use the same reference
			ArrayList<ThreadVO> result = new ArrayList<ThreadVO>();
			result.addAll(threadData.get(key));
			_threadData.put(key, result);
		}
	}

	private void updateThreadData(Map<String, List<ThreadVO>> dataMap) {
		//20 thread in one category (ZK forum setting)
		ArrayList<ThreadVO> inbox = new ArrayList<ThreadVO>(AbstractServer.CATEGORY_LIST.length * 20);
		ArrayList<ThreadVO> important = new ArrayList<ThreadVO>();
		ArrayList<ThreadVO> starred = new ArrayList<ThreadVO>();
		ArrayList<ThreadVO> trash = new ArrayList<ThreadVO>();
		dataMap.put(AbstractServer.INBOX, inbox);
		dataMap.put(AbstractServer.IMPORTANT, important);
		dataMap.put(AbstractServer.STARRED, starred);
		dataMap.put(AbstractServer.TRASH, trash);

		for (String type : AbstractServer.CATEGORY_LIST) {
			List<ThreadVO> data = fetchThreadUrl(type);
			dataMap.put(type, data);
			//fetch every thread data and prepare Inbox, Important and Starred
			for (ThreadVO tvo : data) {
				inbox.add(tvo);
				if (tvo.isHot()) {
					starred.add(tvo);
				}
				if (tvo.isPopular()) {
					important.add(tvo);
				}
			}
		}
	}

	private String mockThread(int no) {
		URL url = WebApps.getCurrent().getResource("~./mock/" + no + ".html");
		if (url != null)
			return Utility.getString(new File(url.getFile()));
		else
			return "";
	}

	private String mockPost(String x) {
		URL url = WebApps.getCurrent().getResource("~./mock/" + x + ".html");
		if (url != null)
			return Utility.getString(new File(url.getFile()));
		else
			return "";
	}

	private List<ThreadVO> fetchThreadUrl(String type) {
		String content = mockThread(CATEGORY_URL.get(type));
		ArrayList<ThreadVO> result = new ArrayList<ThreadVO>();

		int start = 0, end;
		while ((start = content.indexOf(THREAD_START, start)) != -1) {
			ThreadVO thread = new ThreadVO();
			thread.setCategory(type);

			//XXX "hot" or "popular" will ignore at last thread
			int tmp;
			if ((tmp = content.indexOf(POPULAR, start)) != -1) {
				if (tmp < content.indexOf(THREAD_START, start + 1)) {
					thread.setPopular(true);
				}
			}

			if ((tmp = content.indexOf(HOT, start)) != -1) {
				if (tmp < content.indexOf(THREAD_START, start + 1)) {
					thread.setHot(true);
				}
			}

			start = content.indexOf(TITLE_HEADER, start);
			end = content.indexOf(TITLE_TAIL, start);
			thread.setUrl(content.substring(start + TITLE_HEADER.length(), end));

			start = content.indexOf(">", end);
			end = content.indexOf("</a>", start);
			thread.setTitle(content.substring(start + 1, end));

			start = content.indexOf(AUTHOR_HEADER, end);
			end = content.indexOf(AUTHOR_TAIL, start);
			thread.setAuthor(content.substring(start + AUTHOR_HEADER.length(), end));

			start = content.indexOf(POST_HEADER, end);
			end = content.indexOf(POST_TAIL, start);
			thread.setPost(Integer.parseInt(content.substring(start + POST_HEADER.length(), end)));

			start = content.indexOf(AUTHOR_HEADER, end);
			end = content.indexOf(AUTHOR_TAIL, start);
			thread.setLastPoster(content.substring(start + AUTHOR_HEADER.length(), end));

			result.add(thread);
			start = end;
		}
		return result;
	}

	@Override
	public void moveToTrash(ArrayList<ThreadVO> selectedThread) {
		List<ThreadVO> inbox = _threadData.get(AbstractServer.INBOX);
		List<ThreadVO> important = _threadData.get(AbstractServer.IMPORTANT);
		List<ThreadVO> starred = _threadData.get(AbstractServer.STARRED);
		List<ThreadVO> trash = _threadData.get(AbstractServer.TRASH);

		for (ThreadVO tvo : selectedThread) {
			List<ThreadVO> type = _threadData.get(tvo.getCategory());
			trash.add(tvo);
			if (inbox.contains(tvo)) {
				inbox.remove(tvo);
			}
			if (important.contains(tvo)) {
				important.remove(tvo);
			}
			if (starred.contains(tvo)) {
				starred.remove(tvo);
			}
			if (type.contains(tvo)) {
				type.remove(tvo);
			}
		}
	}

	@Override
	public List<ThreadVO> getThreadList(String type) {
		return _threadData.get(type);
	}

	@Override
	public List<ContentVO> getContentList(ThreadVO thread) {
		ArrayList<ContentVO> result = new ArrayList<ContentVO>();
		String content = mockPost(thread.getUrl());

		int start = 0, end;
		while ((start = content.indexOf(CONTENT_AUTHOR_HEADER, start)) != -1) {
			ContentVO cvo = new ContentVO();

			end = content.indexOf(CONTENT_AUTHOR_TAIL, start);
			cvo.setAuthor(content.substring(start + CONTENT_AUTHOR_HEADER.length(), end));

			start = content.indexOf(CONTENT_DATE_HEADER, end);
			end = content.indexOf(CONTENT_DATE_TAIL, start);
			cvo.setDate(content.substring(start + CONTENT_DATE_HEADER.length(), end));

			start = content.indexOf(CONTENT_PRE_HEADER, end);
			start = content.indexOf(CONTENT_HEADER, start);
			end = content.indexOf(CONTENT_TAIL, start);
			if (end == -1) {    //last post of thread, no more <div class="author">
				end = content.indexOf("</div></div>", start);
			}
			cvo.setContent(content.substring(start + CONTENT_HEADER.length(), end));
			cvo.setOpen(false);
			result.add(cvo);
			start = end;
		}
		return result;
	}

	private static final String THREAD_START = "<div class=\"discussion-subject\">";
	private static final String TITLE_HEADER = "<a class=\"discussion-title-unread\" href=\"/forum/";
	private static final String TITLE_TAIL = ";jsessionid";

	//not unique, just in case
	private static final String POST_HEADER = "<td class=\"align-center\">";
	private static final String POST_TAIL = "</td>";

	private static final String AUTHOR_HEADER = ",label:'";
	private static final String AUTHOR_TAIL = "'},";

	private static final String POPULAR = "<img alt=\"Popular\" title=\"Popular\"";
	private static final String HOT = "<img alt=\"Hot\" title=\"Hot\"";
}