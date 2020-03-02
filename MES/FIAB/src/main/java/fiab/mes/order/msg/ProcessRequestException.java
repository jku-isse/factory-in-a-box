package fiab.mes.order.msg;

import java.util.Collections;
import java.util.List;

import ProcessCore.Parameter;

public class ProcessRequestException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3280703063619216232L;

	public static enum Type {
		PROCESS_STEP_MISSING,
		UNSUPPORTED_CAPABILITY,
		STEP_MISSES_CAPABILITY,
		CAPABILITY_MISSING_REQUIRED_INPUT_PARAM,
		INPUT_PARAM_WRONG_TYPE,
		INPUT_PARAMS_MISSING_VALUE
	}
	
	protected Type type;
	protected List<Parameter> affectedParams = Collections.emptyList();
	protected String msg;
	
	public ProcessRequestException(Type type, String errorMsg) {
		super(errorMsg);
		this.type = type;
	}

	public ProcessRequestException(Type type, String errorMsg, List<Parameter> affectedParams) {
		super(errorMsg);
		this.type = type;
		this.affectedParams = affectedParams;
	}

	public Type getType() {
		return type;
	}

	public List<Parameter> getAffectedParams() {
		return affectedParams;
	}

	
	
	
	
}
