package ast;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Function;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Wither;
import ast.Expression;
import ast.Ast.*;
import ast.ExpCore.ClassB;
import ast.ExpCore.MCall;
import ast.ExpCore.ClassB.Member;
import ast.ExpCore.ClassB.MethodImplemented;
import ast.ExpCore.ClassB.MethodWithType;
import ast.ExpCore.ClassB.NestedClass;
import ast.Util.CachedStage;
import auxiliaryGrammar.Program;

public interface ExpCore {
  <T> T accept(coreVisitors.Visitor<T> v);
  default String toS(){return sugarVisitors.ToFormattedText.of(this);}
  @Value @EqualsAndHashCode(exclude = "p") @ToString(exclude = "p") @Wither public static class MCall implements ExpCore,HasPos, WithInner<MCall>{
    ExpCore inner;
    MethodSelector s;
    Doc doc;
    List<ExpCore> es;
    Position p;
    public MCall withEsi(int i,ExpCore ei){
      List<ExpCore> es2=new ArrayList<>(es);
      es2.set(i,ei);
      return this.withEs(es2);
      }
    @Override public <T> T accept(coreVisitors.Visitor<T> v) {
      return v.visit(this);
    }
  }

  @Value public static class X implements ExpCore, Ast.Atom {
    String inner;
    public String toString() {
      return this.inner;
    }
    @Override public <T> T accept(coreVisitors.Visitor<T> v) {
      return v.visit(this);
    }
  }

  @Value @EqualsAndHashCode(exclude = "p") @ToString(exclude = "p") @Wither public static class Block implements ExpCore,HasPos,WithInner<Block> {
    Doc doc;
    List<Dec> decs;
    ExpCore inner;
    List<On> ons;
    Position p;
    public ExpCore getE(){return inner;}
    public Block withE(ExpCore e){return this.withInner(e);}
    public Block withDeci(int i,Dec di){
      List<Dec> decs2=new ArrayList<>(decs);
      decs2.set(i,di);
      return this.withDecs(decs2);
      }
    public Block withDeci(int i,On oi){
    List<On> ons2=new ArrayList<>(ons);
    ons2.set(i,oi);
    return this.withOns(ons2);
    }  
    @Override public <T> T accept(coreVisitors.Visitor<T> v) {
      return v.visit(this);
    }
    @Value @Wither public static class Dec implements WithInner<Dec>{
      Type t;
      String x;
      ExpCore inner;
    }
    public List<String> domDecs() {
      List<String> dom = new java.util.ArrayList<String>();
      for (Dec d : this.decs) {
        dom.add(d.x);
      }
      return dom;
    }

    @Value @Wither @EqualsAndHashCode(exclude = "p") @ToString(exclude = "p") public static class On implements HasPos, WithInner<On>{
      SignalKind kind;
      String x;
      Type t;
      ExpCore inner;
      Position p;
      public ExpCore getE(){return inner;}
      public On withE(ExpCore e){return this.withInner(e);}

    }
  }

