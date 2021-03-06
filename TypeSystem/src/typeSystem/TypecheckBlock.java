package typeSystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import ast.Ast;
import ast.Ast.Doc;
import ast.Ast.FreeType;
import ast.Ast.Mdf;
import ast.Ast.NormType;
import ast.Ast.Path;
import ast.Ast.Ph;
import ast.Ast.Position;
import ast.Ast.SignalKind;
import ast.Ast.Type;
import ast.ErrorMessage;
import ast.ExpCore;
import ast.ExpCore.Block;
import ast.ExpCore.Block.Dec;
import ast.ExpCore.Block.On;
import auxiliaryGrammar.Functions;
import auxiliaryGrammar.Norm;
import auxiliaryGrammar.Program;
import coreVisitors.FreeVariables;

public class TypecheckBlock {

  public static Type typecheckBlock(TypeSystem that,Block s) {
    //assert suggested instanceof Ast.FreeType || !((NormType)suggested).getPath().equals(Path.Any());
    //it can happen correctly with signal!

    int splitPoint=splitPoint(s);//0 or -1 for no split
    if(splitPoint>0){return typecheckBlock(that,splitBlock(s,splitPoint));}
    return typeCheckMinimalBlockAdaptCatch(that, s);
  }
  private static Type typeCheckMinimalBlockAdaptCatch(TypeSystem that, Block s) {
    try{return typeCheckMinimalBlock(that,s);}
    catch(ErrorMessage e){
      if(s.getOns().isEmpty()){throw e;}//no catch to adapt
      List<On> k=s.getOns();
      On on=k.get(0);
      if(k.get(0).getKind()!=SignalKind.Return){throw e;}
      if(k.size()!=1){throw e;}
      ExpCore inn=on.getInner();
      if(!(inn instanceof ExpCore.X)){throw e;}
      ExpCore.X x=(ExpCore.X)inn;
      if(!x.getInner().equals(on.getX())){throw e;}
      Type t=on.getT();
      if(!(t instanceof Ast.NormType)){throw e;}
      NormType nt=(NormType)t;
      if(nt.getMdf()!=Mdf.Capsule && nt.getMdf()!=Mdf.Immutable){throw e;}
      ArrayList<On> onsMut = new ArrayList<>();
      onsMut.add(on.withT(nt.withMdf(Mdf.Mutable)));
      Block sMut=s.withOns(onsMut);
      try{return typeCheckMinimalBlock(that,sMut);}
      catch(ErrorMessage e2){
        if(nt.getMdf()!=Mdf.Immutable){throw e;}//ok e not e2
        ArrayList<On> onsRead = new ArrayList<>();
        onsRead.add(on.withT(nt.withMdf(Mdf.Readable)));
        Block sRead=s.withOns(onsRead);
        try{return typeCheckMinimalBlock(that,sRead);}catch(ErrorMessage e3){throw e;}
        }
    }
  }
  private static Type typeCheckMinimalBlock(TypeSystem that, Block s) {
    List<HashMap<String, NormType>> varEnvs=splitVarEnvsForBlock(that.p,that.varEnv,s);
    ThrowEnv throwsEnv2 = TypecheckBlock.catchExtentions(that.p,that.throwEnv,s.getOns());
    ArrayList<NormType> tsExp=new ArrayList<NormType>();
    //ArrayList<Type> ts=new ArrayList<Type>();
    {int i=0;for(Block.Dec di:s.getDecs()){
      i+=1;//correct to start from 1
      NormType expectedi=Functions.forceNormType(that.p,di.getInner(), di.getT());
      expectedi=Functions.toPartial(expectedi);
      tsExp.add(expectedi);
      HashMap<String, NormType> varEnvi = varEnvs.get(i);
      varEnvi=catchRestrictions(varEnvi, s.getOns(),that.sealEnv);
      TypeSystem.typecheckSure(false,that.p,varEnvi,that.sealEnv, throwsEnv2,expectedi,di.getInner());
    }}
    Type res1=checkBlockBody(that,varEnvs.get(0),s);
    //TODO: suspended waiting for new ts that.p.exePlusOk(varEnvs.get(0));
    Type res2=checkCatch(that.p,s.getP(),varEnvs.get(0),that.sealEnv,that.throwEnv,that.suggested,s.getDecs(),s.getOns());
    return searchCommonSupertype(that, s, res1, res2);
  }

