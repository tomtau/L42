package platformSpecific.fakeInternet;


import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import platformSpecific.javaTranslation.Resources;
import com.sun.org.apache.xml.internal.security.utils.resolver.ResourceResolverException;
import facade.Configuration;
import facade.L42;
import facade.Parser;
import sugarVisitors.Desugar;
import sugarVisitors.InjectionOnCore;
import tools.Assertions;
import tools.Map;
import ast.Ast;
import ast.ErrorMessage;
import ast.ExpCore;
import ast.Expression;
import ast.Ast.Doc;
import ast.Ast.MethodSelector;
import ast.Ast.NormType;
import ast.Ast.SignalKind;
import ast.Ast.Stage;
import ast.ExpCore.ClassB;
import ast.ExpCore.Using;
import auxiliaryGrammar.EncodingHelper;
import auxiliaryGrammar.Program;
import auxiliaryGrammar.WellFormedness;

public interface OnLineCode {
  public Expression.ClassB code();
  public static Expression.ClassB getCode(String url){
    assert url.startsWith("reuse");
    url=url.substring("reuse".length());
    url=url.trim();
    if(url.startsWith("L42.is/")){return OnLineCodeHelper.getL42Code(url.substring("L42.is/".length()));}
    throw Assertions.codeNotReachable();
  }
  static PluginWithPart _isPluginWithPart(Doc doc){
    String url=doc._getParameterForPlugin();
    String part=doc._getParameterForPluginPart();
    if (url==null || part==null){return null;}
    url=url.trim();
    part=part.trim();
    return new PluginWithPart(url,part);
    }
  static PluginType _isWellKnownPlugin(Doc doc){
    String url=doc._getParameterForPlugin();
    if (url==null){return null;}
    url=url.trim();
    if(url.startsWith("L42.is/connected/")){
      PluginType plugin= OnLineCodeHelper.getWellKnownPluginType(url.substring("L42.is/connected/".length()));
      return plugin;
      }
    return null;
    }
  public static PluginType plugin(Program p,ExpCore.Using u){
    Doc d=p.extractCb(u.getPath()).getDoc1();
    PluginType pt =_isPluginWithPart(d);
    if(pt!=null){return pt;}
    pt = _isWellKnownPlugin(d);
    if(pt!=null){return pt;}
    throw Assertions.codeNotReachable("Other plugings not supported yet");
  }
  public static ExpCore pluginAction(Program p,ExpCore.Using u){
    try{
      Object o= plugin(p,u).execute(p,u);
      return EncodingHelper.wrapResource(o);
      }
    catch(Resources.Error err){return EncodingHelper.wrapResource(err);}
  }
  public static List<NormType> pluginType(Program p,ExpCore.Using u){
    return plugin(p,u).typeOf(p,u);
  }
  //Unuseful variant, since program not availabe at desugaring time
  public static List<NormType> pluginType(Program p,Expression.Using u){
  MethodSelector ms = new MethodSelector(u.getName(),u.getPs().getXs());
  ExpCore.Using uCore=new ExpCore.Using(u.getPath(),ms,Doc.empty(),Collections.emptyList(),new ExpCore._void()); 
  return plugin(p,uCore).typeOf(p,uCore);
}
}

class OnLineCodeHelper{
  static PluginType getWellKnownPluginType(String url){
    String className="is.L42.connected."+url+".Plugin";
    try {
      Class<?> clazz=Class.forName(className);
      return (PluginType)clazz.newInstance();
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
      throw new ErrorMessage.InvalidURL("L42.is/connected/"+url,null);
    }
    //is.L42.connected.withSystem.Plugin;
    /*if(url.startsWith("withSandboxingOver/")){
        return new?? withSandboxingOver.Plugin(getPluginType(url.substring("withSandboxingOver/".length())));
    }
    switch (url){//TODO: NO NONO
      case "withAlu": return new Plugin();
      case "withHtml": return new Plugin();
      case "withIntrospection": return new Plugin();
      default: throw new ErrorMessage.InvalidURL("L42.is/connected/"+url);
      }*/
    }
  static Expression.ClassB getL42Code(String url){
    try{
      Expression data=load(url);//can be classB or classBReuse
      data=Desugar.of(data);//TODO: can be removed later, but now allows librares to be real source code
      //    Configuration.reduction.of((ClassB) data.accept(new InjectionOnCore()));
      return (Expression.ClassB)data;
      }
    catch( org.antlr.v4.runtime.misc.ParseCancellationException pce){
      System.err.println("Url is: "+url);
      throw pce;
      }
    }
  private static Expression load(String name) {
    //URL res = OnLineCode.class.getResource(name+".L42");
    //Path res = L42.path.resolve(name+".L42");
    Path res = Paths.get("localhost",name+".L42");
    assert res!=null:name;
    String s= L42.pathToString(res);
    Expression e=Parser.parse(res.toString(),s);
    assert WellFormedness.checkAll(e);
    return e;
  }

}