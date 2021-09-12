
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Demo {

  public Demo(){}
 
  public static void main(String[] args) throws IOException {
    Demo Demo = new Demo();
    Demo.doGet(null, null);
  }

  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String s1 = req.getParameter("name");// source
    String s2 = doStuff(s1);
    String s3 = doStuff("random");
    PrintWriter writer = resp.getWriter();
    Container container = new Container(s2);
    String txt=container.getTxt();
    writer.println(txt);// sink
    writer.println(s3);
  }

  private String doStuff(String string) {
    return string;
  }
}
