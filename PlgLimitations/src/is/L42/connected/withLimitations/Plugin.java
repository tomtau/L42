package is.L42.connected.withLimitations;

import static auxiliaryGrammar.EncodingHelper.ensureExtractInt32;
import ast.ErrorMessage;
import platformSpecific.fakeInternet.ActionType;
import platformSpecific.fakeInternet.PluginType;
import platformSpecific.javaTranslation.Resources;


public class Plugin implements PluginType{
  long originalSystemTime=-1;
  private boolean isFirstCall(){return originalSystemTime==-1;}
  @ActionType({ActionType.Type.Void,ActionType.Type.Library})
  public Resources.Void MexecutionTime£lessThan(Object _i1){
    int waitTime=ensureExtractInt32(_i1);
    long systemTime=System.currentTimeMillis();
    if(isFirstCall()){
      this.originalSystemTime=systemTime-1;//a little bit less patient
      throw new ErrorMessage.PluginActionUndefined(waitTime);
      }     
    if(originalSystemTime+waitTime<=systemTime){
      //we wait long enough
      return Resources.Void.instance;
      }
    throw Resources.notAct;
    }
  }
  
 
