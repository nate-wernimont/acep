package edu.iastate.cs.design.asymptotic.datastructures;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import soot.Unit;
import soot.jimple.IfStmt;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.util.Switch;

public class SootLoopStmt implements Switch {

    Unit start = null;
    IfStmt end = null;
    Unit inc = null;
    //ValueBox var = null;
    List<Unit> body = null;
    List<SootLoopStmt> innerLoops = new ArrayList<SootLoopStmt>();
    
    /**
     * 
     */
    private static final long serialVersionUID = -3095785747701274599L;

    
    
    /**
     * Copy constructor.
     * @param src SootLoopStmt to copy.
     */
    public SootLoopStmt(SootLoopStmt src){
        if(src != null){
            //this.var = src.var;
            this.end = (IfStmt)src.end.clone();
            this.start = (Unit)src.start.clone();
            this.inc = (Unit)inc.clone();
            this.body = new ArrayList<Unit>(src.body);
        }
        //TODO: Implement copy constructor if needed.
        throw new UnsupportedOperationException("COPY CONSTRUCTOR FOR VAR BOX NOT FINISHED");
    }
    
    /**
     * Copy the references to the params into the SootLoopStmt state.
     * @param start
     * @param inc
     * @param end
     * @param loopBody
     */
    public SootLoopStmt(Unit start, Unit inc, IfStmt end, List<Unit> loopBody) {
        this.start = start;
        this.end = end;
        this.inc = inc;
        this.body = loopBody;
    }

    
    /**
     * @return the start
     */
    public Unit getStart() {
        return start;
    }

    /**
     * @param start the start to set
     */
    public void setStart(Unit start) {
        this.start = start;
    }

    /**
     * @return the end
     */
    public IfStmt getEnd() {
        return end;
    }

    /**
     * @param end the end to set
     */
    public void setEnd(IfStmt end) {
        this.end = end;
    }

    /**
     * @return the inc
     */
    public Unit getInc() {
        return inc;
    }

    /**
     * @param inc the inc to set
     */
    public void setInc(Unit inc) {
        this.inc = inc;
    }

//    /**
//     * @return the var
//     */
//    public ValueBox getVar() {
//        return var;
//    }
//
//    /**
//     * @param var the var to set
//     */
//    public void setVar(ValueBox var) {
//        this.var = var;
//    }

    /**
     * @return the body
     */
    public List<Unit> getBody() {
        return body;
    }

    /**
     * @param body the body to set
     */
    public void setBody(List<Unit> body) {
        this.body = body;
    }

    @Override 
    public SootLoopStmt clone(){
        return new SootLoopStmt(this);
    }
   
    
//    public void apply(LoopStmtSwitch loopSwitch){
//        loopSwitch.caseSootLoopStmt(this);
//    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }
    
    public void toString(StringBuilder sb){
        sb.append("Start: "); sb.append(start);
        sb.append("\nInc: "); sb.append(inc);
        sb.append("\nEnd: "); sb.append(end);
        
        if(body != null){
            for(Iterator<Unit> iter = body.iterator(); iter.hasNext();){
                sb.append(iter.next());
                if(iter.hasNext()) sb.append(";\n");
            }
        }else{
            sb.append("NO BODY");
        }
        
    }
    
    public Iterator<SootLoopStmt> iterator() {
    	return innerLoops.iterator();
    }
    
    public boolean jumpsOutOfLoop(Unit stmt, ExceptionalUnitGraph eug){
    	List<Unit> stmtSuccs = eug.getSuccsOf(stmt);
    	final int stmtSuccsSize = stmtSuccs.size();
    	for(int i = 0; i < stmtSuccsSize; i++){
    		Unit currSucc = stmtSuccs.get(i);
    		if(!body.contains(currSucc))
    			return true;
    	}
    	return false;
    }

}
