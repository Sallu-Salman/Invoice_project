package Methods;

import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class CommonMethods
{
    public Connection createConnection()
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/invoice_db","root","");
            return connection;
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }

    public boolean conjunction(boolean key, StringBuilder query)
    {
        if(key)
        {
            key = !key;
        }
        else
        {
            query.append(" , ");
        }

        return key;
    }

    public static boolean emptyPath(String path)
    {
        return (path == null || path.equals("/"));
    }

    public static boolean paramPath(String path)
    {
        return path.matches("/\\d+/?");
    }

    public static boolean invoiceFunctionPath(String path)
    {
        return path.matches("/\\d+/[a-z]+/?");
    }

    public static boolean invoiceStatusPath(String path)
    {
        return path.matches("/\\d+/status/[a-z]+/?");
    }
    public long parseId(HttpServletRequest request)
    {
        String pathInfo = request.getPathInfo();

        String[] splits = pathInfo.split("/");
        String modelId = splits[1];

        long id = Integer.parseInt(modelId);

        return id;
    }

    public JSONObject getBodyJson(HttpServletRequest request) throws IOException
    {

        BufferedReader reader = request.getReader();

        StringBuffer jb = new StringBuffer();
        String line = null;
        try {
            while ((line = reader.readLine()) != null)
                jb.append(line);
        } catch (Exception e) { }

        return new JSONObject(jb.toString());
    }

    public String refineJson(String jsonString)
    {
        JSONObject jsonObject = new JSONObject(jsonString);

        if(jsonObject.getInt("item_quantity") < 0)
        {
            jsonObject.remove("item_quantity");
        }
        if(jsonObject.getInt("item_cost") < 0)
        {
            jsonObject.remove("item_cost");
        }

        return jsonObject.toString();
    }
}
