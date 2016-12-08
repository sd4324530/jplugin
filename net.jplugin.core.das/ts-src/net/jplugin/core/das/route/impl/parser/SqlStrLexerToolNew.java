package net.jplugin.core.das.route.impl.parser;

import java.util.ArrayList;
import java.util.List;

import net.jplugin.core.das.route.api.TablesplitException;

public class SqlStrLexerToolNew {
	public static final int STATE_STR_CONST = 1;
	public static final int STATE_WORD = 2;
	public static final int STATE_SPACING = 3;
	public static final int STATE_OPERATOR = 4;

	/**
	 * <PRE>
	 * ����: SELECT  ABC  23.23  AND OR _NOT
	 * �������'='  '<'  '>'  '&'  '^' '+'  '-'  '*' '/'  >=  <=  <>
	 * �ֽ����'('  ')' ','
	 * �ַ���������'   ' ��ת���
	 * ע�ͣ� /star.....star/
	 * <pre>
	 * @param sqlStr
	 * @return
	 */
	char[] buffer;
	int idx=0;
	List<String> list=new ArrayList();
	private static boolean debug;
	
	public SqlStrLexerToolNew(String sql){
		this.buffer = sql.toCharArray();
	}
	
	public List<String> parse(){
		while(true){
			//���Կո�
			while (idx<buffer.length && buffer[idx]==' '){
				idx++;
			}
			//ʶ��
			if (idx<buffer.length){
				char c = buffer[idx];
				if (isWordCharStart()) 
					parseWord();
				else if (isCommentStart()) 
					parseComment();
				else if (isOperatorStart()) 
					parseOperator();
				else if (isConstStart()) 
					parseConst();
				else if (isSplit()) 
					parseSplit();
				else 
					throw new TablesplitException("Error sql ,unexpacted char at ["+idx+"] '"+c+"'  SQL:="+new String(buffer));
				idx++;
			}else
				break;
			if (debug){
				if (list.size()>0)
					System.out.println(list.get(list.size()-1));
			}
		}
		return list;
	}
	
	private boolean isWordCharStart() {
		char c = buffer[idx];
		return (c>='0'&& c<='9')|| (c>='A' && c<='Z') ||(c>='a' && c<='z') || (c=='_') || (c=='.');
	}
	private boolean isWordCharEnd() {
		if (idx==buffer.length-1) return true;
		char c = buffer[idx+1];
		return !((c>='0'&& c<='9')|| (c>='A' && c<='Z') ||(c>='a' && c<='z') || (c=='_') || (c=='.'));
	}

	public boolean isOperatorStart(){
		char c = buffer[idx];
		return (c == '=') || (c ==  '<')|| (c==  '>')|| (c==  '&')|| (c==  '^')|| (c==  '+' )|| (c==  '-' )|| (c==  '*')|| (c==  '/');
	}
	
	public boolean isConstStart(){
		return buffer[idx]=='\'';
	}
	public boolean isConstEnd(){
		return (buffer[idx]=='\'') && (buffer[idx-1]!='\\');
	}

	public boolean isSplit(){
		char c = buffer[idx];
		return c==',' || c=='(' || c==')' || c==']' || c=='[' || c== '{' || c== '}' || c==';' || c=='?';
	}
	
	public boolean isCommentStart(){
		if (idx+1>=buffer.length) return false;
		return buffer[idx]=='/' && buffer[idx+1]=='*';
	}
	public boolean isCommentEnd(){
		if (idx-1<0) return false;
		return buffer[idx]=='/' && buffer[idx-1]=='*';
	}

	public void parseWord(){
		int start = idx;
		while(idx<buffer.length){
			if (isWordCharEnd()){
				list.add(new String(buffer,start,idx-start+1));
				return;
			}
			idx++;
		}
		throw new RuntimeException("logic error.");
	}
	
	public void parseOperator(){
		if (idx+1 >=buffer.length) {
			list.add(new String(buffer,idx,1));
			//idx ��������
			return;
		}else{
			char c1 = buffer[idx];
			char c2 = buffer[idx+1];
			if ( (c1=='>'&& c2=='=') || (c1=='<'&& c2=='=') || (c1=='<'&& c2=='>')) {
				list.add(new String(buffer,idx,2));
				idx = idx + 1;
				return;
			}else{
				list.add(new String(buffer,idx,1));
				//idx ��������
				return;
			}
		}
	}
	
	public void parseConst(){
		int start = idx;
		while(++idx<buffer.length){
			if (isConstEnd()){
				list.add(new String(buffer,start,idx-start+1));
				return;
			}
		}
		list.add(new String(buffer,start,buffer.length-idx));
	}
	
	public void parseComment(){
		int start = idx;
		while(++idx<buffer.length){
			if (isCommentEnd()){
				list.add(new String(buffer,start,idx-start+1));
				return;
			}
		}
		list.add(new String(buffer,start,buffer.length-idx));
	}
	
	public void parseSplit(){
		list.add(new String(buffer,idx,1));
	}
	
	

	
	public static void main(String[] args) {
		debug = true;
//		List<String> result = new SqlStrLexerToolNew("  select 'AAA\\''/*CROSSTABLE S*/* from customer a where a.x=??) and a.y='12' and () (a.z<3 or a.z>=5)  ").parse();
//		for (String s:result){
//			System.out.println("- "+ s);
//		}

		List<String> result = new SqlStrLexerToolNew("select  /*spantable  */  count(0) from tb_route0 SQL=select  /*spantable  */  count(0) from tb_route0").parse();
//		for (String s:result){
//			System.out.println("- "+ s);
//		}
		
		
//		List<String> result = new SqlStrLexerToolNew("insert into tb_route0(f1,f2,f3) values(?,?,?)").parse();
//		for (String s:result){
//			System.out.println("- "+ s);
//		}
		
	}
}