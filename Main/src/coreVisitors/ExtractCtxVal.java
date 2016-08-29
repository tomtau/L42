package coreVisitors;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import tools.Assertions;
import ast.Ast.Path;
import ast.ErrorMessage;
import ast.ExpCore;
import ast.ExpCore.Block;
import ast.ExpCore.ClassB;
import ast.ExpCore.Loop;
import ast.ExpCore.MCall;
import ast.ExpCore.Signal;
import ast.ExpCore.Using;
import ast.ExpCore.WalkBy;
import ast.ExpCore.X;
import ast.ExpCore._void;
import ast.ExpCore.Block.Dec;
import ast.Redex;
import auxiliaryGrammar.Ctx;
import auxiliaryGrammar.Functions;
import auxiliaryGrammar.Program;

public class ExtractCtxVal implements Visitor<Ctx<Redex>>{
  Program p;
  private ExtractCtxVal(Program p){this.p=p;}
  
  public Ctx<Redex> visit(Path s) {return null;}
  public Ctx<Redex> visit(X s) {return null;}
  public Ctx<Redex> visit(_void s) { return null; }
  public Ctx<Redex> visit(WalkBy s) {throw Assertions.codeNotReachable();}
  
  public Ctx<Redex> visit(ClassB s) {
    return checkRedex(s);
    }

  public Ctx<Redex> visit(Signal s) {
    return lift(s.getInner().accept(this),
        ctx->s.withInner(ctx));
    }
  public Ctx<Redex> visit(Loop s) {
    return checkRedex(s);
    }
    
  public Ctx<Redex> visit(Using s) {
    Ctx<Redex> res=checkRedex(s);
    if(res!=null){return res;}
    for(ExpCore ei:s.getEs()){
      if(IsValue.of(p,ei)){continue;}
      return lift(ei.accept(this),ctx->{
        List<ExpCore> es=new ArrayList<ExpCore>(s.getEs());
        es.set(es.indexOf(ei), ctx);
        return s.withEs(es);
      });
    }
    return lift(s.getInner().accept(this),
        ctx->s.withInner(ctx));
  }
  public Ctx<Redex> visit(MCall s) {
    Ctx<Redex> res=checkRedex(s);
    if(res!=null){return res;}
    
    if(!IsValue.of(p, s.getInner())){
      return lift(s.getInner().accept(this),
        ctx->s.withInner(ctx));}
    for(ExpCore ei:s.getEs()){
      if(IsValue.of(p,ei)){continue;}
      return lift(ei.accept(this),ctx->{
        List<ExpCore> es=new ArrayList<ExpCore>(s.getEs());
        es.set(es.indexOf(ei), ctx);
        return s.withEs(es);
      });
    }
    return null;  
  }
  public Ctx<Redex> visit(Block s) {
    Ctx<Redex> res=checkRedex(s);
    if(res!=null){return res;}
    
    for(int i=0;i<s.getDecs().size();i++){
      int ii=i;//final restrictions
      Dec di=s.getDecs().get(i);
      boolean isDv=new IsValue(p).validDvs(di);
      Redex isGarbage=null;
      if(isDv && di.getInner() instanceof Block){
        Block diB=(Block)di.getInner();
        isGarbage=IsValue.nestedGarbage(diB);
        }
      if(isGarbage!=null){
        List<Block.Dec> ds=new ArrayList<Block.Dec>(s.getDecs());
        ds.set(ii,di.withInner(new WalkBy()));
        return new Ctx<Redex>(s.withDecs(ds),isGarbage);
        }
      if(isDv){continue;}
      //otherwise, i is the first non dv
      return lift(di.getInner().accept(this),ctx->{
        List<Block.Dec> es=new ArrayList<Block.Dec>(s.getDecs());
        es.set(ii,es.get(ii).withInner(ctx));
        return s.withDecs(es);
        });
      }
    if(!s.getOns().isEmpty()){return null;}
    return lift(s.getInner().accept(this),
        ctx->s.withInner(ctx));
    }
  
  private Ctx<Redex> lift(Ctx<Redex> res,Function<ExpCore,ExpCore> f){
    if(res!=null){res.ctx=f.apply(res.ctx);}
    return res;
  }
  private Ctx<Redex> checkRedex(ExpCore s) {
    Redex r=IsRedex.of(p, s);
    if(!(r instanceof Redex.NoRedex)){
      return new Ctx<Redex>(new WalkBy(),r);}
    return null;
  }
  public static Ctx<Redex> of(Program p,ExpCore e){
    //what if check garbage first?
    Ctx<Redex> result=e.accept(new ExtractCtxVal(p));
    if(result!=null){return result;}
    throw new ErrorMessage.CtxExtractImpossible(e,p.getInnerData());
  }
}