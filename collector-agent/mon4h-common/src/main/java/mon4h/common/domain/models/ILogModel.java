package mon4h.common.domain.models;

import java.io.Serializable;

import mon4h.common.domain.models.sub.ModelType;

public interface ILogModel extends Serializable {

	public ModelType getType();
}
