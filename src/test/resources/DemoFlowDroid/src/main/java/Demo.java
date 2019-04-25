
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Demo {

  public static void main(String[] args) throws IOException {
  	 Demo Demo =new Demo();
  	 Demo.doGet(null, null);	
  }
  
  
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String s1 = req.getParameter("name");

    String s2 = doStuff(s1);
    String s3 = doStuff("random");
    
 


    PrintWriter writer = resp.getWriter();  
    
    writer.println(s2);                    /* BAD */
    writer.println(s3);
  }

    private String doStuff(String string){
        return string;
    }
}
