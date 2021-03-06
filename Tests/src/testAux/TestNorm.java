package testAux;

import java.util.Arrays;
import java.util.List;



import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import coreVisitors.From;
import helpers.TestHelper;
import ast.Ast.Path;
import ast.ExpCore.*;
import auxiliaryGrammar.Norm;
import auxiliaryGrammar.Program;

public class TestNorm {
  @RunWith(Parameterized.class)
  public static class Test1 {
    @Parameter(0) public String _p1;
    @Parameter(1) public String _p2;
    @Parameter(2) public String[] prog;
    @Parameterized.Parameters
    public static List<Object[]> createData() {
      return Arrays.asList(new Object[][] {
         {"This0.A","This0.A",new String[]{}
       },{"This0.A","This0.A",new String[]{"{C:{}}"}
       },{"This0.A","This0.A",new String[]{"{C:##walkBy}"}
       },{"This1.A","This1.A",new String[]{"{C:{}}","{A:{}}"}
       },{"This1.C","This1.C",new String[]{"{C:{}}","{A:{}}"}
       //I have lost the possibity of doing this test using caching },{"This1.C","This0",new String[]{"{C:##walkBy}","{A:{}}"}
      //I have lost the possibity of doing this test using caching },{"This2.D.C","This0",new String[]{"{D:##walkBy}","{C:##walkBy}","{A:{}}"}
      //I have lost the possibity of doing this test using caching },{"This2.D.C","This1.C",new String[]{"{D:##walkBy}","{C:{}}","{A:{}}"}
       }});}

    @Test
    public void testOk() {
      TestHelper.configureForTest();
      Path pp1=Path.parse(_p1);
      Path pp2=Path.parse(_p2);
      Program p = TestHelper.getProgram(prog);
      Assert.assertEquals(Norm.of(p,pp1),pp2);
    }

    }

}
