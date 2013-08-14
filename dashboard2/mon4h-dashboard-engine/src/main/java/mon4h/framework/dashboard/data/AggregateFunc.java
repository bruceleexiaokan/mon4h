package mon4h.framework.dashboard.data;

public interface AggregateFunc {
	public void aggregate(InterAggInfo interInfo,Double delta);
	public Double getValue(InterAggInfo interInfo);
}