  private static Type searchCommonSupertype(TypeSystem that, Block s, Type res1,
      Type res2) {
    assert res1!=null;
    assert res2!=null;
    if(res1 instanceof Ast.FreeType){return res2;}
    if(res2 instanceof Ast.FreeType){return res1;}
    if(res1.equals(res2)){return res1;}
    if(Functions.isSubtype(that.p, (NormType)res1,(NormType)res2)){return res2;}
    if(Functions.isSubtype(that.p, (NormType)res2,(NormType)res1)){return res1;}
    if(that.suggested instanceof Ast.FreeType){
      throw new ErrorMessage.ConfusedResultingTypeForCatchAndBlock(s,res1,res2,s.getP());
    }
    if(
      Functions.isSubtype(that.p, (NormType)res1,(NormType)that.suggested)
      && Functions.isSubtype(that.p, (NormType)res2,(NormType)that.suggested)
      ){return that.suggested;}
    throw new ErrorMessage.ConfusedResultingTypeForCatchAndBlock(s,res1,res2,s.getP());
  }

  private static Type checkBlockBody(TypeSystem that,HashMap<String, NormType> varEnv, ExpCore.Block block) {
    SealEnv newSealEnv=new SealEnv(that.sealEnv);
    HashSet<String>fvs=new HashSet<>();
    for(Dec dec:block.getDecs()){
      fvs.addAll(FreeVariables.of(dec.getInner()));
      }
    newSealEnv.xssK.add(fvs);
    //TODO:was tollerant
    return TypeSystem.typecheckSure(false,that.p,varEnv,newSealEnv,that.throwEnv,that.suggested,block.getInner());
  }
  private static Type checkCatch(Program p,Position pos, HashMap<String, NormType> varEnv2, SealEnv sealEnv2,ThrowEnv throwEnv2, Type newSuggested, List<Dec> decs, List<On> k) {
    if(k.isEmpty()){return new FreeType();}
    varEnv2=new HashMap<String, NormType>(varEnv2);
    for(Dec d:decs){varEnv2.remove(d.getX());}
    HashSet<Type> results=new HashSet<Type>();
    for(On on:k){
      checkCatchSingle(on.getKind(),on.getX(),on,p, varEnv2, sealEnv2, throwEnv2, newSuggested,results);
    }
    if(results.isEmpty()){return new FreeType();}//all free types
    if(results.size()==1){return results.iterator().next();}
    if(newSuggested instanceof Ast.FreeType){
      throw new ErrorMessage.ConfusedResultingTypeForMultiCatch(k,results,pos);
    }
      return newSuggested;
  }

  private static void checkCatchSingle(
      SignalKind kind,String x, On on,
      Program p, HashMap<String, NormType> varEnv2, SealEnv sealEnv2, ThrowEnv throwEnv2, Type newSuggested, HashSet<Type> results) {
    NormType nt=on.getT().match(n->n, hType->Norm.resolve(p, hType));
    try{
      HashMap<String, NormType> varEnvi=new HashMap<String, NormType>(varEnv2);
      varEnvi.put(x,Functions.forceNormType(p,on.getInner(), on.getT()));
      //TODO:was tollerant
      Type ti=TypeSystem.typecheckSure(false,p,varEnvi,sealEnv2,throwEnv2,newSuggested,on.getInner());
      if(!(ti instanceof Ast.FreeType)){results.add(ti);}
    }
    catch(ErrorMessage.InvalidTypeForThrowe err){
      if(!nt.getPath().equals(Path.Any())){throw err;}
      if(kind==SignalKind.Error){throw err;}
      NormType nt1=nt.withPath(Path.Library());
      NormType nt2=nt.withPath(Path.Void());
      if(kind==SignalKind.Return){
        if(nt2.getMdf()==Mdf.Class){nt2=nt2.withMdf(Mdf.Immutable);}
        else {nt2=nt2.withMdf(Mdf.Class);}
      }
      checkCatchAnyButUsing(nt1,kind, x, on.withT(nt1), p, varEnv2, sealEnv2, throwEnv2,
          newSuggested, results, err);
      checkCatchAnyButUsing(nt2,kind, x, on.withT(nt2), p, varEnv2, sealEnv2, throwEnv2,
          newSuggested, results, err);
    }
  }

