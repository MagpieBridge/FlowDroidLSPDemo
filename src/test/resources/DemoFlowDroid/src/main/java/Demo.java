
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
    String str = req.getParameter("name"); 
    PrintWriter writer = resp.getWriter();
    writer.println(str); /* BAD */ 
  }

}