  @Value @Wither @EqualsAndHashCode(exclude = {"stage","p","phase","uniqueId"}) public static class ClassB implements ExpCore, Ast.Atom,HasPos {
    public ClassB(Doc doc1, Doc doc2, boolean isInterface, List<Path> supertypes, List<Member> ms,Position p,ast.Util.CachedStage stage, Phase phase, String uniqueId) {
      this.doc1 = doc1;
      this.doc2 = doc2;
      this.isInterface = isInterface;
      this.supertypes = supertypes;
      this.ms = ms;
      this.stage=stage;
      this.p=p;
      this.phase=phase;
      this.uniqueId=uniqueId;
      assert stage!=null;
      assert isConsistent();
      }//lombock fails me here :-(
    Doc doc1;
    Doc doc2;
    boolean isInterface;
    List<Ast.Path> supertypes;
    List<Member> ms;
    Position p;
    ast.Util.CachedStage stage;
    Phase phase;
    String uniqueId;
    public String toString() {return sugarVisitors.ToFormattedText.of(this);}
    public boolean isConsistent() { return _Aux.isConsistent(this);}
    public ClassB withMember(Member m) {return _Aux.withMember(this, m);}
    public ClassB onClassNavigateToPathAndDo(List<String>cs,Function<ClassB,ClassB>op){return _Aux.onClassNavigateToPathAndDo(this, cs, op);}
    public ClassB onNestedNavigateToPathAndDo(List<String>cs,Function<ClassB.NestedClass,Optional<ClassB.NestedClass>>op){return _Aux.onNestedNavigateToPathAndDo(this, cs, op);}
    public ExpCore.ClassB.NestedClass getNested(List<String>cs){return _Aux.getNested(this, cs);}
    public List<ExpCore.ClassB.NestedClass> getNestedList(List<String>cs){return _Aux.getNestedList(this, cs);}
    public ExpCore.ClassB getClassB(List<String>cs){return _Aux.getClassB(this, cs);}
    
    public static ExpCore.ClassB docClass(Doc d){return new ClassB(d,Doc.empty(),false,Collections.emptyList(),Collections.emptyList(),Position.noInfo,verifiedStage.copyMostStableInfo(),Phase.Typed,"");}
    //TODO: remove when chachd stage is out
    private static final CachedStage verifiedStage=new CachedStage();
    static{verifiedStage.setVerified(true);}
    
    public static ExpCore.ClassB membersClass(List<Member> ms,Position pos){return new ClassB(Doc.empty(),Doc.empty(),false,Collections.emptyList(),ms,pos,new CachedStage(),Phase.None,"");}    
  
    @Override public <T> T accept(coreVisitors.Visitor<T> v) {return v.visit(this);}
    public static enum Phase{None,Norm,Typed;
    public Phase acc(Phase other){
      if(this==other){return this;}
      if(other==Typed){return this;}
      if(this==Typed){return other;}
      return None;
      }}
    public interface Member extends HasPos, WithInner<Member> {
      <T> T match(Function<NestedClass, T> nc, Function<MethodImplemented, T> mi, Function<MethodWithType, T> mt);
      }
    @Value @Wither @EqualsAndHashCode(exclude = "p") @ToString(exclude = "p")public static class NestedClass implements Member {
      @NonNull Doc doc;
      @NonNull String name;
      @NonNull ExpCore inner;
      Position p;
      public ExpCore getE(){return inner;}
      public NestedClass withE(ExpCore e) {return this.withInner(e);}
      public <T> T match(Function<NestedClass, T> nc, Function<MethodImplemented, T> mi, Function<MethodWithType, T> mt) {
        return nc.apply(this);
        }
      }
    @Value @Wither @EqualsAndHashCode(exclude = {"p"/*,"mt"*/}) @ToString(exclude = {"p"/*,"mt"*/})public static class MethodImplemented implements Member {
      @NonNull Doc doc;
      @NonNull MethodSelector s;
      @NonNull ExpCore inner;
      Position p;
      public ExpCore getE(){return inner;}
      public MethodImplemented withE(ExpCore e) {return this.withInner(e);}
      public <T> T match(Function<NestedClass, T> nc, Function<MethodImplemented, T> mi, Function<MethodWithType, T> mt) {
        return mi.apply(this);
        }
      }
    @Value @Wither @EqualsAndHashCode(exclude = "p") @ToString(exclude = "p")public static class MethodWithType implements Member {
      @NonNull Doc doc;
      @NonNull MethodSelector ms;
      @NonNull MethodType mt;
      @NonNull Optional<ExpCore> _inner;
      Position p;
      public ExpCore getInner(){return _inner.get();}//and boom if there is not
      public MethodWithType withInner(ExpCore e) {return this.with_inner(Optional.of(e));}

      public <T> T match(Function<NestedClass, T> nc, Function<MethodImplemented, T> mi, Function<MethodWithType, T> mt) {
        return mt.apply(this);
        }
      }
    }

