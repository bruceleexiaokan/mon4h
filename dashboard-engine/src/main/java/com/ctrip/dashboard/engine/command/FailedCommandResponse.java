package com.ctrip.dashboard.engine.command;

import com.ctrip.dashboard.engine.data.InterfaceConst;

public class FailedCommandResponse implements CommandResponse{
	private int resultCode = InterfaceConst.ResultCode.SERVER_INTERNAL_ERROR;
	private String resultInfo;
	private Throwable t;
	
	public FailedCommandResponse(Throwable t){
		this.t = t;
	}
	
	public FailedCommandResponse(String resultInfo){
		this.resultInfo = resultInfo;
	}
	
	public FailedCommandResponse(int resultCode,Throwable t){
		this.resultCode = resultCode;
		this.t = t;
	}
	
	public FailedCommandResponse(int resultCode,String resultInfo){
		this.resultCode = resultCode;
		this.resultInfo = resultInfo;
	}
	
	public FailedCommandResponse(int resultCode,String resultInfo,Throwable t){
		this.resultCode = resultCode;
		this.resultInfo = resultInfo;
		this.t = t;
	}
	
	private static void appendJson(StringBuilder sb,String str){
		if(str != null){
			int len = str.length();
			for(int i=0;i<len;i++){
				char c = str.charAt(i);
				if(c == '\"'){
					sb.append("\\\"");
				}else if(c == '\\'){
					if(c<len-1){
						if(str.charAt(i+1) == 'u'){
							if(c<len-5){
								boolean checked = true;
								for(int j=0;j<4;j++){
									char uchar = str.charAt(i+2+j);
									if((uchar>='0' && uchar <= '9')
											||(uchar>='a' && uchar <= 'h')
											||(uchar>='A' && uchar <= 'H')){
										continue;
									}else{
										checked = false;
									}
								}
								if(checked){
									sb.append(c);
									continue;
								}
							}
						}
					}
					sb.append("\\\\");
				}else if(c == '\r'){
					sb.append("\\r");
				}else if(c == '\n'){
					sb.append("\\n");
				}else if(c == '\b'){
					sb.append("\\b");
				}else if(c == '\t'){
					sb.append("\\t");
				}else if(c == '\f'){
					sb.append("\\f");
				}else if(c == '/'){
					sb.append("\\/");
				}else{
					sb.append(c);
				}
			}
		}
	}
	
	public static String build(int resultCode,String resultInfo,Throwable t){
		StringBuilder sb = new StringBuilder();
		sb.append("{\"result-code\": ");
		sb.append(resultCode);
		sb.append(",\"result-info\": \"");
		if(resultInfo != null){
			appendJson(sb,resultInfo);
		}
		if(t != null){
			if(resultInfo != null){
				appendJson(sb,"\r\n");
			}
			appendJson(sb,t.getClass().getName());
			appendJson(sb,": ");
			appendJson(sb,t.getMessage());
			StackTraceElement[] trace = t.getStackTrace();
			for(StackTraceElement elem : trace){
				appendJson(sb,"\r\n");
				appendJson(sb,"\t");
				appendJson(sb,"at ");
				appendJson(sb,elem.getClassName());
				appendJson(sb,".");
				appendJson(sb,elem.getMethodName());
				appendJson(sb,"(");
				appendJson(sb,elem.getFileName());
				appendJson(sb,":");
				appendJson(sb,Integer.toString(elem.getLineNumber()));
				appendJson(sb,")");
			}
			Throwable cause = t.getCause();
			while(cause != null){
				appendJson(sb,"\r\n");
				appendJson(sb,"Caused by: ");
				appendJson(sb,t.getClass().getName());
				appendJson(sb,": ");
				appendJson(sb,t.getMessage());
				trace = t.getStackTrace();
				for(StackTraceElement elem : trace){
					appendJson(sb,"\r\n");
					appendJson(sb,"\t");
					appendJson(sb,"at ");
					appendJson(sb,elem.getClassName());
					appendJson(sb,".");
					appendJson(sb,elem.getMethodName());
					appendJson(sb,"(");
					appendJson(sb,elem.getFileName());
					appendJson(sb,":");
					appendJson(sb,Integer.toString(elem.getLineNumber()));
					appendJson(sb,")");
				}
				cause = cause.getCause();
			}
		}
		sb.append("\"}");
		return sb.toString();
	}

	@Override
	public boolean isSuccess(){
		if(resultCode == InterfaceConst.ResultCode.SUCCESS){
			return true;
		}else{
			return false;
		}
	}

	@Override
	public String build() {
		return build(resultCode,resultInfo,t);
	}

}
