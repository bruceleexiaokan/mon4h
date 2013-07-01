package mon4h.collector.configuration;

public class Constants {
	public static final int MAX_MESSAGE_SIZE = 2 * 1024 * 1024;
	public static final int DEFAULT_PAGE_SIZE = 128 * 1024 * 1024;
	public static final int DEFAULT_MEMORY_QUEUE_MAX_ITEMS = 51200;
	public static final int DEFAULT_MAX_DISK_FILE_NUMBER = 10;
	public static final String CONFIG_FILE = "collector.properties";
	
	public static final String queueNames = "mon4h.collector.queue.names";
	public static final String diskQueueDir = "mon4h.collector.diskqueue.dir";
	public static final String pageSize = "mon4h.collector.diskqueue.pagesize";
	public static final String memoryQueueMaxItems = "mon4h.collector.memqueue.maxitems";
	public static final String queuePrefix = "mon4h.collector.queue.";
	public static final String queueMaxFilesPostfix = ".maxfiles";
	public static final String queueTypesPostfix = ".types";

	public static final String MESSAGE_NUMBER_HTTP_HEADER = "Message-Number";
	public static final String MESSAGE_NAME_HTTP_HEADER = "Message-Name";
}