  @Value public static class _void implements ExpCore, Ast.Atom {
    @Override public <T> T accept(coreVisitors.Visitor<T> v) {
      return v.visit(this);
      }
    }
  @Value public static class WalkBy implements ExpCore {
    @Override public <T> T accept(coreVisitors.Visitor<T> v) {
      return v.visit(this);
      }
    }

  @Value @Wither public static class Using implements ExpCore, WithInner<Using> {
    Path path;
    MethodSelector s;
    Doc doc;
    List<ExpCore> es;
    ExpCore inner;
    public Using withEsi(int i,ExpCore ei){
    List<ExpCore> es2=new ArrayList<>(es);
    es2.set(i,ei);
    return this.withEs(es2);
    }
    @Override public <T> T accept(coreVisitors.Visitor<T> v) {
      return v.visit(this);
    }
  }

  @Value @Wither public static class Signal implements ExpCore, WithInner<Signal>{
    SignalKind kind;
    ExpCore inner;
    
    @Override public <T> T accept(coreVisitors.Visitor<T> v) {
      return v.visit(this);
    }
  }

  @Value @Wither public static class Loop implements ExpCore, WithInner<Loop> {
    ExpCore inner;
    
    @Override public <T> T accept(coreVisitors.Visitor<T> v) {
      return v.visit(this);
    }
  }
  interface WithInner<T>{
    ExpCore getInner(); T withInner(ExpCore e);
    }
  
  }
class _Aux{
  static ClassB wrapCast(ExpCore e){
    try{return (ClassB)e;}
    catch(ClassCastException cce){
      throw new ErrorMessage.ProgramExtractOnMetaExpression(null,null);
      }
    }
  static void checkIndex(int index){
    if (index==-1){
      throw new ErrorMessage.PathNonExistant(null,null,null);
      }
    }
  static ClassB onNestedNavigateToPathAndDo(ClassB cb,List<String>cs,Function<NestedClass,Optional<NestedClass>>op){
    assert !cs.isEmpty();
    assert cb!=null;
    List<Member> newMs=new ArrayList<>(cb.getMs());
    String nName=cs.get(0);
    int index=getIndex(newMs, nName);
    checkIndex(index);
    NestedClass nc=(NestedClass)newMs.get(index);
    if(cs.size()>1){
      nc=nc.withInner(onNestedNavigateToPathAndDo(wrapCast(nc.getInner()),cs.subList(1,cs.size()),op));
      newMs.set(index, nc);
      return cb.withMs(newMs);
      }
    assert cs.size()==1;
    Optional<NestedClass> optNc = op.apply(nc);
    if(optNc.isPresent()){
      newMs.set(index,optNc.get());
      }
    else{newMs.remove(index);}
    return cb.withMs(newMs);
    }

  static ClassB onClassNavigateToPathAndDo(ClassB cb,List<String>cs,Function<ClassB,ClassB>op){
    if(cs.isEmpty()){return op.apply(cb);}
    List<Member> newMs=new ArrayList<>(cb.getMs());
    String nName=cs.get(0);
    int index=getIndex(newMs, nName);
    checkIndex(index);
    NestedClass nc=(NestedClass)newMs.get(index);
    if(cs.size()>1){
      nc=nc.withInner(onClassNavigateToPathAndDo(wrapCast(nc.getInner()),cs.subList(1,cs.size()),op));
      newMs.set(index, nc);
      return cb.withMs(newMs);
      }
    assert cs.size()==1;
    ClassB newCb = op.apply(wrapCast(nc.getInner()));
    newMs.set(index, nc.withInner(newCb));
    return cb.withMs(newMs);
    }  
  