  private static void checkCatchAnyButUsing(NormType ntAlt,SignalKind kind, String x, On on,
      Program p, HashMap<String, NormType> varEnv2, SealEnv sealEnv2,
      ThrowEnv throwEnv2, Type newSuggested, HashSet<Type> results,
      ErrorMessage.InvalidTypeForThrowe err ) {
    ThrowEnv trAlt=new ThrowEnv();
    if(kind==SignalKind.Exception){
      trAlt.exceptions.add(ntAlt.getPath());
      trAlt.resAddAll(throwEnv2.res());
      }
    else{
      trAlt.resAddAll(ThrowEnv.accResult(p,throwEnv2.res(),Arrays.asList(on)));
      }
    HashMap<String, NormType> varEnvi=new HashMap<String, NormType>(varEnv2);
    varEnvi.put(x,Functions.forceNormType(p,on.getInner(), on.getT()));
    try{ checkCatchSingle(kind,x,on,p,varEnv2,sealEnv2,trAlt,newSuggested,results); }
    catch(ErrorMessage _ignored){throw err;}
  }

  private static Block splitBlock(Block s, int splitPoint) {
    ArrayList<Dec> decs1 = new ArrayList<Dec>(s.getDecs().subList(0, splitPoint));
    ArrayList<Dec> decs2 = new ArrayList<Dec>(s.getDecs().subList(splitPoint,s.getDecs().size()));
    return s.withDecs(decs1).withInner(s.withDecs(decs2));
  }

  private static List<HashMap<String, NormType>> splitVarEnvsForBlock(Program p,HashMap<String, NormType> thatVarEnv,Block s){
    List<ExpCore> es=new ArrayList<ExpCore>();
    ExpCore ek=s.getInner();
    if(!s.getOns().isEmpty()){
      ek=new Block(Doc.empty(),new ArrayList<>(),ek,s.getOns(),s.getP());
    }
    es.add(ek);
    for(Block.Dec dec:s.getDecs()){
      es.add(dec.getInner());
    }
    HashMap<String, NormType> varEnvPrime=new HashMap<String, NormType>();
    for(Block.Dec dec:s.getDecs()){
      varEnvPrime.put(dec.getX(),Functions.forceNormType(p,dec.getInner(), dec.getT()));
    }
    List<HashMap<String,NormType>> varEnvs=new ArrayList<HashMap<String,NormType>>();
    HashMap<String, NormType> varEnvPrimeForEi=Functions.toPh(Functions.complete(varEnvPrime));
    varEnvPrimeForEi.putAll(thatVarEnv);
    //varEnvs.add(varEnvPrimeForEi);
    for( ExpCore e:es){varEnvs.add(TypeSystem.splitVarEnv(e,varEnvPrimeForEi));}
    List<HashMap<String, NormType>> varEnvEis = varEnvs.subList(1, varEnvs.size());
    boolean isClosed=TypecheckBlock.isClosed(varEnvEis,varEnvPrimeForEi);
    HashMap<String, NormType> varEnv0=varEnvs.get(0);
    varEnv0.putAll(Functions.nonComplete(varEnvPrime));
    if(isClosed){//update with full type
      for(String key:varEnv0.keySet()){
        if(varEnvPrime.containsKey(key)){
          varEnv0.put(key, varEnvPrime.get(key));
      }}}
    else{//update with partial type
      for(String key:varEnv0.keySet()){
        if(varEnvPrime.containsKey(key)){
          varEnv0.put(key, Functions.toPartial(varEnvPrime.get(key)));
      }}}
    TypeSystem.checkDisjointCapsule(es, varEnvs);
    assert TypeSystem.noFreeVar(es,varEnvs,varEnvPrimeForEi);
    return varEnvs;
  }

  private static int allUnusedUpTo(Set<String> fv,Block s){
    List<Dec>decs=s.getDecs();
    int size=s.getDecs().size();
    for(int i=0;i<size;i++){
      String x=decs.get(i).getX();
      if(fv.contains(x)){
          return i;
        }
      }
    return size;
  }
  private static int allUnusedAfter(Set<String> fv,Block s){
    List<Dec>decs=s.getDecs();
    int size=s.getDecs().size();
    for(int i=size-1;i>=0;i--){
      String x=decs.get(i).getX();
      if(fv.contains(x)){
          return i+1;
        }
      }
    return 0;
  }

