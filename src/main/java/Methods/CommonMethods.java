package Methods;

import models.SendEnhancedRequestBody;
import models.SendEnhancedResponseBody;
import models.SendRequestMessage;
import org.json.JSONException;
import org.json.JSONObject;
import services.Courier;
import services.SendService;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;

public class CommonMethods
{
    public static Connection createConnection()
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

    public static boolean conjunction(boolean key, StringBuilder query)
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

    public static JSONObject readBodyJson(HttpServletRequest request)
    {
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader reader;

        try
        {
            String line;
            reader =  request.getReader();
            while ((line = reader.readLine()) != null)
            {
                stringBuilder.append(line).append('\n');
            }

            reader.close();

            return new JSONObject(stringBuilder.toString());
        }
        catch (IOException e)
        {
            return null;
        }
        catch (JSONException e)
        {
            return null;
        }

    }
    public static boolean emptyPath(String path)
    {
        return (path == null || path.equals("/"));
    }

    public static boolean paramPath(String path)
    {
        return (path != null && path.matches("/\\d+/?"));
    }

    public static boolean invoiceFunctionPath(String path)
    {
        return path.matches("/\\d+/[a-z]+/?");
    }

    public static boolean invoiceStatusPath(String path)
    {
        return path.matches("/\\d+/status/[a-z]+/?");
    }
    public static long parseId(HttpServletRequest request)
    {
        String pathInfo = request.getPathInfo();

        String[] splits = pathInfo.split("/");
        String modelId = splits[1];

        long id = Long.parseLong(modelId);

        return id;
    }

    public static JSONObject getBodyJson(HttpServletRequest request) throws IOException
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

    public static String refineJson(String jsonString)
    {
        JSONObject jsonObject = new JSONObject(jsonString);

        if(jsonObject.getInt("item_quantity") < 0)
        {
            jsonObject.remove("item_quantity");
        }
        if(jsonObject.getFloat("item_cost") < 0)
        {
            jsonObject.remove("item_cost");
        }
        if(jsonObject.getFloat("stock_rate") < 0)
        {
            jsonObject.remove("stock_rate");
        }

        return jsonObject.toString();
    }

    public static void sendEmail(long invoice_id, String customer_name, String customer_email, int total_cost, String invoice_date)
    {
        Courier.init("pk_prod_2HQDMJRGV8M9Z2HQA1ABRBDTMQ3N");

        SendEnhancedRequestBody requestBody = new SendEnhancedRequestBody();
        SendRequestMessage sendRequestMessage = new SendRequestMessage();

        HashMap<String, String> to = new HashMap<String, String>();
        to.put("email", customer_email);
        sendRequestMessage.setTo(to);
        sendRequestMessage.setTemplate("TGJENRVNNK4ZMGH5FW94MPCPM8JK");


        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("invoice_id", invoice_id);
        data.put("invoice_amount", total_cost);
        data.put("invoice_date", invoice_date);
        data.put("customer_name", customer_name);
        sendRequestMessage.setData(data);

        requestBody.setMessage(sendRequestMessage);
        try
        {
            SendEnhancedResponseBody responseBody = new SendService().sendEnhancedMessage(requestBody);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