  static int getIndex(List<ExpCore.ClassB.Member> map, ast.Ast.MethodSelector elem){
    int i=-1;for(ExpCore.ClassB.Member m: map){i++;
    if(m.match(nc->false,mi->mi.getS().equals(elem) ,mt->mt.getMs().equals(elem))){return i;}
      }
    return -1;  
    }
  
  static int getIndex(List<ExpCore.ClassB.Member> map, String elem){
    int i=-1;for(ExpCore.ClassB.Member m: map){i++;
      if(m.match(nc->nc.getName().equals(elem), mi->false, mt->false)){return i;}
      }
    return -1;
    }
  
  static int getIndex(List<ExpCore.ClassB.Member> map, ExpCore.ClassB.Member elem){
    return elem.match(nc->getIndex(map,nc.getName()), mi->getIndex(map,mi.getS()),mt->getIndex(map,mt.getMs()));
    }
  
  static ExpCore.ClassB.NestedClass getNested(ExpCore.ClassB cb, List<String>cs){
    assert !cs.isEmpty();
    String nName=cs.get(0);
    int index=getIndex(cb.getMs(), nName);
    checkIndex(index);
    NestedClass nc=(NestedClass)cb.getMs().get(index);
    if(cs.size()==1){return nc;}
    return getNested(wrapCast(nc.getInner()),cs.subList(1, cs.size()));
    }
  
  static List<ExpCore.ClassB.NestedClass> getNestedList(ExpCore.ClassB cb, List<String>cs){
    assert !cs.isEmpty();
    List<ExpCore.ClassB.NestedClass> result=new ArrayList<>();
    getNestedList(cb,cs,result);
    return result;
    }

  static void getNestedList(ExpCore.ClassB cb, List<String>cs,List<ExpCore.ClassB.NestedClass> result){
    String nName=cs.get(0);
    int index=getIndex(cb.getMs(), nName);
    checkIndex(index);
    NestedClass nc=(NestedClass)cb.getMs().get(index);
    result.add(nc);
    if(cs.size()!=1){
      getNestedList(wrapCast(nc.getInner()),cs.subList(1, cs.size()),result);
      }
    }
  
  static ExpCore.ClassB getClassB(ExpCore.ClassB cb, List<String>cs){
    if(cs.isEmpty()){return cb;}
    return wrapCast(getNested(cb,cs).getInner());
    }

  static ClassB withMember(ClassB cb,Member m) {
    assert cb.isConsistent();
    List<Member> newMs = new java.util.ArrayList<>(cb.getMs());
    int index=_Aux.getIndex(newMs,m);
    if(index==-1){newMs.add(m);}
    else {newMs.set(index, m);}
    ClassB result = cb.withMs(newMs);
    return result;
    }

  static boolean isConsistent(ClassB cb) {
    int countWalkBy = 0;
    HashSet<String> keys = new HashSet<String>();
    for (Member m : cb.getMs()) {
      if (m instanceof MethodWithType) {
        MethodWithType mwt = (MethodWithType) m;
        String key = mwt.getMs().toString();
        assert !keys.contains(key);
        keys.add(key);
        assert mwt.getMt().getTDocs().size() == mwt.getMt().getTs().size();
        }
      if (m instanceof NestedClass) {
        NestedClass nc = (NestedClass) m;
        String key = nc.getName();
        assert !keys.contains(key);
        keys.add(key);
        if (nc.getInner() instanceof ExpCore.WalkBy) {
          countWalkBy += 1;
          }
        }
      if (m instanceof MethodImplemented) {
        MethodImplemented mi = (MethodImplemented) m;
        String key = mi.getS().toString();
        assert !keys.contains(key);
        keys.add(key);
        }
      }
    assert  (cb.getPhase()==ast.ExpCore.ClassB.Phase.None && cb.getUniqueId().isEmpty())
         || ((cb.getPhase()!=ast.ExpCore.ClassB.Phase.None && !cb.getUniqueId().isEmpty()) );
    assert countWalkBy <= 1 : cb;
    return true;
    }
  }