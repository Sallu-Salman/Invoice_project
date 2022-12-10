package Methods;

import javax.servlet.http.HttpServletRequest;
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

    public long parseId(HttpServletRequest request)
    {
        String pathInfo = request.getPathInfo();

        if(pathInfo == null || pathInfo.equals("/"))
        {
            return -1;
        }

        String[] splits = pathInfo.split("/");
        String modelId = splits[1];

        long id = Integer.parseInt(modelId);

        return id;
    }
}