  private static int splitPoint(Block s) {
    if(s.getDecs().size()<=1){return 0;}
   int candidate=s.getDecs().size()-1;
   int minSplit=0;
   Type t0=s.getDecs().get(0).getT();
   Set<String> fv=FreeVariables.ofBlock(s);
   int earlyBreak=allUnusedAfter(fv, s);
   if(earlyBreak==0){return -1;/*all unused*/}
   if(earlyBreak<s.getDecs().size()){return earlyBreak;}
   assert earlyBreak==s.getDecs().size();
   if(t0.equals(NormType.immVoid)){minSplit=allUnusedUpTo(fv,s);}
    while(true){
      if(candidate<=minSplit){return minSplit;}
      int minUse=TypecheckBlock.getMinUse(candidate,s.getDecs());
      if(minUse>=candidate){return candidate;}
      candidate=minUse;
      if(candidate==0){return -1;}
      }
  }

  private static int getMinUse(int candidate, List<Dec> decs) {
    String x=decs.get(candidate).getX();//TODO: is it possible to avoid recursion here?
    int minUse=candidate;
    for(int i=0;i<candidate;i++){
      if(FreeVariables.of(decs.get(i).getInner()).contains(x)){
        minUse=i;break;
      }
    }
    if(candidate==decs.size()-1){return minUse;}
    return Math.min(minUse, getMinUse(candidate+1,decs));
  }

  private static boolean isClosed(List<HashMap<String, NormType>> varEnvs,HashMap<String, NormType>newIntroduced) {
    for(HashMap<String, NormType> ve:varEnvs){
      for(String key:ve.keySet()){
        if(newIntroduced.containsKey(key)){continue;}
        if(ve.get(key).getPh()!=Ph.None){return false;}
      }
    }
    return true;
  }

  private static HashMap<String, NormType> catchRestrictions(HashMap<String, NormType> varEnv, List<On> k, SealEnv sealEnv) {
    if(k.isEmpty()){return varEnv;}
    if(k.get(0).getKind()!=SignalKind.Error){return varEnv;}
    HashMap<String, NormType> result=new HashMap<String, NormType>();
    Set<String> fvk =new HashSet<>();
    for(On on:k){
      Set<String> fri = FreeVariables.of(on.getInner());
      fri.remove(on.getX());
      fvk.addAll(fri);
      }
    for(String x:varEnv.keySet()){
      boolean toProtect=toProtect(x,fvk,sealEnv);
      if(toProtect){
        result.put(x,Functions.sharedAndLentToReadable(varEnv.get(x)));
        }
      else{
        result.put(x,varEnv.get(x));
      }
      }
    return result;
    //how to in  42?
    //VarEnv[with s in ve.keys(), nt in ve.vals() (use[VarEnv::Entry(key:s,val:mutAndlentToReadable(nt)])]
    //VarEnv[with e in ve.entries() (use[VarEnv::Entry(key:e.key(),val:mutAndlentToReadable(e.val()])]
  }

  private static boolean toProtect(String x,Set<String> fv, SealEnv sealEnv) {
    for(HashSet<String> xs:sealEnv.xssK){
      if(!xs.contains(x)){continue;}
      Set<String> intersection = new HashSet<>(xs);
      intersection.retainAll(fv);
      if(intersection.isEmpty()){continue;}
      return true;
    }
    return false;
  }


  private static  ThrowEnv catchExtentions(Program p,ThrowEnv throwEnv, List<On> k) {
  if(k.isEmpty()){return throwEnv;}
  On on0=k.get(0);
  SignalKind kind = on0.getKind();
  boolean allEq=true;
  for(On on:k){ if(on.getKind()!=kind){allEq=false;}}
  assert allEq;//TODO: for now ok, then we will capture a more general exception on need.

  if(kind==SignalKind.Error){return throwEnv;}
  ThrowEnv result=new ThrowEnv();
  if(kind==SignalKind.Exception){
    for(Block.On on:k){
      NormType onT=Functions.forceNormType(p,on.getInner(),on.getT());
      result.exceptions.add(onT.getPath());
      }
    result.exceptions.remove(Path.Any());
    result.exceptions.addAll(throwEnv.exceptions);
    assert result.res().isEmpty();
    result.resAddAll(throwEnv.res());
    return result;
  }
  result.exceptions.addAll(throwEnv.exceptions);
  result.resAddAll(ThrowEnv.accResult(p,throwEnv.res(),k));
  return result;
  }

  static boolean sameMdf(Set<NormType> res) {
    if(res.size()==0){return true;}
    Mdf mdf0=res.iterator().next().getMdf();
    for(NormType nt:res){if(nt.getMdf()!=mdf0){return false;}}
    return true;
  }

}