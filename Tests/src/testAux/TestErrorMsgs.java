package testAux;

import helpers.TestHelper;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.Test;

import ast.ErrorMessage;
import ast.ErrorMessage.UserLevelError;
import static ast.ErrorMessage.UserLevelError.Kind.*;
import facade.ErrorFormatter;
import facade.L42;

public class TestErrorMsgs {
  public void testCode(String fileName,UserLevelError.Kind expectedKind,int expectedLine,String ...src){
    String code=TestHelper.multiLine(src);
    try{L42.runSlow(fileName,code);}
    catch(ErrorMessage msg){
      UserLevelError err = ErrorFormatter.formatError(msg);
      Assert.assertEquals(err.getKind(), expectedKind);
      Assert.assertEquals(err.getP().getFile(),fileName);
      Assert.assertEquals(err.getP().getLine1(),expectedLine);
      }
  }
  //@Test(singleThreaded=false)
  public void test1() throws IOException{
   assert false;
    //testCode("a/b/c",TypeError,10,
        
        
        
        
      //  );
  }  
  
  
}
