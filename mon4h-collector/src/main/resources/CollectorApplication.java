
public class CollectorApplication extends Application {

	public Set<Class<?>> getClasses() {
        Set<Class<?>> s = new HashSet<Class<?>>();
        s.add(ModelResource.class);
        s.add(MessageResource.class);
        s.add(CollectorExceptionMapper.class);
        return s;
    }
}
